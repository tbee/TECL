package org.tbee.tecl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * TODO:
 * - conditions
 * - dot notation
 * - indexed properties via key (first column)
 * - references via dot notation
 * - many type methods for int, dbl, localDate, etc 
 * - encrypted strings
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
	
	public int indexOf(String key, String value) {
		return properties.indexOf(key, value);		
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
	public String str(String indexOfKey, String indexOfValue, String key) {
		return properties.get(indexOfKey, indexOfValue, key);
	}
	public String str(String indexOfKey, String indexOfValue, String key, String def) {
		return properties.get(indexOfKey, indexOfValue, key, def);
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

	// public int indexOfGrp(String key, String value) {
	//	return groups.indexOf(key, value); // group id's are indentical, so what to compare on?
	//}


	// =====================================
	// SUPPORT
	
	/**
	 * Implements an indexed store
	 */
	private class IndexedValues<T> {
		private final Map<String, List<T>> keyTovaluesMap = new LinkedHashMap<>();
		
		void set(int idx, String key, T value) {
			
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

		int add(String key, T value) {
			int idx = count(key);
			set(idx, key, value);
			return idx;
		}

		int count(String key) {
			List<T> values = keyTovaluesMap.get(key);
			if (values == null) {
				return 0;
			}
			return values.size();
		}
		
		boolean contains(String key) {
			return keyTovaluesMap.containsKey(key);
		}
		
		T get(int idx, String key) {
			List<T> values = keyTovaluesMap.get(key);
			if (values == null) {
				return null;
			}
			return values.get(idx);
		}
		
		T get(int idx, String key, T def) {
			T value = get(idx, key);
			return value == null ? def : value;
		}
		
		/*
		 * Find the index of a value within a key.
		 * This can be used to determine the row in a table, in order to support value-based-indexes. 
		 * For example
		 * 
		 * | id  | value |
		 * | id1 | val1  |
		 * | id2 | val2  |
		 * | id3 | val3  |
		 * 
		 * int idx = indexof("id", "id2");
		 * String val = get(idx, "value"); // This will hold "val2";
		 */
		int indexOf(String key, T value) {
			List<T> values = keyTovaluesMap.get(key);
			if (values == null) {
				return -1;
			}
			return values.indexOf(value);
		}
		
		/*
		 * Get method using indexOf to determine the index first
		 */
		public T get(String indexOfKey, T value, String key) {
			return get(indexOfKey, value, key, null);
		}
		
		/*
		 * Get method using indexOf to determine the index first
		 */
		public T get(String indexOfKey, T value, String key, T def) {
			int idx = indexOf(indexOfKey, value);
			if (idx < 0) {
				return null;
			}
			return get(idx, key, def);
		}
	}
}
