package nl.nn.adapterframework.testutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.NamespaceRemovingFilter;
import nl.nn.adapterframework.xml.PrettyPrintFilter;
import nl.nn.adapterframework.xml.XmlWriter;

public class MatchUtils {
	
    public static Map<String,Object> stringToMap(String mapInStr) throws IOException {
		Properties inProps=new Properties();
		inProps.load(new StringReader(mapInStr));
		Map<String,Object> mapIn=new HashMap<String,Object>();
		for (Object key:inProps.keySet()) {
			mapIn.put((String)key, inProps.getProperty((String)key));
		}
		return mapIn;
    }
    
	public static String mapToString(Map<String,String> map) {
		StringBuffer buf=new StringBuffer();
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
		SortedMap<String,String> exps=new TreeMap<String,String>(exp);
		String expStr=mapToString(exps);
		SortedMap<String,String> acts=new TreeMap<String,String>(act);
		String actStr=mapToString(acts);
		assertEquals(expStr,actStr);
	}
 
	public static String xmlPretty(String xml, boolean removeNamespaces) {
		XmlWriter xmlWriter = new XmlWriter();
		xmlWriter.setIncludeComments(false);
		ContentHandler contentHandler = new PrettyPrintFilter(xmlWriter);
		if (removeNamespaces) {
			contentHandler = new NamespaceRemovingFilter(contentHandler);
		}
		try {
			XmlUtils.parseXml(xml, contentHandler);
			return xmlWriter.toString();
		} catch (IOException | SAXException e) {
			return "ERROR: could not prettyfy: ("+e.getClass().getName()+") "+e.getMessage();
		}
	}

	public static String jsonPretty(String json) {
		StringWriter sw = new StringWriter();
		JsonReader jr = Json.createReader(new StringReader(json));
		JsonObject jobj = jr.readObject();

		Map<String, Object> properties = new HashMap<>(1);
		properties.put(JsonGenerator.PRETTY_PRINTING, true);

		JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
		try (JsonWriter jsonWriter = writerFactory.createWriter(sw)) {
			jsonWriter.writeObject(jobj);
		}

		return sw.toString().trim();
	}

	public static void assertXmlEquals(String xmlExp, String xmlAct) {
		assertXmlEquals(null, xmlExp, xmlAct);
	}

	public static void assertXmlEquals(String description, String xmlExp, String xmlAct) {
		assertXmlEquals(description, xmlExp, xmlAct, false);
	}
	
	public static void assertXmlEquals(String description, String xmlExp, String xmlAct, boolean ignoreNamespaces) {
		String xmlExpPretty = xmlPretty(xmlExp, ignoreNamespaces);
		String xmlActPretty = xmlPretty(xmlAct, ignoreNamespaces);
		assertEquals(description,xmlExpPretty,xmlActPretty);
	}
	
	public static JsonStructure string2Json(String json) {
		JsonStructure jsonStructure = Json.createReader(new StringReader(json)).read();
		return jsonStructure;
	}

	public static void assertJsonEqual(String description, String jsonExp, String jsonAct) {
		JsonStructure jExp=string2Json(jsonExp);
		JsonStructure jAct=string2Json(jsonAct);
		assertEquals(description,jExp.toString(),jAct.toString());
		//assertEquals(description,inputJson,jsonOut);
	}

	public static void assertTestFileEquals(String file1, URL url) throws IOException {
		assertNotNull("url to compare to ["+file1+"] should not be null",url);
		assertTestFileEquals(file1,url.openStream());
	}
	
	public static void assertTestFileEquals(String file1, InputStream fileStream) throws IOException {
		assertTestFileEquals(file1, Misc.streamToString(fileStream));
	}

	public static void assertTestFileEquals(String file1, String file2) throws IOException {
		String testFile = TestFileUtils.getTestFile(file1);
		assertNotNull("testFile ["+file1+"] is null",testFile);

		TestAssertions.assertEqualsIgnoreWhitespaces(testFile.trim(), file2.trim());
	}
}
