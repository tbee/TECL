package org.tbee.tecl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * TODO:
 * - encrypt
 * - many type methods for int, dbl, localDate, etc... 
 *
 */
public class TECL {
	private final static String ENV_PREFIX = "env@";
	private final static String SYS_PREFIX = "sys@";

	final Logger logger = LoggerFactory.getLogger(TECL.class);
	
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
	
	/**
	 * Path from root to here
	 */
	public String getPath() {
		String path 
			= (parent == null ? "" : parent.getPath())
		    + id
		    + (idxInParent == null ? "" : "[" + idxInParent + "]")
			+ "/"
		    ;
		return path;
	}
	
	private String createFullPathToKey(int idx, String key) {
		String path 
		     = getPath()
		     + key
		     + "[" + idx + "]"
		     ;
		return path;
	}
	
	/**
	 * Get the parent TECL
	 * @return
	 */
	public TECL getParent() {
		return parent;
	}
	
	/**
	 * Get the root TECL
	 * @return
	 */
	public TECL getRoot() {
		TECL tecl = this;
		while (tecl.parent != null) {
			tecl = tecl.parent;
		}
		return tecl;
	}
	
	
	// =====================================
	// get
	
	/**
	 * Lookup function:  
	 * - First search for the occurence of indexOfValue in indexOfPath.
	 * - Using that index get the value for path.
	 * 
	 * @param <R>
	 * @param indexOfPath
	 * @param indexOfValue
	 * @param path
	 * @param def
	 * @param convertFunction
	 * @return
	 */
	public <R> List<R> get(String indexOfPath, String indexOfValue, String path, List<R> def, Function<String, R> convertFunction) {
		int idx = strs(indexOfPath).indexOf(indexOfValue);
		if (idx < 0) {
			return def;
		}
		return get(path + "[" + idx + "]", def, convertFunction);
	}

	/**
	 * Get a value using a directory-style path, like /group1/group2[4]/value
	 * 
	 * This is the main way to access values in TECL, all convenience method use this method
	 * 
	 * @param <R>
	 * @param path the path the access
	 * @param def the value to return if nothing is found
	 * @param convertFunction the conversion function to convert properties to their end type, if you access groups this should be null
	 * @return a list of found values
	 */
	@SuppressWarnings("unchecked")
	public <R> List<R> get(String path, List<R> def, Function<String, R> convertFunction) {
		String context = this.getPath() + " -> " + path + ": ";
		
		// Specials 
		if (path.startsWith(ENV_PREFIX)) {
			return getEnv(path, convertFunction, context);
		}
		if (path.startsWith(SYS_PREFIX)) {
			return getSys(path, convertFunction, context);
		}
		
		// Determine the starting point
		TECL tecl = null;
		if (path.startsWith("/")) {
			tecl = this.getRoot();
			logger.atDebug().log(context + "start at root, tecl = " + tecl);
		}
		else {
			tecl = this;
			logger.atDebug().log(context + "start at current, tecl = " + tecl);
		}
		context = tecl.getPath() + " -> " + path + ": ";
		
		// First split into its parts
		List<String> nodes = new StringTokenizer(path, "/").getTokenList();
		logger.atDebug().log(context + "var tokenized: "  + nodes);
		
		// Navigate through the tree
		// All intermediate tokens must be a single group, the last token can be more complex
		String node = null;
		List<Integer> idxs = new ArrayList<Integer>();
		while (!nodes.isEmpty()) { 
			
			// Get current token
			node = nodes.remove(0);
			logger.atDebug().log(context + "node = "  + node);
			context = tecl.getPath() + node + ": ";
			
			// extract the indexes from the node
			node = extractIdxs(node, idxs);
			logger.atDebug().log(context + "node = "  + node + ", idxs = "  + idxs);
			
			// Is this the last token? 
			// If so, break out, because that is much more complex
			boolean lastToken = nodes.isEmpty();
			if (lastToken) {
				break;
			}
			
			// Not the last token, so this either is a group or a variable resolving to a group
			int idx = (idxs.isEmpty() ? 0 : idxs.get(0));
			// if it is a variable
			List<String> properties = tecl.properties.get(node);
			if (properties != null && properties.size() > idx && properties.get(idx).startsWith("$")) {
				logger.atDebug().log(context + "Found variable: " + properties.get(idx));
				String var = properties.get(idx).substring(1);
				tecl = get(var, emptyGroup(idx), null).get(0);
				logger.atDebug().log(context + "Resolved variable: " + var + " -> TECL= " + tecl.getPath());
			}
			else {
				if ("..".equals(node)) {
					tecl = tecl.getParent();
				}
				else {
					tecl = tecl.grp(idx, node);
				}
				logger.atDebug().log(context + "Assumed group, TECL= " + tecl.getPath());
			}
		}
		
		// This is the last node, it may be a property, group, list or variable
		// First get all relevant info
		// Properties
		List<String> properties = tecl.properties.get(node);
		logger.atDebug().log(context + "Properties = " + properties);
		// Group
		List<TECL> groups = tecl.groups.get(node); 
		logger.atDebug().log(context + "Groups = " + groups);
		// List
		List<TECL> list = tecl.groups.get("|" + node + "|"); 
		logger.atDebug().log(context + "Lists = " + list);
		
		// Construct the results
		Integer idx = (idxs.isEmpty() ? null : idxs.get(0));
		List<R> results = null;
		// If there is no convertFunction, then it the result are groups
		if (convertFunction == null) {
			logger.atDebug().log(context + "There no convert function, so the last token must be groups.");
			
			// If we have a variable overlapping the groups
			if (groups.isEmpty() && properties.size() == 1 && properties.get(0).startsWith("$")) {
				
				// Resolve the variable
				logger.atDebug().log(context + "We have no groups, but we do a single property which is a variable: " + properties);					
				String var = properties.get(0).substring(1);
				results = get(var, null, null);
			}
			else {
				// The result are the groups
				groups = optionallyApplyIndex(context, groups, idx);
				results = (List<R>) groups;
			}
		}
		// A convertFunction says it is from the properties
		else {
			logger.atDebug().log(context + "There is a convert function, so the last token must be properties.");
			
			// If there is a variable
			if (properties.size() == 1 && properties.get(0).startsWith("$")) {
				logger.atDebug().log(context + "We have a single property which is a variable, going to resolve that: " + properties);					
				String var = properties.get(0).substring(1);
				results = get(var, null, convertFunction);
				results = optionallyApplyIndex(context, results, idx);
			}
			// No variable, so we're processing the properties
			else {
				
				// If we have a list overlapping the properties, replace the properties with those in the list
				if (idx != null && list.size() > idx && list.get(idx) != null) {
					properties = list.get(idx).properties.get(node);
					logger.atDebug().log(context + "There is an overlapping list, replaced properties with its contents. Properties = " + properties);
				}
				
				// Apply the index
				properties = optionallyApplyIndex(context, properties, idx);
			
				// Convert to end value
				results = new ArrayList<R>();
				for (String property : properties) {
					
					// But each property can be a variable again
					if (property.startsWith("$")) {
						
						// Resolve variable
						logger.atDebug().log(context + "Property is a variable: " + property);
						String var = property.substring(1);
						List<R> varResult = get(var, null, convertFunction);
						results.addAll(varResult);
					}
					else {
						
						// Convert property to end type
						property = sanatizeString(property);
						R result = convertFunction.apply(property);
						logger.atDebug().log(context + "Property converted: " + property + " -> "  + result);
						results.add(result);
					}
				};
			}
		}	
		logger.atDebug().log(context + "Results: " + results);

		// secondary index (for lists)
		Integer idx1 = (idxs.size() <= 1 ? null : idxs.get(1));
		results = optionallyApplyIndex(context, results, idx1);
		
		// done
		return results.isEmpty() ? def : results;
	}


	private String extractIdxs(String node, List<Integer> idxs) {
		idxs.clear();
		while (node.contains("[")) {
			int startIdx = node.indexOf("[");
			int endIdx = node.indexOf("]");
			String idxString = node.substring(startIdx + 1,endIdx);
			Integer idx = idxString.trim().isEmpty() ? null : Integer.parseInt(idxString);
			idxs.add(idx);
			String remainingIdx = node.substring(endIdx + 1);
			node = node.substring(0, startIdx) + remainingIdx;				
		}
		return node;
	}


	private <R> List<R> getSys(String path, Function<String, R> convertFunction, String context) {
		String sys = path.substring(SYS_PREFIX.length());
		R result = convertFunction.apply(System.getProperty(sys));
		logger.atDebug().log(context + "sys path, result = " + result);
		return Arrays.asList(result);
	}


	private <R> List<R> getEnv(String path, Function<String, R> convertFunction, String context) {
		String env = path.substring(ENV_PREFIX.length());
		R result = convertFunction.apply(System.getenv(env));
		logger.atDebug().log(context + "env path, result = " + result);
		return Arrays.asList(result);
	}
	
	/*
	 * 
	 */
	private <R> List<R> optionallyApplyIndex(String context, List<R> list, Integer idx) {
		if (idx != null && list.size() > idx) {
			R result = list.get(idx);
			list = new ArrayList<R>();
			list.add(result);
			logger.atDebug().log(context + "Limit to idx = " + idx + ", result: " + list);
		}
		return list;
	}
	
	
	// =====================================
	// properties
	
	private final IndexedValues<String> properties = new IndexedValues<>();
	
	/**
	 * Set a property value. This is identical to setting an indexed property with index 0.
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, String value) {
		properties.set(0, key, value, false);
	}	
	
	/**
	 * Set an indexed property value. You cannot override a value.
	 * @param idx
	 * @param key
	 * @param value
	 */
	public void setProperty(int idx, String key, String value) {
		properties.set(idx, key, value, false);
	}
	
	/**
	 * Set an indexed property value. 
	 * @param idx
	 * @param key
	 * @param value
	 * @param allowOverwrite
	 */
	public void setProperty(int idx, String key, String value, boolean allowOverwrite) {
		properties.set(idx, key, value, allowOverwrite);
	}	
	
	/**
	 * Clear property. This means all values in case of an index property.
	 * @param key
	 */
	public void clearProperty(String key) {
		properties.clear(key);
	}	
	
	/**
	 * Clear single indexed property.
	 * @param key
	 */
	public void clearProperty(int idx, String key) {
		properties.clear(idx, key);
	}	
	
	/**
	 * Return the index of the specified value for this specified property
	 * @param key
	 * @param value
	 * @return
	 */
	public int indexOf(String key, String value) {
		return properties.indexOf(key, value);		
	}
	
	/**
	 * Count the number of values for the specified property
	 * @param key
	 * @return
	 */
	public int count(String key) {
		return properties.count(key);		
	}

	/**
	 * Get the raw uninterpreted value of a property.
	 * This is a last resort when, for example, there is an overlap using variables.
	 * But these situations preferably are prevented.
	 * 
	 * @param idx
	 * @param key
	 * @param def
	 * @return
	 */
	public String raw(int idx, String key, String def) {
		return properties.get(idx, key, null);
	}
	
	/** Convenience method to return a string */
	public String str(String key) {
		return str(0, key, null);
	}
	public String str(String key, String def) {
		return str(0, key, def);
	}
	public String str(int idx, String key) {
		return str(idx, key, null);
	}
	public String str(int idx, String key, String def) {
		return get(key + "[" + idx + "]", asList(def), (s) -> s).get(0);
	}
	public List<String> strs(String key) {
		return get(key, Collections.emptyList(), (s) -> s);
	}
	public String str(String indexOfKey, String indexOfValue, String key, String def) {
		return get(indexOfKey, indexOfValue, key, asList(def), (s) -> s).get(0);
	}
	
	/** Convenience method to return an Integer */
	public Integer integer(String key) {
		return integer(0, key, null);
	}
	public Integer integer(String key, Integer def) {
		return integer(0, key, def);
	}
	public Integer integer(int idx, String key) {
		return integer(idx, key, null);
	}
	public Integer integer(int idx, String key, Integer def) {
		return get(key + "[" + idx + "]", asList(def), Integer::valueOf).get(0);
	}
	public List<Integer> integers(String key) {
		return get(key, Collections.emptyList(), Integer::valueOf);
	}
	public Integer integer(String indexOfKey, String indexOfValue, String key, Integer def) {
		return get(indexOfKey, indexOfValue, key, asList(def), Integer::valueOf).get(0);
	}
	
	
	/** Convenience method to return a Double */
	public Double dbl(String key) {
		return dbl(0, key, null);
	}
	public Double dbl(String key, Double def) {
		return dbl(0, key, def);
	}
	public Double dbl(int idx, String key) {
		return dbl(idx, key, null);
	}
	public Double dbl(int idx, String key, Double def) {
		return get(key + "[" + idx + "]", asList(def), Double::valueOf).get(0);
	}
	public List<Double> dbls(String key) {
		return get(key, Collections.emptyList(), Double::valueOf);
	}
	public Double dbl(String indexOfKey, String indexOfValue, String key, Double def) {
		return get(indexOfKey, indexOfValue, key, asList(def), Double::valueOf).get(0);
	}

	/** Convenience method to return a LocalDate */
	public LocalDate localDate(String key) {
		return localDate(0, key, null);
	}
	public LocalDate localDate(String key, LocalDate def) {
		return localDate(0, key, def);
	}
	public LocalDate localDate(int idx, String key) {
		return localDate(idx, key, null);
	}
	public LocalDate localDate(int idx, String key, LocalDate def) {
		return get(key + "[" + idx + "]", asList(def), LocalDate::parse).get(0);
	}
	public List<LocalDate> localDates(String key) {
		return get(key, Collections.emptyList(), LocalDate::parse);
	}
	public LocalDate localDate(String indexOfKey, String indexOfValue, String key, LocalDate def) {
		return get(indexOfKey, indexOfValue, key, asList(def), LocalDate::parse).get(0);
	}

	/** Convenience method to return a LocalDateTime */
	public LocalDateTime localDateTime(String key) {
		return localDateTime(0, key, null);
	}
	public LocalDateTime localDateTime(String key, LocalDateTime def) {
		return localDateTime(0, key, def);
	}
	public LocalDateTime localDateTime(int idx, String key) {
		return localDateTime(idx, key, null);
	}
	public LocalDateTime localDateTime(int idx, String key, LocalDateTime def) {
		return get(key + "[" + idx + "]", asList(def), LocalDateTime::parse).get(0);
	}
	public List<LocalDateTime> localDateTimes(String key) {
		return get(key, Collections.emptyList(), LocalDateTime::parse);
	}
	public LocalDateTime localDateTime(String indexOfKey, String indexOfValue, String key, LocalDateTime def) {
		return get(indexOfKey, indexOfValue, key, asList(def), LocalDateTime::parse).get(0);
	}
	
	// =====================================
	// groups
	
	private final IndexedValues<TECL> groups = new IndexedValues<>();

	/**
	 * Add a group.
	 * @param id
	 * @return
	 */
	public TECL addGroup(String id) {
		TECL tecl = new TECL(id);
		int idx = groups.add(id, tecl);
		tecl.setParent(this, idx);
		return tecl;
	}
	
	TECL setGroup(int idx, String id) {
		TECL tecl = new TECL(id);
		groups.set(idx, id, tecl, false);
		tecl.setParent(this, idx);
		return tecl;
	}

	/**
	 * Count the number of groups with the same id
	 * @param id
	 * @return
	 */
	public int countGrp(String id) {
		return groups.count(id);		
	}

	/**
	 * Get the group with the specified key. This is the same as getting thee first (index 0) group.
	 * @param id
	 * @return
	 */
	public TECL grp(String id) {
		return grp(0, id);
	}
	
	/**
	 * Get the indexed group.
	 * If the group is not found, an empty group is returned. 
	 * @param idx
	 * @param id
	 * @return
	 */
	public TECL grp(int idx, String key) {
		return get(key + "[" + idx + "]", emptyGroup(idx), null).get(0);
	}


	private List<TECL> emptyGroup(int idx) {
		return asList(new TECL("<group '" + createFullPathToKey(idx, id) + "' does not exist>"));
	}
	
	/**
	 * Get all groups for a key.
	 * @param id
	 * @return
	 */
	public List<TECL> grps(String key) {
		return get(key, Collections.emptyList(), null);
	}

	// =====================================
	// SUPPORT
	
	/*
	 * Implements an indexed store
	 */
	private class IndexedValues<T> {
		private final Map<String, List<T>> keyTovaluesMap = new LinkedHashMap<>();
		
		void clear(String key) {
			List<T> values = keyTovaluesMap.get(key);
			if (values != null) {
				logger.atDebug().log(getPath() + ": clear property " + key);
				values.clear();
			}
		}
		
		void clear(int idx, String key) {
			List<T> values = keyTovaluesMap.get(key);
			if (values != null) {
				values.clear();
			}
			
			// Check if the value can be cleared
			if (values.size() <= idx) {
				return;
			}
			logger.atDebug().log(getPath() + ": clear property " + key + "[" + idx + "]");
			values.set(idx, null);
		}
		
		void set(int idx, String key, T value, boolean allowOverwrite) {
			
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
			if (oldValue != null && !allowOverwrite) {
				throw new IllegalStateException(createFullPathToKey(idx, key) + " value is overwritten! " + oldValue + " -> " + value);
			}
			logger.atDebug().log(getPath() + ": set property "  + key + "[" + idx + "] = " + value);
			values.set(idx, value);
		}

		int add(String key, T value) {
			int idx = count(key);
			set(idx, key, value, false);
			return idx;
		}

		int count(String key) {
			List<T> values = keyTovaluesMap.get(key);
			if (values == null) {
				return 0;
			}
			return values.size();
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
		 * Get collection
		 */
		List<T> get(String key) {
			List<T> values = keyTovaluesMap.get(key);
			if (values == null) {
				return Collections.emptyList();
			}
			values = new ArrayList<T>(values); // End users are not allowed to modify the list
			return values;
		}

		/*
		 * Get based on index
		 */
		T get(int idx, String key, T def) {
			List<T> values = keyTovaluesMap.get(key);
			if (values == null) {
				return def;
			}
			T value = values.get(idx);
			if (value == null) {
				return def;
			}
			return value;
		}
		
		public String toString() {
			return keyTovaluesMap.toString();
		}
	}
	
	public static class ParseException extends RuntimeException {
		public ParseException(Exception e) {
			super(e);
		}
	}
	

	/**
	 * @param s
	 * @return
	 */
	private String sanatizeString(String s) {
		
		logger.atDebug().log("-----");
		logger.atDebug().log("sanatize: >"  + s + "<");
		
		// check to see if it is quoted
		String trimmed = s.trim();
		int trimmedLen = s.length();
		if ( s.length() > 1 
		  && "\"".contentEquals(trimmed.substring(0, 1))
		  && "\"".contentEquals(trimmed.substring(trimmedLen - 1, trimmedLen))
		  ) {
			s = sanatizeQuotedString(s);
		}
		else {
			s = sanatizeUnquotedString(s);
		}
		
		logger.atDebug().log("sanatize: done: >"  + s + "<");
		return s;
	}

	/*
	 * 
	 */
	private String sanatizeQuotedString(String s) {
		logger.atDebug().log("sanatize: treat as quoted string: >"  + s + "<");

		// strip quoted
		s = s.substring(1, s.length() - 1);
		logger.atDebug().log("sanatize: trimmed quotes: >"  + s + "<");
		
		// unescape
		s = StringEscapeUtils.unescapeJava(s);
		
		// done
		return s;
	}

	/*
	 * 
	 */
	private String sanatizeUnquotedString(String s) {
		logger.atDebug().log("sanatize: treat as unquoted string: >"  + s + "<");

		// done
		return s.trim();
	}

	/*
	 * 
	 */
	private <R> List<R> asList(R value) {
		List<R> values = new ArrayList<R>();
		values.add(value);
		return values;
	}
	
	public String toString() {
		return getPath();
	}
}
