package org.tbee.tecl.antlr;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
		TECL tecl = parse("groupId { key = value }");
		Assert.assertNotNull(tecl.get("groupId"));
		Assert.assertEquals("value", tecl.get("groupId").str("key"));
	}

	private TECL parse(String s) {
		CodePointCharStream input = CharStreams.fromString(s);
        TECLLexer lexer = new TECLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
		TECLParser actionsParser = new TECLParser(tokens);
		actionsParser.parse();
		// TODO: fail on parse errors
		return actionsParser.getTECL();
	}
}

