package nl.nn.adapterframework.align;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * @author Gerrit van Brakel
 */
public class TestValidations {

	public void validationTest(String namespace, String xsd, String xmlResource, boolean expectValid) throws Exception {

		String xmlString = Utils.getTestXml(xmlResource);
		URL schemaUrl = Utils.class.getResource(xsd);
		assertEquals("valid XML", expectValid, Utils.validate(schemaUrl, xmlString));

		Document dom = Utils.string2Dom(xmlString);
		assertEquals("valid XML DOM", expectValid, Utils.validate(schemaUrl, dom));
		String xml1 = Utils.dom2String1(dom);
		assertEquals("valid XML dom2string1", expectValid, Utils.validate(schemaUrl, xml1));
		String xml2 = Utils.dom2String2(dom);
		assertEquals("valid XML dom2string1", expectValid, Utils.validate(schemaUrl, xml2));

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
