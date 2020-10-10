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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbee.tecl.TECLSchema.ValidationException;
import org.tbee.tecl.TECLSchema.Validator;
import org.tbee.tecl.antlr.PrintLexer;

public class TECLSchemaTest {
	final Logger logger = LoggerFactory.getLogger(TECLSchemaTest.class);

	
	@Test
	public void emptyFileEmptySchema() {
		TECL tecl = parse("", "");
	}

	// ========================
	// MISSING KEY
	
	@Test
	public void undefinedKey() {
		assertEquals("'key' is not defined in the schema at /key[0]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : abc \n"
				, ""
				);
		}).getMessage());
	}

	// ========================
	// MIN/MAX VALUES
	
	@Test
	public void minValuesFail() {
		assertEquals("'key' should occur at least 1 times at /key[1]", assertThrows(ValidationException.class, () -> {
			parse(""
				, ""
				+ "| id  | minValues | \n" 
				+ "| key | 1         | \n" 
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
	
	// ========================
	// PROPERTY TYPE
	
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
	public void typeThroughRefIntegerFail() {
		assertEquals("Error validating value against type for /key[0]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : $key2 \n"
				+ "key2 : abc \n"
				, ""
				+ "| id   | type    | \n" 
				+ "| key  | Integer | \n" 
				+ "| key2 | Integer | \n" 
				);
		}).getMessage());
	}

	@Test
	public void typeThroughRefIntegerOk() {
		parse(""
			+ "key : $key2 \n"
			+ "key2 : 1 \n"
			, ""
			+ "| id   | type    | \n" 
			+ "| key  | Integer | \n" 
			+ "| key2 | Integer | \n" 
			);
	}	

	// ========================
	// GROUP
	
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
	
	// ========================
	// MIN/MAX LEN
	
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
	
	// ========================
	// MIN/MAX
	
	@Test
	public void minFail() {
		assertEquals("'key' should be equal or greater than 11 at /key[0]",assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : 10\n"
				, ""
				+ "| id  | min | \n" 
				+ "| key | 11  | \n" 
				);
		}).getMessage());
	}	
	
	@Test
	public void minOk() {
		parse(""
			+ "key : 10 \n"
			, ""
			+ "| id  | min | \n" 
			+ "| key | 1   | \n"
			);
	}
	
	@Test
	public void maxFail() {
		assertEquals("'key' should be equal or less than 1 at /key[0]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : 10 \n"
				, ""
				+ "| id  | max | \n" 
				+ "| key | 1   | \n" 
			);
		}).getMessage());
	}	
	
	@Test
	public void maxOk() {
		parse(""
			+ "key : 10 \n"
			, ""
			+ "| id  | max | \n" 
			+ "| key | 11  | \n" 
		);
	}	
	
	// ========================
	// LIST
	
	@Test
	public void listIntegerFail() {
		assertEquals("Error validating value against type for /key[1]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : [1, abc, 3] \n"
				, ""
				+ "| id  | type  | subtype | \n" 
				+ "| key | list  | Integer | \n" 
				);
		}).getMessage());
	}

	@Test
	public void listIntegerOk() {
		parse(""
			+ "key : [1, 2, 3] \n"
			, ""
			+ "| id  | type  | subtype | \n" 
			+ "| key | list  | Integer | \n" 
			);
	}	
	
	@Test
	public void listThroughRefIntegerFail() {
		assertEquals("Error validating value against type for /key[1]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : [1, $key2, 3] \n"
				+ "key2 : abc \n"
				, ""
				+ "| id  | type     | subtype | \n" 
				+ "| key | list     | Integer | \n" 
				+ "| key2 | Integer |         | \n" 
				);
		}).getMessage());
	}

	@Test
	public void listThroughRefIntegerOk() {
		parse(""
			+ "key : [1, $key2, 3] \n"
			+ "key2 : 2 \n"
			, ""
			+ "| id  | type     | subtype | \n" 
			+ "| key | list     | Integer | \n" 
			+ "| key2 | Integer |         | \n" 
			);
	}	
	
	// ========================
	// ENUM
	
	@Test
	public void enumIntegerFail() {
		assertEquals("Value '4' does not occur in the enum 'anEnum' for /key[0]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : 4 \n"
				, ""
				+ "| id  | type    | enum   |\n" 
				+ "| key | Integer | anEnum |\n" 
				+ "anEnum : [1, 2, 3]\n"
				);
		}).getMessage());
	}

	@Test
	public void enumIntegerOk() {
		parse(""
			+ "key : 2 \n"
			, ""
			+ "| id  | type    | enum   |\n" 
			+ "| key | Integer | anEnum |\n" 
			+ "anEnum : [1, 2, 3]\n"
			);
	}	
	
	@Test
	public void enumThroughRefIntegerFail() {
		assertEquals("Value '4' does not occur in the enum 'anEnum' for /key[0]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : $key2 \n"
				+ "key2 : 4 \n"
				, ""
				+ "| id   | type    | enum   |\n" 
				+ "| key  | Integer | anEnum |\n" 
				+ "| key2 | Integer |        |\n" 
				+ "anEnum : [1, 2, 3]\n"
				);
		}).getMessage());
	}

	@Test
	public void enumThroughRefIntegerOk() {
		parse(""
			+ "key : $key2 \n"
			+ "key2 : 2 \n"
			, ""
			+ "| id   | type    | enum   |\n" 
			+ "| key  | Integer | anEnum |\n" 
			+ "| key2 | Integer |        |\n" 
			+ "anEnum : [1, 2, 3]\n"
			);
	}	
	
	@Test
	public void listEnumIntegerFail() {
		assertEquals("Value '4' does not occur in the enum 'anEnum' for /key[1]", assertThrows(ValidationException.class, () -> {
			parse(""
				+ "key : [1, 4] \n"
				, ""
				+ "| id  | type  | subtype | enum   |\n" 
				+ "| key | list  | Integer | anEnum |\n" 
				+ "anEnum : [1, 2, 3]\n"
				);
		}).getMessage());
	}

	@Test
	public void listEnumIntegerOk() {
		parse(""
			+ "key : [1, 2] \n"
			, ""
			+ "| id  | type  | subtype | enum   |\n" 
			+ "| key | list  | Integer | anEnum |\n" 
			+ "anEnum : [1, 2, 3]\n"
			);
	}	
	
	// ========================
	// CUSTOM VALIDATOR
	
	@Test
	public void customValidatorFail() {
		assertEquals("Is not 'abc'", assertThrows(ValidationException.class, () -> {
			parse(""
					+ "key : def \n"
					, ""
					+ "| id  | type  | \n" 
					+ "| key | String  | \n"
					, new CustomMustBeABCValidator()
					);
		}).getMessage());
	}

	@Test
	public void customValidatorOk() {
		parse(""
			+ "key : abc \n"
			, ""
			+ "| id  | type  | \n" 
			+ "| key | String  | \n"
			, new CustomMustBeABCValidator()
			);
	}	
	
	public static class CustomMustBeABCValidator implements Validator {

		@Override
		public void validate(TECL tecl, TECL schemaTECL, int schemaPropertyIdx, String schemaPropertyId, TECLSchema teclSchema) {
			String value = tecl.str(schemaPropertyId);
			if (!"abc".equals(value)) {
				throw new ValidationException("Is not 'abc'");
			}
		}
	}

	// ========================
	
	private TECL parse(String tecl, String tesd, Validator... validators) {
		if (logger.isDebugEnabled()) logger.debug("Parsing:\n" + tecl + new PrintLexer().lex(tecl));
		return TECL.parser()
				.schema(tesd, validators)
				.parse(tecl);
	}
}
