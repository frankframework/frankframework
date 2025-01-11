/*
   Copyright 2018 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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
package org.frankframework.testutil;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.TimeZone;

import javax.xml.transform.Transformer;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.RenamingObjectInputStream;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlUtils;

/**
 * This class is a 'comparison helper' for file assertions
 *
 * @author Niels Meijer
 */
public class TestAssertions extends org.junit.jupiter.api.Assertions {
	private static final Logger LOG = LogUtil.getLogger(TestAssertions.class);
	private static String SERIALIZATION_WIRE = "aced0005737200276e6c2e6e6e2e616461707465726672616d65776f726b2e687474702e506172744d65737361676541c94d37efd077bc020000787200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e767200346e6c2e6e6e2e616461707465726672616d65776f726b2e687474702e506172744d657373616765546573742454657374506172740000000000000000000000787078";


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

	public static void assertXpathValueEquals(String expected, String source, String xpathExpr) throws Exception {
		String xslt = XmlUtils.createXPathEvaluatorSource(xpathExpr);
		Transformer transformer = XmlUtils.createTransformer(xslt);

		source = XmlUtils.removeNamespaces(source);
		String result = XmlUtils.transformXml(transformer, source);
		LOG.debug("xpath [{}] result [{}]", xpathExpr, result);
		assertEquals(expected, result, xpathExpr);
	}

	public static void assertXpathValueEquals(int expected, String source, String xpathExpr) throws Exception {
		String xslt = XmlUtils.createXPathEvaluatorSource(xpathExpr);
		Transformer transformer = XmlUtils.createTransformer(xslt);

		String result = XmlUtils.transformXml(transformer, source);
		LOG.debug("xpath [{}] result [{}]", xpathExpr, result);
		assertEquals(expected + "", result, xpathExpr);
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

	public static boolean isTestRunningOnARM() {
		String osArch = System.getProperty("os.arch").toLowerCase();
		return osArch.contains("aarch64") || osArch.contains("arm");
	}

	/**
	 * When the JUnitTest is run via SureFire it uses the 'surefirebooter.jar'.
	 * When run directly in an IDE, the IDE usually provides a 'Test Runner'.
	 */
	public static boolean isTestRunningWithSurefire() {
		return System.getProperty("sun.java.command").contains("surefire");
	}

	public static boolean isTestRunningOnWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	public static boolean isTimeZone(TimeZone timeZone) {
		return TimeZone.getDefault().hasSameRules(timeZone);
	}

	public static boolean isRunningWithAddOpens() {
		try {
			byte[] wire = Hex.decodeHex(SERIALIZATION_WIRE);
			try (ByteArrayInputStream bais = new ByteArrayInputStream(wire); ObjectInputStream in = new RenamingObjectInputStream(bais)) {
				in.readObject();
				return true;
			}
		} catch (Exception e) {
			// pretend we were not here (suppress the exception), and let the actual test fail instead!
			return !e.getMessage().contains("module java.base does not \"opens java.io\" to unnamed module");
		}
	}
}
