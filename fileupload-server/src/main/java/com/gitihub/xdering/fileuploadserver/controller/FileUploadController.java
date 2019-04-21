package com.gitihub.xdering.fileuploadserver.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.DateUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gitihub.xdering.common.utils.IPUtils;
import com.gitihub.xdering.common.utils.StringSubstitutorUtils;
import com.gitihub.xdering.common.utils.thread.NamedThreadFactory;
import com.gitihub.xdering.fileuploadserver.FileUploadTask;
import com.gitihub.xdering.fileuploadserver.RichResponseEntity;

@RestController
public class FileUploadController {
	
	private static int BUFFER_SIZE = 4096;
	
	private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);
	
	@Value("${file.upload.directory}")
	private String uploadDirectory;
	
	@Value("${file.upload.dispose.enabled}")
	private String disposeEnabled;
	
	@Value("${file.upload.finish.suffix}")
	private String finishSuffix;
	
	@Value("${file.upload.dispose.url}")
	private String disposeIp;
	
	@Value("${file.upload.dispose.port}")
	private String disposePort;
	
	@Value("${file.upload.dispose.commandPath}")
	private String disposeCommandPath;
	
	@Value("${file.upload.overwrite}")
	private String fileOverwrite;
	
	@Autowired
	private Environment env;
	
	private static final int uploadMaxThreadSize = 10;
	
	private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(uploadMaxThreadSize, new NamedThreadFactory("FileUpload-Dispose"));
	
	private static final String ARRAY_SEPARATOR = ";";
	@PostMapping("/upload")
	@ResponseBody
	public RichResponseEntity upload(HttpServletRequest request, @RequestParam("file") List<MultipartFile> multipartFiles, @RequestParam("md5")List<String> md5s, @RequestParam("tranSeq")String tranSeq) {
		if (multipartFiles.size() == 0) {
			return RichResponseEntity.error("000001", "文件不能为空！");
		}
		StringBuilder requestLog = new StringBuilder();
		requestLog.append("流水号 " + tranSeq).append(" ");
		for (int i = 0; i < multipartFiles.size(); i++) {
			MultipartFile file = multipartFiles.get(i);
			if (file.isEmpty()) {
				return RichResponseEntity.error("000002", "文件不能为空！");
			}
			requestLog.append("文件名 " + file.getOriginalFilename()).append(" 大小 ").append(file.getSize()).append(" ");
		}
		String ip = IPUtils.getIpAddr(request);
		requestLog.append("IP ").append(ip);
		log.info(requestLog.toString());
		List<String> strFileNames = new ArrayList<String>();
		String systemFullName = null;
		for (int i = 0; i < multipartFiles.size(); i++) {
			MultipartFile file = multipartFiles.get(i);
	        String fileName = file.getOriginalFilename();
	        if (systemFullName == null) {
	        	int index = fileName.lastIndexOf("-");
	        	if (index == -1) {
	        		log.error("文件 " + fileName + " 命名不符合规范！");
	        		return RichResponseEntity.error("000003", "文件命名不符合规范！");
	        	}
	        	systemFullName = fileName.substring(0, index);
	        }
	        //int size = (int) file.getSize();
	       	String fileFullName = uploadDirectory + File.separator + fileName;
	       	String date = DateUtils.formatDate(new Date(),"yyyyMMdd");
	       	
	       	Map<String,Object> parameter = new HashMap<String,Object>();
	       	parameter.put("date", date);
	       	parameter.put("systemFullName", systemFullName);
	       	
	       	fileFullName = StringSubstitutorUtils.replace(fileFullName, parameter, "${", "}", true);
	       	strFileNames.add(fileFullName);
	       	File dest = new File(fileFullName);
	        if(!dest.getParentFile().exists()){ //判断文件父目录是否存在
	            dest.getParentFile().mkdir();
	        }
	        
	        if (dest.exists()) {
	        	log.info("流水号 "+ tranSeq + " 文件 " + fileFullName +  " 已经存在,将执行覆盖操作！" );
	        	dest.delete();
	        }
	        try {
	            file.transferTo(dest); //保存文件
	            log.info("流水号 " + tranSeq + " 保存文件[" + fileFullName + "]成功");
	        } catch (IllegalStateException e) {
	        	log.error("流水号 "+ tranSeq+" 保存文件失败："+e.getMessage(),e);
	            return RichResponseEntity.error("000002", e.getMessage());
	        } catch (IOException e) {
	        	log.error("流水号 "+ tranSeq+" 保存文件失败："+e.getMessage(),e);
	            return RichResponseEntity.error("000003", e.getMessage());
	        }
		}
		if (StringUtils.isNotEmpty(disposeEnabled)) {
			String[] disposeEnableds = disposeEnabled.split(ARRAY_SEPARATOR);
			String[] disposeIps = disposeIp.split(ARRAY_SEPARATOR);
			String[] disposePorts = disposePort.split(ARRAY_SEPARATOR);
			String[] disposeCommandPaths = disposeCommandPath.split(ARRAY_SEPARATOR);
			
			if (StringUtils.isNotEmpty(systemFullName)) {
				for (int i = 0; i < disposeEnableds.length; i++) {
					String tempDisposeEnabled = disposeEnableds[i];
					// 判断是否需要发送到其他服务器
			        if (tempDisposeEnabled != null && tempDisposeEnabled.equals("1")) {
			    		FileUploadTask task = new FileUploadTask(strFileNames, scheduledExecutorService, disposeIps[i], disposePorts[i], disposeCommandPaths[i]);
			    		long delay = Long.valueOf(env.getProperty("file.upload.dispose.delay." + systemFullName, "60"));
			    		log.info("新建计划任务  FileUploadTask 将延迟 " + delay + "s 执行");
			    		scheduledExecutorService.schedule(task, delay, TimeUnit.SECONDS);
			        }
				}
			} else {
				log.info("系统编号为空，不执行上传任务");
			}
		}
		return RichResponseEntity.ok();
	}
	
	@GetMapping("/download")
	public void download(HttpServletRequest request, HttpServletResponse response) {
		InputStream in = null;
		OutputStream out = null;
		try {
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/octet-stream");
			
			String fileName = request.getHeader("fileName");
			File file = new File(fileName);
			response.setContentLength((int)file.length());
			response.setHeader("Accept-Ranges", "bytes");
			int readLength = 0;
			
			in = new BufferedInputStream(new FileInputStream(file),BUFFER_SIZE);
			out = new BufferedOutputStream(response.getOutputStream());
			byte[] buffer = new byte[BUFFER_SIZE];
			while ((readLength = in.read(buffer)) > 0) {
				byte[] bytes = new byte[readLength];
				System.arraycopy(buffer, 0, bytes, 0, readLength);
				out.write(bytes);
			}
			out.flush();
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}
}
