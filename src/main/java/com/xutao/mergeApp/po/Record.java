package com.xutao.mergeApp.po;

import java.util.ArrayList;

/**
 * 代表文件中每一行记录的类
 * 
 * @author xutao
 *
 */
public class Record {
	private String tableName; // 表名
	private long timestamp; // 时间戳
	private ArrayList<Long> fields; // 除了表名和时间戳以外的列
	private ArrayList<Long> keyFields; // 作为key值的列
	private ArrayList<Long> statFields; // 作为value值的列

	public Record(){}
	
	public Record(String[] strs) {
		this.tableName = strs[0];
		this.timestamp = Long.parseLong(strs[1]);
		this.fields = new ArrayList<Long>();
		for (int i = 2; i < strs.length; i++) {
			this.fields.add(Long.parseLong(strs[i]));
		}
		this.keyFields = new ArrayList<Long>();
		this.statFields = new ArrayList<Long>();
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public ArrayList<Long> getFields() {
		return fields;
	}

	public void setFields(ArrayList<Long> fields) {
		this.fields = new ArrayList<Long>(fields);
	}

	public ArrayList<Long> getKeyFields() {
		return keyFields;
	}

	public void setKeyAndStatFields(ArrayList<Integer> keyIndexs) { // e.g. 2,3
		int i = 0;
		for(; i < keyIndexs.size(); i++) {
			this.keyFields.add(this.fields.get(i));
		}
		for(; i < this.fields.size(); i++) {
			this.statFields.add(this.fields.get(i));
		}
	}

	public ArrayList<Long> getStatFields() {
		return statFields;
	}

	
	/**
	 * 根据seeds生成每个表的key值
	 * @param keySeed
	 * @return
	 */
	public String generateKey(){
		StringBuilder key = new StringBuilder();
		key.append(this.tableName);
		for(Long seed : this.keyFields) {
			key.append(String.valueOf(seed));
		}
		return key.toString();
	}

	public void setKeyFields(ArrayList<Long> keyFields) {
		this.keyFields = new ArrayList<Long>(keyFields);
	}

	public void setStatFields(ArrayList<Long> statFields) {
		this.statFields = new ArrayList<Long>(statFields);
	}

	@Override
	public String toString() {
		return "Record [tableName=" + tableName + ", keyFields=" + keyFields
				+ ", statFields=" + statFields + "]";
	}
	
}
