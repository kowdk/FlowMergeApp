package com.xutao.mergeApp.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.xutao.mergeApp.fields.GlobalSettings;
import com.xutao.mergeApp.po.Record;
import com.xutao.mergeApp.po.StatFile;
import com.xutao.mergeApp.utils.FileProcesser;

/**
 * 将五分钟的文件合并为1小时
 * 
 * @author xutao
 *
 */
public class Merge5MinTask extends Thread{
	
	private Logger logger = Logger.getLogger(Merge5MinTask.class);
	
	// 存放tableName和每一个tableName维护的hashmap的大表
	private Map<String, Map<String, Record>> map = new HashMap<String, Map<String, Record>>();
	private List<List<StatFile>> waitMergeLists = new ArrayList<List<StatFile>>();

	public Merge5MinTask(List<List<StatFile>> list) {
		this.waitMergeLists = new ArrayList<List<StatFile>>(list);
	}

	@Override
	public void run() {
		for(int i = 0; i < waitMergeLists.size(); i++) {
			List<StatFile> list = waitMergeLists.get(i);
			logger.info("processing statFile list = " + list.toString());
			
			map = FileProcesser.mergeFileList(list, GlobalSettings.FLAG_MTOH); // 将文件合并到map中
			
			StatFile f = list.get(0);
			String newDir = f.getDirPath().replace("hour", "day");
			String mergePath = f.constructDstFilePath(newDir); // 生成合并文件路径
			logger.info("merged file path = " + mergePath);
			
			FileProcesser.dumpRecords(mergePath, this.map, GlobalSettings.FLAG_MTOH); // 将合并完的map写出到mergePath中，并生成.ok
			
			//FileProcesser.printMap(this.map);
			
			//将map中的根据表名分割好的数据入库，相当于插入合并文件，减少了IO
			FileProcesser.insertMapToDataBase(map);
			
			// 将list中的文件移动到相应的bak文件夹下
			if(f.getFileType().equals("router")){
				FileProcesser.moveFileListForHour(list, GlobalSettings.BAK_ROUTER_SRC);
			} else if(f.getFileType().equals("customer")) {
				FileProcesser.moveFileListForHour(list, GlobalSettings.BAK_CUSTOMER_SRC);
			} else if(f.getFileType().equals("group")) {
				FileProcesser.moveFileListForHour(list, GlobalSettings.BAK_GROUP_SRC);
			} else {
				//什么也不做
			}
		}
	}
}
