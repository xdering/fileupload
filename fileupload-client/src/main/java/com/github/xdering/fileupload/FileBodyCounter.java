package com.github.xdering.fileupload;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;

import org.apache.http.entity.mime.content.FileBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBodyCounter extends FileBody {
	private static final Logger log = LoggerFactory.getLogger(ClientMultipartFormPost.class);
	
	private static NumberFormat numberFormat = NumberFormat.getPercentInstance();
	
	private volatile long total;
	
	private volatile boolean showProgress = false;
	
    public FileBodyCounter(File file) {
		super(file);
		total = file.length();
		numberFormat.setMinimumFractionDigits(0);
	}

	private volatile long byteCount;
	
	private String progress = null;
	
    public long getBytesWritten() {
        return byteCount;
    }

    public void writeTo(OutputStream out) throws IOException {
        super.writeTo(new FilterOutputStream(out) {
            // Other write() methods omitted for brevity. Implement for better performance
            public void write(int b) throws IOException {
                byteCount++;
                if (showProgress) {
	                String newProgress = numberFormat.format((float)byteCount/total);// format.format(byteCount/total);
	                if (progress == null || !newProgress.equalsIgnoreCase(progress) ) {
	                	progress = newProgress;
		                new Thread(new Runnable(){
							@Override
							public void run() {
									log.info("�ļ��ϴ����ȣ�" + newProgress);
							}
		                }).start();
	                }
                }
                super.write(b);
            }
        });
    }
    
    
}