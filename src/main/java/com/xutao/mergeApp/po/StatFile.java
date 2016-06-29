package com.xutao.mergeApp.po;

import org.apache.log4j.Logger;

import com.xutao.mergeApp.utils.DateHelper;

/**
 * 统计文件类
 * 
 * @author xutao
 *
 */
public class StatFile {
	private long timeStamp; // 文件的下一跳时间戳
	private String fileType; // 统计文件类型：router、customer、group
	private String filePath; // 文件的绝对路径
	private String nameOnly; // 文件名
	private String prefix; // 文件名前缀
	private String dirPath; // 文件夹路径

	private Logger logger = Logger.getLogger(StatFile.class);
	
	public StatFile(String dir, String filePath) {
		this.dirPath = dir;
		this.filePath = filePath;
		this.nameOnly = filePath.substring(filePath.lastIndexOf('/') + 1);
		this.prefix = nameOnly.substring(0, nameOnly.lastIndexOf('_') + 1);
		//R5预留,要不要处理
		//String[] str = nameOnly.split("_");
		//String nickName = str[str.length - 3]
	}

	public String constructDstFilePath(String newDirPath) {
		StringBuilder sb = new StringBuilder();
		sb.append(newDirPath);
		sb.append(this.getPrefix());
		String dateStr = DateHelper.stampToString(this.getTimeStamp());
		sb.append(dateStr);
		sb.append(".txt");
		
		logger.info("prefix = " + prefix + "; dateString = " + dateStr + "; nameOnly = " + nameOnly); 
		
		return sb.toString();
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getNameOnly() {
		return nameOnly;
	}

	public void setNameOnly(String nameOnly) {
		this.nameOnly = nameOnly;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getDirPath() {
		return dirPath;
	}

	public void setDirPath(String dirPath) {
		this.dirPath = dirPath;
	}

	@Override
	public String toString() {
		return "StatFile [filePath = " + filePath + "]";
	}

	
}
