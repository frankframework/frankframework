package org.frankframework.align;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * @author Gerrit van Brakel
 */
public class TestValidations {

	public void validationTest(String namespace, String xsd, String xmlResource, boolean expectValid) throws Exception {

		String xmlString = Utils.getTestXml(xmlResource);
		URL schemaUrl = Utils.class.getResource(xsd);
		assertEquals(expectValid, Utils.validate(schemaUrl, xmlString), "valid XML");

		Document dom = Utils.string2Dom(xmlString);
		assertEquals(expectValid, Utils.validate(schemaUrl, dom), "valid XML DOM");
		String xml1 = Utils.dom2String1(dom);
		assertEquals(expectValid, Utils.validate(schemaUrl, xml1), "valid XML dom2string1");
		String xml2 = Utils.dom2String2(dom);
		assertEquals(expectValid, Utils.validate(schemaUrl, xml2), "valid XML dom2string1");

	}

	@Test
	public void testValidToSchema() throws Exception {
//    	validationTest("http://www.ing.com/testxmlns", "/GetIntermediaryAgreementDetails/xsd/A_correct.xsd","/intermediaryagreementdetails.xml",false,true);
		validationTest("urn:test", "/Align/Abc/abc.xsd", "/Align/Abc/abc.xml", true);
	}

	@Test
	public void testNotValidToSchema() throws Exception {
		validationTest("urn:test", "/Align/Abc/abc.xsd", "/Align/Abc/acb.xml", false);
	}

}
