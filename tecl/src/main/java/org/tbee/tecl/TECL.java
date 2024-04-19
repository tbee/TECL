package org.tbee.tecl;

import java.io.IOException;

/*-
 * #%L
 * TECL
 * %%
 * Copyright (C) 2020 Tom Eugelink
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>For a more detail explanation refer to the <a href="https://bitbucket.org/tbee/tecl/src/master/" target="_blank">README</a></p>
 *   
 * <p>Basic usage:
 * <pre>{@code
 *     TECL tecl = TECL.parser()
 *         .addParameter("env", "production") // Optional
 *         .schema("..TESD file.." ) // Optional
 *         .parse("..TECL file..");
 *     String title = tecl.str("title");
 *     int timeout = tecl.integer("/servers/settings[3]/timeout", 1000);  
 * }</pre>
 * </p>
 *   
 * <p>Quick usage:
 * <pre>{@code
 *     TECL tecl = TECL.parser().findAndParse();
 *     String title = tecl.str("title");
 *     int timeout = tecl.integer("/servers/settings[3]/timeout", 1000);  
 * }</pre>
 * </p>
 */
public class TECL {
	private final static String ENV_PREFIX = "env@";
	private final static String SYS_PREFIX = "sys@";

	final Logger logger = LoggerFactory.getLogger(TECL.class);
	
	static Map<Class<?>, BiFunction<String, ?, ?>> buildinConvertFunctions = new HashMap<>();
	static {
		buildinConvertFunctions.put(String.class, (s, d) -> s);
		buildinConvertFunctions.put(Integer.class, (s, d) -> s.isBlank() ? d : Integer.valueOf(s));
		buildinConvertFunctions.put(BigInteger.class, (s, d) -> s.isBlank() ? d : new BigInteger(s));
		buildinConvertFunctions.put(BigDecimal.class, (s, d) -> s.isBlank() ? d : new BigDecimal(s));
		buildinConvertFunctions.put(Boolean.class, (s, d) -> s.isBlank() ? d : Boolean.valueOf(s));
		buildinConvertFunctions.put(Double.class, (s, d) -> s.isBlank() ? d : Double.valueOf(s));
		buildinConvertFunctions.put(LocalDate.class, (s, d) -> s.isBlank() ? d : LocalDate.parse(s));
		buildinConvertFunctions.put(LocalDateTime.class, (s, d) -> s.isBlank() ? d : LocalDateTime.parse(s));
		buildinConvertFunctions.put(URI.class, (s, d) -> s.isBlank() ? d : toRuntimeException(() -> new URI(s)));
		buildinConvertFunctions.put(URL.class, (s, d) -> s.isBlank() ? d : toRuntimeException(() -> new URL(s)));
	}

	static <T> T toRuntimeException(Callable<T> callable) {
        try {
            return callable.call();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
	
	// =====================================
	// parser
	
	/**
	 * 
	 */
	static public TECLParser parser() {
		return new TECLParser();
	}
	
	/**
	 * Will try to find a config.tecl and then parse that.
	 * First match will be used:
	 * 1. See if system property config.tecl is defined (-Dconfig.tecl)
	 * 2. See if environment variable config_tecl is defined
	 * 3. See if the file ./config.tecl exists
	 * 4. See if we can open the resource /config.tecl
	 * 
	 * Assuming the config.tecl is encoded in UTF-8.
	 * 
	 * @return TECL or null if not found, may throw RuntimeException containing a IOException
	 * 
	 * @see TECLParser
	 */
	static public TECL findAndParse() {
		try {
			return parser().findAndParse();
		} 
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// =====================================
	// Constructor
	
	public TECL(String id) {
		this.id = id;
		populateConvertFunctions();
	}
	private final String id;

	TECL(String id, TECL parent) {
		this(id);
		if (parent != null) {
			setParent(parent, 0);
		}
	}

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
	
	public String createFullPathToKey(int idx, String key) {
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
	
	/**
	 * Is the TECL empty; no properties, no groups
	 * @return
	 */
	public boolean isEmpty() {
		return properties.isEmpty() && groups.isEmpty();
	}

	/**
	 * Return all the keys
	 * @return
	 */
	public List<String> keys() {
		return new ArrayList<>(properties.keyTovaluesMap.keySet());
	}

	// =====================================
	// get
	
	
	/**
	 * Get a value using a directory-style path, like /group1/group2[4]/value
	 * 
	 * @param <R>
	 * @param path the path the access
	 * @param clazz the return type (also used to get appropriate convert function)
	 * @return a list of found values
	 */
	public <R> R get(String path, Class<R> clazz) {
		return get(path, null, clazz);
	}

	/**
	 * Get a value using a directory-style path, like /group1/group2[4]/value
	 * 
	 * @param <R>
	 * @param path the path the access
	 * @param def
	 * @param clazz the return type (also used to get appropriate convert function)
	 * @return a list of found values
	 */
	public <R> R get(String path, R def, Class<R> clazz) {
		List<R> list = list(path, clazz);
		if (list.isEmpty()) {
			return def;
		}
		return list.get(0);
	}

	/**
	 * Get a value using a directory-style path, like /group1/group2[4]/value
	 * 
	 * @param <R>
	 * @param path the path the access
	 * @param def
	 * @param convertFunction
	 * @return a list of found values
	 */
	public <R> R getUsingFunction(String path, R def, BiFunction<String, R, R> convertFunction) {
		List<R> list = listUsingFunction(path, Arrays.asList(def), convertFunction);
		return list.isEmpty() ? def : list.get(0);
	}
	
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
	 * @param clazz
	 * @return
	 */
	public <R> List<R> list(String indexOfPath, String indexOfValue, String path, List<R> def, Class<R> clazz) {
		int idx = strs(indexOfPath).indexOf(indexOfValue);
		if (idx < 0) {
			return def;
		}
		return listUsingFunction(path + "[" + idx + "]", def, convertFunction(clazz));
	}
	
	/**
	 * Get a value using a directory-style path, like /group1/group2[4]/value
	 * 
	 * @param <R>
	 * @param path the path the access
	 * @param clazz the return type (also used to get appropriate convert function)
	 * @return a list of found values
	 */
	public <R> List<R> list(String path, Class<R> clazz) {
		return list(path, Collections.emptyList(), clazz);
	}
	
	/**
	 * Get a value using a directory-style path, like /group1/group2[4]/value
	 * 
	 * This is the main way to access values in TECL, all convenience methods use this method
	 * 
	 * @param <R>
	 * @param path the path the access
	 * @param def the value to return if nothing is found
	 * @param clazz the return type (also used to get appropriate convert function)
	 * @return a list of found values
	 */
	public <R> List<R> list(String path, List<R> def, Class<R> clazz) {
		return listUsingFunction(path, def, convertFunction(clazz));
	}
	
	/**
	 * Get values using a directory-style path, like /group1/group2[4]/value
	 * 
	 * This is the main way to access values in TECL, all convenience methods use this method
	 * 
	 * @param <R>
	 * @param path the path the access
	 * @param def the value to return if nothing is found
	 * @param convertFunction the conversion function to convert properties to their end type, if you access groups this should be null
	 * @return a list of found values
	 */
	@SuppressWarnings("unchecked")
	public <R> List<R> listUsingFunction(String path, List<R> def, BiFunction<String, R, R> convertFunction) {
		String context = this.getPath() + " -> " + path + ": ";
		
		// Specials 
		if (path.startsWith(ENV_PREFIX)) {
			return getEnv(path, convertFunction, context);
		}
		if (path.startsWith(SYS_PREFIX)) {
			return getSys(path, convertFunction, context);
		}
		
		// Travel the TECL tree using the path
		EndNode endNode = travelHierarchy(path, context);
		final TECL tecl = endNode.tecl;
		final String node = endNode.node;
		final List<Integer> idxs = endNode.idxs;
		context = tecl.getPath() + " -> " + path + ": ";
		
		// This is the last node, it may be a property, group, list or reference
		// First get all relevant info
		List<ValueAttibutesPair<String>> valueAttibutesPairs = tecl.properties.get(node);
		List<String> properties = (valueAttibutesPairs == null ? null : valueAttibutesPairs.stream()
				.map(vap -> vap == null ? null : vap.value)
				.collect(Collectors.toList()));
		if (logger.isDebugEnabled()) logger.debug(context + "Properties = " + properties);
		List<TECL> groups = tecl.groups.get(node); 
		if (logger.isDebugEnabled()) logger.debug(context + "Groups = " + groups);
		List<TECL> list = tecl.groups.get("|" + node + "|"); 
		if (logger.isDebugEnabled()) logger.debug(context + "Lists = " + list);
		
		// Construct the results
		List<R> results = null;
		Integer idx = (idxs.isEmpty() ? null : idxs.get(0));
		// If there is no convertFunction, then the result are groups
		if (convertFunction == null) {
			if (logger.isDebugEnabled()) logger.debug(context + "There no convert function, so the last token must be groups.");			
			results = resolveFinalGroup(properties, groups, idx, context);
		}
		else {
			if (logger.isDebugEnabled()) logger.debug(context + "There is a convert function, so the last token must be properties.");			
			results = resolveFinalProperty(node, properties, list, idx, convertFunction, def, context);
		}	
		if (logger.isDebugEnabled()) logger.debug(context + "Results: " + results);

		// secondary index (for lists)
		Integer idx1 = (idxs.size() <= 1 ? null : idxs.get(1));
		results = optionallyApplyIdx(context, results, idx1);
		
		// done
		return results.isEmpty() ? def : results;
	}

	/* */
	private <R> List<R> getSys(String path, BiFunction<String, R, R> convertFunction, String context) {
		String sys = path.substring(SYS_PREFIX.length());
		R result = convertFunction.apply(System.getProperty(sys), null);
		if (logger.isDebugEnabled()) logger.debug(context + "sys path, result = " + result);
		return Arrays.asList(result);
	}

	/* */
	private <R> List<R> getEnv(String path, BiFunction<String, R, R> convertFunction, String context) {
		String env = path.substring(ENV_PREFIX.length());
		R result = convertFunction.apply(System.getenv(env), null);
		if (logger.isDebugEnabled()) logger.debug(context + "env path, result = " + result);
		return Arrays.asList(result);
	}

	/* */
	private EndNode travelHierarchy(String path, String context) {

		// Determine the starting point
		TECL tecl = determineStartingPoint(path, context);
		context = tecl.getPath() + " -> " + path + ": ";
		
		// First split into its parts
		List<String> nodes = new ArrayList<>(new StringTokenizer(path, "/").getTokenList());
		if (logger.isDebugEnabled()) logger.debug(context + "path tokenized: " + nodes);
		
		// step over the nodes in the path
		String node = null;
		List<Integer> idxs = new ArrayList<Integer>();
		while (!nodes.isEmpty()) { 
			
			// Get current node
			node = nodes.remove(0);
			if (logger.isDebugEnabled()) logger.debug(context + "node = "  + node);
			context = tecl.getPath() + node + ": ";
			
			// extract the indexes from the node (if any, may be two)
			node = extractIdxs(node, idxs);
			if (logger.isDebugEnabled()) logger.debug(context + "node = "  + node + ", idxs = "  + idxs);
			
			// Is this the last token? 
			// If so, break out, because the travel part is done (the end node is handled differently)
			boolean lastToken = nodes.isEmpty();
			if (lastToken) {
				break;
			}
			
			// Not the last token, get the properties for this node
			List<String> properties = tecl.properties.get(node).stream().map(vap -> vap.value).collect(Collectors.toList());;
			int idx = (idxs.isEmpty() ? 0 : idxs.get(0));
			
			// This either is a group or a reference resolving to a group
			// If it is a reference
			if (isReference(properties, idx)) {
				
				// it must be a list of groups at this point
				List<TECL> tecls = resolveReference(properties, idx, notExistingGroup(idx), null, context);
				tecl = tecls.get(0);
				if (logger.isDebugEnabled()) logger.debug(context + "Resolved reference: TECL= " + tecl.getPath());
				continue;
			}
			
			// If it is a goto parent
			if ("..".equals(node)) {
				tecl = tecl.getParent();
				if (logger.isDebugEnabled()) logger.debug(context + "Assumed group, TECL= " + tecl.getPath());
				continue;
			}
			
			// It must a the name of a group then
			tecl = tecl.grp(idx, node);
			if (logger.isDebugEnabled()) logger.debug(context + "Assumed group, TECL= " + tecl.getPath());
		}

		// return the node we ended on
		EndNode endNode = new EndNode();
		endNode.tecl = tecl;
		endNode.node = node;
		endNode.idxs = idxs;
		return endNode;
	}
	class EndNode {
		TECL tecl;
		String node;
		List<Integer> idxs;
	}

	/* */
	private TECL determineStartingPoint(String path, String context) {
		TECL tecl = null;
		if (path.startsWith("/")) {
			tecl = this.getRoot();
			if (logger.isDebugEnabled()) logger.debug(context + "start at root, tecl = " + tecl);
		}
		else {
			tecl = this;
			if (logger.isDebugEnabled()) logger.debug(context + "start at current, tecl = " + tecl);
		}
		return tecl;
	}

	/* */
	private <R> List<R> resolveFinalGroup(List<String> properties, List<TECL> groups, Integer idx, String context) {
		
		// If we have a reference overlapping the groups
		if (groups.isEmpty() && isReference(properties, 0)) {
			
			// Resolve the reference
			if (logger.isDebugEnabled()) logger.debug(context + "We have no groups, but we do a single property which is a reference: " + properties);					
			List<R> results = resolveReference(properties, idx, null, null, context);
			return results;
		}
			
		// The result are the groups
		groups = optionallyApplyIdx(context, groups, idx);
		List<R> results = (List<R>) groups;
		return results;
	}

	/* */
	private <R> List<R> resolveFinalProperty(String node, List<String> properties, List<TECL> list, Integer idx, BiFunction<String, R, R> convertFunction, List<R> def, String context) {
		
		// If there is a reference
		if (isReference(properties, 0)) {
			if (logger.isDebugEnabled()) logger.debug(context + "We have a single property which is a reference, going to resolve that: " + properties);					
			List<R> results = resolveReference(properties, 0, null, convertFunction, context);
			results = optionallyApplyIdx(context, results, idx);
			return results;
		}
			
		// If we have a list overlapping the properties, replace the properties with those in the list
		if (idx != null && list.size() > idx && list.get(idx) != null) {
			properties = list.get(idx).properties.get(node).stream().map(vap -> vap.value).collect(Collectors.toList());
			if (logger.isDebugEnabled()) logger.debug(context + "There is an overlapping list, replaced properties with its contents. Properties = " + properties);
		}
		
		// Apply the index
		properties = optionallyApplyIdx(context, properties, idx);
	
		// Convert to end value
		List<R> results = new ArrayList<R>();
		for (String property : properties) {
			
			// But each property can be a reference again
			if (isReference(property)) {
				
				// Resolve reference
				if (logger.isDebugEnabled()) logger.debug(context + "Property is a reference: " + property);
				List<R> varResult = resolveReference(property, null, convertFunction, context);
				results.addAll(varResult);
			}
			else {
				
				// Convert property to end type
				property = sanatizeString(property);
				R result = convertFunction.apply(property, def == null || def.isEmpty() ? null : def.get(0));
				if (logger.isDebugEnabled()) logger.debug(context + "Property converted: " + property + " -> "  + result);
				results.add(result);
			}
		};
		return results;
	}

	/*
	 * 
	 */
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
	
	/* */
	private <R> List<R> optionallyApplyIdx(String context, List<R> list, Integer idx) {
		if (idx != null && list.size() > idx) {
			R result = list.get(idx);
			list = new ArrayList<R>();
			list.add(result);
			if (logger.isDebugEnabled()) logger.debug(context + "Limit to idx = " + idx + ", result: " + list);
		}
		return list;
	}
	
	/* */
	private boolean isReference(List<String> properties, int idx) {
		return properties != null // 
		    && properties.size() > idx // 
		    && properties.get(idx) != null // 
		    && properties.get(idx).startsWith("$");
	}
	
	/* */
	private boolean isReference(String property) {
		return property != null && property.startsWith("$");
	}
	
	/* */
	private <T, R> T resolveReference(List<String> properties, Integer idx, List<R> def, BiFunction<String, R, R> convertFunction, String context) {
		if (idx == null) {
			idx = 0;
		}
		String var = properties.get(idx);
		return resolveReference(var, def, convertFunction, context);
	}
	
	/* */
	private <T, R> T resolveReference(String var, List<R> def, BiFunction<String, R, R> convertFunction, String context) {
		if (logger.isDebugEnabled()) logger.debug(context + "Found reference: " + var);
		var = var.substring(1); // strip "$"
		T t = (T)listUsingFunction(var, def, convertFunction);
		if (logger.isDebugEnabled()) logger.debug(context + "Resolved reference: " + var + " -> " + t);
		return t;
	}
	
	// =====================================
	// properties
	
	private class ValueAttibutesPair<T> {
		T value;
		TECL attributes;
		
		ValueAttibutesPair(T value, TECL attributes) {
			this.value = value;
			this.attributes = attributes;
			this.attributes.convertFunctions = TECL.this.getRoot().convertFunctions; // use the convertFunctions from the official TECL tree
		}
		ValueAttibutesPair(T value, List<Attribute> attributes) {
			this(value, convertToTECL(attributes));
		}	
		ValueAttibutesPair(T value) {
			this(value, EMPTY_ATTRIBUTES);
		}	
		
		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			if (o == null || !(o instanceof ValueAttibutesPair)) {
				return false;
			}
			return value.equals(((ValueAttibutesPair)o).value);
		}
		
		@Override
		public String toString() {
			return "" + value + (attributes == EMPTY_ATTRIBUTES ? "" : attributes.toString());
		}
	}
	static private TECL convertToTECL(List<Attribute> attributes) {
		if (attributes == null || attributes.isEmpty()) {
			return EMPTY_ATTRIBUTES;
		}
		TECL tecl = new TECL("");
		attributes.forEach(a -> tecl.setProperty(a.key, a.value, null)); // Do we need to support multiple attributes (x=0 x=1 x=2 using something like list("x").size as index (1st parameter)? 
		return tecl;
	}
	final static private TECL EMPTY_ATTRIBUTES = new TECL("");
	
	
	final IndexedValues<ValueAttibutesPair<String>> properties = new IndexedValues<>();
	
	/**
	 * Set a property value. This is identical to setting an indexed property with index 0.
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, String value, List<Attribute> attributes) {
		properties.set(0, key, new ValueAttibutesPair<String>(value, attributes), false);
	}	
	
	/**
	 * Set an indexed property value. You cannot override a value.
	 * @param idx
	 * @param key
	 * @param value
	 * @param attributes 
	 */
	public void setProperty(int idx, String key, String value, List<Attribute> attributes) {
		properties.set(idx, key, new ValueAttibutesPair<String>(value, attributes), false);
	}
	
	/**
	 * Set an indexed property value. 
	 * @param idx
	 * @param key
	 * @param value
	 * @param allowOverwrite
	 * @param attributes 
	 */
	public void setProperty(int idx, String key, String value, boolean allowOverwrite, List<Attribute> attributes) {
		properties.set(idx, key, new ValueAttibutesPair<String>(value, attributes), allowOverwrite);
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
		return properties.indexOf(key, new ValueAttibutesPair<String>(value));		
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
	 * This is a last resort when, for example, there is an overlap using references.
	 * But these situations preferably are prevented.
	 * 
	 * @param idx
	 * @param key
	 * @param def
	 * @return
	 */
	public String raw(int idx, String key, String def) {
		ValueAttibutesPair<String> valueAttibutesPair = properties.get(idx, key, null);
		return valueAttibutesPair == null ? null : valueAttibutesPair.value;
	}
	
	/**
	 * Get the raw uninterpreted value of a property.
	 * This is a last resort when, for example, there is an overlap using references.
	 * But these situations preferably are prevented.
	 * 
	 * @param idx
	 * @param key
	 * @return
	 */
	public TECL attr(int idx, String key) {
		ValueAttibutesPair<String> valueAttibutesPair = properties.get(idx, key, null);
		return valueAttibutesPair == null ? new TECL("", this) : valueAttibutesPair.attributes;
	}
	public TECL attr(String key) {
		return attr(0, key);
	}
	
	/**
	 * Add a custom convert function to the tecl
	 */
	public <R> void addConvertFunction(Class<R> clazz, BiFunction<String, R, R> convertFunction) {
		convertFunctions.put(clazz, convertFunction);
	}
	@SuppressWarnings("unchecked")
	public <R> BiFunction<String, R, R> convertFunction(Class<R> clazz) {
		return (BiFunction<String, R, R>)getRoot().convertFunctions.get(clazz);
	}
	public void populateConvertFunctions() {
		convertFunctions = new HashMap<>(buildinConvertFunctions);
	}
	Map<Class<?>, BiFunction<String, ?, ?>> convertFunctions = null;
	
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
		return list(key + "[" + idx + "]", asList(def), String.class).get(0);
	}
	public List<String> strs(String key) {
		return list(key, Collections.emptyList(), String.class);
	}
	public String str(String indexOfKey, String indexOfValue, String key, String def) {
		return list(indexOfKey, indexOfValue, key, asList(def), String.class).get(0);
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
		return list(key + "[" + idx + "]", asList(def), Integer.class).get(0);
	}
	public List<Integer> integers(String key) {
		return list(key, Collections.emptyList(), Integer.class);
	}
	public Integer integer(String indexOfKey, String indexOfValue, String key, Integer def) {
		return list(indexOfKey, indexOfValue, key, asList(def), Integer.class).get(0);
	}

	/** Convenience method to return an Double */
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
		return list(key + "[" + idx + "]", asList(def), Double.class).get(0);
	}
	public List<Double> dbls(String key) {
		return list(key, Collections.emptyList(), Double.class);
	}
	public Double dbl(String indexOfKey, String indexOfValue, String key, Double def) {
		return list(indexOfKey, indexOfValue, key, asList(def), Double.class).get(0);
	}

	/** Convenience method to return an Boolean */
	public Boolean bool(String key) {
		return bool(0, key, null);
	}
	public Boolean bool(String key, Boolean def) {
		return bool(0, key, def);
	}
	public Boolean bool(int idx, String key) {
		return bool(idx, key, null);
	}
	public Boolean bool(int idx, String key, Boolean def) {
		return list(key + "[" + idx + "]", asList(def), Boolean.class).get(0);
	}
	public List<Boolean> bools(String key) {
		return list(key, Collections.emptyList(), Boolean.class);
	}
	public Boolean bool(String indexOfKey, String indexOfValue, String key, Boolean def) {
		return list(indexOfKey, indexOfValue, key, asList(def), Boolean.class).get(0);
	}

	/** Convenience method to return a BigInteger */
	public BigInteger bi(String key) {
		return bi(0, key, null);
	}
	public BigInteger bi(String key, BigInteger def) {
		return bi(0, key, def);
	}
	public BigInteger bi(int idx, String key) {
		return bi(idx, key, null);
	}
	public BigInteger bi(int idx, String key, BigInteger def) {
		return list(key + "[" + idx + "]", asList(def), BigInteger.class).get(0);
	}
	public List<BigInteger> bis(String key) {
		return list(key, Collections.emptyList(), BigInteger.class);
	}
	public BigInteger bi(String indexOfKey, String indexOfValue, String key, BigInteger def) {
		return list(indexOfKey, indexOfValue, key, asList(def), BigInteger.class).get(0);
	}


	/** Convenience method to return a BigDecimal */
	public BigDecimal bd(String key) {
		return bd(0, key, null);
	}
	public BigDecimal bd(String key, BigDecimal def) {
		return bd(0, key, def);
	}
	public BigDecimal bd(int idx, String key) {
		return bd(idx, key, null);
	}
	public BigDecimal bd(int idx, String key, BigDecimal def) {
		return list(key + "[" + idx + "]", asList(def), BigDecimal.class).get(0);
	}
	public List<BigDecimal> bds(String key) {
		return list(key, Collections.emptyList(), BigDecimal.class);
	}
	public BigDecimal bd(String indexOfKey, String indexOfValue, String key, BigDecimal def) {
		return list(indexOfKey, indexOfValue, key, asList(def), BigDecimal.class).get(0);
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
		return list(key + "[" + idx + "]", asList(def), LocalDate.class).get(0);
	}
	public List<LocalDate> localDates(String key) {
		return list(key, Collections.emptyList(), LocalDate.class);
	}
	public LocalDate localDate(String indexOfKey, String indexOfValue, String key, LocalDate def) {
		return list(indexOfKey, indexOfValue, key, asList(def), LocalDate.class).get(0);
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
		return list(key + "[" + idx + "]", asList(def), LocalDateTime.class).get(0);
	}
	public List<LocalDateTime> localDateTimes(String key) {
		return list(key, Collections.emptyList(), LocalDateTime.class);
	}
	public LocalDateTime localDateTime(String indexOfKey, String indexOfValue, String key, LocalDateTime def) {
		return list(indexOfKey, indexOfValue, key, asList(def), LocalDateTime.class).get(0);
	}

	/** Convenience method to return a URI */
	public URI uri(String key) {
		return uri(0, key, null);
	}
	public URI uri(String key, URI def) {
		return uri(0, key, def);
	}
	public URI uri(int idx, String key) {
		return uri(idx, key, null);
	}
	public URI uri(int idx, String key, URI def) {
		return list(key + "[" + idx + "]", asList(def), URI.class).get(0);
	}
	public List<URI> uris(String key) {
		return list(key, Collections.emptyList(), URI.class);
	}
	public URI uri(String indexOfKey, String indexOfValue, String key, URI def) {
		return list(indexOfKey, indexOfValue, key, asList(def), URI.class).get(0);
	}

	/** Convenience method to return a URL */
	public URL url(String key) {
		return url(0, key, null);
	}
	public URL url(String key, URL def) {
		return url(0, key, def);
	}
	public URL url(int idx, String key) {
		return url(idx, key, null);
	}
	public URL url(int idx, String key, URL def) {
		return list(key + "[" + idx + "]", asList(def), URL.class).get(0);
	}
	public List<URL> urls(String key) {
		return list(key, Collections.emptyList(), URL.class);
	}
	public URL url(String indexOfKey, String indexOfValue, String key, URL def) {
		return list(indexOfKey, indexOfValue, key, asList(def), URL.class).get(0);
	}

	// =====================================
	// decrypt

	// Storage
	private String decryptKeyBase64 = null;
	void setDecryptKeyBase64(String decryptKeyBase64) {
		this.decryptKeyBase64 = decryptKeyBase64;
	}
	
	/** Methods for decrypting */
	public String decrypt(String key) {
		return decrypt(0, key, null);
	}
	public String decrypt(String key, String def) {
		return decrypt(0, key, def);
	}
	public String decrypt(int idx, String key) {
		return decrypt(idx, key, null);
	}
	public String decrypt(int idx, String key, String def) {
		String str = str(idx, key, def);
		return decryptOnly(str);
	}	
	public String decrypt(String indexOfKey, String indexOfValue, String key, String def) {
		String str = str(indexOfKey, indexOfValue, key, def);
		return decryptOnly(str);
	}

	/**
	 * Provide a value to decrypt directly
	 * 
	 * @param encryptedBase64
	 * @return
	 */
	public String decryptOnly(String encryptedBase64) {
		String decryptKeyBase64 = getRoot().decryptKeyBase64;
		if (decryptKeyBase64 == null) {
			throw new IllegalStateException("No decrypt key set. \n1) Generate a key pair using the EncrpytionHelper class (has a main, just start it). \n2) Encrypt the value with the public key using the EncryptionHelper and store in the TECL file. \n3) provide the private key to the parser.");
		}
		String decoded = EncryptionHelper.me.decode(encryptedBase64, decryptKeyBase64);
		return decoded;
	}
	
	// =====================================
	// groups
	
	final IndexedValues<TECL> groups = new IndexedValues<>();

	/**
	 * Add a group.
	 * @param id
	 * @return
	 */
	public TECL addGroup(String id) {
		TECL tecl = new TECL(id, null);
		int idx = groups.add(id, tecl);
		tecl.setParent(this, idx);
		return tecl;
	}
	
	TECL setGroup(int idx, String id) {
		TECL tecl = new TECL(id, null);
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
	 * @param key
	 * @return
	 */
	public TECL grp(int idx, String key) {
		return listUsingFunction(key + "[" + idx + "]", notExistingGroup(idx), null).get(0);
	}


	private List<TECL> notExistingGroup(int idx) {
		TECL group = new TECL("<group '" + createFullPathToKey(idx, id) + "' does not exist>", this);
		group.exists = false;
		return asList(group);
	}
	private boolean exists = true;
	
	/**
	 * Get all groups for a key.
	 * @param key
	 * @return
	 */
	public List<TECL> grps(String key) {
		return listUsingFunction(key, Collections.emptyList(), null);
	}

	// =====================================
	// ARGS
	
	/**
	 * 
	 * @param args
	 */
	public void addCommandLineArguments(String[] args) {
		
		String key = null;
		for (String arg : args) {
			
			// do we have a key?
			if (arg.startsWith("--")) {
				key = arg.substring(2);
				continue;
			}
			if (arg.startsWith("-")) {
				key = arg.substring(1);
				continue;
			}
			// else it is a value, but for that we need a key
			if (key != null) {
				TECL group = getRoot();
				String propertyId = key;
				
				// process intermediate groups
				if (key.startsWith("/")) {
					
					// split up the path
					String[] groupIds = key.split("\\/");
					
					// the first one is empty because of the starting /, the last one is a property, so skip those
					for (int i = 1; i < groupIds.length - 1; i++) {
						
						// get the child group for this id
						String groupId = groupIds[i];
						TECL parentGroup = group;
						group = group.grp(groupId);
						
						// If the child group does not exist, add it
						if (!group.exists) {
							group = parentGroup.addGroup(groupId);							
						}
					}
					
					// the last node is the property id
					propertyId = groupIds[groupIds.length - 1];
				}
				
				// set value
				group.setProperty(group.count(propertyId), propertyId, arg, null);
				key = null;
			}
		}
	}
	
	
	// =====================================
	// SUPPORT
	
	/*
	 * Implements an indexed store
	 */
	class IndexedValues<T> {
		private final Map<String, List<T>> keyTovaluesMap = new LinkedHashMap<>();
		
		/*
		 * Completely clear a single key; remove all values.
		 */
		void clear(String key) {
			List<T> values = keyTovaluesMap.get(key);
			if (values != null) {
				if (logger.isDebugEnabled()) logger.debug(getPath() + ": clear property " + key);
				values.clear();
			}
		}
		
		/*
		 * Clear a single value for a single key
		 */
		void clear(int idx, String key) {
			List<T> values = keyTovaluesMap.get(key);
			if (values != null) {
				values.clear();
			}
			
			// Check if the value can be cleared
			if (values.size() <= idx) {
				return;
			}
			if (logger.isDebugEnabled()) logger.debug(getPath() + ": clear property " + key + "[" + idx + "]");
			values.set(idx, null);
		}
		
		/*
		 * Set a single value for a single key
		 */
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
			if (logger.isDebugEnabled()) logger.debug(getPath() + ": set property "  + key + "[" + idx + "] = " + value);
			values.set(idx, value);
		}

		/*
		 * Append a single value to a key
		 */
		int add(String key, T value) {
			int idx = count(key);
			set(idx, key, value, false);
			return idx;
		}

		/*
		 * Count the number of values of a key 
		 */
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
		 * Get all values for a key
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
		 * Get a single value for a key
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
		 * Get all keys
		 */
		List<String> getKeys() {
			return new ArrayList<String>(keyTovaluesMap.keySet());
		}
		
		/*
		 * empty
		 */
		boolean isEmpty() {
			return keyTovaluesMap.isEmpty();
		}

		/*
		 * 
		 */
		@Override
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
		
		if (logger.isDebugEnabled()) logger.debug("-----");
		if (logger.isDebugEnabled()) logger.debug("sanatize: >"  + s + "<");
		
		// check to see if it is quoted
		String trimmed = s.trim();
		if ( s.length() >= 6
		  && trimmed.startsWith("\"\"\"")
		  && trimmed.endsWith("\"\"\"")
		  ) {
			s = sanitizeMultiline(s);
		}
		else if ( s.length() >= 2 
		  && trimmed.startsWith("\"")
		  && trimmed.endsWith("\"")
		  ) {
			s = sanatizeQuotedString(s);
		}
		else {
			s = sanatizeUnquotedString(s);
		}
		
		if (logger.isDebugEnabled()) logger.debug("sanatize: done: >"  + s + "<");
		return s;
	}
	

	/**
	 * A normal string is "...", a multiline string is """..."""
	 * 
	 * @param s
	 * @return
	 */
	String sanitizeMultiline(String s) {
		
		// Strip multiline markers
		s = s.substring(3, s.length() - 3);
			
		// There is possible whitespace plus a newline after the CDATA-start,
		// and a newline plus white spaces before the CDATA-end
		s = s.replaceAll("^\\s*\n", "") // preceding whitespace + first newline
			.replaceAll("\n\\s*$", ""); // last newline + trailing whitespace
		
		// split in lines
		List<String> lines = s.lines().collect(Collectors.toList());
		
		// determine the minimal number of whitespaces prefixing any of the lines
		int numberOfWhitespaceMin = Integer.MAX_VALUE;		        
		for (String line : lines) {
		    int numberOfWhitespace = 0;		        
		    while (Character.isWhitespace(line.charAt(0))) {
		    	line = line.substring(1);
		    	numberOfWhitespace++;
		    }
		    if (numberOfWhitespace < numberOfWhitespaceMin) {
		    	numberOfWhitespaceMin = numberOfWhitespace;
		    }
		}
		
		// strip that number of whitespace from each line
		int numberOfWhitespaceMinFinal = numberOfWhitespaceMin;
		s = lines.stream()
			.map((line) -> line.substring(numberOfWhitespaceMinFinal))
			.collect(Collectors.joining("\n"));
		
		// done
		return s;
	}

	/*
	 * 
	 */
	private String sanatizeQuotedString(String s) {
		if (logger.isDebugEnabled()) logger.debug("sanatize: treat as quoted string: >"  + s + "<");

		// strip quoted
		s = s.substring(1, s.length() - 1);
		if (logger.isDebugEnabled()) logger.debug("sanatize: trimmed quotes: >"  + s + "<");
		
		// unescape
		s = StringEscapeUtils.unescapeJava(s);
		
		// done
		return s;
	}
	

	/*
	 * 
	 */
	private String sanatizeUnquotedString(String s) {
		if (logger.isDebugEnabled()) logger.debug("sanatize: treat as unquoted string: >"  + s + "<");

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
	
	@Override
	public String toString() {
		return getPath();
	}
	
	
	public static class Attribute {
		final String key;
		final String value;
		
		public Attribute(String key, String value) {
			this.key = key;
			this.value = value;
		}
		
		public String toString() {
			return key + "=" + value;
		}
	}
	
}
