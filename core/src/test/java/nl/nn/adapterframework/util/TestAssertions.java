/*
   Copyright 2018 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package nl.nn.adapterframework.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import org.junit.Test;

/**
 * This class is a 'comparison helper' for file assertions
 * 
 * @author Niels Meijer
 */
public class TestAssertions extends org.junit.Assert {

	static public void assertEqualsIgnoreWhitespaces(String str1, String str2) throws IOException {
		assertEquals(trimMultilineString(str1), trimMultilineString(str2));
	}

	private static String trimMultilineString(String str) throws IOException {
		if(str == null || str.isEmpty())
			return "";

		StringBuffer buffer = new StringBuffer();

		BufferedReader bufReader = new BufferedReader(new StringReader(str));
		String line = null;
		while((line = bufReader.readLine()) != null) {
			buffer.append(line.trim());
			buffer.append("\n");
		}

		return buffer.toString();
	}

	static public void assertEqualsIgnoreCRLF(String str1, String str2) {
		assertEquals(str1.trim().replace("\r",""), str2.trim().replace("\r",""));
	}

	@Test
	public void testAssertEqualsIgnoreWhitespacesNull() throws IOException {
		assertEqualsIgnoreWhitespaces(null, null);
	}

	@Test
	public void testAssertEqualsIgnoreWhitespacesEmpty() throws IOException {
		assertEqualsIgnoreWhitespaces("", "");
	}

	@Test
	public void testAssertEqualsIgnoreWhitespacesDummy() throws IOException {
		String string = "dummy";

		assertEqualsIgnoreWhitespaces(string, string);
	}

	@Test
	public void testAssertEqualsIgnoreWhitespacesString() throws IOException {
		String a2h = "a\r\nb\r\nc\r\nd\r\ne\r\nf\r\ng\r\nh";
		String a2hlf = a2h.replace("\r", "");

		assertEqualsIgnoreWhitespaces(a2h, a2hlf);
	}

	@Test
	public void testAssertEqualsIgnoreWhitespacesFile() throws IOException {
		URL svg = ClassUtils.getResourceURL(this, "test1.xml");
		String str1 = Misc.streamToString(svg.openStream());
		String str2 = str1.replace("\r", "");

		assertEqualsIgnoreWhitespaces(str1, str2);
	}
}
