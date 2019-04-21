package com.gitihub.xdering.fileuploadserver;

public class RichResponseEntity {
	private String code;
	private String message;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public static RichResponseEntity ok(){
       RichResponseEntity richResponseEntity = new RichResponseEntity();
       richResponseEntity.message = "上传成功";
       richResponseEntity.code = "0";
       return richResponseEntity;
    }
	
	public static RichResponseEntity error(String code, String msg){
       RichResponseEntity richResponseEntity = new RichResponseEntity();
       richResponseEntity.message = msg;
       richResponseEntity.code = code;
       return richResponseEntity;
     }
	public static RichResponseEntity error(String msg){
       RichResponseEntity richResponseEntity = new RichResponseEntity();
       richResponseEntity.message = msg;
       richResponseEntity.code = "000001";
       return richResponseEntity;
     }
}
