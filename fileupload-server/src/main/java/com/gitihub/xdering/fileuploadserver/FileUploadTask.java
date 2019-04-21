package com.gitihub.xdering.fileuploadserver;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitihub.xdering.common.utils.CommandExecutor;

public class FileUploadTask implements Runnable {
	
	
	private static final Logger log = LoggerFactory.getLogger(FileUploadTask.class);
	
	private static final int MAX_RETRY_TIMES = 10;
	private static final int RETRY_DELAY_IN_MILLISECONDS = 5000;
	private List<String> files;
	private final ScheduledExecutorService scheduledExecutorService;
	
	//上传文件服务器的 ip 地址
	private String ip;
	//上传文件服务器的 port 地址
	private String port;
	//用于上传文件的命令的路径
	private String commandPath;
	
	//重试次数
	private volatile int retryTimes;
	
	public FileUploadTask(List<String> files,ScheduledExecutorService scheduledExecutorService, String ip, String port, String commandPath) {
	        this.files = files;
	        this.scheduledExecutorService = scheduledExecutorService;
	        this.retryTimes = 0;
	        this.ip = ip;
	        this.port = port;
	        this.commandPath = commandPath;
	        this.retryTimes = 0;
	}
	
	@Override
    public void run() {
		log.info("开始执行任务 FileUploadTask ");
        if (uploadFiles(files)) {
            log.info("文件上传成功!");
        } else {
            scheduledExecutorService.schedule(this, RETRY_DELAY_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        }
    }
	
	public boolean uploadFiles(List<String> files) {
		if (retryTimes >= MAX_RETRY_TIMES) {
			log.info("同步文件超过重试次数，程序退出！");
			return true;
		}
		int result = -1;
		try {
			String osName = StringUtils.substringBefore(System.getProperty("os.name"), " ");
			StringBuilder strFiles = new StringBuilder();
			for (String file:files) {
				strFiles.append(file).append(" ");
			}
			log.info("开始上传文件 " + strFiles);
			StringBuilder strcmd = new StringBuilder();
			boolean windows = false;
			if (osName.equals("Windows")) {
				windows = true;
				strcmd.append("cmd /c start /b ");
			}
			strcmd.append(commandPath).append(" -f ").append(strFiles.toString()).append(" -u ").append(ip).append(" -p ").append(port);
			log.info("执行命令：" + strcmd.toString());
			if (windows) {
				result = CommandExecutor.executeBat(strcmd.toString());
			} else {
				result = CommandExecutor.execute(strcmd.toString(), null);
			}
			log.info("命令返回结果：" + result);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			retryTimes++;
		}
		return result ==0 ? true:false;
	}
	
	
}
