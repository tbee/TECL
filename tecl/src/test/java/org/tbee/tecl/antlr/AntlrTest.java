package org.tbee.tecl.antlr;


import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.*;
import org.junit.Test;
import org.tbee.tecl.TECL;

public class AntlrTest {

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
	public void propertyWithQuotedString() {
		String value = " value and more difficult!&# 345 symbols ";
		TECL tecl = parse("key : \"" + value + "\" \n");
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
		assertEquals(Integer.valueOf(123), tecl.integer("key"));
	}
	
	@Test
	public void doubleProperty() {
		TECL tecl = parse("key : 123.4 \n");
		assertEquals(Double.valueOf(123.4), tecl.dbl("key"));
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
		assertEquals("groupId", tecl.grp("groupId").getId());
	}
	
	@Test
	public void groupWithContent() {
		TECL tecl = parse(""
				+ "groupId { \n" 
				+ "    key1 : value1\n"
				+ "}\n");
		assertEquals("groupId", tecl.grp("groupId").getId());
		assertEquals("value1", tecl.grp("groupId").str("key1"));
	}
	
	@Test
	public void notExistingGroup() {
		TECL tecl = parse("");
		assertTrue(tecl.grp("groupId").getId().contains("not exist"));
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
		assertEquals("value1", tecl.grp(0, "groupId").str("key"));
		assertEquals("value2", tecl.grp(1, "groupId").str("key"));
	}
	
	@Test
	public void nestedGroups() {
		TECL tecl = parse(""
				+ "groupId1 { \n"
				+ "    groupId2 {"
				+ "        groupId3 { }"
				+ "        }\n"
				+ "    }\n"
				+ "}\n"
				);
		assertEquals("groupId1", tecl.grp("groupId1").getId());
		assertEquals("groupId2", tecl.grp("groupId1").grp("groupId2").getId());
		assertEquals("groupId3", tecl.grp("groupId1").grp("groupId2").grp("groupId3").getId());
	}
	
	// ========================
	// TABLE
	
	@Test
	public void table() {
		TECL tecl = parse(""
				+ "| id  | type   | \n "
				+ "| id1 | string | \n"
				+ "| id2 | int    | \n"				
				+ "| id3 | date   | \n"				
				);
		assertEquals("id1", tecl.str(0, "id"));
		assertEquals("int", tecl.str(1, "type"));
		assertEquals("int", tecl.str("id", "id2", "type"));
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

	// ========================
	// CONDITIONS
	
	@Test
	public void conditionedPropertyWithMatchingCondition() {
		TECL tecl = parse("key[sys=A] : value\n");
		assertEquals("value", tecl.str("key"));
	}

	@Test
	public void conditionedPropertyWithNotMatchingCondition() {
		TECL tecl = parse("key[sys=other] : value\n");
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

	public void conditionedGroupWithMatchingCondition() {
		TECL tecl = parse("groupId[sys=A] { }");
		assertEquals("groupId", tecl.grp("groupId").getId());
	}

	@Test
	public void conditionedGroupWithNotMatchingCondition() {
		TECL tecl = parse(""
				+ "groupId[sys=other] {\n"
				+ "     key : value\n"
				+ "} \n"
				);
		assertTrue(tecl.grp("groupId").getId().contains("not exist"));
	}	

	// ========================
	// FILE

	@Test
	public void testFile() throws IOException {
		TECL tecl = TECL.parser().parse(this.getClass().getResourceAsStream("test.tecl"), Charset.forName("UTF-8"));
		assertEquals("TECL rulez", tecl.str("title"));
		assertEquals("escaped\"quote", tecl.str("escaped"));
	}
	
	// ========================
	
	private TECL parse(String s) {
		System.out.println(s); new PrintLexer().lex(s);
		return TECL.parser()
				.addParameter("sys", "A")
				.addParameter("env", "test")
				.parse(s);
	}
}

