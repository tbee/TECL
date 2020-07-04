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
 * - encrypted strings
 * - lists List<String> hosts = tecl.strs("hosts");
 * - slf4j
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
	
	/*
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
	
	public void setProperty(int idx, String key, String value) {
		properties.set(idx, key, value, false);
	}	
	public void setProperty(int idx, String key, String value, boolean allowOverwrite) {
		properties.set(idx, key, value, allowOverwrite);
	}	
	public void clearProperty(String key) {
		properties.clear(key);
	}	
	
	public int indexOf(String key, String value) {
		return properties.indexOf(key, value);		
	}
	
	public int count(String key) {
		return properties.count(key);		
	}

	// GET: methods that have a convert function to change the String value to whatever is wanted
	public <R> R get(String key, Function<String, R> convertFunction) {
		String value = properties.get(0, key, null);
		return optionallResolveVar(value, null, convertFunction);
	}
	public <R> R get(String key, R def, Function<String, R> convertFunction) {
		String value = properties.get(0, key, null);
		return optionallResolveVar(value, def, convertFunction);
	}
	public <R> R get(int idx, String key, Function<String, R> convertFunction) {
		String value = properties.get(idx, key, null);
		return optionallResolveVar(value, null, convertFunction);
	}
	public <R> R get(int idx, String key, R def, Function<String, R> convertFunction) {
		String value = properties.get(idx, key, null);
		return optionallResolveVar(value, def, convertFunction);
	}
	public <R> R get(String indexOfKey, String indexOfValue, String key, Function<String, R> convertFunction) {
		String value = properties.get(indexOfKey, indexOfValue, key, null);
		return optionallResolveVar(value, null, convertFunction);
	}
	public <R> R get(String indexOfKey, String indexOfValue, String key, R def, Function<String, R> convertFunction) {
		String value = properties.get(indexOfKey, indexOfValue, key, null);
		return optionallResolveVar(value, def, convertFunction);
	}
	private <R> R optionallResolveVar(String value, R def, Function<String, R> convertFunction) {
		if (value == null) {
			return def;
		}
		if (value.startsWith("$")) {
			return var(value, def, convertFunction);
		}
		else {
			value = sanatizeAssignment(value);
		}
		R returnvalue = convert(value, convertFunction);
		return returnvalue;		
	}
	private <R> R convert(String value, Function<String, R> convertFunction) {
		try {
			return convertFunction.apply(value);
		}
		catch (Exception e) {
			throw new ParseException(e);
		}
	}
	
	// STR
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
	
	// INTEGER
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
	
	// DOUBLE
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
	
	// LocalDate
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
	
	// LocalDateTime
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
	
	// =====================================
	// groups
	
	private final IndexedValues<TECL> groups = new IndexedValues<>();
	
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

	public int countGrp(String key) {
		return groups.count(key);		
	}

	public TECL grp(String id) {
		return grp(0, id);
	}
	
	public TECL grp(int idx, String id) {
		TECL tecl = groups.get(idx, id, null);
		if (tecl == null) {
			tecl = new TECL("<group '" + id + "' does not exist>");
			tecl.setParent(this, -1);
		}
		return tecl;
	}
	
	public List<TECL> grps(String id) {
		List<TECL> tecls = groups.get(id);
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
		TokenContext tokenContext = resolve(address);
		return tokenContext.tecl.get(tokenContext.token, def, convertFunction);
	}
	
	/**
	 * Use a path notation to access a property
	 * - Start at the root: $groupId.groupId2
	 * - Starting at the current node: $.groupID
	 * - Via an index $groupId.groupId2
	 */
	public TECL var(String address) {
		TokenContext tokenContext = resolve(address + ".<dummy>"); // resolve groups all the way to the last token, so we add one to act as the final property 
		return tokenContext.tecl;
	}
	
	private TokenContext resolve(String address) {
		if (!address.startsWith("$")) {
			throw new IllegalArgumentException("Variables must start with a $");
		}
	
		// First split into its parts
		List<String> tokens = new StringTokenizer(address, ".").getTokenList();
		logger.atDebug().log("var tokenized: "  + tokens);
		
		// Determine the starting point
		String token = tokens.remove(0);
		logger.atDebug().log("first token= "  + token);
		TECL tecl = null;
		if ("$".equals(token)) {
			// This means the address started with "$."
			tecl = this;
			token = tokens.remove(0);
			logger.atDebug().log("start at current tecl, token= "  + token);
		}
		else {
			tecl = this.getRoot();
			token = token.substring(1);
			logger.atDebug().log("start at root, token= "  + token);
		}
		
		// Navigate
		do {
			// separate an index
			token = token.trim();
			int idx = 0;
			if (token.endsWith("]")) {
				int startIdx = token.indexOf("[");
				idx = Integer.parseInt(token.substring(startIdx + 1, token.length() - 1));
				token= token.substring(0, startIdx);				
			}
			
			// navigate
			if ("parent".equals(token)) {
				tecl = tecl.parent;
			}
			else {
				tecl = tecl.grp(idx, token);				
			}
			
			// next token
			token = tokens.remove(0);
			logger.atDebug().log("TECL= "  + tecl.getPath());
			logger.atDebug().log("Next token= "  + token);
		} while (tokens.size() > 0);
		
		// final token
		return new TokenContext(tecl, token);
	}
	class TokenContext {
		final TECL tecl;
		final String token;
		
		TokenContext(TECL tecl,String token) {
			this.tecl = tecl;
			this.token = token;
		}
	}
	
	// =====================================
	// SUPPORT
	
	/**
	 * Implements an indexed store
	 */
	private class IndexedValues<T> {
		private final Map<String, List<T>> keyTovaluesMap = new LinkedHashMap<>();
		
		void clear(String key) {
			List<T> values = keyTovaluesMap.get(key);
			if (values != null) {
				values.clear();;
			}
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
				throw new IllegalStateException("WARN: " + createFullPath(idx, key) + " value is overwritten! " + oldValue + " -> " + value);
			}
			//logger.atDebug().log("@"  + id + ": Adding property "  + key + " = " + value);
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
		
		/*
		 * Get method using indexOf to determine the index first
		 */
		T get(String indexOfKey, T value, String key, T def) {
			int idx = indexOf(indexOfKey, value);
			if (idx < 0) {
				return null;
			}
			return get(idx, key, def);
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
	private String sanatizeAssignment(String s) {
//		if (!(t instanceof String)) {
//			return t;
//		}
//		String s = ""  + t;
		
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
	

}
