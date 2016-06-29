package com.xutao.mergeApp.fields;


/**
 * 全局变量设置
 * @author xutao
 *
 */
public class GlobalSettings {
	/** 日志文件夹路径列表 **/
	//private static final String PROJECT_DIR = "G:/Experiment/flow/";
	private static final String PROJECT_DIR = "/home/FLOW/";
	private static final String SRC_BASE_DIR = PROJECT_DIR + "src/";
	public static final String SRC_ROUTER_5_PATH = SRC_BASE_DIR + "R5/"; // ROUTER_5分钟文件夹路径
	public static final String SRC_CUSTOMER_5_PATH = SRC_BASE_DIR + "C5/"; // CUSTOMER_1分钟文件夹路径
	public static final String SRC_GROUP_5_PATH = SRC_BASE_DIR + "G5/"; // RG_CUSTOMER_5分钟文件夹路径
	
	/** 合并统计文件夹列表 **/
	private static final String STAT_BASE_DIR = PROJECT_DIR + "stat/"; 
	public static final String STAT_ROUTER_HOUR = STAT_BASE_DIR + "router_hour/"; 
	public static final String STAT_ROUTER_DAY = STAT_BASE_DIR + "router_day/"; 
	public static final String STAT_GROUP_HOUR = STAT_BASE_DIR + "group_hour/"; 
	public static final String STAT_GROUP_DAY = STAT_BASE_DIR + "group_day/"; 
	public static final String STAT_CUSTOMER_HOUR = STAT_BASE_DIR + "customer_hour/";
	public static final String STAT_CUSTOMER_DAY = STAT_BASE_DIR + "customer_day/";
	
	/** 备份文件夹列表 **/
	private static final String BAK_BASE_DIR = PROJECT_DIR + "bak/";
	public static final String BAK_ROUTER_SRC = BAK_BASE_DIR + "router_src/";
	public static final String BAK_ROUTER_STAT = BAK_BASE_DIR + "router_stat/";
	public static final String BAK_CUSTOMER_SRC = BAK_BASE_DIR + "customer_src/";
	public static final String BAK_CUSTOMER_STAT = BAK_BASE_DIR + "customer_stat/";
	public static final String BAK_GROUP_SRC = BAK_BASE_DIR + "group_src/";
	public static final String BAK_GROUP_STAT = BAK_BASE_DIR + "group_stat/";
	
	
	/** 线程相关参数 **/
	public static final int BATCH_SIZE = 1000; //多个表都统计top1000
	public static final int THREAD_POOL_SIZE = 10;
	public static final int SLEEP_TIME_MIN = 3000; // 3s
	public static final int SLEEP_TIME_RT = 1000;  // 1s
	
	public static final int FLAG_MTOH = 0;
	public static final int FLAG_HTOD = 1;
	
	/** 配置相关参数 **/
	public static final String ID_CONF_PATH = "/var/nflow/nc_device_list2/"; //ip-id映射
	//public static final String ID_CONF_PATH = "D:/"; //ip-id映射
}
