package org.frankframework.align;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;

import jakarta.json.JsonStructure;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

/**
 * @author Gerrit van Brakel
 */
@Log4j2
public class TestXml2Json extends AlignTestBase {

	@Override
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems, String expectedFailureReason) throws Exception {
		boolean expectValidRoundTrip=true;

		testXml2Json(schemaFile, inputFile, rootElement, false, false, expectValidRoundTrip, expectedFailureReason);
		testXml2Json(schemaFile, inputFile, rootElement, true, false, expectValidRoundTrip, expectedFailureReason);
		testXml2Json(schemaFile, inputFile, rootElement, false, true, expectValidRoundTrip, expectedFailureReason);
		testXml2Json(schemaFile, inputFile, rootElement, true, true, expectValidRoundTrip, expectedFailureReason);
	}

	public void testXml2Json(String schemaFile, String xml, String rootElement, boolean compactArrays, boolean skipJsonRootElements, boolean expectValidRoundTrip, String expectedFailureReason) throws Exception {
		URL schemaUrl=getSchemaURL(schemaFile);
		String xmlString=getTestFile(xml+".xml");

		boolean expectValid=expectedFailureReason==null;

		// check the validity of the input XML
		if (expectValid) {
			assertEquals(expectValid, Utils.validate(schemaUrl, xmlString), "XML invalid");
		}

		log.debug("input xml:"+xmlString);
		JsonStructure json;
		String jsonOut;
		try {
			jsonOut= Xml2Json.translate(xmlString, schemaUrl, compactArrays, skipJsonRootElements).toString(true);
			log.debug("result compactArrays ["+compactArrays+"] skipJsonRootElements ["+skipJsonRootElements+"] json:\n" +jsonOut);
			if (!expectValid) {
				fail("expected to fail with reason ["+ expectedFailureReason +"]");
			}
			json = Utils.string2Json(jsonOut);
		} catch (Exception e) {
			if (expectValid) {
				e.printStackTrace();
				log.error("exception compactArrays ["+compactArrays+"] skipJsonRootElements ["+skipJsonRootElements+"]");
				fail(e.getMessage());
			}
			return;
		}
		if (expectValidRoundTrip) {
			if(log.isDebugEnabled()) {
				String backToXml1= Json2Xml.translate(json, schemaUrl, compactArrays, skipJsonRootElements?rootElement:null, null);
				log.debug("back to xml compactArrays ["+compactArrays+"] xml:\n" +backToXml1);
				String backToXml2=Json2Xml.translate(json, schemaUrl, !compactArrays, skipJsonRootElements?rootElement:null, null);
				log.debug("back to xml compactArrays ["+!compactArrays+"] xml:\n" +backToXml2);
			}

			String jsonCompactExpected=getTestFile(xml+"-compact.json");
			String jsonFullExpected=getTestFile(xml+"-full.json");

			if (jsonCompactExpected!=null && compactArrays && skipJsonRootElements) {
				assertEquals(jsonCompactExpected,jsonOut);
			}
			if (jsonFullExpected!=null && !compactArrays && !skipJsonRootElements) {
				assertEquals(jsonFullExpected,jsonOut);
			}
		}

//
//		// setup the chain
//		XMLReader parser = new SAXParser();
//		ValidatorHandler validator = schema.newValidatorHandler();
//		XmlAligner aligner = new XmlAligner(validator);
//		Xml2Json xml2json = new Xml2Json(aligner, false);
//
//		parser.setContentHandler(validator);
//		aligner.setContentHandler(xml2json);
//
//		System.out.println();
//		System.out.println("start aligning "+xml);
//
//		InputSource is = new InputSource(new StringReader(xmlString));
//		try {
//			parser.parse(is);
//			String jsonOut=xml2json.toString();
//			System.out.println("jsonOut="+jsonOut);
//			if (!expectValid) {
//				fail("expected to fail");
//			}
//		} catch (Exception e) {
//			if (expectValid) {
//				e.printStackTrace();
//				fail(e.getMessage());
//			}
//		}
//
//		assertTrue("valid aligned XML", Utils.validate(namespace, xsdUri, dom)); // only if dom  itself is modified...
//
//		testXmlAligner(instance, dom.getDocumentElement(), namespace, xsdUri);

//		JSONObject json=Utils.xml2Json(xmlString);
//		System.out.println("JSON:"+json);
//		String jsonString = json.toString();
//
//		jsonString=jsonString.replaceAll(",\"xmlns\":\"[^\"]*\"", "");
//		System.out.println("JSON with fixed xmlns:"+jsonString);
//
//		String xmlFromJson = Utils.json2Xml(Utils.string2Json(jsonString));
//		if (StringUtils.isNotEmpty(namespace)) {
//			xmlFromJson=xmlFromJson.replaceFirst(">", " xmlns=\""+namespace+"\">");
//		}
//		System.out.println("start aligning xml from json "+xmlFromJson);
//		Document domFromJson = Utils.string2Dom(xmlFromJson);
//		instance.startParse(domFromJson.getDocumentElement());

	}

//	private String getTestFile(String file) throws IOException, TimeoutException {
//		URL url=ClassLoaderUtils.getResourceURL(this, file);
//		if (url==null) {
//			return null;
//		}
//		String string=FileUtils.resourceToString(url);
//		return string;
//	}

//	private String getTestXml(String testxml) throws IOException {
//		BufferedReader buf = new BufferedReader(new InputStreamReader(Utils.class.getResourceAsStream(testxml)));
//		StringBuilder string = new StringBuilder();
//		String line = buf.readLine();
//		while (line != null) {
//			string.append(line).append('\n');
//			line = buf.readLine();
//		}
//		return string.toString();
//
//	}
//
//	@Test
//	public void testOK_abc() throws Exception {
//		testXml2Json("/Abc/abc.xsd","/Abc/abc","a",true);
//	}
//
//	@Test
//	public void testNOK_abc_acb() throws Exception {
//		testXml2Json("/Abc/abc.xsd","/Abc/acb","a",false);
//	}
//
//	@Test
//	public void testAttributes() throws Exception {
//		testXml2Json("/DataTypes/DataTypes.xsd","/DataTypes/Attributes","DataTypes",true);
//	}
//
//	@Test
//	public void testStrings() throws Exception {
//		testXml2Json("/DataTypes/DataTypes.xsd","/DataTypes/Strings","DataTypes",true);
//	}
//
//	@Test
//	public void testSpecialCharacters() throws Exception {
//		testXml2Json("/DataTypes/DataTypes.xsd","/DataTypes/SpecialChars","DataTypes",true);
//	}
//
//	@Test
//	public void testDiacritics() throws Exception {
//		testXml2Json("/DataTypes/DataTypes.xsd","/DataTypes/Diacritics","DataTypes",true);
//	}
//
//	@Test
//	public void testBooleans() throws Exception {
//		testXml2Json("/DataTypes/DataTypes.xsd","/DataTypes/Booleans","DataTypes",true);
//	}
//
//	@Test
//	public void testNumbers() throws Exception {
//		testXml2Json("/DataTypes/DataTypes.xsd","/DataTypes/Numbers","DataTypes",true);
//	}
//
//	@Test
//	public void testDateTime() throws Exception {
//		testXml2Json("/DataTypes/DataTypes.xsd","/DataTypes/DateTime","DataTypes",true);
//	}
//
//	@Test
//	public void testNull() throws Exception {
//		testXml2Json("/DataTypes/DataTypes.xsd","/DataTypes/Null","DataTypes",true);
//	}
//
//	@Test
//	public void testOK_hcdba() throws Exception {
//		testXml2Json("/HCDA/HandleCollectionDisbursementAccount3_v3.0.xsd","/HCDA/HandleCollectionDisbursementAccount","HandleCollectionDisbursementAccount",true);
//	}
//
//	@Test
//	public void testOK_hcdba_unaligned() throws Exception {
//		testXml2Json("/HCDA/HandleCollectionDisbursementAccount3_v3.0.xsd","/HCDA/HandleCollectionDisbursementAccount-Unaligned","HandleCollectionDisbursementAccount",false);
//	}
//
//	@Test
//	public void testOK_Arrays1() throws Exception {
//		testXml2Json("/Arrays/arrays.xsd","/Arrays/arrays","arrays",true);
//	}
//
//	@Test
//	public void testEmptyArrays() throws Exception {
//		testXml2Json("/Arrays/arrays.xsd","/Arrays/empty-arrays","arrays",true);
//	}
//
//	@Test
//	public void testSingleElementArrays() throws Exception {
//		testXml2Json("/Arrays/arrays.xsd","/Arrays/single-element-arrays","arrays",true);
//	}
//
//	@Test
//	public void testSingleComplexArray() throws Exception {
//		testXml2Json("/Arrays/arrays.xsd","/Arrays/single-complex-array","array1",true);
//	}
//
//	@Test
//	public void testSingleSimpleArray() throws Exception {
//		testXml2Json("/Arrays/arrays.xsd","/Arrays/single-simple-array","singleSimpleRepeatedElement",true);
//	}
//
//	@Test
//	public void testSimpleStruct() throws Exception {
//		testXml2Json("/Simple/simple.xsd","/Simple/simple","simple",true);
//	}
//
//	@Test
//	public void testMixedContent() throws Exception {
//		testXml2Json("/Mixed/mixed.xsd","/mixed/mixed-simple","root",true);
//		testXml2Json("/Mixed/mixed.xsd","/mixed/mixed-complex","root",true);
//		testXml2Json("/Mixed/mixed.xsd","/mixed/mixed-empty","root",true);
//		testXml2Json("/Mixed/mixed.xsd","/mixed/mixed-unknown","root",true, false);
//	}
//
//	@Test
//	public void testChoiceOfSequence() throws Exception {
//		//testStrings("<a><b></b><c></c></a>","{\"a\":{\"b\":\"\",\"c\":\"\"}}");
//		testXml2Json("/Transaction/transaction.xsd","/Transaction/order","transaction",true);
//		testXml2Json("/Transaction/transaction.xsd","/Transaction/invoice","transaction",true);
//	}
//
	@Test
	public void testLeadingZeroes() throws Exception {
		testXml2Json("DataTypes/DataTypes.xsd", "/DataTypes/Numbers-leadingzeroes", "DataTypes", true, true, false, null);
	}


	@Override
	@Test
	@Disabled("test on erronous json input")
	public void testMixedContentUnknown() throws Exception {
		super.testMixedContentUnknown();
	}

}
