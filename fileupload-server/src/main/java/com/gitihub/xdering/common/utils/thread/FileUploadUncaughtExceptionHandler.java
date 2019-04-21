package com.gitihub.xdering.common.utils.thread;

import org.slf4j.Logger;

public class FileUploadUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

	private Logger log;

	public FileUploadUncaughtExceptionHandler(Logger logger) {
		this.log = logger;
	}

	public void uncaughtException(Thread t, Throwable e) {
		log.error("uncaught exception", e);
	}
}
