package com.github.xdering.fileupload;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;


public class ClientMultipartFormPost {
	private static final String CLASSPATH_URL_PREFIX = "classpath:";
	
	private static final Logger log = LoggerFactory.getLogger(ClientMultipartFormPost.class);
	
	private static String ip = "";
	private static String port = "";
	private static int retryTimes = 3;
	private static String token = "";
	
	//private static int socketTimeout = 5000;
	//private static int connectTimeout = 5000;
	//private static int connectionRequestTimeout = 5000;
	
	private static String usage = "usage： java -jar uploader.jar -f D:/0417-MISDATA-20190409-tar.z D:/0417-MISDATA-20190409.flag -u www.url.com -p 80";
	public static void main(String[] args) throws Exception {
		
		String conf = System.getProperty("fileupload.conf", "classpath:fileupload.properties");
        PropertiesConfiguration config = new PropertiesConfiguration();
        if (conf.startsWith(CLASSPATH_URL_PREFIX)) {
            conf = StringUtils.substringAfter(conf, CLASSPATH_URL_PREFIX);
            config.load(ClientMultipartFormPost.class.getClassLoader().getResourceAsStream(conf));
        } else {
            config.load(new FileInputStream(conf));
        }
        
        ip = ObjectUtils.toString(config.getProperty("fileupload.ip"));
        port = ObjectUtils.toString(config.getProperty("fileupload.port"));
        token = ObjectUtils.toString(config.getProperty("fileupload.token"));
        
		if (args.length <= 1) {
			log.info(usage);
			System.exit(1);
		}
		
		List<String> files = new ArrayList<String>();
		boolean filestart = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-f")) {
				filestart = true;
				continue;
			}
			
			
			if (args[i].startsWith("-")) {
				filestart = false;
			} else {
				if (filestart) {
					files.add(args[i]);
				}
			}
			
			if (args[i].equals("-u")) {
				ip = args[i+1];
				i++;
			}
			if (args[i].equals("-p")) {
				port = args[i+1];
				i++;
			}
		}
		
		if (StringUtils.isEmpty(ip)) {
			log.error("未指定远程地址");
			log.info(usage);
			System.exit(1);
		}
		
		if (StringUtils.isEmpty(port)) {
			log.error("未指定远程端口");
			log.info(usage);
			System.exit(1);
		}
		if (files.size() == 0) {
			log.error("未指定上传文件");
			log.info(usage);
			System.exit(1);
		}
		String uuid = UUID.randomUUID().toString().replace("-", "");
		String strFileList = "";
		boolean uploadState = true;
		for (int i = 0; i < retryTimes; i++) {
			strFileList = "";
			//RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
			//		.setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectionRequestTimeout).build();
			CloseableHttpClient httpclient = HttpClients.createDefault();
			try {
					HttpPost httppost = new HttpPost("http://" + ip + ":" + port + "/upload");
					//httppost.setConfig(defaultRequestConfig);
					StringBody tranSeq = new StringBody(uuid, ContentType.TEXT_PLAIN);
					StringBody tokenPart = new StringBody(token, ContentType.TEXT_PLAIN);
					MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()
							//.addPart("file", bin)
							//.addPart("md5", md5)
							.addPart("tranSeq", tranSeq)
							.addPart("token", tokenPart);
					for (String filename : files) {
						File file = new File(filename);
						FileInputStream is = new FileInputStream(file);
						FileBody bin = new FileBodyCounter(file);
						String md5Hex = DigestUtils.md5Hex(is);
						StringBody md5 = new StringBody(md5Hex, ContentType.TEXT_PLAIN);
						multipartEntityBuilder.addPart("file", bin);
						multipartEntityBuilder.addPart("md5", md5);
						strFileList += filename + ";";
					}
					HttpEntity reqEntity = multipartEntityBuilder.build();
					httppost.setEntity(reqEntity);
					log.info("流水号[" + uuid + "],执行请求：" + httppost.getRequestLine() + ",文件名称：" + strFileList);
					CloseableHttpResponse response = httpclient.execute(httppost);
					try {
						HttpEntity resEntity = response.getEntity();
						if (resEntity != null) {
							String data = EntityUtils.toString(resEntity,Charset.forName("UTF-8"));
							log.info("流水号["+ uuid + "],返回状态码[" + response.getStatusLine().getStatusCode()+"],返回报文: " + data);
							//获取code，如果code=0，则跳出循环
							EntityUtils.consume(resEntity);
							JSONObject object = JSONObject.parseObject(data);
							Object code = object.get("code");
							Object msg = object.get("message");
							if (code != null && code.equals("0")) {
								break;
							} else if (i == retryTimes - 1) {
								uploadState = false;
								log.error("流水号[" + uuid + "], 文件上传失败：" + msg + ", 重试次数：" + retryTimes);
							}
						}
					} finally {
						response.close();
					}
					
			} catch (Exception e) {
				log.error("流水号[" + uuid + "],文件上传失败：" + e.getMessage());
				log.error(e.getMessage(),e);
			} finally {
				httpclient.close();
			}
		}
		if (uploadState==false) {
			log.error("流水号[" + uuid + "],文件[" + strFileList + "]上传失败, 程序退出！");
			System.exit(1);
		} else {
			System.exit(0);
		}
	}
}
