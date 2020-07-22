package org.tbee.tecl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbee.tecl.TECLSchema.ValidationException;
import org.tbee.tecl.antlr.PrintLexer;

public class TECLSchemaTest {
	final Logger logger = LoggerFactory.getLogger(TECLSchemaTest.class);

	
	@Test
	public void emptyFileEmptySchema() {
		TECL tecl = parse("", "");
	}
	
	@Test
	public void minValuesFail() {
		assertThrows(ValidationException.class, () -> {
			parse(""
				, ""
				+ "| id              | minValues | \n" 
				+ "| key             | 1         | \n" 
				);
		});
	}	
	
	@Test
	public void minValuesOk() {
		parse(""
			+ "key : abc \n"
			, ""
			+ "| id  | minValues | \n" 
			+ "| key | 1         | \n" 
			);
	}
	
	@Test
	public void maxValuesFail() {
		assertThrows(ValidationException.class, () -> {
			parse(""
				+ "| key  | \n" 
				+ "| val1 | \n" 
				+ "| val2 | \n" 
				, ""
				+ "| id  | maxValues | \n" 
				+ "| key | 1         | \n" 
			);
		});
	}	
	
	@Test
	public void maxValuesOk() {
		parse(""
			+ "| key  | \n" 
			+ "| val1 | \n" 
			, ""
			+ "| id  | maxValues | \n" 
			+ "| key | 1         | \n" 
		);
	}
	
	@Test
	public void typeIntegerFail() {
		assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : abc \n"
				, ""
				+ "| id  | type    | \n" 
				+ "| key | Integer | \n" 
				);
		});
	}

	@Test
	public void typeIntegerOk() {
		parse(""
			+ "key : 1 \n"
			, ""
			+ "| id  | type    | \n" 
			+ "| key | Integer | \n" 
			);
	}	

//	@Test
//	public void emptyFile() {
//		assertThrows(ValidationException.class, () -> {
//			TECL tecl = parse(""
//					, ""
//					+ "| id              | type          | subtype  | minValues | maxValues |\n" 
//					+ "| key             | string        |          | 1         |           |\n" 
//					);
//		});
//	}
	
	// ========================
	
	private TECL parse(String tecl, String tesd) {
		logger.atDebug().log("Parsing:\n" + tecl + new PrintLexer().lex(tecl));
		return TECL.parser()
				.addParameter("sys", "A")
				.addParameter("env", "test")
				.schema(tesd)
				.parse(tecl);
	}
}
