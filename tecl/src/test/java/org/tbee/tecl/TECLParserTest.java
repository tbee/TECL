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
		TECL tecl = TECL.parser().findAndParse("src/test/resources/localFile.tecl");
		Assert.assertNotNull(tecl);
		Assert.assertEquals("localFile", tecl.str("type"));
	}
	
	@Test
	public void resource() throws IOException {
		TECL tecl = TECL.parser().findAndParse();
		Assert.assertNotNull(tecl);
		Assert.assertEquals("resource", tecl.str("type"));
	}
}
