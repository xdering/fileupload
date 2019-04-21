package com.github.xdering.fileuploadclient;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApacheHCUploader implements Uploader {

	private HttpClient client = null;
	private static final Logger log = LoggerFactory.getLogger(ApacheHCUploader.class);
	
	public ApacheHCUploader() {
		try {
			this.client = createClient();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void upload(Part part) {

		String partName = part.getName();
		Map<String, ContentBody> params = new HashMap<String, ContentBody>();
		params.put(Config.keyFile, new ByteArrayBody(part.getContent(), partName));
		post(params);
		log.debug(partName + " uploaded.");
	}

	@Override
	public void done(String fileName, long partCount) {

		Map<String, ContentBody> params = new HashMap<String, ContentBody>();
		params.put(Config.keyFileName, new StringBody(fileName, ContentType.TEXT_PLAIN));
		params.put(Config.keyPartCount, new StringBody(String.valueOf(partCount), ContentType.TEXT_PLAIN));
		post(params);
		log.debug(fileName + " notification is done.");
	}

	private void post(Map<String, ContentBody> params) {
		HttpPost post = new HttpPost(Config.url);
		MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
		for (Entry<String, ContentBody> e : params.entrySet()) {
			multipartEntityBuilder.addPart(e.getKey(), e.getValue());
		}
		post.setEntity(multipartEntityBuilder.build());
		//RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
		//		.setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectionRequestTimeout).build();
		//httppost.setConfig(defaultRequestConfig);
		try {
			HttpResponse response = client.execute(post);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				throw new RuntimeException("Upload failed.");
			}
		} catch (Exception e) {
			post.abort();
			throw new RuntimeException(e);
		} finally {
			post.releaseConnection();
		}
	}

	/**
	 * The timeout should be adjusted by network condition.
	 * @return
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 */
	private static HttpClient createClient() throws KeyManagementException, NoSuchAlgorithmException {

		//采用绕过验证的方式处理https请求
        SSLContext sslcontext = createIgnoreVerifySSL();
        
		// 设置协议http和https对应的处理socket链接工厂的对象
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)
	            .register("https", new SSLConnectionSocketFactory(sslcontext))
	            .build();
		
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		connManager.setMaxTotal(Config.maxUpload);
		
        //创建自定义的httpclient对象
        CloseableHttpClient client = HttpClients.custom().setConnectionManager(connManager).build();

		return client;
	}
	
	/**
     * 绕过验证
     *     
     * @return
     * @throws NoSuchAlgorithmException 
     * @throws KeyManagementException 
     */
    public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance("SSLv3");

        // 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) {
            }

            @Override
            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        sc.init(null, new TrustManager[] { trustManager }, null);
        return sc;
    }
}
