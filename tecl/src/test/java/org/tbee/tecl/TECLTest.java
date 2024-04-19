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
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbee.tecl.antlr.PrintLexer;

public class TECLTest {
	final Logger logger = LoggerFactory.getLogger(TECLTest.class);

	@Test
	public void emptyFile() {
		TECL tecl = parse("");
		assertNull(tecl.str("key"));
	}
	
	// ========================
	// PROPERTIES
	
	@Test
	public void simpleProperty() {
		String value = "value0$^*%";
		TECL tecl = parse("key : " + value + " \n");
		assertEquals("[" + value + "]", tecl.list("key", null, String.class).toString());
		assertEquals(value, tecl.str("key"));
	}

	@Test
	public void emptyProperty() {
		TECL tecl = parse("key : \n");
		assertEquals("", tecl.str("key"));
	}

	@Test
	public void unquotedProperty() {
		TECL tecl = parse("key : more words than one \n");
		assertEquals("morewordsthanone", tecl.str("key"));
	}

	@Test
	public void twoProperties() {
		TECL tecl = parse(""
				+ "key1 : value1\n"
				+ "key2 : value2\n"
				);
		assertEquals("value1", tecl.str("key1"));
		assertEquals("value2", tecl.str("key2"));
	}

	@Test
	public void getProperties() {
		TECL tecl = parse(""
				+ "key1 : value1\n"
				+ "key2 : value2\n"
		);
		List<String> keys = tecl.keys();
		keys.sort(Comparator.naturalOrder());
		assertEquals("[key1, key2]", keys.toString());
	}

	@Test
	public void propertyWithQuotedString() {
		String value = " value and more difficult!&# 345 symbols ";
		TECL tecl = parse("key : \"" + value + "\" \n");
		assertEquals("[" + value + "]", tecl.list("key", null, String.class).toString());
		assertEquals(value, tecl.str("key"));
	}

	@Test
	public void propertyWithQuotedStringWithQuoteInside() {
		TECL tecl = parse("key : \" val\\\"ue \" \n");
		assertEquals(" val\"ue ", tecl.str("key"));
	}

	@Test
	public void multilineProperty() {
		String value = "this \n"
				+ "is just!@#$%^&*()\n"
				+ "a text!\n";
		TECL tecl = parse(""
				+ "key : \"" + value + "\""
				);
		assertEquals(value, tecl.str("key"));
	}
	
	@Test
	public void stringPropertyDefault() {
		TECL tecl = parse("key : abc \n");
		assertEquals("default", tecl.str("otherKey", "default"));
	}
	
	@Test
	public void stringPropertyDefaultNotPresent() {
		TECL tecl = parse("key : \n");
		assertEquals("", tecl.str("key", "default"));
	}
	
	@Test
	public void stringPropertyDefaultNotUsed() {
		TECL tecl = parse("key : abc \n");
		assertEquals("abc", tecl.str("key", "default"));
	}
	
	@Test
	public void stringPropertyIdx0() {
		TECL tecl = parse("key : abc \n");
		assertEquals("abc", tecl.str(0, "key"));
	}
	
	@Test
	public void stringPropertyIdx0Default() {
		TECL tecl = parse("key : abc \n");
		assertEquals("default", tecl.str(0, "otherKey", "default"));
	}
	
	@Test
	public void stringPropertyIdx0DefaultNotUsed() {
		TECL tecl = parse("key : abc \n");
		assertEquals("abc", tecl.str(0, "key", "default"));
	}

	@Test
	public void integerProperty() {
		TECL tecl = parse("key : 123 \n");
		assertEquals(Integer.valueOf(123), tecl.get("key", Integer.class));
		assertEquals(Integer.valueOf(123), tecl.integer("key"));
	}

	@Test
	public void doubleProperty() {
		TECL tecl = parse("key : 10.10\n");
		assertEquals("10.10", tecl.str("key"));
		assertEquals(10.1, tecl.dbl("key"), 0.00001);
	}

	@Test
	public void integerPropertyDefault() {
		TECL tecl = parse("key : 1 \n");
		assertEquals(0, tecl.integer("otherKey", 0).intValue());
	}
	
	@Test
	public void integerPropertyDefaultNotPresent() {
		TECL tecl = parse("key : \n");
		assertEquals(0, tecl.integer("key", 0).intValue());
	}
	
	@Test
	public void integerPropertyDefaultNotUsed() {
		TECL tecl = parse("key : 1 \n");
		assertEquals(1, tecl.integer("key", 0).intValue());
	}
	
	@Test
	public void integerPropertyIdx0() {
		TECL tecl = parse("key : 1 \n");
		assertEquals(1, tecl.integer(0, "key").intValue());
	}
	
	@Test
	public void integerPropertyIdx0Default() {
		TECL tecl = parse("key : 1 \n");
		assertEquals(0, tecl.integer(0, "otherKey", 0).intValue());
	}
	
	@Test
	public void integerPropertyIdx0DefaultNotUsed() {
		TECL tecl = parse("key : 1 \n");
		assertEquals(1, tecl.integer(0, "key", 0).intValue());
	}
	
	@Test
	public void boolTrueProperty() {
		TECL tecl = parse("key : true \n");
		assertEquals(Boolean.TRUE, tecl.bool("key"));
	}
	
	@Test
	public void boolFalseProperty() {
		TECL tecl = parse("key : false \n");
		assertEquals(Boolean.FALSE, tecl.bool("key"));
	}
	
	@Test
	public void bdProperty() {
		TECL tecl = parse("key : 123.4 \n");
		assertEquals(BigDecimal.valueOf(123.4), tecl.bd("key"));
	}
	
	@Test
	public void biProperty() {
		TECL tecl = parse("key : 1234567890 \n");
		assertEquals(new BigInteger("1234567890"), tecl.bi("key"));
	}
	
	@Test
	public void localDateProperty() {
		TECL tecl = parse("key : 2020-06-20 \n");
		assertEquals(LocalDate.of(2020, 06, 20), tecl.localDate("key"));
	}
	
	@Test
	public void localDateTimeProperty() {
		TECL tecl = parse("key : \"2020-06-20T12:34:56\" \n");
		assertEquals(LocalDateTime.of(2020, 06, 20, 12, 34, 56), tecl.localDateTime("key"));
	}
	
	// ========================
	// CUSTOM TYPE

	
	@Test
	public void customTypeProperty() {
		TECLParser parser = TECL.parser();
		
		parser.addConvertFunction(Temperature.class, (str, def) -> {
			Temperature t = new Temperature();
			t.value = Integer.parseInt(str.replace("F", ""));
			t.unit = str.substring(str.length() - 1);
			return t;
		});
		
		TECL tecl = parser.parse("key : 123F \n");
		
		Temperature temperature = tecl.get("key", Temperature.class);
		assertEquals(123, temperature.value);
		assertEquals("F", temperature.unit);
		
		List<Temperature> temperatures = tecl.list("key", Temperature.class);
		assertEquals(123, temperatures.get(0).value);
		assertEquals("F", temperatures.get(0).unit);
	}
	public static class Temperature {
		int value;
		String unit;
	}
	
	// ========================
	// LIST
	
	@Test
	public void simpleList() {
		TECL tecl = parse("key : [aaa,bbb,ccc] \n");
		assertEquals("[aaa, bbb, ccc]", tecl.list("key", null, String.class).toString());
		assertEquals("aaa" , tecl.str(0, "key"));
		assertEquals("bbb" , tecl.str(1, "key"));
		assertEquals("ccc" , tecl.str(2, "key"));
		assertEquals("[aaa, bbb, ccc]" , tecl.strs("key").toString());
	}

	@Test
	public void integerList() {
		TECL tecl = parse("key : [1,2,3] \n");
		assertEquals("[1, 2, 3]", tecl.list("key", null, Integer.class).toString());
		assertEquals(1 , tecl.integer(0, "key").intValue());
		assertEquals(2 , tecl.integer(1, "key").intValue());
		assertEquals(3 , tecl.integer(2, "key").intValue());
		assertEquals("[1, 2, 3]" , tecl.integers("key").toString());
	}

	@Test
	public void integerListWithReference() {
		TECL tecl = parse(""
				+ "key : [1,$/someInt,3] \n"
				+ "someInt : 5 \n"
				);
		assertEquals(1 , tecl.integer(0, "key").intValue());
		assertEquals(5 , tecl.integer(1, "key").intValue());
		assertEquals(3 , tecl.integer(2, "key").intValue());
		assertEquals("[1, 5, 3]" , tecl.integers("key").toString());
	}

	@Test
	public void simpleListQuotedStrings() {
		String value = " value and more difficult!&# 345 symbols ";
		TECL tecl = parse("key : [\"aa a\",\""  + value + "\",ccc] \n");
		assertEquals("[aa a]" , tecl.list("key[0]", null, String.class).toString());
		assertEquals("aa a" , tecl.str(0, "key"));
		assertEquals(value , tecl.str(1, "key"));
		assertEquals("ccc" , tecl.str(2, "key"));
	}
	
	@Test
	public void listNotOverwrite() {
		assertThrows(IllegalStateException.class, () -> {
			TECL tecl = parse("" 
					+ "key : [aaa,bbb,ccc] \n"
					+ "key : [aaa,bbb,ccc] \n"
					);
		});
	}

	// ========================
	// COMMENTS
	
	@Test
	public void comment() {
		TECL tecl = parse("# this is a comment");
	}
	
	@Test
	public void commentPlusNewline() {
		TECL tecl = parse("# this is a comment\n");
	}

	@Test
	public void twoPropertiesWithComments() {
		TECL tecl = parse(""
				+ "# comment\n"
				+ "key1 : value1 # comment\n"
				+ "key2 : value2 # comment\n"
				+ "# comment"
				);
		assertEquals("value1", tecl.str("key1"));
		assertEquals("value2", tecl.str("key2"));
	}
	
	
	// ========================
	// GROUP
	
	@Test
	public void emptyGroup() {
		TECL tecl = parse("groupId { }");
		assertEquals("[/groupId[0]/]", tecl.list("groupId", null, null).toString());
		assertEquals("groupId", tecl.grp("groupId").getId());
	}
	
	@Test
	public void groupWithContent() {
		TECL tecl = parse(""
				+ "groupId { \n" 
				+ "    key : value\n"
				+ "}\n");
		assertEquals("[value]", tecl.list("groupId/key", null, String.class).toString());
		assertEquals("groupId", tecl.grp("groupId").getId());
		assertEquals("value", tecl.grp("groupId").str("key"));
	}
	
	@Test
	public void notExistingGroup() {
		TECL tecl = parse("");
		assertNull(tecl.list("groupId", null, null));
		assertNull(tecl.list("group1/group2", null, null));
		assertTrue(tecl.grp("groupId").getId().contains("not exist"));
		assertTrue(tecl.grp("group1/group2").getId().contains("not exist"));
		assertNull(tecl.str("group1/group2/key"));
	}
	
	@Test
	public void identicalGroups() {
		TECL tecl = parse(""
				+ "groupId { \n" 
				+ "    key : value1\n"
				+ "}\n"
				+ "groupId { \n" 
				+ "    key : value2\n"
				+ "}\n"
				);
		assertEquals("[/groupId[0]/]", tecl.list("groupId[0]", null, null).toString());
		assertEquals("[/groupId[0]/, /groupId[1]/]", tecl.list("groupId", null, null).toString());
		assertEquals("value1", tecl.grp(0, "groupId").str("key"));
		assertEquals("value2", tecl.grp(1, "groupId").str("key"));
		assertEquals(2, tecl.grps("groupId").size());
	}


	@Test
	public void identicalGroupsKeys() {
		TECL tecl = parse(""
				+ "groupId { \n"
				+ "    key : value1\n"
				+ "}\n"
				+ "groupId { \n"
				+ "    key : value2\n"
				+ "}\n"
		);
		assertEquals("[]", tecl.keys().toString());
	}


	@Test
	public void nestedGroups() {
		TECL tecl = parse(""
				+ "groupId1 { \n"
				+ "    groupId2 {"
				+ "        groupId3 { "
				+ "        }\n"
				+ "    }\n"
				+ "}\n"
				);
		assertEquals("[/groupId1[0]/groupId2[0]/groupId3[0]/]", tecl.list("/groupId1/groupId2/groupId3", null, null).toString());
		assertEquals("groupId1", tecl.grp("groupId1").getId());
		assertEquals("groupId2", tecl.grp("groupId1").grp("groupId2").getId());
		assertEquals("groupId3", tecl.grp("groupId1").grp("groupId2").grp("groupId3").getId());
	}
	
	// ========================
	// TABLE
	
	@Test
	public void table() {
		TECL tecl = parse(""
				+ "| id  | type          | \n "
				+ "| id1 | string        | \n"
				+ "| id2 | int           | \n"				
				+ "| id3 | date          | \n"				
				+ "| id3 | [aaa,bbb,ccc] | \n"				
				);
		assertEquals("[id1]", tecl.list("id[0]", null, String.class).toString());
		assertEquals("[int]", tecl.list("type[1]", null, String.class).toString());
		assertEquals("[aaa, bbb, ccc]", tecl.list("type[3]", null, String.class).toString());
		assertEquals("id1", tecl.str("id"));
		assertEquals("id1", tecl.str(0, "id"));
		assertEquals("int", tecl.str(1, "type"));
		assertEquals("[id, type]", tecl.keys().toString());
	}
	
	@Test
	public void tableInGroup() {
		TECL tecl = parse(""
				+ "group { \n "
				+ "    | id  | type            | \n "
				+ "    | id1 | string          | \n"
				+ "    | id2 | int             | \n"
				+ "    | id3 | date            | \n"
				+ "    | id3 | [aaa,bbb,ccc]   | \n"
				+ "    | id4 | \"<xml>text</xml>\" | \n"
				+ "}\n"
				);
		assertEquals("[id1]", tecl.list("/group/id[0]", null, String.class).toString());
		assertEquals("[int]", tecl.list("/group/type[1]", null, String.class).toString());
		assertEquals("[aaa, bbb, ccc]", tecl.list("/group/type[3]", null, String.class).toString());
		assertEquals("id1", tecl.str("/group/id"));
		assertEquals("id1", tecl.str(0, "/group/id"));
		assertEquals("int", tecl.str(1, "/group/type"));
	}
	
	
	@Test
	public void tableWithComments() {
		TECL tecl = parse(""
				+ "    | id  | type   | # comment \n "
				+ "    | id1 | string | # comment \n"
				+ "    # comment \n"
				+ "    | id2 | int    | \n"				
				);
		assertEquals("id1", tecl.str(0, "id"));
		assertEquals("int", tecl.str(1, "type"));
	}

	@Test
	public void tableInGroupWithComments() {
		TECL tecl = parse(""
				+ "group { \n "
				+ "    | id  | type   | # comment \n "
				+ "    | id1 | string | # comment \n"
				+ "    # comment \n"
				+ "    | id2 | int    | \n"
				+ "} \n "
		);
		assertEquals("id1", tecl.grp("group").str(0, "id"));
		assertEquals("int", tecl.grp("group").str(1, "type"));
	}

	@Test
	public void twoTablesAreNotAllowed() {
		assertThrows(IllegalStateException.class, () -> {
			TECL tecl = parse(""
					+ "| id  | type   | \n "
					+ "| id1 | string | \n"
					+ "| id2 | int    | \n"				
					+ "\n"				
					+ "| len  | dep   | \n "
					+ "| 10   | 20    | \n"
					);
		});
	}

	@Test
	public void twoTablesAreNotAllowed2() {
		assertThrows(IllegalStateException.class, () -> {
			TECL tecl = parse(""
					+ "| id  | type   | \n "
					+ "| id1 | string | \n"
					+ "| id2 | int    | \n"				
					+ "key : value\n"				
					+ "| len  | dep   | \n "
					+ "| 10   | 20    | \n"
					);
		});
	}

	@Test
	public void tableWithList() {
		TECL tecl = parse(""
				+ "| type      | \n "
				+ "| [a,b]     | \n"
				+ "| \"[a,b]\" | \n"
				+ "| int       | \n"				
				);
		assertEquals("a", tecl.grp(0, "|type|").str(0, "type"));
		assertEquals("b", tecl.grp(0, "|type|").str(1, "type"));
		assertEquals("[a,b]", tecl.str(1, "type"));
		assertEquals("int", tecl.str(2, "type"));
	}

	@Test
	public void tableAndGroupConflict() {
		TECL tecl = parse(""
				+ "type { \n"
				+ "    key : value\n"				
				+ "} \n"
				+ "\n"
				+ "| type      | \n "
				+ "| [a,b]     | \n"
				+ "\n"
				+ "type { \n"
				+ "    key : value\n"				
				+ "} \n"
				);
		assertEquals(1, tecl.countGrp("|type|"));
		assertEquals(2, tecl.countGrp("type"));
	}

	// ========================
	// PARAMETERS
	
	@Test
	public void oneAttributeProperty() {
		TECL tecl = parse("key(x=0) : value\n");
		// assert value 
		assertEquals("[value]", tecl.list("key", null, String.class).toString());
		assertEquals("value", tecl.str("key"));
		// assert arguments 
		assertEquals(Integer.valueOf(0), tecl.attr("key").integer("x"));
	}

	@Test
	public void manyAttributesProperty() {
		TECL tecl = parse("key(a=0 b=1.23 c=\"a\" d=\"2020-06-20T12:34:56\") : value\n");
		// assert value 
		assertEquals("[value]", tecl.list("key", null, String.class).toString());
		assertEquals("value", tecl.str("key"));
		// assert arguments 
		assertEquals(Integer.valueOf(0), tecl.attr("key").integer("a"));
		assertEquals(BigDecimal.valueOf(1.23), tecl.attr("key").bd("b"));
		assertEquals("a", tecl.attr("key").str("c"));
		assertEquals(LocalDateTime.of(2020, 06, 20, 12, 34, 56), tecl.attr("key").localDateTime("d"));
	}

	// ========================
	// CONDITIONS
	
	@Test
	public void conditionedPropertyWithMatchingCondition() {
		TECL tecl = parse("key[sys=A] : value\n");
		assertEquals("[value]", tecl.list("key", null, String.class).toString());
		assertEquals("value", tecl.str("key"));
	}

	@Test
	public void conditionedPropertyWithNotMatchingCondition() {
		TECL tecl = parse("key[sys=other] : value\n");
		assertNull(tecl.list("key", null, String.class));
		assertEquals(null, tecl.str("key"));
	}

	@Test
	public void conditionedPropertyWithMatching2Conditions() {
		TECL tecl = parse("key[sys=A & env=test] : value\n");
		assertEquals("value", tecl.str("key"));
	}

	@Test
	public void conditionedPropertyWithNotMatching2Conditions() {
		TECL tecl = parse("key[sys=other & env=test] : value\n");
		assertEquals(null, tecl.str("key"));
	}

	@Test
	public void conditionedPropertyWithFirstBestMatch() {
		TECL tecl = parse(""
				+ "key[sys=A & env=test] : value1\n"
				+ "key[sys=A] : value2\n"
				);
		assertEquals("value1", tecl.str("key"));
		assertEquals("[key]", tecl.keys().toString());
	}

	@Test
	public void conditionedPropertyWithLastBestMatch() {
		TECL tecl = parse(""
				+ "key[sys=A] : value2\n"
				+ "key[sys=A & env=test] : value1\n"
				);
		assertEquals("value1", tecl.str("key"));
	}

	public void conditionedGroupWithMatchingCondition() {
		TECL tecl = parse("groupId[sys=A] { }");
		assertEquals("groupId", tecl.grp("groupId").getId());
	}

	@Test
	public void conditionedGroupWithNotMatchingCondition() {
		TECL tecl = parse(""
				+ "groupId[sys=other] { } \n"
				);
		assertTrue(tecl.grp("groupId").getId().contains("not exist"));
	}	

	@Test
	public void conditioned2GroupsWithMatchingCondition() {
		TECL tecl = parse("" 
				+ "groupId[sys=A & env=test] { \n"
				+ "    key : group1 \n"
				+ "} \n"
				+ "groupId[sys=A] { \n"
				+ "    key : group2 \n"
				+ "} \n"
				);
		assertEquals("group1", tecl.grp(0, "groupId").str("key"));
		assertEquals("group2", tecl.grp(1, "groupId").str("key"));
	}

	@Test
	public void conditionedList() {
		TECL tecl = parse("key[sys=A] : [aaa,bbb]\n");
		assertEquals("aaa", tecl.str(0, "key"));
		assertEquals("bbb", tecl.str(1, "key"));
	}

	@Test
	public void conditionedListNoMath() {
		TECL tecl = parse("key[sys=other] : [aaa,bbb]\n");
		assertNull(tecl.str(0, "key"));
		assertNull(tecl.str(1, "key"));
	}

	
	// ========================
	// indexOf
	
	@Test
	public void indexOf() {
		TECL tecl = parse(""
				+ "group1 { \n"
				+ "    key : value1 \n "
				+ "    group2 {"
				+ "        key : value2 \n "
				+ "        group3 { \n"
				+ "            key : value3 \n "
				+ "            | id  | val  | \n "
				+ "            | id0 | val0 | \n"
				+ "            | id1 | val1 | \n"
				+ "            | id2 | val2 | \n"
				+ "        }\n"
				+ "    }\n"
				+ "}\n"
				);
		
		assertEquals(1, tecl.grp("/group1/group2/group3").indexOf("id", "id1"));
		assertEquals(1, tecl.strs("/group1/group2/group3/id").indexOf("id1"));
		assertEquals(-1, tecl.grp("/group1/notExist/group3").indexOf("id", "id1"));
		assertEquals("val1", tecl.str("/group1/group2/group3/id", "id1", "/group1/group2/group3/val", null));
		assertEquals("val1", tecl.grp("/group1/group2/group3").str("id", "id1", "val", null));
	}
	
	// ========================
	// GET
	
	@Test
	public void get() {
		TECL tecl = parse(""
				+ "group1 { \n"
				+ "    key : value1 \n "
				+ ""
				+ "    group2 {"
				+ "        key : value2 \n "
				+ ""
				+ "        group3 { \n"
				+ "            key : value3 \n "
				+ "        }\n"
				+ "    }\n"
				+ ""
				+ "    group2 {"
				+ "        key : value2a \n "
				+ "    }\n"
				+ "    \n"
				+ "    | id  | \n "
				+ "    | id0 | \n"
				+ "    | id1 | \n"
				+ "    | id2 | \n"
				+ "}\n"
				);
		assertEquals("value2", tecl.get("group1/group2/key", null, String.class));
		assertEquals("value1", tecl.get("group1/group2/../key", null, String.class));
		assertEquals("value2a", tecl.get("group1/group2[1]/key", null, String.class));
		assertEquals("id1", tecl.get("group1/id[1]", null, String.class));
		
		// Start half way
		TECL group2TECL = tecl.grp("group1").grp("group2");
		assertEquals("value3", group2TECL.get("group3/key", null, String.class));
		assertEquals("id2", group2TECL.get("../id[2]", null, String.class));
		
		// access a group
		assertEquals("[/group1[0]/group2[0]/, /group1[0]/group2[1]/]", tecl.list("group1/group2", null,null).toString());
	}
	
	// ========================
	// REFERENCE
	
	@Test
	public void addArgs() {
		TECL tecl = parse(""
				+ "key : value \n "
				+ "group1 { \n"
				+ "    key1 : value1 \n "
				+ "}\n"
				);
		tecl.addCommandLineArguments(new String[]{"-key2", "value2", "-/group1/key3", "value3", "-/group2/key4", "value4", "-/group2/key4", "value4a"});
		assertEquals("value", tecl.str("key"));
		assertEquals("value2", tecl.str("key2"));
		assertEquals("value4", tecl.grp("group2").str("key4"));
		assertEquals("value4a", tecl.grp("group2").str("key4[1]"));
	}
	
	
	// ========================
	// REFERENCE
	
	@Test
	public void reference() {
		TECL tecl = parse(""
				+ "group1 { \n"
				+ "    key : $/group2/key \n "
				+ "}\n"
				+ "group2 {"
				+ "    key : value2 \n "
				+ "}\n"
				);
		assertEquals("[value2]", tecl.list("/group1/key", null, String.class).toString());
		assertEquals("[value2]", tecl.list("group1/key", null, String.class).toString());
		assertEquals("value2", tecl.grp("group1").str("key"));
	}
	
	@Test
	public void referenceKeyInTable() {
		TECL tecl = parse(""
				+ "| id  | type        | \n "
				+ "| id1 | $group1/key | \n"
				+ "\n"
				+ "group1 { \n"
				+ "    key : $group2/key \n "
				+ "}\n"
				+ "group2 {"
				+ "    key : value2 \n "
				+ "}\n"
				);
		assertEquals("[value2]", tecl.list("/type[0]", null, String.class).toString());
		assertEquals("value2", tecl.str(0, "type"));
	}
	
	@Test
	public void referenceGrpInTable() {
		TECL tecl = parse(""
				+ "| id  | type   | \n "
				+ "| id1 | $group | \n"
				+ "\n"
				+ "group { \n"
				+ "    key : value \n "
				+ "}\n"
				);
		assertEquals("[/group[0]/]", tecl.list("/type[0]", null, null).toString());
		assertEquals("[value]", tecl.list("/type[0]/key", null, String.class).toString());
		assertEquals("value", tecl.grp(0, "type").str("key"));
		assertEquals("value", tecl.grp("group").str("key"));
	}
	
	@Test
	public void referenceGrpInTableOverlap() {
		TECL tecl = parse(""
				+ "| id  | type   | \n "
				+ "| id1 | $group | \n"
				+ "\n"
				+ "group { \n"
				+ "    key : value1 \n "
				+ "}\n"
				+ "\n"
				+ "type { \n"
				+ "    key : value2 \n "
				+ "}\n"
				);
		
		// The column name 'type' in the table is the same as the group name 'type' 
		// This normally is no problem, because we have separate sets of properties and groups.
		// But here the column type is referring through a reference to a group, so property type becomes a group.
		// And that conflicts with the group 'type'.
		// The group wins, so in order to access the property as a group, you need to get the raw value and push that through var.
		// Better is not to do this of course :-)
		
		// TODO xxx
		assertEquals("value2", tecl.grp("type").str("key"));
		String raw = tecl.raw(0, "type", null);
		assertEquals("$group", raw);
		assertEquals("value1", tecl.grp(raw.substring(1)).str("key"));
		assertEquals("value1", tecl.grp("group").str("key"));
		assertEquals(1, tecl.countGrp("type"));
	}
	
	@Test
	public void referenceToAList() {
		TECL tecl = parse(""
				+ "key : $list \n"
				+  "list : [aaa,bbb,ccc] \n"
				);
		assertEquals("[aaa, bbb, ccc]", tecl.list("/key", null, String.class).toString());
		assertEquals("[ccc]", tecl.list("/key[2]", null, String.class).toString());
		assertEquals("ccc", tecl.str(2, "key"));
		assertEquals("bbb", tecl.strs("key").get(1));
		assertEquals("[aaa, bbb, ccc]", tecl.strs("key").toString());
	}
	
	@Test
	public void referenceToIndexedInTable() {
		TECL tecl = parse(""
				+ "| id  | type  | \n "
				+ "| id0 | int   | \n"
				+ "| id1 | $list | \n"
				+ "\n"
				+  "list : [aaa,bbb,ccc] \n"
				);
		// A list is an index property
		// And a table is an indexed property
		// So we are having a double indexed property here; first to get to the row and then inside the list		
		assertEquals("[aaa, bbb, ccc]", tecl.list("/type[1]", null, String.class).toString());
		assertEquals("[ccc]", tecl.list("/type[1][2]", null, String.class).toString());

		String raw = tecl.raw(1, "type", null);
		assertEquals("$list", raw);
		assertEquals("[aaa, bbb, ccc]", tecl.strs(raw.substring(1)).toString());
	}
	
	@Test
	public void envAsValue() {
		String value = System.getenv("USERNAME");
		TECL tecl = parse(""
				+ "key : $env@USERNAME\n "
				);
		assertEquals("[" + value + "]", tecl.list("key", null, String.class).toString());
		assertEquals(value, tecl.str("key"));
	}
	
	@Test
	public void env() {
		String value = System.getenv("USERNAME");
		TECL tecl = parse("");
		assertEquals(value, tecl.get("env@USERNAME", String.class));
	}
	
	@Test
	public void sysAsKey() {
		String value = System.getProperty("user.language");
		TECL tecl = parse(""
				+ "key : $sys@user.language # comment\n"
				);
		assertEquals("[" + value + "]", tecl.list("key", String.class).toString());
		assertEquals(value, tecl.str("key"));
	}
	
	@Test
	public void sys() {
		String value = System.getProperty("user.language");
		TECL tecl = parse("");
		assertEquals(value, tecl.get("sys@user.language", String.class));
	}


	// ========================
	// decrypt
	
	@Test
	public void encrypt() throws NoSuchAlgorithmException {
		
		// Keypair
		List<String> keyPair = EncryptionHelper.me.generateKeyPair(2048);
		String publicBase64 = keyPair.get(0);
		String privateBase64 = keyPair.get(1);
		
		// Encode
		String decoded = "Some text " + System.currentTimeMillis();
		String encoded = EncryptionHelper.me.encode(decoded, publicBase64);
		assertTrue(!encoded.equals(decoded));
		
		// Decode
		String decodedAgain = EncryptionHelper.me.decode(encoded, privateBase64);
		assertEquals(decodedAgain, decoded);
	}
	
	@Test
	public void decryptWithoutConfig() {
		assertThrows(IllegalStateException.class, () -> {
			TECL tecl = parse("key : value \n");
			tecl.decrypt("key");
		});
	}
	
	@Test
	public void decryptURL() throws IOException {
		TECL tecl = TECL.parser()
			.decryptKey(this.getClass().getResource("privateKey.txt"))
			.parse("" 
					+ "group {\n" 
					+ "    key : \"ACsM4Dn8e9Ck9Z7Q9BUpcmILcIR5eJYgqGNr22cQcOvHTRNRQHylDuXqHLaSku8MKLz/itNixBbOhZWNTJ1Mzn3WA6Hv4dLOo/719AxVzli6ru6+BZymesDzdpJIG1PxA+YZW7hefyxwpwo/DzLc8GtG60lpd9rQbgUNeKaJBWFtBfPjYb2YZpoiBIqYSsvMnHv0reePVcLw+XvOl6V1o0mxlS5sc8TrsBYwje4AfuvBaBDj2Gj5Jkx2s8CdOqNMW04B+GReTcEaJRgPdDGh09ZskhaYXuOQIh1CX5/5SEpnQq2fjEvGzUdybFjGjtNksduk8gap0m0idBeCGHJ/kw==\" \n"
					+ "}\n"
					);
		assertEquals("This is the text to encrypt", tecl.decrypt("/group/key"));
	}

	// ========================
	// PREPROCESSING

	@Test
	public void differentVersions() {
		assertThrows(IllegalStateException.class, () -> {
			TECL tecl = parse(""
					+ "@version 1\n"
					+ "key : value\n"
					+ "@version 2\n"
					);
		});
	}

	@Test
	public void sameVersions() {
		TECL tecl = parse(""
				+ "@version 1\n"
				+ "key : value\n"
				+ "@version 1\n"
				);
	}

	@Test
	public void supportedVersion1() {
		TECL tecl = parse(""
				+ "@version 1\n"
				);
	}

	@Test
	public void notSupportedVersion() {
		assertThrows(IllegalStateException.class, () -> {
			TECL tecl = parse(""
					+ "@version 2\n"
					);
		});
	}

	@Test
	public void simpleURLImport() {
		TECL tecl = parse(""
				+ "key1 : value1\n"
				+ "@import " + this.getClass().getResource("test.tecl") + "\n"
				+ "key2 : value2\n"
				);
		assertEquals("value1", tecl.str("key1"));
		assertEquals("TECL rulez", tecl.str("title"));
		assertEquals("value2", tecl.str("key2"));
	}

	@Test
	public void simpleFile() {
		System.out.println(new File(".").getAbsolutePath());
		TECL tecl = parse(""
				+ "key1 : value1\n"
				+ "@import src/test/resources/org/tbee/tecl/import2.tecl\n"
				+ "key2 : value2\n"
				);
		assertEquals("import2", tecl.str("import2"));
	}

	@Test
	public void nestedImportFile() {
		System.out.println(new File(".").getAbsolutePath());
		TECL tecl = parse(""
				+ "key1 : value1\n"
				+ "@import src/test/resources/org/tbee/tecl/import1.tecl\n"
				+ "key2 : value2\n"
				);
		assertEquals("import1", tecl.str("import1"));
		assertEquals("import2", tecl.str("import2"));
	}

	
	// ========================
	// FILE

	@Test
	public void testFile() throws IOException {
		TECL tecl = TECL.parser()
				.addParameter("env", "production")
				.parse(this.getClass().getResourceAsStream("test.tecl"), Charset.forName("UTF-8"));
		assertEquals("TECL rulez", tecl.str("title"));
		assertEquals("escaped\"quote", tecl.str("escaped"));
		assertEquals(LocalDateTime.of(2020, 9, 12, 12, 34, 56), tecl.localDateTime("releaseDateTime"));
		assertEquals("prd", tecl.str("/servers/settings[4]/datasource"));
		
		assertEquals(3, tecl.grp("/servers").indexOf("name", "gamma"));
		assertEquals(Integer.valueOf(12), tecl.integer("/servers/name", "gamma", "/servers/maxSessions", null));
		assertEquals(Integer.valueOf(12), tecl.grp("/servers").integer("name", "gamma", "maxSessions", null));
		
		// multiline text 
		assertEquals(" \n" + 
				"					This should not be a problem\n" + 
				"					Having indented multiple lines \n" + 
				"					" + 
				"", tecl.str("/indented/description1"));
		assertEquals("This should not be a problem\nHaving indented multiple lines ", tecl.str("/indented/description2"));
		assertEquals("{ \"bla\" : 3; } Braces and quotes should not be a problem", tecl.str("/indented/description3"));
	}
	
	// ========================
	
	private TECL parse(String s) {
		if (logger.isDebugEnabled()) logger.debug("Parsing:\n" + s + new PrintLexer().lex(s));
		return TECL.parser()
				.addParameter("sys", "A")
				.addParameter("env", "test")
				.parse(s);
	}
}
