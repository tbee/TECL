package org.tbee.tecl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.text.StringEscapeUtils;

public class TECLParser {
	
	// ======================================
	// PARAMETERS
	
	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void addParameter(String key, String value) {
		parameters.put(key, value);
	}
	private Map<String, String> parameters = new LinkedHashMap<>(); 
	
	
	// ======================================
	// PARSE
	
	/**
	 * 
	 * @param config
	 * @return
	 */
	public TECL parse(String config) {
		return parse(CharStreams.fromString(config));
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
			return parse(CharStreams.fromStream(fileInputStream, charset));
		}
	}
	
	/**
	 * @param inputStream inputStream to parse
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public TECL parse(InputStream inputStream, java.nio.charset.Charset charset) throws IOException {
		return parse(CharStreams.fromStream(inputStream, charset));
	}

	/*
	 * The actual parsing
	 */
	private TECL parse(CharStream input) {	
		
		// init
		
		// Trigger the ANTLR parser
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
			System.out.println("startGroup $");
			teclStack.push(toplevelTECL);
		}
		// This is the toplevel
		private final TECL toplevelTECL = new TECL("$");	
		
		// This is the active TECL within the group
		private final Stack<TECL> teclStack = new Stack<>();
		private TECL tecl = toplevelTECL;
			
		// --------------
		// PROPERTY
		
		public void addProperty(String key, String value) {		
			tecl.addProperty(key, sanatizeAssignment(value));
		}	
	
		public void setProperty(int idx, String key, String value) {
			tecl.addProperty(key, "");
		}	
	
		// --------------
		// GROUP
		
		public void startGroup(String id) {
			System.out.println("startGroup " + id);
			teclStack.push( teclStack.peek().addGroup(id) ); 
			tecl = teclStack.peek();		
		}
		
		public void endGroup() {
			System.out.println("endGroup " + teclStack.peek().getId());
			teclStack.pop(); 
			tecl = teclStack.peek();		
		}
		
		// --------------
		// CONDITIONS
		
		private List<Condition> conditions;
		
		public void startConditions() {
			conditions = new ArrayList<Condition>();	
		}
		
		public void addCondition(String key, String comparator, String value) {
			conditions.add(new Condition(key, comparator, value));
		}
		
		public List<Condition> useConditions() {
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
			if (teclsContainingTable.contains(tecl)) {
				throw new IllegalStateException("Group " + tecl.getId() + " already contains a table, only one table per group is allowed.");
			} 
			teclsContainingTable.add(tecl);
		}
		private void validateTerminatedTable() {
			if (teclsWithTerminatedTable.contains(tecl)) {
				throw new IllegalStateException("Group " + tecl.getId() + " already contains a table, only one table per group is allowed.");
			} 
		}
			
		public void startTable() {
			System.out.println("startTable");
			validateOneTablePerGroup();  
			tableKeys.clear(); 
			tableRowIdx = -2;		
		}
		
		public void terminateTable() {
			System.out.println("terminateTable");
			teclsWithTerminatedTable.add(tecl);
		}	
		
		public void startTableRow() {
			tableColIdx = 0;
			tableRowIdx++;
			System.out.println("startTableRow row=" + tableRowIdx + ", col=" + tableColIdx);
		}	
	
		public void addTableData(String value) {
			validateTerminatedTable();
			System.out.println("addTableRow row=" + tableRowIdx + ", col=" + tableColIdx + ", value=" + value);
			if (tableRowIdx < 0) {
				System.out.println("addTableRow add header " + value);
				tableKeys.add(value);
			}
			else {
				String key = tableKeys.get(tableColIdx);
				System.out.println("addTableRow add data " + key + "[" + tableRowIdx + "]=" + value);
				tecl.setProperty(tableRowIdx, key, value);
			}
			tableColIdx++;
		}
	}
	
	
	// ======================================
	// SUPPORT
	
	public static class Condition {
		final String key;
		final String comparator;
		final String value;
		
		public Condition(String key, String comparator, String value) {
			this.key = key;
			this.comparator = comparator;
			this.value = value;
		}
	}

	/**
	 * @param s
	 * @return
	 */
	private String sanatizeAssignment(String s) {
		System.out.println("-----");
		System.out.println("sanatize:"  + s);
		
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
		
		System.out.println("sanatize done: >"  + s + "<");
		return s;
	}

	/*
	 * 
	 */
	private String sanatizeQuotedString(String s) {
		System.out.println("sanatize: treat as quoted string" + s);

		// strip quoted
		s = s.substring(1, s.length() - 1);
		System.out.println("sanatize: trimmed quotes"  + s);
		
		// unescape
		s = StringEscapeUtils.unescapeJava(s);
		
		// done
		return s;
	}

	/*
	 * 
	 */
	private String sanatizeUnquotedString(String s) {
		System.out.println("sanatize: treat as unquoted string" + s);

		// done
		return s.trim();
	}
	
}
