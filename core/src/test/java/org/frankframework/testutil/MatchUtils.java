package org.frankframework.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import jakarta.json.Json;
import jakarta.json.JsonStructure;

import org.apache.commons.lang3.StringUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.json.JsonUtil;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.NamespaceRemovingFilter;
import org.frankframework.xml.PrettyPrintFilter;
import org.frankframework.xml.XmlWriter;

@Log4j2
public class MatchUtils {

	public static Map<String, Object> stringToMap(String mapInStr) throws IOException {
		Properties inProps = new Properties();
		inProps.load(new StringReader(mapInStr));
		Map<String, Object> mapIn = new HashMap<>();
		for(Object key : inProps.keySet()) {
			mapIn.put((String) key, inProps.getProperty((String) key));
		}
		return mapIn;
	}

	public static String mapToString(Map<String,String> map) {
		StringBuilder buf=new StringBuilder();
		for (String key:map.keySet()) {
			buf.append(key).append('=');
			if (map.containsKey(key)) {
				buf.append(map.get(key));
			}
			buf.append("\n");
		}
		return buf.toString();
	}

	public static void assertMapEquals(Map<String,String> exp, Map<String,String> act) {
		SortedMap<String,String> exps=new TreeMap<>(exp);
		String expStr=mapToString(exps);
		SortedMap<String,String> acts=new TreeMap<>(act);
		String actStr=mapToString(acts);
		assertEquals(expStr,actStr);
	}

	public static String xmlPretty(String xml, boolean removeNamespaces, boolean includeComments) {
		if (StringUtils.isAllBlank(xml)) {
			return "";
		}
		XmlWriter xmlWriter = new XmlWriter();
		xmlWriter.setIncludeComments(includeComments);
		ContentHandler contentHandler = new PrettyPrintFilter(xmlWriter, true);
		if (removeNamespaces) {
			contentHandler = new NamespaceRemovingFilter(contentHandler);
		}
		try {
			XmlUtils.parseXml(xml, contentHandler);
			return xmlWriter.toString();
		} catch (IOException | SAXException e) {
			throw new RuntimeException("ERROR: could not prettify ["+xml+"]",e);
		}
	}

	public static void assertXmlEquals(String xmlExp, String xmlAct) {
		assertXmlEquals(null, xmlExp, xmlAct);
	}

	public static void assertXmlEquals(String description, String xmlExp, String xmlAct) {
		assertXmlEquals(description, xmlExp, xmlAct, false);
	}

	public static void assertXmlEquals(String description, String xmlExp, String xmlAct, boolean ignoreNamespaces) {
		assertXmlEquals(description, xmlExp, xmlAct, ignoreNamespaces, false);
	}

	public static void assertXmlEquals(String description, String xmlExp, String xmlAct, boolean ignoreNamespaces, boolean includeComments) {
		String xmlExpPretty;
		String xmlActPretty;
		try {
			xmlExpPretty = xmlPretty(xmlExp, ignoreNamespaces, includeComments);
		} catch (RuntimeException e) {
			xmlExpPretty = e.getMessage();
		}
		try {
			xmlActPretty = xmlPretty(xmlAct, ignoreNamespaces, includeComments);
		} catch (RuntimeException e) {
			xmlActPretty = e.getMessage();
		}
		assertEquals(xmlExpPretty,xmlActPretty,description);
	}

	public static void assertXmlSimilar(String expected, String actual) {
		try {
			String expectedCanonalized = XmlUtils.canonicalize(expected);
			String actualCanonalized = XmlUtils.canonicalize(actual);

			DetailedDiff diff = new DetailedDiff(new Diff(expectedCanonalized, actualCanonalized));
			if(!diff.similar()) {
				log.debug("expected: \n{}", expectedCanonalized);
				log.debug("actual: \n{}", actualCanonalized);
				assertEquals("xml not similar: " + diff, expectedCanonalized, actualCanonalized);
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	public static JsonStructure string2Json(String json) {
		JsonStructure jsonStructure = Json.createReader(new StringReader(json)).read();
		return jsonStructure;
	}

	public static void assertJsonEquals(String jsonExp, String jsonAct) {
		assertJsonEquals(null, jsonExp, jsonAct);
	}

	public static void assertJsonEquals(String description, String jsonExp, String jsonAct) {
		assertEquals(JsonUtil.jsonPretty(jsonExp), JsonUtil.jsonPretty(jsonAct), description);
	}

	public static void assertTestFileEquals(String file1, URL url) throws IOException {
		assertNotNull(url, "url to compare to ["+file1+"] should not be null");
		assertTestFileEquals(file1,url.openStream());
	}

	public static void assertTestFileEquals(String file1, InputStream fileStream) throws IOException {
		assertTestFileEquals(file1, StreamUtil.streamToString(fileStream));
	}

	public static void assertTestFileEquals(String file1, String file2) throws IOException {
		String testFile = TestFileUtils.getTestFile(file1);
		assertNotNull("testFile ["+file1+"] is null", testFile);

		TestAssertions.assertEqualsIgnoreWhitespaces(testFile.trim(), file2.trim());
	}
}
