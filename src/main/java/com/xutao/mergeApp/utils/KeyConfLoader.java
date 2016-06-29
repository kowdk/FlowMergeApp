package com.xutao.mergeApp.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * 静态配置，为records寻找key和value的类
 * 
 * @author xutao
 *
 */
public class KeyConfLoader {

	private Map<String, ArrayList<Integer>> keyFieldMap = new HashMap<String, ArrayList<Integer>>();
	private static Logger logger = Logger.getLogger(KeyConfLoader.class);
	private final String confName = "posConfig.properties";
	
	public KeyConfLoader() {
		loadConfig();
	}

	
	/**
	 * 根据配置文件加载作为键的列信息
	 */
	private void loadConfig() {
		Properties prop = new Properties();
		try {
			prop.load(SqlLoader.class.getClassLoader().getResourceAsStream(confName));
			for (String key : prop.stringPropertyNames()) {
				String[] vs = prop.getProperty(key).split(",");
				ArrayList<Integer> list = new ArrayList<Integer>();
				for (String s : vs) {
					list.add(Integer.parseInt(s));
				}
				keyFieldMap.put(key, list);
			}

		} catch (IOException e) {
			logger.error(e);
		}
	}

	/**
	 * 根据tableName查询key值的列位置信息
	 * 
	 * @param tableName
	 * @return
	 */
	public ArrayList<Integer> searchConfig(String tableName) {
		return keyFieldMap.containsKey(tableName) ? keyFieldMap.get(tableName)
				: null;
	}

	public static void main(String[] args) {
		KeyConfLoader loader = new KeyConfLoader();
		System.out.println(loader.searchConfig("ROUTER_FAKE"));
	}
}
