package com.xutao.mergeApp.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.xutao.mergeApp.po.StatFile;
import com.xutao.mergeApp.utils.DateHelper;

/**
 * 文件夹监测线程，监测.ok，累积到合并条件，调用合并线程进行合并：
 * 
 * @author xutao
 *
 */
public class DirMonitorTask implements Runnable {

	private Logger logger = Logger.getLogger(DirMonitorTask.class);
	private Map<Long, List<StatFile>> map = new HashMap<Long, List<StatFile>>();

	private String fileType = ""; // 文件夹文件类型
	private String timeInterval = ""; // 文件夹时间类型

	private String dirPath = ""; // 监测的文件夹路径
	private long maxTimeStamp = 0; // 当前最大时间戳
	private int timeSpan = 0; // 该文件夹对应的时间间隔
	private String padding = ""; // 时间字符串添0

	// e.g. /home/flow/stat/router_hour/
	public DirMonitorTask(String dirPath) {
		this.dirPath = dirPath;
		initLocalVars();
		loadHistoryFiles();//在程序重启时，文件的.ok都不存在了，因此需要为它们生成.ok
	}

	/**
	 * 程序重启时为文件夹中的txt生成.ok
	 */
	private void loadHistoryFiles() {
		File rootDir = new File(dirPath);
		File[] files = rootDir.listFiles();

		if (files == null) {
			logger.error("文件夹监控异常");
		}

		for (int i = 0; i < files.length; i++) {
			String filePath = files[i].getAbsolutePath(); // 文件的绝对路径, 此时该txt文件应该没有.ok
			if(filePath.endsWith(".txt")){ //对于txt
				File okFile = new File(filePath+".ok"); 
				if(!okFile.exists()) { // 如果.ok不存在，就生成.ok
					try {
						okFile.createNewFile();
					} catch (IOException e) {
						logger.error("ok文件生成出错");
					}
				}
			}
		}
	}
	
	private void initLocalVars() {
		String[] strs = dirPath.split("/");
		String dirType = strs[strs.length - 1]; // e.g. router_hour
		String[] tmp = dirType.split("_");
		this.fileType = tmp[0]; // e.g. router
		this.timeInterval = tmp[1]; // e.g. hour

		if (timeInterval.equals("hour")) {
			this.timeSpan = 10;
			this.padding = "0000";
		}
		if (timeInterval.equals("day")) {
			this.timeSpan = 8;
			this.padding = "000000";
		}
	}

	public void run() {
		while (true) {
			File rootDir = new File(dirPath);
			File[] files = rootDir.listFiles();

			if (files == null) {
				logger.error("文件夹监控异常");
			}

			for (int i = 0; i < files.length; i++) {
				String filePath = files[i].getAbsolutePath(); // 文件的绝对路径
				if (filePath.endsWith(".ok")) {
					String realFilePath = filePath.substring(0,
							filePath.length() - 3);
					
					// 获取当前的细粒度时间戳
					String oriTime = realFilePath.substring(realFilePath.lastIndexOf("_") + 1,
							realFilePath.lastIndexOf("_") + 1 + this.timeSpan);
					long timestamp = DateHelper.getOriTimeStamp(oriTime + this.padding); // 处理后的时间戳
					this.maxTimeStamp = Math.max(this.maxTimeStamp, timestamp); // 更新当前最大时间戳

					logger.info("max timestamp = " + maxTimeStamp);
					StatFile file = new StatFile(this.dirPath, realFilePath);
					file.setFileType(this.fileType);
					file.setTimeStamp(timestamp);

					if (map.containsKey(timestamp)) { // 包含进位后的时间戳
						map.get(timestamp).add(file);
					} else {
						List<StatFile> list = new ArrayList<StatFile>();
						list.add(file);
						map.put(timestamp, list);
					}
					new File(filePath).delete(); // 删除.ok避免重复加入文件map
				}
			}

			/*for(Long s : map.keySet()) {
				logger.info("timestamp : " + s);
				logger.info(map.get(s));
			}*/
			
			
			if (!map.isEmpty()) {
				List<List<StatFile>> waitMergeLists = new ArrayList<List<StatFile>>(); // 如果有积压，会组合成多个文件列表，一次性传入
				List<Long> removeKey = new ArrayList<Long>();
				for (Long keyTime : map.keySet()) {
					if (keyTime < this.maxTimeStamp) { // 已经进入下一小时或者下一天
						List<StatFile> fin = map.get(keyTime);
						waitMergeLists.add(fin);
						removeKey.add(keyTime);
					}
				}
				
				logger.info(waitMergeLists);
				
				if (this.timeInterval.equals("hour")) {
					Merge5MinTask task = new Merge5MinTask(waitMergeLists);
					task.start();
				} else if (this.timeInterval.equals("day")) {
					MergeHourTask task = new MergeHourTask(waitMergeLists);
					task.start();
				}

				for (int i = 0; i < removeKey.size(); i++) { // 从文件map中移除
					map.remove(removeKey.get(i));
					//System.out.println("[fileMap Info]" + map.size());
				}
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
	}
}
