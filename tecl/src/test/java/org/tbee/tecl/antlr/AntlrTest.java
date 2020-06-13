package org.tbee.tecl.antlr;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.tbee.tecl.TECL;

@RunWith(MockitoJUnitRunner.class)
public class AntlrTest {

	@Test
	public void empty() {
		TECL tecl = parse("");
		Assert.assertNull(tecl.str("key"));
	}
	
	@Test
	public void simpleProperty() {
		TECL tecl = parse("key = value");
		Assert.assertEquals("value", tecl.str("key"));
	}

	@Test
	public void propertyWithQuotedString() {
		TECL tecl = parse("key = \" value \" ");
		Assert.assertEquals(" value ", tecl.str("key"));
	}

	// TODO: @Test
	public void propertyWithQuotedStringWithQuoteInside() {
		TECL tecl = parse("key = \" val\\\"ue \" ");
		Assert.assertEquals(" val\"ue ", tecl.str("key"));
	}
	
	@Test
	public void emptyGroup() {
		TECL tecl = parse("groupId { }");
		Assert.assertNotNull(tecl.get("groupId"));
	}
	
	@Test
	public void groupWithSimpleProperty() {
		TECL tecl = parse("groupId { \n    key = value \n}");
		Assert.assertNotNull(tecl.get("groupId"));
		Assert.assertEquals("value", tecl.get("groupId").str("key"));
	}
	
	@Test
	public void table() {
		TECL tecl = parse(""
				+ "| id  | type   | \n "
				+ "| id1 | string | \n"
				+ "| id2 | int    | \n"				
				);
		Assert.assertEquals("id1", tecl.str(0, "id"));
		Assert.assertEquals("int", tecl.str(1, "type"));
	}

	private TECL parse(String s) {
		return TECL.parser().parse(s);
	}
}

