package com.gitihub.xdering.common.utils;

import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CommandExecutor {
	
	private static final Log log = LogFactory.getLog(CommandExecutor.class);
	
	private static final String PLACEHOLDER_PREFIX = "${";
    private static final String PLACEHOLDER_SUFFIX = "}";
    
	public static int executeBat(String command) {
		int result = -1;
		Runtime runtime = Runtime.getRuntime();
		Process ps = null;
		try {
			ps = runtime.exec(command);
			ps.waitFor();
			result = ps.exitValue();
			ps.destroy();
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return result;
	}
	
	public static int execute(String name, Map<String,Object> params) throws Exception {
        String command = StringSubstitutorUtils.replace(name, params, PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, false);
        String[] commands = command.split(" ");
        String commandFile = commands[0];
        int result = -1;
        File file = new File(commandFile);
        if (!file.exists()) {
            throw new Exception("脚本文件不存在，请检查脚本路径：" + commandFile);
        } else {
            try {
                //String[] command = new String[] { commandFile };
                ProcessBuilder builder = new ProcessBuilder(commands);
                builder.directory(new File(file.getParent()));
                builder.redirectErrorStream(true);
                Process process = builder.start();
                log.info("正执行脚本: " + command);
                process.waitFor();
                result = process.exitValue();
                if (result == 0) {
                    log.info("脚本执行完成，执行返回状态：" + result);
                } else {
                    process.destroy();
                    throw new Exception("脚本执行异常，执行返回状态：" + result);
                }
                process.destroy();
            } catch (Exception e) {
                log.error(e.getMessage(),e);
                throw e;
            }
        }
        return result;
    }

}
