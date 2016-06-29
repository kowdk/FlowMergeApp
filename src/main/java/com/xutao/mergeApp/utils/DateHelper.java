package com.xutao.mergeApp.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

public class DateHelper {
	private Logger logger = Logger.getLogger(DateHelper.class);
	
	/**
	 * 根据路径filePath中的变量生成timeStamp
	 * @param filePath
	 * @return
	 */
	public static long getOriTimeStampFromFile(String filePath) {
		
		// eg. 10.157.192.62_R5_0_20160610235500.txt
		String nameOnly = filePath.substring(filePath.lastIndexOf('/') + 1);
		String[] fields = nameOnly.split("_");
		long stamp = 0L;
		Date date = null;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		try {
			date = sdf.parse(fields[fields.length - 1]);// e.g. 20160610235500.txt
			stamp = date.getTime() / 1000L;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return stamp;
	}

	public static long getOriTimeStamp(String str) {
		long stamp = 0L;
		Date date = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		try {
			date = sdf.parse(str);// e.g. 20160610235500.txt
			stamp = date.getTime() / 1000L;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stamp;
	}
	
	public final static String stampToString(long stamp) {
		String dateString = null;
		
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		dateString = df.format(new Date(stamp * 1000L));
		
		return dateString;
	}
	
	public static void main(String[] args) {
		String oriTime1 = "20160611000000";
		String oriTime2 = "20160611235555";
		
		int x = 86400;
		int sup = 28800;
		System.out.println((getOriTimeStamp(oriTime1) + sup) / x * x);
		System.out.println((getOriTimeStamp(oriTime2) + sup) / x * x);
		
		//System.out.println(stampToString(1466697600));
	}
}
