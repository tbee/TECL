package org.tbee.tecl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * - anonymous groups in table? Or are we sticking to variables referring groups
 * - many type methods for int, dbl, localDate, etc... 
 *
 */
public class TECL {
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
	 * Dot separated path from root to here
	 */
	public String getPath() {
		String path = (parent == null ? "" : parent.getPath());
		String idxSuffix = idxInParent == null ? "" : "[" + idxInParent + "]" ;
		if ("$".equals(path)) {
			return path + id + idxSuffix;
		}
		return (path.isEmpty() ? "" : path + ".") + id + idxSuffix;
	}
	
	private String createFullPathToKey(int idx, String key) {
		String field = key + "[" + idx + "]";
		String path = getPath();
		if ("$".equals(path)) {
			return path + field;
		}
		return path + "." + field;
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
	
	/**
	 * Get a property; a convert function needs to be provided to convert the String to the required type.
	 * Usually a property is accessed through one of the convenience methods like str, integer, localDate, etc.
	 * If a variable is encountered, it will be resolved and the convert function is applied on the resolved value.
	 * 
	 * @param <R>
	 * @param key
	 * @param convertFunction
	 * @return
	 */
	public <R> R get(String key, Function<String, R> convertFunction) {
		return get(0, key, null, convertFunction);
	}
	public <R> R get(String key, R def, Function<String, R> convertFunction) {
		return get(0, key, def, convertFunction);
	}
	public <R> R get(int idx, String key, Function<String, R> convertFunction) {
		return get(idx, key, convertFunction);
	}
	public <R> R get(String indexOfKey, String indexOfValue, String key, Function<String, R> convertFunction) {
		int idx = indexOf(indexOfKey, indexOfValue);
		if (idx < 0) {
			return null;
		}
		return get(idx, key, convertFunction);
	}
	public <R> R get(String indexOfKey, String indexOfValue, String key, R def, Function<String, R> convertFunction) {
		int idx = indexOf(indexOfKey, indexOfValue);
		if (idx < 0) {
			return null;
		}
		return get(idx, key, def, convertFunction);
	}
	public <R> R get(int idx, String key, R def, Function<String, R> convertFunction) {
		
		// Get the values
		List<String> values = properties.get(key);
		if (values == null) {
			return def;
		}
		
		// Check to see if it is a variable
		if (values.size() == 1) {
			String value = values.get(0).trim();
			if (value.startsWith("$")) {
				List<R> results = vars(value, convertFunction);
				if (idx >= results.size()) {
					return def;
				}
				return results.get(idx);
			}
		}
		
		// Get the value, unless the index is out of bounds
		if (idx >= values.size()) {
			return def;
		}
		String value = values.get(idx);
		
		// If variable, resolve it, else make sure it is the correct string (stripping quotes)
		if (value.startsWith("$")) {
			R result = var(value, def, convertFunction);
			return result;
		}
		else {
			value = sanatizeString(value);
		}		
		
		// Apply the convert function
		try {
			return convertFunction.apply(value);
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
	}
	
	/**
	 * Construct a list based on a key.
	 * If a variable is encountered as one of the value, it will be resolved and the convert function is applied on the resolved value.
	 * 
	 * @param <R>
	 * @param key
	 * @param convertFunction
	 * @return
	 */
	public <R> List<R> list(String key, Function<String, R> convertFunction) {			
		
		// Get the contents
		List<String> stringValues = properties.get(key);
		
		// If there is only one value, it may be a variable to a list
		if (stringValues.size() == 1) {
			String stringValue = stringValues.get(0);
			if (stringValue.startsWith("$")) {
				List<R> values = vars(stringValue, convertFunction);
				return values;
			}
		}
		
		// Convert each value
		List<R> values = new ArrayList<>();
		for (String stringValue : stringValues) {
			
			// If variable, resolve it, else make sure it is the correct string (stripping quotes)
			if (stringValue.startsWith("$")) {
				R value = var(stringValue, null, convertFunction);
				values.add(value);
			}
			else {
				// Apply the convert function
				stringValue = sanatizeString(stringValue);
				try {
					R value = convertFunction.apply(stringValue);
					values.add(value);
				}
				catch (Exception e) {
					throw new ParseException(e);
				}
			}
		}		
		return values;
	}

	/** Convenience method to return a string */
	public String str(String key) {
		return get(0, key, null, (s) -> s);
	}
	public String str(String key, String def) {
		return get(0, key, def, (s) -> s);
	}
	public String str(int idx, String key) {
		return get(idx, key, null, (s) -> s);
	}
	public String str(int idx, String key, String def) {
		return get(idx, key, def, (s) -> s);
	}
	public String str(String indexOfKey, String indexOfValue, String key) {
		return get(indexOfKey, indexOfValue, key, null, (s) -> s);
	}
	public String str(String indexOfKey, String indexOfValue, String key, String def) {
		return get(indexOfKey, indexOfValue, key, def, (s) -> s);
	}
	public List<String> strs(String key) {
		return list(key, (s) -> s);
	}
	
	/** Convenience method to return an Integer */
	public Integer integer(String key) {
		return get(0, key, null, Integer::valueOf);
	}
	public Integer integer(String key, Integer def) {
		return get(0, key, def, Integer::valueOf);
	}
	public Integer integer(int idx, String key) {
		return get(idx, key, null, Integer::valueOf);
	}
	public Integer integer(int idx, String key, Integer def) {
		return get(idx, key, def, Integer::valueOf);
	}
	public Integer integer(String indexOfKey, String indexOfValue, String key) {
		return get(indexOfKey, indexOfValue, key, null, Integer::valueOf);
	}
	public Integer integer(String indexOfKey, String indexOfValue, String key, Integer def) {
		return get(indexOfKey, indexOfValue, key, def, Integer::valueOf);
	}
	public List<Integer> integers(String key) {
		return list(key, Integer::valueOf);
	}
	
	
	/** Convenience method to return a Double */
	public Double dbl(String key) {
		return get(0, key, null, Double::valueOf);
	}
	public Double dbl(String key, Double def) {
		return get(0, key, def, Double::valueOf);
	}
	public Double dbl(int idx, String key) {
		return get(idx, key, null, Double::valueOf);
	}
	public Double dbl(int idx, String key, Double def) {
		return get(idx, key, def, Double::valueOf);
	}
	public Double dbl(String indexOfKey, String indexOfValue, String key) {
		return get(indexOfKey, indexOfValue, key, null, Double::valueOf);
	}
	public Double dbl(String indexOfKey, String indexOfValue, String key, Double def) {
		return get(indexOfKey, indexOfValue, key, def, Double::valueOf);
	}
	public List<Double> dbls(String key) {
		return list(key, Double::valueOf);
	}
	
	/** Convenience method to return a LocalDate */
	public LocalDate localDate(String key) {
		return get(0, key, null, LocalDate::parse);
	}
	public LocalDate localDate(String key, LocalDate def) {
		return get(0, key, def, LocalDate::parse);
	}
	public LocalDate localDate(int idx, String key) {
		return get(idx, key, null, LocalDate::parse);
	}
	public LocalDate localDate(int idx, String key, LocalDate def) {
		return get(idx, key, def, LocalDate::parse);
	}
	public LocalDate localDate(String indexOfKey, String indexOfValue, String key) {
		return get(indexOfKey, indexOfValue, key, null, LocalDate::parse);
	}
	public LocalDate localDate(String indexOfKey, String indexOfValue, String key, LocalDate def) {
		return get(indexOfKey, indexOfValue, key, def, LocalDate::parse);
	}
	public List<LocalDate> localDates(String key) {
		return list(key, LocalDate::parse);
	}
	
	/** Convenience method to return a LocalDateTime */
	public LocalDateTime localDateTime(String key) {
		return get(0, key, null, LocalDateTime::parse);
	}
	public LocalDateTime localDateTime(String key, LocalDateTime def) {
		return get(0, key, def, LocalDateTime::parse);
	}
	public LocalDateTime localDateTime(int idx, String key) {
		return get(idx, key, null, LocalDateTime::parse);
	}
	public LocalDateTime localDateTime(int idx, String key, LocalDateTime def) {
		return get(idx, key, def, LocalDateTime::parse);
	}
	public LocalDateTime localDateTime(String indexOfKey, String indexOfValue, String key) {
		return get(indexOfKey, indexOfValue, key, null, LocalDateTime::parse);
	}
	public LocalDateTime localDateTime(String indexOfKey, String indexOfValue, String key, LocalDateTime def) {
		return get(indexOfKey, indexOfValue, key, def, LocalDateTime::parse);
	}
	public List<LocalDateTime> localDateTimes(String key) {
		return list(key, LocalDateTime::parse);
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
	public TECL grp(int idx, String id) {
		TECL tecl = groups.get(idx, id, null);
		
		// if not found, maybe we have variable
		// I do not like this crossing over to properties, but it is needed to resolve a variable referring to a group.
		if (tecl == null) {
			String value = properties.get(idx, id, null);
			if (value != null && value.trim().startsWith("$")) {
				tecl = var(value);
			}
		}
		
		// if not found, return an empty group so we don't get NPE's
		if (tecl == null) {
			tecl = new TECL("<group '" + createFullPathToKey(idx, id) + "' does not exist>");
			tecl.setParent(this, -1);
		}
		return tecl;
	}
	
	/**
	 * Get all groups for a key.
	 * @param id
	 * @return
	 */
	public List<TECL> grps(String id) {
		List<TECL> tecls = groups.get(id);
		
		// if nothing found, maybe we have variable
		// I do not like this crossing over to properties, but it is needed to resolve a variable referring to a group.
		if (tecls.isEmpty()) {
			String value = properties.get(0, id, null);
			if (value != null && value.trim().startsWith("$")) {
				tecls = vars(value);
			}
		}
		
		return tecls;
	}

	// =====================================
	// REFERENCE
	
	/**
	 * Use a path notation to access a property
	 * - Start at the root: $groupId.groupId2.property
	 * - Starting at the current node: $.property
	 * - Via an index $groupId.groupId2.property[2]
	 */
	public <R> R var(String address, R def, Function<String, R> convertFunction) {
		TokenContext tokenContext = resolve(address, false);
		return tokenContext.tecl.get(tokenContext.idx == null ? 0 : tokenContext.idx.intValue(), tokenContext.token, def, convertFunction);
	}
	
	/**
	 * Use a path notation to access a list
	 */
	public <R> List<R> vars(String address, Function<String, R> convertFunction) {
		TokenContext tokenContext = resolve(address, false);
		return tokenContext.tecl.list(tokenContext.token,  convertFunction);
	}
	
	/**
	 * Use a path notation to access a group
	 * - Start at the root: $groupId.groupId2
	 * - Starting at the current node: $.groupID
	 * - Via an index $groupId.groupId2
	 */
	public TECL var(String address) {
		TokenContext tokenContext = resolve(address, true); // resolve groups all the way to the last token, so we add one to act as the leaf property 
		return tokenContext.tecl;
	}
	
	/**
	 * Use a path notation to access groups
	 */
	public List<TECL> vars(String address) {
		TokenContext tokenContext = resolve(address, true); // resolve groups all the way to the last token, so we add one to act as the leaf property 
		return tokenContext.tecls;
	}
	
	private TokenContext resolve(String address, boolean lastTokenIsAGroup) {
		if (!address.startsWith("$")) {
			throw new IllegalArgumentException("Variables must start with a $");
		}
	
		// First split into its parts
		List<String> tokens = new StringTokenizer(address, ".").getTokenList();
		logger.atDebug().log("var tokenized: "  + tokens);
		
		// Determine the starting point
		String token = tokens.get(0);
		logger.atDebug().log("first token= "  + token);
		TECL tecl = null;
		if ("$".equals(token)) {
			// This means the address started with "$."
			tecl = this;
			token = tokens.remove(0);
			logger.atDebug().log("start at current tecl, tokens =" + tokens);
		}
		else {
			tecl = this.getRoot();
			token = token.substring(1);
			tokens.set(0, token);
			logger.atDebug().log("start at root, tokens =" + tokens);
		}
		
		// Navigate through the tree
		Integer idx = null;
		List<TECL> tecls = null;
		while (!tokens.isEmpty()) { 
			
			// separate an index
			token = tokens.remove(0);
			logger.atDebug().log("token= "  + token);
			idx = null;
			if (token.endsWith("]")) {
				int startIdx = token.indexOf("[");
				idx = Integer.parseInt(token.substring(startIdx + 1, token.length() - 1));
				token= token.substring(0, startIdx);				
			}
			
			// all intermediate tokens are groups, the last token can be property or group
			boolean lastToken = tokens.isEmpty();
			if (!lastToken || lastTokenIsAGroup) {
				if ("^".equals(token)) {
					tecl = tecl.parent;
				}
				else {
					if (lastToken && idx == null) {
						tecls = tecl.grps(token);
					}
					tecl = tecl.grp(idx == null ? 0 : idx.intValue(), token);				
				}
				logger.atDebug().log("TECL= "  + tecl.getPath());
			}
		} 
		
		// final token
		return new TokenContext(tecl, tecls, token, idx);
	}
	class TokenContext {
		final TECL tecl;
		final List<TECL> tecls;
		final String token;
		final Integer idx;
		
		TokenContext(TECL tecl, List<TECL> tecls, String token, Integer idx) {
			this.tecl = tecl;
			this.tecls = tecls;
			this.token = token;
			this.idx = idx;
		}
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
				values.clear();;
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
			values = new ArrayList<T>(values);
			values.remove(null);
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
	
	public String toString() {
		return getPath();
	}
}
