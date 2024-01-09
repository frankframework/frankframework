package org.frankframework.align;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.ValidatorHandler;

import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.xs.XSModel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * @author Gerrit van Brakel
 */
public class TestDomTreeAligner extends AlignTestBase {

	@Override
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems, String expectedFailureReason) throws Exception {
		URL schemaUrl = getSchemaURL(schemaFile);
		String xsdUri = schemaUrl.toExternalForm();
		Schema schema = Utils.getSchemaFromResource(schemaUrl);

		XMLSchemaLoader xsLoader = new XMLSchemaLoader();
		XSModel xsModel = xsLoader.loadURI(xsdUri);
		List<XSModel> schemaInformation = new LinkedList<>();
		schemaInformation.add(xsModel);

		String xmlString = getTestFile(inputFile + ".xml");

		boolean expectValid = expectedFailureReason == null;

		assertEquals(expectValid, Utils.validate(schemaUrl, xmlString), "valid XML");

		ValidatorHandler validator = schema.newValidatorHandler();
		DomTreeAligner aligner = new DomTreeAligner(validator, schemaInformation);
		// Xml2Json xml2json = new Xml2Json(aligner, false);

		validator.setContentHandler(aligner);
//    	aligner.setContentHandler(xml2json);

		Document domIn = Utils.string2Dom(xmlString);
		System.out.println("input DOM [" + Utils.dom2String1(domIn) + "]");
//    	System.out.println("xmlString "+xmlString);
//    	System.out.println("dom in "+ToStringBuilder.reflectionToString(domIn.getDocumentElement()));
		Source source = aligner.asSource(domIn);
		System.out.println();
		System.out.println("start aligning " + inputFile);

		String xmlAct = Utils.source2String(source);
		System.out.println("xml aligned via source=" + xmlAct);
		assertNotNull(xmlAct, "xmlAct is null");
		assertTrue(Utils.validate(schemaUrl, xmlAct), "XML is not aligned");

//    	InputSource is = new InputSource(new StringReader(xmlString));
//    	try {
// //       	String jsonOut=xml2json.toString();
//        	System.out.println("jsonOut="+jsonOut);
//        	if (!expectValid) {
//    			fail("expected to fail");
//    		}
//    	} catch (Exception e) {
//    		if (expectValid) {
//    			e.printStackTrace();
//    			fail(e.getMessage());
//    		}
//    	}
//    	String xmlString=getTestXml(xml);
//    	String xsdUri=Utils.class.getResource(xsd).toExternalForm();
//
//    	assertEquals("valid XML", expectValid, Utils.validate(namespace, xsdUri, xmlString));
//
//    	Document dom = Utils.string2Dom(xmlString);
//    	Utils.clean(dom);
//
//    	XMLReader parser = new SAXParser();
//    	Schema schema=Utils.getSchemaFromResource(namespace, xsdUri);
//    	ValidatorHandler validator = schema.newValidatorHandler();
//    	XmlAligner instance = implementation.newInstance();
//    	instance.setPsviProvider((PSVIProvider)validator);
//
//    	parser.setContentHandler(validator);
//    	validator.setContentHandler(instance);
//
//    	System.out.println();
//    	System.out.println("start aligning "+xml);

//    	instance.parse(dom.getDocumentElement());
//    	System.out.println("alignedXml="+Utils.dom2String1(dom));
//    	assertTrue("valid aligned XML", Utils.validate(namespace, xsdUri, dom)); // only if dom  itself is modified...

//    	testXmlAligner(instance, dom.getDocumentElement(), namespace, xsdUri);

//    	JSONObject json=Utils.xml2Json(xmlString);
//    	System.out.println("JSON:"+json);
//    	String jsonString = json.toString();
//
//    	jsonString=jsonString.replaceAll(",\"xmlns\":\"[^\"]*\"", "");
//    	System.out.println("JSON with fixed xmlns:"+jsonString);
//
//    	String xmlFromJson = Utils.json2Xml(Utils.string2Json(jsonString));
//    	if (StringUtils.isNotEmpty(namespace)) {
//    		xmlFromJson=xmlFromJson.replaceFirst(">", " xmlns=\""+namespace+"\">");
//    	}
//    	System.out.println("start aligning xml from json "+xmlFromJson);
//    	Document domFromJson = Utils.string2Dom(xmlFromJson);
//    	instance.startParse(domFromJson.getDocumentElement());

	}

	@Test
	@Disabled("only json to xml")
	public void testNullError1() throws Exception {
		testFiles("DataTypes/DataTypes.xsd", "urn:datatypes", "DataTypes", "/DataTypes/Null-illegal1", "nillable");
		testFiles("DataTypes/DataTypes.xsd", "urn:datatypes", "DataTypes", "/DataTypes/Null-illegal2", "nillable");
	}

	@Override
	@Test
	@Disabled("only json to xml")
	public void testMixedContentUnknown() throws Exception {
		testFiles("Mixed/mixed.xsd", "urn:mixed", "root", "Mixed/mixed-unknown", "Cannot find the declaration of element");
	}

}
