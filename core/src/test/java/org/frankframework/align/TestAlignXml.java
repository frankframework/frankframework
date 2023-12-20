package org.frankframework.align;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.frankframework.util.XmlUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

public class TestAlignXml {

	public static String BASEDIR = "/Align/";

	public void testXml(String xml, URL schemaUrl, String expectedFailureReason, String description) throws Exception {
		testXml(xml, schemaUrl, expectedFailureReason, description, true, false);
	}

	public void testXml(String xml, URL schemaUrl, String expectedFailureReason, String description, boolean inputValid, boolean ignoreExtraElements) throws Exception {
		Document dom = XmlUtils.buildDomDocument(xml, true);

		// check the validity of the input XML
		if (inputValid) assertTrue(Utils.validate(schemaUrl, xml), "valid input XML");

		try {
			String xmlAct = DomTreeAligner.translate(dom, schemaUrl, ignoreExtraElements);
			System.out.println("xml out=" + xmlAct);
			if (expectedFailureReason != null) {
				fail("Expected to fail: " + description);
			}
			if (xmlAct == null) {
				fail("could not convert to xml: " + description);
			}
			assertTrue(Utils.validate(schemaUrl, xmlAct), "converted XML is not aligned: " + description);
//	       	assertEquals("round tripp",xml,xmlAct);
		} catch (Exception e) {
			if (expectedFailureReason == null) {
				e.printStackTrace();
				fail("Expected conversion to succeed: " + description);
			}
			String msg = e.getMessage();
			if (msg == null) {
				e.printStackTrace();
				fail("msg==null (" + e.getClass().getSimpleName() + ")");
			}
			if (!msg.contains(expectedFailureReason)) {
				e.printStackTrace();
				fail("expected reason [" + expectedFailureReason + "] in msg [" + msg + "]");
			}
		}
	}

	public void testStrings(String xmlIn, URL schemaUrl, String targetNamespace, String rootElement, String expectedFailureReason) throws Exception {
		System.out.println("schemaUrl [" + schemaUrl + "]");
		if (xmlIn != null) assertTrue(Utils.validate(schemaUrl, xmlIn), "input not valid");

		testXml(xmlIn, schemaUrl, expectedFailureReason, "");
	}

	protected String getTestFile(String file) throws IOException {
		URL url = AlignTestBase.class.getResource(BASEDIR + file);
		if (url == null) {
			return null;
		}
		BufferedReader buf = new BufferedReader(new InputStreamReader(url.openStream()));
		StringBuilder string = new StringBuilder();
		String line = buf.readLine();
		while (line != null) {
			string.append(line);
			line = buf.readLine();
			if (line != null) {
				string.append("\n");
			}
		}
		return string.toString();
	}

	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile) throws Exception {
		testFiles(schemaFile, namespace, rootElement, inputFile, null);
	}

	public void testFiles(String schemaFile, String namespace, String rootElement, String file, String expectedFailureReason) throws Exception {
		URL schemaUrl = Utils.class.getResource(BASEDIR + schemaFile);
		String xmlString = getTestFile(file + ".xml");
		testStrings(xmlString, schemaUrl, namespace, rootElement, expectedFailureReason);
	}

	@Test
	public void testOK_abc() throws Exception {
		// testStrings("<a><b></b><c></c></a>","{\"a\":{\"b\":\"\",\"c\":\"\"}}");
		testFiles("Abc/abc.xsd", "urn:test", "a", "Abc/abc");
	}

	@Test
	public void testAbcExtraElementsNotAllowed() throws Exception {
		// testStrings("<a><b></b><c></c></a>","{\"a\":{\"b\":\"\",\"c\":\"\"}}");
		String schemaFile = "Abc/abc.xsd";
		String inputFile = "Abc/abc-x";
		String expectedFailureReason = "Cannot find the declaration of element [x]";
		URL schemaUrl = Utils.class.getResource(BASEDIR + schemaFile);
		String xmlString = getTestFile(inputFile + ".xml");
		testXml(xmlString, schemaUrl, expectedFailureReason, "extra elements not allowed", false, false);
	}

	@Test
	public void testAbcExtraElementsIgnored() throws Exception {
		// testStrings("<a><b></b><c></c></a>","{\"a\":{\"b\":\"\",\"c\":\"\"}}");
		String schemaFile = "Abc/abc.xsd";
		String inputFile = "Abc/abc-x";
		String expectedFailureReason = null;
		URL schemaUrl = Utils.class.getResource(BASEDIR + schemaFile);
		String xmlString = getTestFile(inputFile + ".xml");
		testXml(xmlString, schemaUrl, expectedFailureReason, "extra elements not allowed", false, true);
	}

	@Test
	public void testOK_hcda() throws Exception {
		testFiles("HCDA/HandleCollectionDisbursementAccount3_v3.0.xsd", "", "HandleCollectionDisbursementAccount", "HCDA/HandleCollectionDisbursementAccount");
	}

	@Test
	public void testArrays() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd", "urn:arrays", "arrays", "Arrays/arrays", null);
	}

	@Test
	public void testEmptyArrays() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd", "urn:arrays", "arrays", "Arrays/empty-arrays", null);
	}

	@Test
	public void testSingleElementArrays() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd", "urn:arrays", "arrays", "Arrays/single-element-arrays", null);
	}

	@Test
	public void testSingleComplexArray() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd", "urn:arrays", "array1", "Arrays/single-complex-array", null);
	}

	@Test
	public void testSingleSimpleArray() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd", "urn:arrays", "singleSimpleRepeatedElement", "Arrays/single-simple-array", null);
	}

	@Test
	public void testAttributes() throws Exception {
		testFiles("/DataTypes/DataTypes.xsd", "urn:datatypes", "DataTypes", "/DataTypes/Attributes");
	}

	@Test
	public void testStrings() throws Exception {
		testFiles("/DataTypes/DataTypes.xsd", "urn:datatypes", "DataTypes", "/DataTypes/Strings", null);
	}

	@Test
	public void testSpecialChars() throws Exception {
		testFiles("/DataTypes/DataTypes.xsd", "urn:datatypes", "DataTypes", "/DataTypes/SpecialChars", null);
	}

	@Test
	public void testDiacritics() throws Exception {
		testFiles("/DataTypes/DataTypes.xsd", "urn:datatypes", "DataTypes", "/DataTypes/Diacritics", null);
	}

	@Test
	public void testBooleans() throws Exception {
		testFiles("/DataTypes/DataTypes.xsd", "urn:datatypes", "DataTypes", "/DataTypes/Booleans", null);
	}

	@Test
	public void testNumbers() throws Exception {
		testFiles("/DataTypes/DataTypes.xsd", "urn:datatypes", "DataTypes", "/DataTypes/Numbers");
	}

	@Test
	public void testDateTime() throws Exception {
		testFiles("/DataTypes/DataTypes.xsd", "urn:datatypes", "DataTypes", "/DataTypes/DateTime");
	}

	@Test
	public void testNull() throws Exception {
		testFiles("/DataTypes/DataTypes.xsd", "urn:datatypes", "DataTypes", "/DataTypes/Null");
	}

//    @Test
//    public void testNullError1() throws Exception {
//    	testFiles("/DataTypes/Null-illegal1","urn:datatypes","/DataTypes/DataTypes.xsd","DataTypes", false, true, "nillable");
//    	testFiles("/DataTypes/Null-illegal2","urn:datatypes","/DataTypes/DataTypes.xsd","DataTypes", false, true, "nillable");
//    }

	@Test
	public void testChoiceOfSequence() throws Exception {
		// testStrings("<a><b></b><c></c></a>","{\"a\":{\"b\":\"\",\"c\":\"\"}}");
		testFiles("ChoiceOfSequence/transaction.xsd", "", "transaction", "ChoiceOfSequence/order");
		testFiles("ChoiceOfSequence/transaction.xsd", "", "transaction", "ChoiceOfSequence/invoice");
	}

	@Test
	public void testRepeatedElements() throws Exception {
//    	testFiles("/RepeatedElements/sprint-withRepeatedElement","","/RepeatedElements/sprint.xsd","sprint");
//    	testFiles("/RepeatedElements/sprint-withoutRepeatedElement","","/RepeatedElements/sprint.xsd","sprint");
		testFiles("/RepeatedElements/sprint.xsd", "", "sprint", "/RepeatedElements/sprint-emptyRepeatedElement", null);
	}

	@Test
	public void testSimple() throws Exception {
		testFiles("/Simple/simple.xsd", "urn:simple", "simple", "/Simple/simple");
	}

}
