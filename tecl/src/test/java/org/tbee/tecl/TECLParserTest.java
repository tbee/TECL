package org.tbee.tecl;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class TECLParserTest {
	
	// resource is the final match, so we cannot test undefined
	//@Test
	public void undefined() throws IOException {
		TECL tecl = TECL.parser().findAndParse();
		Assert.assertNull(tecl);
	}
	
	@Test
	public void systemProperty() throws IOException {
		try {
			System.setProperty("config.tecl", "src/test/resources/systemProperty.tecl");
			TECL tecl = TECL.parser().findAndParse();
			Assert.assertNotNull(tecl);
			Assert.assertEquals("systemProperty", tecl.str("type"));
		}
		finally {
			System.clearProperty("config.tecl");
		}
	}
	
	// Can't set the environment programmatically, so don't know how to test this
	@Test
	public void env() throws IOException {
	}
	
	// Changing the current directory does not work, we abuse a constant
	@Test
	public void localFile() throws IOException {
		String originalValue = TECLParser.CONFIG_TECL_FILENAME;
		try {
			TECLParser.CONFIG_TECL_FILENAME = "src/test/resources/localFile.tecl";
			TECL tecl = TECL.parser().findAndParse();
			Assert.assertNotNull(tecl);
			Assert.assertEquals("localFile", tecl.str("type"));
		}
		finally {
			TECLParser.CONFIG_TECL_FILENAME = originalValue;
		}
	}
	
	@Test
	public void resource() throws IOException {
		TECL tecl = TECL.parser().findAndParse();
		Assert.assertNotNull(tecl);
		Assert.assertEquals("resource", tecl.str("type"));
	}
}
