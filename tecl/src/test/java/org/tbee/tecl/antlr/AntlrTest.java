package org.tbee.tecl.antlr;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbee.tecl.TECL;

public class AntlrTest {
	final Logger logger = LoggerFactory.getLogger(AntlrTest.class);

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
	
	@Test
	public void localDateProperty() {
		TECL tecl = parse("key : 2020-06-20 \n");
		assertEquals(LocalDate.of(2020, 06, 20), tecl.localDate("key"));
	}
	
	@Test
	public void localDateTmeProperty() {
		TECL tecl = parse("key : \"2020-06-20T12:34:56\" \n");
		assertEquals(LocalDateTime.of(2020, 06, 20, 12, 34, 56), tecl.localDateTime("key"));
	}
	
	
	// ========================
	// LIST
	
	@Test
	public void simpleList() {
		TECL tecl = parse("key : [aaa,bbb,ccc] \n");
		assertEquals("aaa" , tecl.str(0, "key"));
		assertEquals("bbb" , tecl.str(1, "key"));
		assertEquals("ccc" , tecl.str(2, "key"));
		assertEquals("[aaa, bbb, ccc]" , tecl.strs("key").toString());
	}

	@Test
	public void integerList() {
		TECL tecl = parse("key : [1,2,3] \n");
		assertEquals(1 , tecl.integer(0, "key").intValue());
		assertEquals(2 , tecl.integer(1, "key").intValue());
		assertEquals(3 , tecl.integer(2, "key").intValue());
		assertEquals("[1, 2, 3]" , tecl.integers("key").toString());
	}

	@Test
	public void integerListWithVariable() {
		TECL tecl = parse(""
				+ "key : [1,$.someInt,3] \n"
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
		assertEquals(2, tecl.grps("groupId").size());
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
		assertEquals("[a,b]", tecl.str(0, "type"));
		assertEquals("int", tecl.str(1, "type"));
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

	@Test
	public void conditionedPropertyWithFirstBestMatch() {
		TECL tecl = parse(""
				+ "key[sys=A & env=test] : value1\n"
				+ "key[sys=A] : value2\n"
				);
		assertEquals("value1", tecl.str("key"));
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
	// VAR
	
	@Test
	public void var() {
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
		assertEquals("value2", tecl.var("$group1.group2.key", null, (s) -> s));
		assertEquals("value1", tecl.var("$group1.group2.^.key", null, (s) -> s));
		assertEquals("value2a", tecl.var("$group1.group2[1].key", null, (s) -> s));
		assertEquals("id1", tecl.var("$group1.id[1]", null, (s) -> s));
		
		// Start half way
		TECL group2TECL = tecl.grp("group1").grp("group2");
		assertEquals("value3", group2TECL.var("$.group3.key", null, (s) -> s));
		assertEquals("id2", group2TECL.var("$.^.id[2]", null, (s) -> s));
		
		// access a group
		assertEquals("value2", tecl.var("$group1.group2").str("key"));
	}
	
	@Test
	public void reference() {
		TECL tecl = parse(""
				+ "group1 { \n"
				+ "    key : $group2.key \n "
				+ "}\n"
				+ "group2 {"
				+ "    key : value2 \n "
				+ "}\n"
				);
		assertEquals("value2", tecl.grp("group1").str("key"));
		assertEquals("value2", tecl.var("$group1.key", null, (s) -> s));
	}
	
	@Test
	public void referenceKeyInTable() {
		TECL tecl = parse(""
				+ "| id  | type        | \n "
				+ "| id1 | $group1.key | \n"
				+ "\n"
				+ "group1 { \n"
				+ "    key : $group2.key \n "
				+ "}\n"
				+ "group2 {"
				+ "    key : value2 \n "
				+ "}\n"
				);
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
		// But here the column type is referring through a variable to a group, so property type becomes a group.
		// And that conflicts with the group 'type'.
		// The group wins, so in order to access the property as a group, you need to get the raw value and push that through var.
		// Better is not to do this of course :-)
		
		assertEquals("value2", tecl.grp("type").str("key"));
		String raw = tecl.raw(0, "type", null);
		assertEquals("$group", raw);
		assertEquals("value1", tecl.var(raw).str("key"));
		assertEquals("value1", tecl.grp("group").str("key"));
		assertEquals(1, tecl.countGrp("type"));
	}
	
	@Test
	public void referenceToGroups() {
		TECL tecl = parse(""
				+ "key : $group \n"
				+ "\n"
				+ "group { \n"
				+ "    key : value1 \n "
				+ "}\n"
				+ "group {"
				+ "    key : value2 \n "
				+ "}\n"
				);
		assertEquals("value2", tecl.grps("key").get(1).str("key"));
	}
	
	@Test
	public void referenceToAList() {
		TECL tecl = parse(""
				+ "key : $list \n"
				+  "list : [aaa,bbb,ccc] \n"
				);
		assertEquals("ccc", tecl.str(2, "key"));
		assertEquals("bbb", tecl.strs("key").get(1));
		assertEquals("[aaa, bbb, ccc]", tecl.strs("key").toString());
	}
	
	@Test
	public void referenceToIndexedInTable() {
		TECL tecl = parse(""
				+ "| id  | type | \n "
				+ "| id0 | int  | \n"
				+ "| id1 | $key | \n"
				+ "\n"
				+  "key : [aaa,bbb,ccc] \n"
				);
		// A list is an index property
		// And a table is an indexed property
		// So we are having a double indexed property here; first to get to the row and then the list
		
		String raw = tecl.raw(1, "type", null);
		assertEquals("$key", raw);
		assertEquals("[aaa, bbb, ccc]", tecl.vars(raw, (s) -> s).toString());
		
// TBEERNOT: this may very well be a common scenario, how to better solve this? 		
//		assertEquals("ccc", tecl.grp(1, "|type|").str(2, "type"));
//		assertEquals("bbb", tecl.grp(1, "type").str(1, "type"));
	}
	
	@Test
	public void envAsValue() {
		String value = System.getenv("USERNAME");
		TECL tecl = parse(""
				+ "key : $env@USERNAME\n "
				);
		assertEquals(value, tecl.str("key"));
	}
	
	@Test
	public void env() {
		String value = System.getenv("USERNAME");
		TECL tecl = parse("");
		assertEquals(value, tecl.var("$env@USERNAME", null, (s) -> s));
	}
	
	@Test
	public void sysAsKey() {
		String value = System.getProperty("user.language");
		TECL tecl = parse(""
				+ "key : $sys@user.language # comment\n"
				);
		assertEquals(value, tecl.str("key"));
	}
	
	@Test
	public void sys() {
		String value = System.getProperty("user.language");
		TECL tecl = parse("");
		assertEquals(value, tecl.var("$sys@user.language", null, (s) -> s));
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

	
	// ========================
	// FILE

	@Test
	public void testFile() throws IOException {
		TECL tecl = TECL.parser().parse(this.getClass().getResourceAsStream("test.tecl"), Charset.forName("UTF-8"));
		assertEquals("TECL rulez", tecl.str("title"));
		assertEquals("escaped\"quote", tecl.str("escaped"));
		assertEquals(LocalDateTime.of(2020, 9, 12, 12, 34, 56), tecl.localDateTime("releaseDateTime"));
	}
	
	// ========================
	
	private TECL parse(String s) {
		logger.atDebug().log("Parsing:\n" + s + new PrintLexer().lex(s));
		return TECL.parser()
				.addParameter("sys", "A")
				.addParameter("env", "test")
				.parse(s);
	}
}

