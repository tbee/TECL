package org.tbee.tecl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * TODO:
 * - escaped characters in quoted string 
 * - conditions
 * - dot notation
 * - indexed properties via key (first column)
 * - references via dot notation 
 * 
 *
 */
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
	
	public void setParent(TECL parent, int idxInParent) {
		this.parent = parent;
		this.idxInParent = idxInParent;
	}
	private TECL parent;
	private Integer idxInParent;
	
	public String getId() {
		return id;
	}
	
	/*
	 * Dot separated path from toplevel to here
	 */
	public String getPath() {
		String path = (parent == null ? "" : parent.getPath());
		String idxSuffix = idxInParent == null ? "" : "[" + idxInParent + "]" ;
		if ("$".equals(path)) {
			return path + id + idxSuffix;
		}
		return (path.isEmpty() ? "" : path + ".") + id + idxSuffix;
	}
	
	private String createFullPath(int idx, String key) {
		String field = key + "[" + idx + "]";
		String path = getPath();
		if ("$".equals(path)) {
			return path + field;
		}
		return path + "." + field;
	}
	
	public TECL getParent() {
		return parent;
	}
	
	
	// =====================================
	// properties
	
	private final IndexedValues<String> properties = new IndexedValues<>();
	
	public void addProperty(String key, String value) {		
		properties.add(key, value);
	}	

	public void setProperty(int idx, String key, String value) {
		properties.set(idx, key, value);
	}	

	public String str(String key) {
		return properties.get(0, key);
	}
	public String str(String key, String def) {
		return properties.get(0, key, def);
	}

	public String str(int idx, String key) {
		return properties.get(idx, key);
	}
	public String str(int idx, String key, String def) {
		return properties.get(idx, key, def);
	}
	
	// =====================================
	// groups
	
	private final IndexedValues<TECL> groups = new IndexedValues<>();
	
	public TECL addGroup(String id) {
		TECL tecl = new TECL(id);
		int idx = groups.add(id, tecl);
		tecl.setParent(this, idx);
		return tecl;
	}
	
	public TECL grp(String id) {
		return grp(0, id);
	}
	
	public TECL grp(int idx, String id) {
		TECL tecl = groups.get(idx, id);
		if (tecl == null) {
			tecl = new TECL("<group '" + id + "' does not exist>");
			tecl.setParent(this, -1);
		}
		return tecl;
	}

	// =====================================
	// SUPPORT
	
	/**
	 * Implements an indexed store
	 */
	private class IndexedValues<T> {
		private final Map<String, List<T>> keyTovaluesMap = new HashMap<>();
		
		public void set(int idx, String key, T value) {
			
			// First get the list of values
			List<T> values = keyTovaluesMap.get(key);
			if (values == null) {
				values = new ArrayList<T>();
				keyTovaluesMap.put(key, values);
			}
			
			// Check if the value can be put in
			while (values.size() <= idx) {
				values.add(null);
			}
			
			// Store the value
			T oldValue = values.get(idx);
			if (oldValue != null) {
				System.out.println("WARN: " + createFullPath(idx, key) + " value is overwritten! " + oldValue + " -> " + value); // TBEERNOT
			}
			//System.out.println("@"  + id + ": Adding property "  + key + " = " + value);
			values.set(idx, value);
		}

		public int add(String key, T value) {
			int idx = count(key);
			set(idx, key, value);
			return idx;
		}

		public int count(String key) {
			List<T> values = keyTovaluesMap.get(key);
			if (values == null) {
				return 0;
			}
			return values.size();
		}
		
		public boolean contains(String key) {
			return keyTovaluesMap.containsKey(key);
		}
		
		public T get(int idx, String key) {
			List<T> values = keyTovaluesMap.get(key);
			if (values == null) {
				return null;
			}
			return values.get(idx);
		}
		
		public T get(int idx, String key, T def) {
			T value = get(idx, key);
			return value == null ? def : value;
		}
	}
}
