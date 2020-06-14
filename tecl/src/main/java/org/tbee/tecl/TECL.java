package org.tbee.tecl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TECL {
	
	// =====================================
	// parser
	
	static public TECLParser parser() {
		return new TECLParser();
	}
	
	
	// =====================================
	// Constructor
	
	public TECL(String id) {
		this.id = id;
	}
	private final String id;
	
	public String getId() {
		return id;
	}
	
	// =====================================
	// properties
	
	private final Map<String, String> properties = new HashMap<>();
	public void addProperty(String key, String value) {
		//System.out.println("@"  + id + ": Adding property "  + key + " = " + value);
		if (this.groups.containsKey(key) ) {
			throw new IllegalStateException("Property " + key + " already exists in the context");
		}
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
	// indexed properties aka table
	
	private final List<TECL> tableTECLS = new ArrayList();
	public void addIndexedProperty(int rowIdx, String key, String value) {
		
		// Each row is a TECL on its own, make sure it is present for the specified row
		while (tableTECLS.size() <= rowIdx) {
			tableTECLS.add(null);
		}
		TECL tableTECL = tableTECLS.get(rowIdx);
		if (tableTECL == null) {
			tableTECL = new TECL("<table row " + rowIdx + ">");
			tableTECLS.set(rowIdx, tableTECL);
		}
		
		// Store the value
		tableTECL.addProperty(key, value);
	}
	final static private String UNDEFINED = "?";

	public String str(int idx, String key) {
		return tableTECLS.get(idx).str(key);
	}
	public String str(int idx, String key, String def) {
		return tableTECLS.get(idx).str(key, def);
	}
	
	// =====================================
	// groups
	
	private final Map<String, TECL> groups = new HashMap<>();
	public TECL addGroup(String id) {
		//System.out.println("@"  + this.id + ": Adding group "  + id);
		if (this.groups.containsKey(id) ) {
			throw new IllegalStateException("Group " + id + " already exists in the context");
		}
		TECL tecl = new TECL(id);
		this.groups.put(id, tecl);
		return tecl;
	}
	
	public TECL get(String id) {
		TECL tecl = groups.get(id);
		if (tecl == null) {
			tecl = new TECL("<group '" + id + "' does not exist>");
		}
		return tecl;
	}
}
