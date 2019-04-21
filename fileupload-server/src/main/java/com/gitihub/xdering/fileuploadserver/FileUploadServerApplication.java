package com.gitihub.xdering.fileuploadserver;

import javax.servlet.MultipartConfigElement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
@EnableAutoConfiguration
@SpringBootApplication
@ComponentScan(basePackageClasses = {FileUploadServerApplication.class})
public class FileUploadServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(FileUploadServerApplication.class, args);
	}
	
	@Bean  
    public MultipartConfigElement multipartConfigElement() {  
        MultipartConfigFactory factory = new MultipartConfigFactory();  
        //单个文件最大  
        factory.setMaxFileSize(DataSize.ofMegabytes(4096));
        /// 设置总上传数据总大小  
        factory.setMaxRequestSize(DataSize.ofGigabytes(4));
        return factory.createMultipartConfig();  
    }
}
