package com.xutao.mergeApp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.xutao.mergeApp.fields.GlobalSettings;
import com.xutao.mergeApp.tasks.DirMonitorTask;

/**
 * 合并程序入口点
 * @author xutao
 *
 */
public class MergeAppEntry {
	private Logger logger = Logger.getLogger(MergeAppEntry.class);
	
	public MergeAppEntry(){
		ExecutorService executor = Executors.newFixedThreadPool(10);

		executor.submit(new DirMonitorTask(GlobalSettings.STAT_ROUTER_HOUR));
		executor.submit(new DirMonitorTask(GlobalSettings.STAT_ROUTER_DAY));
		executor.submit(new DirMonitorTask(GlobalSettings.STAT_CUSTOMER_HOUR));
		executor.submit(new DirMonitorTask(GlobalSettings.STAT_CUSTOMER_DAY));
		executor.submit(new DirMonitorTask(GlobalSettings.STAT_GROUP_HOUR));
		executor.submit(new DirMonitorTask(GlobalSettings.STAT_GROUP_DAY));
		
		executor.shutdown();
		logger.info("Merge App Start.....");
	}
	
	public static void main(String[] args) {
		new MergeAppEntry();
	}
}
