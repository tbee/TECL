package org.tbee.tecl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;

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
		ThrowingErrorListener throwingErrorListener = new ThrowingErrorListener();
		org.tbee.tecl.antlr.TECLLexer lexer = new org.tbee.tecl.antlr.TECLLexer(input);
		lexer.addErrorListener(throwingErrorListener);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        org.tbee.tecl.antlr.TECLParser parser = new org.tbee.tecl.antlr.TECLParser(tokens);
        parser.addErrorListener(throwingErrorListener);
		parser.parse();
		return parser.getTECL();
	}
	
	private class ThrowingErrorListener extends BaseErrorListener {
		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) throws ParseCancellationException {
			throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
		}
	}
}
