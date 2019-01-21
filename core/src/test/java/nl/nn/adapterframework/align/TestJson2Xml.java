package nl.nn.adapterframework.align;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import org.junit.Test;

import nl.nn.adapterframework.testutil.MatchUtils;

public class TestJson2Xml extends AlignTestBase {
	

	public void testJsonNoRoundTrip(String jsonIn, URL schemaUrl, String targetNamespace, String rootElement, boolean compactConversion, boolean strictSyntax, String expectedFailureReason, String description) throws Exception {
		testJson(jsonIn, null, false, schemaUrl, targetNamespace, rootElement, compactConversion, strictSyntax, null, expectedFailureReason, description);
	}
	public void testJson(String jsonIn, Map<String,Object> properties, boolean deepSearch, URL schemaUrl, String targetNamespace, String rootElement, boolean compactConversion, boolean strictSyntax, String resultJsonExpected, String expectedFailureReason, String description) throws Exception {
		Object json = Utils.string2Json(jsonIn);
		try {
			JsonStructure jsonStructure = Json.createReader(new StringReader(jsonIn)).read();
			String xmlAct = Json2Xml.translate(jsonStructure, schemaUrl, compactConversion, rootElement, strictSyntax, deepSearch, targetNamespace, properties);
	    	System.out.println("xml out="+xmlAct);
	    	if (expectedFailureReason!=null) {
	    		fail("Expected to fail: "+description);
	    	}
	    	if (xmlAct==null) {
	    		fail("could not convert to xml: "+description);
	    	}
	       	assertTrue("converted XML is not aligned: "+description,  Utils.validate(schemaUrl, xmlAct));
	       	if (resultJsonExpected!=null) {
		       	String roundTrippedJson=Xml2Json.translate(xmlAct, schemaUrl, compactConversion, rootElement!=null).toString(true);
		       	assertEquals("roundTrippedJson",resultJsonExpected,roundTrippedJson);
	       	}
		} catch (Exception e) {
			if (expectedFailureReason==null) {
				e.printStackTrace();
				fail("Expected conversion to succeed: "+description);
			}
			String msg=e.getMessage();
			if (msg==null) {
				e.printStackTrace();
				fail("msg==null ("+e.getClass().getSimpleName()+")");
			}
			if (!msg.contains(expectedFailureReason)) {
				e.printStackTrace();
				fail("expected reason ["+expectedFailureReason+"] in msg ["+msg+"]");
			}
		}
	}
	
	public void testStrings(String xmlIn, String jsonIn, URL schemaUrl, String targetNamespace, String rootElement, boolean compactInput, boolean potentialCompactionProblems, boolean checkRoundTrip, String expectedFailureReason) throws Exception {
		System.out.println("schemaUrl ["+schemaUrl+"]");
		if (xmlIn!=null) assertTrue("Expected XML is not valid to XSD",Utils.validate(schemaUrl, xmlIn));

		JsonStructure json = Utils.string2Json(jsonIn);
		System.out.println("jsonIn ["+json+"]");
    	Map<String,Object> overrideMap = new HashMap<String,Object>();
    	overrideMap.put("Key not expected", "value of unexpected key");
    	if (json instanceof JsonObject) {
    		JsonObject jo = (JsonObject)json;
    		for (String key:jo.keySet()) {
    			if (overrideMap.containsKey(key)) {
    				System.out.println("multiple occurrences in object for element ["+key+"]");
    			}
    			overrideMap.put(key, null);
    		}
    	}
    	testJson(jsonIn, null,        false, schemaUrl, targetNamespace, rootElement, compactInput, false, checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], relaxed");
    	testJson(jsonIn, null,        false, schemaUrl, targetNamespace, rootElement, compactInput, true,  checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], strict");
    	testJson(jsonIn, overrideMap, false, schemaUrl, targetNamespace, rootElement, compactInput, false, checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], relaxed, parameters");
    	testJson(jsonIn, overrideMap, false, schemaUrl, targetNamespace, rootElement, compactInput, true,  checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], strict, parameters");
//    	testJson(jsonIn, null,        true, schemaUrl, targetNamespace, rootElement, compactInput, false, checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], relaxed, deep search");
//    	testJson(jsonIn, overrideMap, true, schemaUrl, targetNamespace, rootElement, compactInput, false, checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], relaxed, parameters");
//    	testJson(jsonIn, null,        true, schemaUrl, targetNamespace, rootElement, compactInput, true, checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], strict, deep search");
//    	testJson(jsonIn, overrideMap, true, schemaUrl, targetNamespace, rootElement, compactInput, true, checkRoundTrip?jsonIn:null,expectedFailureReason,"(compact in and conversion) ["+compactInput+"], strict, parameters");
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
		URL schemaUrl=getSchemaURL(schemaFile);
		String xmlString=getTestFile(inputFile+".xml");
		String jsonFullString=getTestFile(inputFile+"-full.json");
		String jsonCompactString=getTestFile(inputFile+"-compact.json");
		String jsonSingleString=getTestFile(inputFile+".json");
//		String schemaString=FileUtils.resourceToString(schemaUrl);
		if (jsonFullString!=null) testStrings(xmlString,jsonFullString,   schemaUrl,namespace, null,       false, potentialCompactionProblems,checkRoundTrip,expectedFailureReason);
		if (jsonCompactString!=null) testStrings(xmlString,jsonCompactString,schemaUrl,namespace, rootElement, true, potentialCompactionProblems,checkRoundTrip,expectedFailureReason);
		if (jsonSingleString!=null) testStrings(xmlString,jsonSingleString,schemaUrl,namespace, rootElement, true, false,checkRoundTrip,expectedFailureReason);
		if (jsonFullString==null && jsonCompactString==null && jsonSingleString==null) {
			fail("no json input files found for ["+inputFile+"]");
		}
	}

	public void testTreeAndMap(String schemaFile, String namespace, String rootElement, String inputFile, String resultFile, String expectedFailureReason) throws Exception {
		URL schemaUrl=getSchemaURL(schemaFile);
		String xmlString=getTestFile(inputFile+".xml");
		String jsonString=getTestFile(inputFile+".json");
		String propertiesString=getTestFile(inputFile+".properties");
		String resultJsonString=getTestFile(resultFile+".json");
		
		JsonStructure jsonIn = Utils.string2Json(jsonString);
		System.out.println("jsonIn ["+jsonIn+"]");
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
    public void testEmbeddedChoice() throws Exception {
    	testFiles("EmbeddedChoice/EmbeddedChoice.xsd","","EmbeddedChoice", "/EmbeddedChoice/EmbeddedChoice",false,null,false);
    }

}
