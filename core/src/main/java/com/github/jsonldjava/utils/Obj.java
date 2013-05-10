package com.github.jsonldjava.utils;

import java.util.Map;

public class Obj {

	/**
	 * Used to make getting values from maps embedded in maps embedded in maps easier
	 * TODO: roll out the loops for efficiency
	 * 
	 * @param map
	 * @param keys
	 * @return
	 */
	public static Object get(Object map, String... keys) {
		for (String key: keys) {
			map = ((Map<String,Object>)map).get(key);
			// make sure we don't crash if we get a null somewhere down the line
			if (map == null) return map;
		}
		return map;	
	}
	
	public static Object put(Object map, String key1, Object value) {
		((Map<String,Object>)map).put(key1, value);
		return map;
	}
	
	public static Object put(Object map, String key1, String key2, Object value) {
		((Map<String, Object>) ((Map<String,Object>)map).get(key1)).put(key2, value);
		return map;
	}
	
	public static Object put(Object map, String key1, String key2, String key3, Object value) {
		((Map<String, Object>) ((Map<String, Object>) ((Map<String,Object>)map).get(key1)).get(key2)).put(key3, value);
		return map;
	}
	public static Object put(Object map, String key1, String key2, String key3, String key4, Object value) {
		((Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) ((Map<String,Object>)map).get(key1)).get(key2)).get(key3)).put(key4, value);
		return map;
	}

	public static boolean contains(Object map, String... keys) {
		for (String key: keys) {
			map = ((Map<String,Object>)map).get(key);
			if (map == null) {
				return false;
			}
		}
		return true;
	}

	public static Object remove(Object map, String k1, String k2) {
		return ((Map<String, Object>) ((Map<String, Object>)map).get(k1)).remove(k2);
	}
}
