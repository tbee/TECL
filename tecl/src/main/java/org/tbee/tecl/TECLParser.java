package org.tbee.tecl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class TECLParser {

	/**
	 * 
	 * @param config
	 * @return
	 */
	public TECL parse(String config) {
		return parse(CharStreams.fromString(config));
	}
	
	/**
	 * @param file assumed to use UTF-8 encoding
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public TECL parse(File file) throws FileNotFoundException, IOException {
		try (
			FileInputStream fileInputStream = new FileInputStream(file); 
		){ 
			return parse(CharStreams.fromStream(fileInputStream));
		}
	}

	/*
	 * The actual parsing
	 */
	private TECL parse(CharStream input) {		
		org.tbee.tecl.antlr.TECLLexer lexer = new org.tbee.tecl.antlr.TECLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        org.tbee.tecl.antlr.TECLParser actionsParser = new org.tbee.tecl.antlr.TECLParser(tokens);
		actionsParser.parse();
		// TODO: fail on parse errors
		return actionsParser.getTECL();
		
	}
}
