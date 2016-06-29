package com.xutao.mergeApp.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.xutao.mergeApp.fields.GlobalSettings;
import com.xutao.mergeApp.po.Record;
import com.xutao.mergeApp.po.StatFile;
import com.xutao.mergeApp.utils.DateHelper;
import com.xutao.mergeApp.utils.FileProcesser;
import com.xutao.mergeApp.utils.OscarDao;
import com.xutao.mergeApp.utils.SqlLoader;

/**
 * 将一小时的文件合并为1天
 * 
 * @author xutao
 *
 */
public class MergeHourTask extends Thread{
	private Logger logger = Logger.getLogger(MergeHourTask.class);
	
	// 存放tableName和每一个tableName维护的hashmap的大表
	private Map<String, Map<String, Record>> map = new HashMap<String, Map<String, Record>>();
	private List<List<StatFile>> waitMergeLists = new ArrayList<List<StatFile>>();

	public MergeHourTask(List<List<StatFile>> list) {
		this.waitMergeLists = new ArrayList<List<StatFile>>(list);
	}

	@Override
	public void run() {
		for(int i = 0; i < waitMergeLists.size(); i++) {
			List<StatFile> list = waitMergeLists.get(i);
			map = FileProcesser.mergeFileList(list, GlobalSettings.FLAG_HTOD); // 合并到map中
	
			StatFile f = list.get(0);
			StringBuilder mergePath = new StringBuilder();
			if (f.getFileType().equals("router")) {
				mergePath.append(GlobalSettings.BAK_ROUTER_STAT);
			} else if (f.getFileType().equals("customer")) {
				mergePath.append(GlobalSettings.BAK_CUSTOMER_STAT);
			} else if (f.getFileType().equals("group")) {
				mergePath.append(GlobalSettings.BAK_GROUP_STAT);
			} else {
				// 什么也不做
			}
			mergePath.append(f.getPrefix());
			String dateStr = DateHelper.stampToString(f.getTimeStamp());
			mergePath.append(dateStr).append(".txt");
			logger.info("router合并文件路径 = " + mergePath.toString());
			
			FileProcesser.dumpRecords(mergePath.toString(), this.map, GlobalSettings.FLAG_HTOD); // 将合并完的map写出到mergePath中，并生成.ok
			
			//FileProcesser.printMap(this.map);
			
			// 将map中的根据表名分割好的数据入库，相当于插入合并文件，减少了IO
			FileProcesser.insertMapToDataBase(this.map);
	
			// 将list中的文件移动到相应的bak文件夹下
			if (f.getFileType().equals("router")) {
				FileProcesser.moveFileListForDay(list, GlobalSettings.BAK_ROUTER_STAT);
			} else if (f.getFileType().equals("customer")) {
				FileProcesser.moveFileListForDay(list, GlobalSettings.BAK_CUSTOMER_STAT);
			} else if (f.getFileType().equals("group")) {
				FileProcesser.moveFileListForDay(list, GlobalSettings.BAK_GROUP_STAT);
			} else {
				// 什么也不做
			}
		}
	}
}
