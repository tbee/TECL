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
	
	// =====================================
	
	private final Map<String, TECL> groups = new HashMap<>();
	public TECL addGroup(String id) {
		if (this.groups.containsKey(id) ) {
			throw new IllegalStateException("Group " + id + " already exists in the context");
		}
		TECL tecl = new TECL();
		this.groups.put(id, tecl);
		return tecl;
	}
	
	public TECL get(String id) {
		TECL tecl = groups.get(id);
		return (tecl == null ? new TECL() : tecl);
	}
}
