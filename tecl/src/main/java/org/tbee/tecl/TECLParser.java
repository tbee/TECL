package org.tbee.tecl;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbee.tecl.TECLParser.ParserListener.TECLContext;
import org.tbee.tecl.TECLSchema.Validator;

public class TECLParser {
	final Logger logger = LoggerFactory.getLogger(TECLParser.class);
	
	/*private final*/ static String CONFIG_TECL_FILENAME = "./config.tecl"; // to allow unit testing

	private final TECL toplevelTECL;	

	public TECLParser() {
		toplevelTECL = new TECL("");	
		toplevelTECL.populateConvertFunctions();
	}
	
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
	// Decrypt
	
	/** 
	 * Specify the decode key directly 
	 * @param keyInBase64
	 * @return 
	 */
	public TECLParser decryptKey(String keyInBase64) {
		toplevelTECL.setDecryptKeyBase64(keyInBase64);
		return this;
	}
	
	/**
	 * 
	 * @param inputStream The key in base64 on disk
	 * @return 
	 * @throws IOException 
	 */
	public TECLParser decryptKey(InputStream inputStream) throws IOException {
		String keyBase64 = new String(inputStream.readAllBytes());
		return decryptKey(keyBase64);
	}
	
	/**
	 * 
	 * @param file A file containing the decrypt key in base64 notation
	 * @return 
	 * @throws IOException 
	 */
	public TECLParser decryptKey(File file) throws IOException {
		try (
			FileInputStream fileInputStream = new FileInputStream(file);
		) {
			return decryptKey(fileInputStream);
		}
	}
	
	/**
	 * 
	 * @param url A url containing the decrypt key in base64 notation
	 * @return 
	 * @throws IOException 
	 */
	public TECLParser decryptKey(URL url) throws IOException {
		try (
			InputStream inputStream = url.openStream(); 
		) {
			return decryptKey(inputStream);
		}
	}
	
	// ======================================
	// PARSE
	
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
	 * @return TECL or null if not found
	 * @throws IOException 
	 */
	public TECL findAndParse() throws IOException {
		InputStream inputStream = null;
		
		// 1. system property
		{
			String value = System.getProperty("config.tecl");
			File file = (value == null ? null : new File(value));
			if (logger.isDebugEnabled()) logger.debug("-Dconfig.tecl=" + value + " -> " + (file == null ? null : file.getAbsolutePath() + (file.exists() ? ", exists" : ", does not exist")));
			if (inputStream == null && file != null && file.exists()) {
				if (logger.isInfoEnabled()) logger.info("Using -Dconfig.tecl -> " + file.getAbsolutePath());
				inputStream = new FileInputStream(file);
			}
		}
		
		// 2. env
		{
			String value = System.getenv("config_tecl");
			File file = (value == null ? null : new File(value));
			if (logger.isDebugEnabled()) logger.debug("env:config_tecl=" + value + " -> " + (file == null ? null : file.getAbsolutePath() + (file.exists() ? ", exists" : ", does not exist")));
			if (inputStream == null && file != null && file.exists()) {
				if (logger.isInfoEnabled()) logger.info("Using env:config_tecl -> " + file.getAbsolutePath());
				inputStream = new FileInputStream(file);
			}
		}
		
		// 3. file
		{
			File file = new File(CONFIG_TECL_FILENAME);
			if (logger.isDebugEnabled()) logger.debug("file:./config.tecl -> " + file.getAbsolutePath() + (file.exists() ? ", exists" : ", does not exist"));
			if (inputStream == null && file != null && file.exists()) {
				if (logger.isInfoEnabled()) logger.info("Using -file:./config.tecl -> " + file.getAbsolutePath());
				inputStream = new FileInputStream(file);
			}
		}
		
		// 4. resource
		{
			URL resourceURL = this.getClass().getResource("/config.tecl");
			InputStream resourceInputStream = this.getClass().getResourceAsStream("/config.tecl");
			if (logger.isDebugEnabled()) logger.debug("resource:/config.tecl=" + resourceURL + " -> " + (resourceInputStream != null ? "found" : "not found"));
			if (inputStream == null && resourceInputStream != null) {
				if (logger.isInfoEnabled()) logger.info("Using resource:/config.tecl");
				inputStream = resourceInputStream;
			}
		}
		
		// done
		if (inputStream == null) {
			return null;
		}
		try {
			return parse(inputStream, Charset.forName("UTF-8"));
		}
		finally {
			inputStream.close();
		}
	}
	
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
		lines = preprocess(lines, new File("."));
		
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
        ParserListener parserListener = new ParserListener(toplevelTECL);
		parser.parse(parserListener);
		
		// validate
		if (teclSchema != null) {
			teclSchema.validate(parserListener.toplevelTECL);
		}
		
		// Done
		return toplevelTECL;
	}
	
	/*
	 * Process the lines and return a new list 
	 */
	private List<String> preprocess(List<String> lines, File curDir) {
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
				List<String> importedLines = preprocessImport(line, curDir);
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
	private List<String> preprocessImport(String line, File curDir) {
		String additionalInfo = "";
		try {
			// get URL to import
			String source = line.substring(importPrefix.length()).trim();
			InputStream inputStream;
			if (source.contains(":")) {
				inputStream = new URL(source).openStream();
			}
			else {
				File file = new File(curDir, source);
				additionalInfo = file.getAbsolutePath();
				inputStream = new FileInputStream(file);
				curDir = file.getParentFile();
			}

			// read
			String content = readToString(inputStream, Charset.forName("UTF-8"));
			
			// split into lines
			List<String> importedLines = new BufferedReader(new StringReader(content)).lines().collect(Collectors.toList());
			importedLines = preprocess(importedLines, curDir);
			return importedLines;
		}
		catch (IOException e) {
			throw new RuntimeException(additionalInfo, e);
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
	
		public ParserListener(TECL toplevelTECL) {
			this.toplevelTECL = toplevelTECL;
			teclContext = new TECLContext(toplevelTECL);
			if (logger.isDebugEnabled()) logger.debug("startGroup $");
			teclContextStack.push(teclContext);
		}
		private final TECL toplevelTECL;	
		
		// This is the active TECL within the group
		private final Stack<TECLContext> teclContextStack = new Stack<>();
		private TECLContext teclContext;
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

			if (logger.isDebugEnabled()) logger.debug("startGroup '" + id + "'");
			Boolean matchConditions = matchConditions(useConditions(), null /*teclContext*/, id);
			if (matchConditions == null || matchConditions) {
				teclContext = new TECLContext(teclContext.tecl.addGroup(id));
				if (logger.isDebugEnabled()) logger.debug("new group '" + id + "' added at " + teclContext.tecl.getPath());
			}
			else {
				// We create a TECL so we can continue parsing the file, but it is not added as a group
				teclContext = new TECLContext(new TECL("<skipping all contents because of conditions>"));
			}
			teclContextStack.push(teclContext);
		}
		
		@Override
		public void endGroup() {
			if (logger.isDebugEnabled()) logger.debug("endGroup " + teclContext.tecl.getId());
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
			if (logger.isDebugEnabled()) logger.debug("startTable");
			validateOneTablePerGroup();  
			tableKeys.clear(); 
			tableRowIdx = -2;		
		}
		
		@Override
		public void terminateTable() {
			if (logger.isDebugEnabled()) logger.debug("terminateTable");
			teclsWithTerminatedTable.add(teclContext.tecl);
		}	
		
		@Override
		public void startTableRow() {
			tableColIdx = 0;
			tableRowIdx++;
			if (logger.isDebugEnabled()) logger.debug("startTableRow row=" + tableRowIdx + ", col=" + tableColIdx);
		}	
	
		@Override
		public void addTableData(String value) {
			validateTerminatedTable();
			if (logger.isDebugEnabled()) logger.debug("addTableRow row=" + tableRowIdx + ", col=" + tableColIdx + ", value=" + value);
			
			if (tableRowIdx < 0) {
				if (logger.isDebugEnabled()) logger.debug("addTableRow add header " + value);
				tableKeys.add(value);
			}
			else {
				String key = tableKeys.get(tableColIdx);
				if (logger.isDebugEnabled()) logger.debug("addTableRow add data " + key + "[" + tableRowIdx + "]=" + value);
				teclContext.tecl.setProperty(tableRowIdx, key, value);
			}
			tableColIdx++;
		}
		
		@Override
		public void addTableData(List<String> values) {
			validateTerminatedTable();
			if (logger.isDebugEnabled()) logger.debug("addTableRow row=" + tableRowIdx + ", col=" + tableColIdx + ", values=" + values);
			
			if (tableRowIdx < 0) {
				if (logger.isDebugEnabled()) logger.debug("addTableRow add header " + values);
				throw new IllegalArgumentException("Cannot use a list in the table header: " + values);
			}
			else {
				String key = tableKeys.get(tableColIdx);
				if (logger.isDebugEnabled()) logger.debug("addTableRow add data " + key + "[" + tableRowIdx + "]=" + values);
				
				String id = "|" + key + "|";
				TECL listTECL = teclContext.tecl.setGroup(tableRowIdx, id);
				if (logger.isDebugEnabled()) logger.debug("new group '" + id + "' added at " + teclContext.tecl.getPath());
				AtomicInteger idx = new AtomicInteger();
				values.forEach(value -> {
					listTECL.setProperty(idx.getAndIncrement(), key, value);
				});
			}
			tableColIdx++;
		}
	}
	
	
	// ======================================
	// SCHEMA

	private TECLSchema teclSchema = null;
	
	/**
	 * @param file file containing the schema
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public TECLParser schema(File file, java.nio.charset.Charset charset, Validator... validators) throws FileNotFoundException, IOException {
		try (
			FileInputStream fileInputStream = new FileInputStream(file); 
		){ 
			return schema(fileInputStream, charset, validators);
		}
	}
	
	/**
	 * @param inputStream inputStream containing the schema
	 * @return
	 * @throws IOException 
	 */
	public TECLParser schema(InputStream inputStream, java.nio.charset.Charset charset, Validator... validators) throws IOException {
		String content = readToString(inputStream, charset);
		return schema(content, validators);
	}

	/**
	 * 
	 * @param tesd the schema
	 * @param validators
	 * @return
	 */
	public TECLParser schema(String tesd, Validator... validators) {		
		this.teclSchema = new TECLSchema(tesd);
		for (Validator validator : validators) {
			this.teclSchema.addValidator(validator);
		}
		return this;
	}

		
	// ======================================
	// ConvertFunction
	
	public <R> TECLParser addConvertFunction(Class<R> clazz, BiFunction<String, R, R> convertFunction) {
		toplevelTECL.addConvertFunction(clazz, convertFunction);
		return this;
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
			if (logger.isDebugEnabled()) logger.debug(key + ": " + parameter + " matching " + condition);
			
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
			if (logger.isDebugEnabled()) logger.debug(key + ": " + "cnt=" + cnt);
		}
		
		// We have a match, but is it better that the previous match
		if (teclContext != null && key != null) {
			
			// find previous match
			Integer previousCnt = teclContext.bestMatchConditions.get(key);
			if (logger.isDebugEnabled()) logger.debug(key + ": " + "previous bestMatchConditions = " + previousCnt);
			
			// Compare with current match
			if (previousCnt != null && previousCnt.intValue() >= cnt) {
				// Nope, not better
				return false;
			}
			
			// We have a (better) match!
			if (logger.isDebugEnabled()) logger.debug(key + ": " + "bestMatchConditions = " + cnt);
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
