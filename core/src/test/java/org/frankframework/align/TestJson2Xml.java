package org.frankframework.align;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.xml.XmlWriter;

@Log4j2
public class TestJson2Xml extends AlignTestBase {

	public void testJsonNoRoundTrip(String jsonIn, URL schemaUrl, String targetNamespace, String rootElement, boolean compactConversion, boolean strictSyntax, String expectedFailureReason, String description) throws Exception {
		testJson(jsonIn, null, false, schemaUrl, targetNamespace, rootElement, compactConversion, strictSyntax, null, expectedFailureReason, description);
	}
	public void testJson(String jsonIn, Map<String,Object> properties, boolean deepSearch, URL schemaUrl, String targetNamespace, String rootElement, boolean compactConversion, boolean strictSyntax, String resultJsonExpected, String expectedFailureReason, String description) throws Exception {
		XmlWriter xmlWriter = new XmlWriter();
		try {
			JsonStructure jsonStructure = Json.createReader(new StringReader(jsonIn)).read();
			Json2Xml j2x = Json2Xml.create(schemaUrl, compactConversion, rootElement, strictSyntax, deepSearch, targetNamespace, properties);
			j2x.translate(jsonStructure, xmlWriter);
			String xmlAct = xmlWriter.toString();
			log.debug("xml out={}", xmlAct);
			if (expectedFailureReason!=null) {
				fail("Expected to fail: "+description);
			}
			if (xmlAct==null) {
				fail("could not convert to xml: "+description);
			}
			assertTrue(Utils.validate(schemaUrl, xmlAct), "converted XML is not aligned: "+description);
			if (resultJsonExpected!=null) {
				String roundTrippedJson= Xml2Json.translate(xmlAct, schemaUrl, compactConversion, rootElement!=null).toString(true);
				assertEquals(resultJsonExpected, roundTrippedJson, "roundTrippedJson");
			}
		} catch (Exception e) {
			if (expectedFailureReason==null) {
				log.error("expected conversion to succeed", e);
				fail("Expected conversion to succeed: "+description);
			}
			String msg=e.getMessage();
			if (msg==null) {
				log.error("msg == null", e);
				fail("msg == null ("+e.getClass().getSimpleName()+")");
			}
			if (!msg.contains(expectedFailureReason)) {
				log.error("expected reason [{}] in msg [{}]", expectedFailureReason, msg, e);
				fail("expected reason ["+expectedFailureReason+"] in msg ["+msg+"]");
			}
		}
	}

	public void testStrings(String xmlIn, String jsonIn, URL schemaUrl, String targetNamespace, String rootElement, boolean compactInput, boolean potentialCompactionProblems, boolean checkRoundTrip, String expectedFailureReason) throws Exception {
		log.debug("schemaUrl [{}]", schemaUrl);
		if (StringUtils.isNotEmpty(xmlIn)) assertTrue(Utils.validate(schemaUrl, xmlIn), "Expected XML is not valid to XSD");

		JsonStructure json = Utils.string2Json(jsonIn);
		log.debug("jsonIn [{}]", json);
		Map<String,Object> overrideMap = new HashMap<>();
		overrideMap.put("Key not expected", "value of unexpected key");
		if (json instanceof JsonObject jo) {
			for (String key:jo.keySet()) {
				if (overrideMap.containsKey(key)) {
					log.debug("multiple occurrences in object for element [{}]", key);
				}
				overrideMap.put(key, null);
			}
		}
		testJson(jsonIn, null,		false, schemaUrl, targetNamespace, rootElement, compactInput, false, checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], relaxed");
		testJson(jsonIn, null,		false, schemaUrl, targetNamespace, rootElement, compactInput, true,  checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], strict");
		testJson(jsonIn, overrideMap, false, schemaUrl, targetNamespace, rootElement, compactInput, false, checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], relaxed, parameters");
		testJson(jsonIn, overrideMap, false, schemaUrl, targetNamespace, rootElement, compactInput, true,  checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], strict, parameters");
//		testJson(jsonIn, null,		true, schemaUrl, targetNamespace, rootElement, compactInput, false, checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], relaxed, deep search");
//		testJson(jsonIn, overrideMap, true, schemaUrl, targetNamespace, rootElement, compactInput, false, checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], relaxed, parameters");
//		testJson(jsonIn, null,		true, schemaUrl, targetNamespace, rootElement, compactInput, true, checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], strict, deep search");
//		testJson(jsonIn, overrideMap, true, schemaUrl, targetNamespace, rootElement, compactInput, true, checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], strict, parameters");
		if (expectedFailureReason==null) {
			if (potentialCompactionProblems) {
				if (compactInput) {
					testJsonNoRoundTrip(jsonIn, schemaUrl, targetNamespace, rootElement, !compactInput, false, expectedFailureReason,"compact input, full expected, relaxed, potentialCompactionProblems");
					testJson(jsonIn, null, false, schemaUrl, targetNamespace, rootElement, !compactInput, true, checkRoundTrip?jsonIn:null,Json2Xml.MSG_EXPECTED_SINGLE_ELEMENT,"compact input, full expected, strict, potentialCompactionProblems");
				} else {
					testJsonNoRoundTrip(jsonIn, schemaUrl, targetNamespace, rootElement, !compactInput, false, expectedFailureReason,"full input, compact expected, relaxed, potentialCompactionProblems");
					testJson(jsonIn, null, false, schemaUrl, targetNamespace, rootElement, !compactInput, true, checkRoundTrip?jsonIn:null,Json2Xml.MSG_FULL_INPUT_IN_STRICT_COMPACTING_MODE,"full input, compact expected, strict, potentialCompactionProblems");
				}
			} else {
				testJson(jsonIn, null, false, schemaUrl, targetNamespace, rootElement, !compactInput, false, checkRoundTrip?jsonIn:null,expectedFailureReason,"compact in ["+compactInput+"] and conversion not, relaxed, no potentialCompactionProblems");
				testJson(jsonIn, null, false, schemaUrl, targetNamespace, rootElement, !compactInput, true, checkRoundTrip?jsonIn:null,expectedFailureReason,"compact in ["+compactInput+"] and conversion not, strict, no potentialCompactionProblems");
			}
		}
	}

	@Override
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems, String expectedFailureReason) throws Exception {
		testFiles(schemaFile, namespace, rootElement, inputFile, potentialCompactionProblems, expectedFailureReason, true);
	}

	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems, String expectedFailureReason, boolean checkRoundTrip) throws Exception {
		testFiles(schemaFile, namespace, rootElement, inputFile, inputFile+".xml", potentialCompactionProblems, expectedFailureReason, checkRoundTrip);
	}

	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, String expectedFile, boolean potentialCompactionProblems, String expectedFailureReason, boolean checkRoundTrip) throws Exception {
		URL schemaUrl=getSchemaURL(schemaFile);
		String xmlString=getTestFile(expectedFile);
		String jsonFullString=getTestFile(inputFile+"-full.json");
		String jsonCompactString=getTestFile(inputFile+"-compact.json");
		String jsonSingleString=getTestFile(inputFile+".json");
//		String schemaString=FileUtils.resourceToString(schemaUrl);
		if (jsonFullString!=null) testStrings(xmlString,jsonFullString,   schemaUrl,namespace, null,	   false, potentialCompactionProblems,checkRoundTrip,expectedFailureReason);
		if (jsonCompactString!=null) testStrings(xmlString,jsonCompactString,schemaUrl,namespace, rootElement, true, potentialCompactionProblems,checkRoundTrip,expectedFailureReason);
		if (jsonSingleString!=null) testStrings(xmlString,jsonSingleString,schemaUrl,namespace, rootElement, true, false,checkRoundTrip,expectedFailureReason);
		if (jsonFullString==null && jsonCompactString==null && jsonSingleString==null) {
			fail("no json input files found for ["+inputFile+"]");
		}
	}

	public void testTreeAndMap(String schemaFile, String namespace, String rootElement, String inputFile, String resultFile, String expectedFailureReason) throws Exception {
		URL schemaUrl=getSchemaURL(schemaFile);
		String jsonString=getTestFile(inputFile+".json");
		String propertiesString=getTestFile(inputFile+".properties");
		String resultJsonString=getTestFile(resultFile+".json");

		JsonStructure jsonIn = Utils.string2Json(jsonString);
		log.debug("jsonIn [{}]", jsonIn);
		Map<String,Object> map = MatchUtils.stringToMap(propertiesString);

		testJson(jsonString, map, true, schemaUrl, namespace, rootElement, true, false, resultJsonString, expectedFailureReason, null);

//		String schemaString=FileUtils.resourceToString(schemaUrl);
	}

	@Test
	public void testEmptyRepeatedElements() throws Exception {
		testFiles("RepeatedElements/sprint.xsd","","sprint","/RepeatedElements/sprint-emptyRepeatedElement",false,null,false); // this one only json to xml
	}

	@Test
	public void testNullError1() throws Exception {
		testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Null-illegal1", "nillable");
		testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Null-illegal2", "nillable");
	}

	@Test
	public void testSmileys() throws Exception {
		testFiles("Smileys/smiley.xsd","","", "/Smileys/smiley");
	}

	@Test
	public void testJsonAndMap() throws Exception {
		testTreeAndMap("TreeAndMap/employee.xsd","urn:employee","updateEmployeeRequest","/TreeAndMap/updateEmployeeRequest","/TreeAndMap/updateEmployeeRequestResult", null);
	}

	@Test
	public void testJsonAndMapHierarchical() throws Exception {
		testTreeAndMap("TreeAndMap/hierarchicalEmployee.xsd","urn:employee","updateHierarchicalEmployeeRequest","/TreeAndMap/updateHierarchicalEmployeeRequest","/TreeAndMap/updateHierarchicalEmployeeRequestResult", null);
	}

	@Test
	public void testNestedValue() throws Exception {
		testTreeAndMap("NestedValue/nestedValue.xsd","urn:gbpd","NestedValue","/NestedValue/nestedValue","/NestedValue/result",null);
	}

	@Test
	public void testIllegalValue() throws Exception {
		testFiles("Abc/abc.xsd","urn:test","a","Abc/abc-err", "Abc/abc", false, "Cannot find the declaration of element [d]", false);
	}

	// Test skipping attributes on ROOT level, and throwing an exception when unable to determine rootElement
	@Test
	public void testTooManyRootElements() throws Exception {
		URL schemaUrl = TestFileUtils.getTestFileURL("/Align/TextAndAttributes/schema.xsd");
		String jsonIn = TestFileUtils.getTestFile("/Align/TextAndAttributes/input-compact.json");
		XmlWriter xmlWriter = new XmlWriter();

		JsonStructure jsonStructure = Json.createReader(new StringReader(jsonIn)).read();
		Json2Xml j2x = Json2Xml.create(schemaUrl, false, null, false, false, "urn:test", null);
		SAXException e = assertThrows(SAXException.class, ()-> j2x.translate(jsonStructure, xmlWriter));
		assertEquals("Cannot determine XML root element, too many names [MetaData,intLabel,location] in JSON", e.getMessage());
	}

	@Test
	public void testMultidimensionalArray() throws Exception {
		URL schemaUrl = TestFileUtils.getTestFileURL("/Align/MultidimensionalArray/schema.xsd");
		String jsonIn = TestFileUtils.getTestFile("/Align/MultidimensionalArray/input.json");
		String xmlOut = TestFileUtils.getTestFile("/Align/MultidimensionalArray/output.xml");

		XmlWriter xmlWriter = new XmlWriter();

		JsonStructure jsonStructure = Json.createReader(new StringReader(jsonIn)).read();
		Json2Xml j2x = Json2Xml.create(schemaUrl, false, "arrays", false, false, "urn:test", null);
		j2x.translate(jsonStructure, xmlWriter);
		assertXmlEquals(xmlOut, xmlWriter.toString());
	}
}
