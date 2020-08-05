package org.tbee.tecl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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
		assertEquals("'key' should occur at least 1 times at /key[1]", assertThrows(ValidationException.class, () -> {
			parse(""
				, ""
				+ "| id              | minValues | \n" 
				+ "| key             | 1         | \n" 
				);
		}).getMessage());
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
		assertEquals("'key' should occur at most 1 times at /key[1]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "| key  | \n" 
				+ "| val1 | \n" 
				+ "| val2 | \n" 
				, ""
				+ "| id  | maxValues | \n" 
				+ "| key | 1         | \n" 
			);
		}).getMessage());
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
		assertEquals("Error validating value against type for /key[0]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : abc \n"
				, ""
				+ "| id  | type    | \n" 
				+ "| key | Integer | \n" 
				);
		}).getMessage());
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

	@Test
	public void groupOk() {
		parse(""
			+ "groupId { \n"
			+ "    key : 1 \n"
			+ "}\n"
			, ""
			+ "| id      | type  | subtype | \n" 
			+ "| groupId | group | myGroup |\n"
			+ "myGroup { \n"
			+ "    | id  | type    | \n" 
			+ "    | key | Integer |\n"
			+ "} \n"
			);
	}	

	@Test
	public void groupFail() {
		assertEquals("Error validating value against type for /groupId[0]/key[0]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "groupId { \n"
				+ "    key : stringValue \n"
				+ "}\n"
				, ""
				+ "| id      | type  | subtype | \n" 
				+ "| groupId | group | myGroup |\n"
				+ "myGroup { \n"
				+ "    | id  | type    | \n" 
				+ "    | key | Integer |\n"
				+ "} \n"
				);
		}).getMessage());
	}	

	@Test
	public void undefinedKey() {
		assertEquals("'key' is not defined in the schema at /key[0]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : abc \n"
				, ""
				);
		}).getMessage());
	}

	@Test
	public void undefinedGroup() {
		assertEquals("'groupId' is not defined in the schema at /groupId[0]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "groupId { \n"
				+ "    key : 1 \n"
				+ "}\n"
				, ""
				+ "| id      | type  | subtype | \n" 
				);
		}).getMessage());
	}
	
	
	@Test
	public void minLenFail() {
		assertEquals("'key' should be at least of length 1 at /key[0]",assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : \n"
				, ""
				+ "| id  | type   | minLen | \n" 
				+ "| key | String | 1      | \n" 
				);
		}).getMessage());
	}	
	
	@Test
	public void minLenOk() {
		parse(""
			+ "key : abc \n"
			, ""
			+ "| id  | type   | minLen | \n" 
			+ "| key | String |1      | \n"
			);
	}
	
	@Test
	public void minLenOkButFailingBecauseOfMissingType() {
		assertEquals("You cannot define min/maxLen without type on /key[0]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : abc \n"
				, ""
				+ "| id  | minLen | \n" 
				+ "| key | 1      | \n"
				);
		}).getMessage());
	}
	
	@Test
	public void maxLenFail() {
		assertEquals("'key' should be no longer than 1 at /key[0]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : abc \n"
				, ""
				+ "| id  | type   | maxLen | \n" 
				+ "| key | String |1      | \n" 
			);
		}).getMessage());
	}	
	
	@Test
	public void maxLenFailMultipleValues() {
		assertEquals("'key' should be no longer than 5 at /key[1]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "| key    | \n" 
				+ "| 1234   | \n" 
				+ "| 123456 | \n" 
				, ""
				+ "| id  | type   | maxLen | \n" 
				+ "| key | String | 5     | \n" 
			);
		}).getMessage());
	}	
	
	@Test
	public void maxLenOk() {
		parse(""
			+ "key : abc \n"
			, ""
			+ "| id  | type   | maxLen | \n" 
			+ "| key | String |10     | \n" 
		);
	}

	//	@Test
//	public void emptyFile() {
//		assertEquals("", assertThrows(ValidationException.class, () -> {
//			TECL tecl = parse(""
//					, ""
//					+ "| id              | type          | subtype  | minValues | maxValues |\n" 
//					+ "| key             | string        |          | 1         |           |\n" 
//					);
//		}).getMessage());
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
