package org.tbee.tecl.antlr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.tbee.tecl.TECL;

public class AntlrTest {

	@Test
	public void empty() {
		TECL tecl = parse("");
		assertNull(tecl.str("key"));
	}
	
	// ========================
	// PROPERTIES
	
	@Test
	public void simpleProperty() {
		TECL tecl = parse("key = value");
		assertEquals("value", tecl.str("key"));
	}

	@Test
	public void emptyProperty() {
		TECL tecl = parse("key = ");
		assertEquals("", tecl.str("key"));
	}

	@Test
	public void twoProperties() {
		TECL tecl = parse(""
				+ "key1 = value1\n"
				+ "key2 = value2\n"
				);
		assertEquals("value1", tecl.str("key1"));
		assertEquals("value2", tecl.str("key2"));
	}

	@Test
	public void propertyWithQuotedString() {
		TECL tecl = parse("key = \" value \" ");
		assertEquals(" value ", tecl.str("key"));
	}

	// TODO: @Test!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	public void propertyWithQuotedStringWithQuoteInside() {
		TECL tecl = parse("key = \" val\\\"ue \" ");
		assertEquals(" val\"ue ", tecl.str("key"));
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
				+ "key1 = value1 # comment\n"
				+ "key2 = value2# comment\n"
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
		assertEquals("groupId", tecl.get("groupId").getId());
	}
	
	@Test
	public void groupWithContent() {
		TECL tecl = parse(""
				+ "groupId { \n" 
				+ "    key1 = value1\n"
				+ "}\n");
		assertEquals("groupId", tecl.get("groupId").getId());
		assertEquals("value1", tecl.get("groupId").str("key1"));
	}
	
	@Test
	public void notExistingGroup() {
		TECL tecl = parse("");
		assertTrue(tecl.get("groupId").getId().contains("not exist"));
	}
	
	@Test
	public void identicalGroups() {
		assertThrows(IllegalStateException.class, () -> {
			TECL tecl = parse(""
					+ "groupId { }\n"
					+ "groupId { }\n"
					);
		});
	}
	
	@Test
	public void nestedGroups() {
		TECL tecl = parse(""
				+ "groupId1 { \n"
				+ "    groupId2 { }\n"
				+ "}"
				);
		assertEquals("groupId1", tecl.get("groupId1").getId());
		assertEquals("groupId2", tecl.get("groupId1").get("groupId2").getId());
	}
	
	// ========================
	// TABLE
	
	@Test
	public void table() {
		TECL tecl = parse(""
				+ "| id  | type   | \n "
				+ "| id1 | string | \n"
				+ "| id2 | int    | \n"				
				);
		assertEquals("id1", tecl.str(0, "id"));
		assertEquals("int", tecl.str(1, "type"));
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

	// ========================
	
	private TECL parse(String s) {
		return TECL.parser().parse(s);
	}
}

