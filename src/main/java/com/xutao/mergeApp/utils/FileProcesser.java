package com.xutao.mergeApp.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.xutao.mergeApp.fields.GlobalSettings;
import com.xutao.mergeApp.po.Record;
import com.xutao.mergeApp.po.StatFile;

/**
 * 文件处理工具类
 * 
 * @author xutao
 *
 */
public class FileProcesser {

	private static Logger logger = Logger.getLogger(FileProcesser.class);
	
	private static SqlLoader sqlLoader = new SqlLoader();
	private static KeyConfLoader keyLoader = new KeyConfLoader();
	private static IDConfLoader idLoader = new IDConfLoader(); 
	
	
	/**
	 * 将Hash落地成文件
	 * 
	 * @param fileName
	 * @param map
	 *            String代表tableName，Map中的String代表signature，Record代表记录
	 */
	public static void dumpRecords(String dstFile,
			Map<String, Map<String, Record>> map, int flag) {
		BufferedWriter wr = null;
		try {
			wr = new BufferedWriter(new FileWriter(dstFile));

			// 对于每一张表
			for (String tableName : map.keySet()) {
				Map<String, Record> oneTableMap = map.get(tableName); // 单表对应的map
				for (Entry<String, Record> entry : oneTableMap.entrySet()) { // 单表map中的每一条记录
					Record record = entry.getValue();
					StringBuilder sb = new StringBuilder();
					sb.append(tableName).append("\t");
					sb.append(record.getTimestamp()).append("\t");
					List<Long> tmp = record.getKeyFields();
					for (int i = 0; i < tmp.size(); i++) { // fields不包含表名和时间戳
						sb.append(String.valueOf(tmp.get(i))).append("\t");
					}
					tmp = record.getStatFields();
					for (int i = 0; i < tmp.size() - 1; i++) { // fields不包含表名和时间戳
						sb.append(String.valueOf(tmp.get(i))).append("\t");
					}
					sb.append(tmp.get(tmp.size() - 1));
					wr.write(sb.toString(), 0, sb.toString().length());
					wr.newLine();
				}
			}
		} catch (IOException e) {
			logger.error(e);
		} finally {
			try {
				wr.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}

		if(flag == GlobalSettings.FLAG_MTOH) { // 五分钟合并为小时，才需要生成.ok
			logger.info("五分钟合并为一小时的文件.ok的路径 = " + dstFile);
			File file = new File(dstFile + ".ok");
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 将map中的元素入库
	 * @param map
	 */
	public static void insertMapToDataBase(Map<String, Map<String, Record>> map) {
    	for (String tableName : map.keySet()) {
    		//logger.info("tableName = " + tableName);
			Map<String, Record> oneTableMap = map.get(tableName); // 单表对应的map，构建一个map，调用批量入库接口
			List<Record> list = new ArrayList<Record>();
			for (Entry<String, Record> entry : oneTableMap.entrySet()) { // 单表map中的每一条记录
				list.add(entry.getValue());
			}
			
			// 找到表名对应的SQL准备语句
			if(sqlLoader == null) {
				sqlLoader = new SqlLoader();
			}
			
			String suffix = tableName.substring(tableName.lastIndexOf("_")+1, tableName.length());
			String prefix = tableName.substring(0, tableName.lastIndexOf("_"));
			
			String insertSql = sqlLoader.searchSql(prefix).replace("XXX", suffix);
			if(insertSql == null) {
				logger.error("insertSql has not been found...");
			}
			
			logger.info("list size = " + list.size() + "; insert sql = " + insertSql);
			if(list.size() <= GlobalSettings.BATCH_SIZE) { // record的数量小于batch，直接入库
				//logger.info("size = " + list.size() + "; insertSql = " + insertSql);
				OscarDao.batchInsert(list, insertSql);
			} else { // record的数量大于batch，分批入库，每批batch个
				int num = GlobalSettings.BATCH_SIZE, i = 0;
				for(; i < list.size() / num; i++) {
					List<Record> tmp = list.subList(i * num, (i+1) * num);
					//logger.info("size = " + tmp.size() + "; insertSql = " + insertSql);
					OscarDao.batchInsert(tmp, insertSql);
				}
				if(i * num < list.size()) {
					List<Record> tmp = list.subList(i * num, list.size() - 1);
					//logger.info("size = " + (list.size() - i * num) + "; insertSql = " + insertSql);
					OscarDao.batchInsert(tmp, insertSql);
				}
			}
		}
    }
	
	/**
	 * 将文件解析成record队列，读取record时就改掉列名，改掉时间戳，用router_id换掉ip
	 * 五分钟合并小时的时候，会读配置改掉router_id，因此小时合并天就不需要
	 * 
	 * @param f
	 * @return
	 */
	public static List<Record> loadRecordsForHour(StatFile f) {
		ArrayList<Record> fileRecords = new ArrayList<Record>();
		if(keyLoader == null) {
			keyLoader = new KeyConfLoader();
		}
		if(idLoader.hasNewConf()) { // 如果检测到.ok文件，就动态重新加载
			idLoader = new IDConfLoader(); 
		}
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(f.getFilePath())));
			String line = "";
			while ((line = br.readLine()) != null) {
				String[] fields = line.split("\t");
				
				/*if(fields[0].startsWith("ROUTER_STAT")) {
					continue;
				}*/
				
				// 如果是router类型，router_id是fields[2]
				if(fields[0].startsWith("ROUTER")) {
					String router_id = idLoader.searchRouterID(fields[2]);
					if(router_id == null){
						//logger.error("no router_id found!!");
						router_id = "10111";
					}
					fields[2] = router_id;
				} 
				// 如果是customer类型，router_id是fields[3]
				if(fields[0].startsWith("CUSTOMER")) {
					String router_id = idLoader.searchRouterID(fields[3]);
					if(router_id == null){
						//logger.error("no router_id found!!");
						router_id = "10111";
					}
					fields[3] = router_id;
				}
				
				Record record = new Record(fields);
				String tableName = record.getTableName();
				ArrayList<Integer> keyList = keyLoader.searchConfig(tableName
						.substring(0, tableName.lastIndexOf("_"))); // e.g. 2,3
				record.setKeyAndStatFields(keyList);
				record.setTimestamp(f.getTimeStamp()); // 更改record记录的timestamp
				record.setTableName(fields[0].substring(0, fields[0].lastIndexOf("_")) + "_H");
				/*if(fields[0].endsWith("_M")) { // 分钟合并为小时
					record.setTableName(fields[0].substring(0, fields[0].lastIndexOf("_")) + "_H");
				}
				if(fields[0].endsWith("_H")) {
					record.setTableName(fields[0].substring(0, fields[0].lastIndexOf("_")) + "_D");
				}*/
				
				fileRecords.add(record);
			}
		} catch (IOException e) {
			logger.error(e);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				logger.error(e);
			} 
		}
		return fileRecords;
	}

	
	public static List<Record> loadRecordsForDay(StatFile f) {
		ArrayList<Record> fileRecords = new ArrayList<Record>();
		if(keyLoader == null) {
			keyLoader = new KeyConfLoader();
		}
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(f.getFilePath())));
			String line = "";
			while ((line = br.readLine()) != null) {
				String[] fields = line.split("\t");
				
				Record record = new Record(fields);
				String tableName = record.getTableName();
				ArrayList<Integer> keyList = keyLoader.searchConfig(tableName
						.substring(0, tableName.lastIndexOf("_"))); // e.g. 2,3
				record.setKeyAndStatFields(keyList);
				record.setTimestamp(f.getTimeStamp()); // 更改record记录的timestamp
				
				record.setTableName(fields[0].substring(0, fields[0].lastIndexOf("_")) + "_D");
				fileRecords.add(record);
			}
		} catch (IOException e) {
			logger.error(e);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				logger.error(e);
			} 
		}
		return fileRecords;
	}
	
	/**
	 * 打印map中的元素
	 * @param map
	 */
	public static void printMap(Map<String, Map<String, Record>> map) {
		for(Entry<String, Map<String, Record>> entry : map.entrySet()) {
			logger.info(entry.getKey());
			Map<String, Record> innerMap = entry.getValue();
			for(Entry<String, Record> innerEntry : innerMap.entrySet()) {
				logger.info(innerEntry.getValue().getStatFields().toString());
			}
		}
	}
	
	/**
	 * 将一个fileList文件集合进行合并
	 * 
	 * @param list
	 * @return
	 */
	public static Map<String, Map<String, Record>> mergeFileList(
			List<StatFile> list, int flag) {
		Map<String, Map<String, Record>> map = new HashMap<String, Map<String, Record>>();
		for (int i = 0; i < list.size(); i++) { // 对于每一个文件
			StatFile f = list.get(i);
			List<Record> records = new ArrayList<Record>();
			if(flag == GlobalSettings.FLAG_MTOH){ // 如果是分钟合并小时，需要修改router_id列
				records = FileProcesser.loadRecordsForHour(f);
			}
			if(flag == GlobalSettings.FLAG_HTOD) { // 如果是小时合并天，不需要修改router_id列
				records = FileProcesser.loadRecordsForDay(f);
			}
			
			for (Record r : records) { // 对于每一个记录

				String outterKey = r.getTableName();
				String innerKey = r.generateKey();

				if (map.containsKey(outterKey)) { // 如果包含该表名
					Map<String, Record> innerMap = map.get(outterKey);

					if (innerMap.containsKey(innerKey)) { // 如果包含该signature
						Record rin = innerMap.get(innerKey);
						ArrayList<Long> found = rin.getStatFields(); // 取出值集合
						for (int j = 0; j < found.size(); j++) {
							found.set(j, found.get(j)
									+ r.getStatFields().get(j)); // 更新值集合
						}
						rin.setStatFields(found); // 重置值集合
					} else { // 如果不包含该signature
						innerMap.put(innerKey, r);
					}

				} else { // 如果不包含该表名
					Map<String, Record> innerMap = new HashMap<String, Record>();
					innerMap.put(innerKey, r);
					map.put(outterKey, innerMap);
				}
			}
		}
		return map;
	}

	/**
	 * 拷贝文件
	 * 
	 * @param src
	 * @param dst
	 */
	public static void copyFile(String src, String dst) {
		FileChannel srcChannel = null;
		FileChannel dstChannel = null;
		try {
			srcChannel = new FileInputStream(new File(src)).getChannel();
			dstChannel = new FileOutputStream(new File(dst)).getChannel();
			dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				dstChannel.close();
				srcChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	/**
	 * 将文件移动到需要合并的文件夹
	 * 
	 * @param filePath
	 * @param dstDirPath
	 */
	public static void moveFileToCal(String filePath, String dstDirPath) {
		
		String nameOnly = filePath.substring(filePath.lastIndexOf('/') + 1); // timestamp.txt
		File txtFile = new File(filePath);
		String newPath = dstDirPath + nameOnly;
		File newFile = new File(newPath);
		if(newFile.exists()) {
			newFile.delete();
			//logger.error(newPath + " has been already exists...");
		}
		txtFile.renameTo(new File(newPath)); // dstDirPath/timestamp.txt
		//logger.info("statfilePath = " + filePath + "; statdstDirPath = " + newPath);
		
		File okFile = new File(filePath + ".ok");
		File newOkFile = new File(newPath + ".ok");
		if(newOkFile.exists()) {
			newOkFile.delete();
			//logger.error(newPath + ".ok has been already exists...");
		}
		okFile.renameTo(new File(newPath + ".ok")); // dstDirPath/timestamp.txt.ok
	}

	
	/**
	 * 将文件移动到备份文件夹
	 * 
	 * @param filePath
	 * @param dstDirPath
	 */
	public static void moveFileToBak(String filePath, String dstDirPath) {
		String nameOnly = filePath.substring(filePath.lastIndexOf('/') + 1); // timestamp.txt
		File txtFile = new File(filePath);
		String newPath = dstDirPath + nameOnly;
		//logger.info("statfilePath = " + filePath + "; statdstDirPath = " + newPath);
		File newFile = new File(newPath);
		if(newFile.exists()) {
			newFile.delete();
			//logger.error(newPath + " has been already exists...");
		}
		txtFile.renameTo(new File(newPath)); // dstDirPath/timestamp.txt

	}

	/**
	 * 将一个文件列表转移到指定文件夹, 每五分钟的合并
	 * 
	 * @param list
	 * @param dstDirPath
	 */
	public static void moveFileListForHour(List<StatFile> list, String dstDirPath) {
		for (int i = 0; i < list.size(); i++) {
			FileProcesser.moveFileToBak(list.get(i).getFilePath(), dstDirPath);
		}
	}
	
	/**
	 * 将一个文件列表转移到指定文件夹, 每小时的合并
	 * @param list
	 * @param dstDirPath
	 */
	public static void moveFileListForDay(List<StatFile> list, String dstDirPath) {
		for (int i = 0; i < list.size(); i++) {
			//String filePath = list.get(i).getFilePath();
			FileProcesser.moveFileToBak(list.get(i).getFilePath(), dstDirPath);
			//new File(filePath+".ok").delete();
		}
	}

}
