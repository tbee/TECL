package org.tbee.tecl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.text.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbee.tecl.TECLParser.ParserListener.TECLContext;

public class TECLParser {
	final Logger logger = LoggerFactory.getLogger(TECLParser.class);
	
	// ======================================
	// PARAMETERS
	
	/**
	 * 
	 * @param key
	 * @param value
	 */
	public TECLParser addParameter(String key, String value) {
		parameters.put(key, value);
		return this;
	}
	private Map<String, String> parameters = new LinkedHashMap<>(); 
	
	
	// ======================================
	// PARSE
	
	/**
	 * @param file file to parse
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public TECL parse(File file, java.nio.charset.Charset charset) throws FileNotFoundException, IOException {
		try (
			FileInputStream fileInputStream = new FileInputStream(file); 
		){ 
			return parse(fileInputStream, charset);
		}
	}
	
	/**
	 * @param inputStream inputStream to parse
	 * @return
	 * @throws IOException 
	 */
	public TECL parse(InputStream inputStream, java.nio.charset.Charset charset) throws IOException {
		String content = readToString(inputStream, charset);
		return parse(content);
	}

	/**
	 * 
	 * @param config
	 * @return
	 */
	public TECL parse(String config) {	
		
		// split into lines
		List<String> lines = new BufferedReader(new StringReader(config)).lines().collect(Collectors.toList());
		
		// preprocess lines
		lines = preprocess(lines);
		
		// rejoin to string
		config = lines.stream().collect(Collectors.joining("\n"));
		
		// Trigger the ANTLR parser
		CharStream input = CharStreams.fromString(config);
		ThrowingErrorListener throwingErrorListener = new ThrowingErrorListener();
		org.tbee.tecl.antlr.TECLLexer lexer = new org.tbee.tecl.antlr.TECLLexer(input);
		lexer.addErrorListener(throwingErrorListener);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        org.tbee.tecl.antlr.TECLParser parser = new org.tbee.tecl.antlr.TECLParser(tokens);
        parser.addErrorListener(throwingErrorListener);
        ParserListener parserListener = new ParserListener();
		parser.parse(parserListener); 
		return parserListener.toplevelTECL;
	}
	
	/*
	 * Process the lines and return a new list 
	 */
	private List<String> preprocess(List<String> lines) {
		List<String> toProcessLines = new ArrayList<String>(lines);
		List<String> newLines = new ArrayList<String>();
		
		while (!toProcessLines.isEmpty()) {
			String line = toProcessLines.remove(0);
			
			// @version
			if (line.startsWith(versionPrefix)) {
				preprocessVersion(line);				
			}
			// @import
			else if (line.startsWith(importPrefix)) {
				List<String> importedLines = preprocessImport(line);
				toProcessLines.addAll(0, importedLines);
			}
			else {
				newLines.add(line);
			}
		}
		
		return newLines;
	}

	/*
	 * 
	 */
	private void preprocessVersion(String line) {
		
		// parse
		Integer newVersion = Integer.valueOf(line.substring(versionPrefix.length()).trim());
		if (version != null && !version.equals(newVersion)) {
			throw new IllegalStateException("All files must have the same version (" + version + " != "  + newVersion + ")"); 
		}
		version = newVersion;
		
		// only version 1 supported
		if (version.intValue() != 1) {
			throw new IllegalStateException("Only version 1 is supported"); 
		}
	}
	private final String versionPrefix = "@version ";
	private Integer version = null;

	/*
	 * 
	 */
	private List<String> preprocessImport(String line) {
		try {
			// get URL to import
			String source = line.substring(importPrefix.length()).trim();
			InputStream inputStream;
			if (source.contains(":")) {
				inputStream = new URL(source).openStream();
			}
			else {
				inputStream = new FileInputStream(source);
			}

			// read
			String content = readToString(inputStream, Charset.forName("UTF-8"));
			
			// split into lines
			List<String> importedLines = new BufferedReader(new StringReader(content)).lines().collect(Collectors.toList());
			return importedLines;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	private final String importPrefix = "@import ";
	
	/*
	 * 
	 */
	private class ThrowingErrorListener extends BaseErrorListener {
		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) throws ParseCancellationException {
			throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
		}
	}
	
	// ======================================
	// GRAMMAR
	
	class ParserListener implements org.tbee.tecl.antlr.TECLParser.Listener { 
	
		public ParserListener() {
			logger.atDebug().log("startGroup $");
			teclContextStack.push(teclContext);
		}
		// This is the toplevel
		private final TECL toplevelTECL = new TECL("$");	
		
		// This is the active TECL within the group
		private final Stack<TECLContext> teclContextStack = new Stack<>();
		private TECLContext teclContext = new TECLContext(toplevelTECL);
		class TECLContext {			
			TECLContext(TECL tecl) {
				this.tecl = tecl;
			}
			final TECL tecl;
			final Map<String, Integer> bestMatchConditions = new LinkedHashMap<>();
		}
			
		// --------------
		// PROPERTY
		
		@Override
		public void setProperty(String key, List<String> values) {	
			Boolean matchConditions = matchConditions(useConditions(), teclContext, key);
			if (matchConditions == null || matchConditions) {
				boolean allowOverwrite = (matchConditions != null);
				if (allowOverwrite) {
					teclContext.tecl.clearProperty(key);
				}
				AtomicInteger idx = new AtomicInteger();
				values.forEach(value -> {
					teclContext.tecl.setProperty(idx.getAndIncrement(), key, value);
				});
			}
		}	
	
		@Override
		public void setProperty(int idx, String key, String value) {
			Boolean matchConditions = matchConditions(useConditions(), teclContext, key);
			if (matchConditions == null || matchConditions) {
				boolean allowOverwrite = (matchConditions != null);
				teclContext.tecl.setProperty(idx, key, value, allowOverwrite);
			}
		}	
	
		// --------------
		// GROUP
		
		@Override
		public void startGroup(String id) {

			logger.atDebug().log("startGroup '" + id + "'");
			Boolean matchConditions = matchConditions(useConditions(), null /*teclContext*/, id);
			if (matchConditions == null || matchConditions) {
				teclContext = new TECLContext(teclContext.tecl.addGroup(id));
				logger.atDebug().log("new group '" + id + "' added at " + teclContext.tecl.getPath());
			}
			else {
				// We create a TECL so we can continue parsing the file, but it is not added as a group
				teclContext = new TECLContext(new TECL("<skipping all contents because of conditions>"));
			}
			teclContextStack.push(teclContext);
		}
		
		@Override
		public void endGroup() {
			logger.atDebug().log("endGroup " + teclContext.tecl.getId());
			teclContextStack.pop(); 
			teclContext = teclContextStack.peek();		
		}
		
		// --------------
		// CONDITIONS
		
		private List<Condition> conditions;
		
		@Override
		public void startConditions() {
			conditions = new ArrayList<Condition>();	
		}
		
		@Override
		public void addCondition(String key, String comparator, String value) {
			conditions.add(new Condition(key, comparator, value));
		}
		
		private List<Condition> useConditions() {
			List<Condition> conditions = this.conditions;
			this.conditions = null;
			return conditions;
		}
		
		// --------------
		// TABLE
		
		private final List<String> tableKeys = new ArrayList<>();
		private int tableRowIdx;
		private int tableColIdx;
		private final List<TECL> teclsContainingTable = new ArrayList<>();
		private final List<TECL> teclsWithTerminatedTable = new ArrayList<>();
	
		private void validateOneTablePerGroup() {
			if (teclsContainingTable.contains(teclContext.tecl)) {
				throw new IllegalStateException("Group " + teclContext.tecl.getId() + " already contains a table, only one table per group is allowed.");
			} 
			teclsContainingTable.add(teclContext.tecl);
		}
		private void validateTerminatedTable() {
			if (teclsWithTerminatedTable.contains(teclContext.tecl)) {
				throw new IllegalStateException("Group " + teclContext.tecl.getId() + " already contains a table, only one table per group is allowed.");
			} 
		}
			
		@Override
		public void startTable() {
			logger.atDebug().log("startTable");
			validateOneTablePerGroup();  
			tableKeys.clear(); 
			tableRowIdx = -2;		
		}
		
		@Override
		public void terminateTable() {
			logger.atDebug().log("terminateTable");
			teclsWithTerminatedTable.add(teclContext.tecl);
		}	
		
		@Override
		public void startTableRow() {
			tableColIdx = 0;
			tableRowIdx++;
			logger.atDebug().log("startTableRow row=" + tableRowIdx + ", col=" + tableColIdx);
		}	
	
		@Override
		public void addTableData(String value) {
			validateTerminatedTable();
			logger.atDebug().log("addTableRow row=" + tableRowIdx + ", col=" + tableColIdx + ", value=" + value);
			
			if (tableRowIdx < 0) {
				logger.atDebug().log("addTableRow add header " + value);
				tableKeys.add(value);
			}
			else {
				String key = tableKeys.get(tableColIdx);
				logger.atDebug().log("addTableRow add data " + key + "[" + tableRowIdx + "]=" + value);
				teclContext.tecl.setProperty(tableRowIdx, key, value);
			}
			tableColIdx++;
		}
		
		@Override
		public void addTableData(List<String> values) {
			validateTerminatedTable();
			logger.atDebug().log("addTableRow row=" + tableRowIdx + ", col=" + tableColIdx + ", values=" + values);
			
			if (tableRowIdx < 0) {
				logger.atDebug().log("addTableRow add header " + values);
				throw new IllegalArgumentException("Cannot use a list in the table header: " + values);
			}
			else {
				String key = tableKeys.get(tableColIdx);
				logger.atDebug().log("addTableRow add data " + key + "[" + tableRowIdx + "]=" + values);
				
				String id = "|" + key + "|";
				TECL listTECL = teclContext.tecl.setGroup(tableRowIdx, id);
				logger.atDebug().log("new group '" + id + "' added at " + teclContext.tecl.getPath());
				AtomicInteger idx = new AtomicInteger();
				values.forEach(value -> {
					listTECL.setProperty(idx.getAndIncrement(), key, value);
				});
			}
			tableColIdx++;
		}
	}
	
	
	// ======================================
	// SUPPORT
	
	static class Condition {
		final String key;
		final String comparator;
		final String value;
		
		public Condition(String key, String comparator, String value) {
			this.key = key;
			this.comparator = comparator;
			this.value = value;
		}
		
		public String toString() {
			return key + comparator + value;
		}
	}
	
	/**
	 * Give a list of conditions, check how many match against the provided parameters.
	 * - If a condition is not present in the parameters, it is not counted.
	 * - If a condition is present, and matches, it is counted as +1
	 * - If a condition is present, and does not match, the process is aborted with a -1
	 * 
	 * @param conditions
	 * @return < 0 means there is at least one condition that does not match, >=0 the number of matching conditions
	 */
	private Boolean matchConditions(List<Condition> conditions, TECLContext teclContext, String key) {
		if (conditions == null || conditions.isEmpty()) {
			return null;
		}

		// count the matching conditions
		int cnt = 0;
		for (Condition condition : conditions) {
			String parameter = this.parameters.get(condition.key);
			logger.atDebug().log(key + ": " + parameter + " matching " + condition);
			
			// condition is not present in the parameters: do not count
			if (parameter == null) {
				continue;
			}
			
			// condition is present in the parameters, but does not match
			if (!parameter.equals(condition.value)) { // TBEERNOT start supporing < > etc?
				return false;
			}
			
			// condition is present in the parameters, but and matches
			cnt++;
			logger.atDebug().log(key + ": " + "cnt=" + cnt);
		}
		
		// We have a match, but is it better that the previous match
		if (teclContext != null && key != null) {
			
			// find previous match
			Integer previousCnt = teclContext.bestMatchConditions.get(key);
			logger.atDebug().log(key + ": " + "previous bestMatchConditions = " + previousCnt);
			
			// Compare with current match
			if (previousCnt != null && previousCnt.intValue() >= cnt) {
				// Nope, not better
				return false;
			}
			
			// We have a (better) match!
			logger.atDebug().log(key + ": " + "bestMatchConditions = " + cnt);
			teclContext.bestMatchConditions.put(key, cnt);
		}
		
		// done
		return true;				
	}
	
	private String readToString(InputStream inputStream, Charset charset) throws IOException {
		StringBuilder sb = new StringBuilder();
		try (
				Reader reader = new BufferedReader(new InputStreamReader(inputStream, charset))
		) {
			int c = 0;
			while ((c = reader.read()) != -1) {
				sb.append((char)c);
			}
		}
		return sb.toString();
	}
}
