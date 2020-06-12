package org.tbee.tecl;

import java.util.HashMap;
import java.util.Map;

public class TECL {
	
	private final Map<String, String> properties = new HashMap<>();
	public void addProperty(String key, String value) {
		this.properties.put(key, value);
	}	

	public String str(String key) {
		return properties.get(key);
	}
	public String str(String key, String def) {
		String value = properties.get(key);
		return (value == null ? def : value);
	}
}
