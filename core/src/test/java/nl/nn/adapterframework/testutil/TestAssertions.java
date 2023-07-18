/*
   Copyright 2018 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package nl.nn.adapterframework.testutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.TimeZone;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.util.ClassLoaderUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * This class is a 'comparison helper' for file assertions
 *
 * @author Niels Meijer
 */
public class TestAssertions extends org.junit.jupiter.api.Assertions {
	private static final Logger LOG = LogUtil.getLogger(TestAssertions.class);

	public static void assertEqualsIgnoreWhitespaces(String expected, String actual) throws IOException {
		assertEqualsIgnoreWhitespaces(trimMultilineString(expected), trimMultilineString(actual), null);
	}

	public static void assertEqualsIgnoreWhitespaces(String expected, String actual, String message) throws IOException {
		assertEquals(trimMultilineString(expected), trimMultilineString(actual), message);
	}

	public static void assertEqualsIgnoreRNTSpace(String a, String b) {
		assertEquals(removeRegexCharactersFromInput(a, "[\\n\\t\\r ]"), removeRegexCharactersFromInput(b, "[\\n\\t\\r ]"));
	}

	private static String removeRegexCharactersFromInput(String input, String regex) {
		if(input == null) {
			return null;
		}
		return input.replaceAll(regex, "");
	}

	private static String trimMultilineString(String str) throws IOException {
		if(str == null || str.isEmpty())
			return "";

		StringBuilder buffer = new StringBuilder();

		BufferedReader bufReader = new BufferedReader(new StringReader(str));
		String line = null;
		while((line = bufReader.readLine()) != null) {
			buffer.append(line.trim());
			buffer.append("\n");
		}

		return buffer.toString();
	}

	public static void assertEqualsIgnoreCRLF(String expected, String actual) {
		assertEqualsIgnoreCRLF(expected, actual, null);
	}

	public static void assertEqualsIgnoreCRLF(String expected, String actual, String message) {
		assertNotNull(expected);
		assertNotNull(actual);
		assertEquals(expected.trim().replace("\r",""), actual.trim().replace("\r",""), message);
	}

	public static void assertXpathValueEquals(String expected, String source, String xpathExpr) throws SAXException, XPathExpressionException, TransformerException, IOException {
		String xslt=XmlUtils.createXPathEvaluatorSource(xpathExpr);
		Transformer transformer = XmlUtils.createTransformer(xslt);

		String result=XmlUtils.transformXml(transformer, source);
		LOG.debug("xpath [{}] result [{}]", xpathExpr, result);
		assertEquals(expected,result,xpathExpr);
	}

	public static void assertXpathValueEquals(int expected, String source, String xpathExpr) throws SAXException, XPathExpressionException, TransformerException, IOException {
		String xslt=XmlUtils.createXPathEvaluatorSource(xpathExpr);
		Transformer transformer = XmlUtils.createTransformer(xslt);

		String result=XmlUtils.transformXml(transformer, source);
		LOG.debug("xpath [{}] result [{}]", xpathExpr, result);
		assertEquals(expected+"",result,xpathExpr);
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
		assertEqualsIgnoreWhitespaces(string+" ", "  "+string);
	}

	@Test
	public void testAssertEqualsIgnoreWhitespacesString() throws IOException {
		String a2h = "a\r\nb\r\nc\r\nd\r\ne\r\nf\r\ng\r\nh";
		String a2hlf = a2h.replace("\r", "");

		assertEqualsIgnoreWhitespaces(a2h, a2hlf);
	}

	@Test
	public void testAssertEqualsIgnoreWhitespacesFile() throws IOException {
		URL svg = ClassLoaderUtils.getResourceURL("test1.xml");
		String str1 = StreamUtil.streamToString(svg.openStream());
		String str2 = str1.replace("\r", "");

		assertEqualsIgnoreWhitespaces(str1, str2);
	}

	public static boolean isTestRunningOnGitHub() {
		return "GITHUB".equalsIgnoreCase(System.getProperty("CI_SERVICE")) || "GITHUB".equalsIgnoreCase(System.getenv("CI_SERVICE"));
	}

	public static boolean isTestRunningOnCI() {
		return StringUtils.isNotEmpty(System.getProperty("CI")) || StringUtils.isNotEmpty(System.getenv("CI")) || isTestRunningOnGitHub();
	}

	public static boolean isTestRunningOnWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	public static boolean isTimeZone(TimeZone timeZone) {
		return TimeZone.getDefault().hasSameRules(timeZone);
	}
}
