package nl.nn.adapterframework.pipes;

import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class Json2XmlValidatorTest extends PipeTestBase<Json2XmlValidator> {

	@Override
	public Json2XmlValidator createPipe() {
		return new Json2XmlValidator();
	}

	@Test
	public void testNullInput() throws Exception {
		//Arrange
		pipe.setName("null_input");
		pipe.setSchema("/Align/OptionalArray/hbp.xsd");
		pipe.setRoot("Root");
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		//Act
		PipeRunResult prr = doPipe(Message.nullMessage());

		//Assert
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><ns1:Root xmlns:ns1=\"urn:pim\"/>", prr.getResult().asString());
	}

	@Test
	public void testEmptyInput() throws Exception {
		//Arrange
		pipe.setName("empty_input");
		pipe.setSchema("/Align/OptionalArray/hbp.xsd");
		pipe.setRoot("Root");
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		//Act
		PipeRunResult prr = doPipe("");

		//Assert
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><ns1:Root xmlns:ns1=\"urn:pim\"/>", prr.getResult().asString());
	}

	@Test
	public void testInputWithWhitespace() throws Exception {
		//Arrange
		pipe.setName("empty_input");
		pipe.setSchema("/Align/OptionalArray/hbp.xsd");
		pipe.setRoot("Root");
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		//Act
		PipeRunResult prr = doPipe("         			{}");

		//Assert
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><ns1:Root xmlns:ns1=\"urn:pim\"/>", prr.getResult().asString());
	}

	@Test
	public void testNoNamespaceXml2Json() throws Exception {
		pipe.setName("Response_To_Json");
		pipe.setOutputFormat(DocumentFormat.JSON);
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response.xml");

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, input,session));
		assertThat(e.getMessage(), containsString("Cannot find the declaration of element 'BusinessPartner'"));
	}

	@Test
	public void testNoNamespaceXml() throws Exception {
		pipe.setName("Response_Validator");
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
		pipe.setThrowException(true);
		pipe.registerForward(new PipeForward("success",null));
		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response.xml");

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, input,session));
		assertThat(e.getMessage(), containsString("Cannot find the declaration of element 'BusinessPartner'"));
	}

	@Test
	public void testAcceptNamespacelessXMLtoJSON() throws Exception {
		pipe.setName("Response_To_Json");
		pipe.setOutputFormat(DocumentFormat.JSON);
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
		pipe.setTargetNamespace("http://nn.nl/XSD/CustomerAdministration/Party/1/GetPartiesOnAgreement/7");
		pipe.setAcceptNamespacelessXml(true);
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response.xml");
		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-compact.json");

		PipeRunResult prr = doPipe(pipe, input,session);

		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testAcceptNamespacelessXMLvalidation() throws Exception {
		pipe.setName("Response_To_Json");
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
		pipe.setTargetNamespace("http://nn.nl/XSD/CustomerAdministration/Party/1/GetPartiesOnAgreement/7");
		pipe.setAcceptNamespacelessXml(true);
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response.xml");
		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-withNamespace.xml");

		PipeRunResult prr = doPipe(pipe, input,session);

		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testWithNamespace() throws Exception {
		pipe.setName("Response_To_Json");
		pipe.setOutputFormat(DocumentFormat.JSON);
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
//		pipe.setRoot("GetPartiesOnAgreement_Response");
//		pipe.setTargetNamespace("http://nn.nl/XSD/CustomerAdministration/Party/1/GetPartiesOnAgreement/7");
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-withNamespace.xml");
		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-compact.json");

		PipeRunResult prr = doPipe(pipe, input,session);

		assertEquals(expected, prr.getResult().asString());
	}


	public String setupAcceptHeaderTest(String acceptHeaderValue) throws Exception {
		pipe.setName("Response_To_Json_from_acceptHeader");
		pipe.setInputFormatSessionKey("AcceptHeader");
		pipe.setOutputFormat(DocumentFormat.XML);
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
		pipe.setResponseRoot("BusinessPartner");

		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		session.put("AcceptHeader", acceptHeaderValue);

		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-withNamespace.xml");

		doPipe(pipe, input, session); // first run the request validation ...

		IValidator validator = pipe.getResponseValidator();
		PipeRunResult prr_response = validator.doPipe(new Message(input), session);

		return prr_response.getResult().asString();
	}

	@Test
	public void testAcceptHeaderFromMessage() throws Exception {
		pipe.setName("Response_To_Json_from_acceptHeader");
		pipe.setOutputFormat(DocumentFormat.XML);
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
		pipe.setResponseRoot("BusinessPartner");
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-withNamespace.xml");
		Message inputMessage = new Message(input, new MessageContext().with("Header.Accept", "application/json"));

		doPipe(inputMessage);

		assertEquals(DocumentFormat.JSON, pipe.getOutputFormat(session, true));
	}

	@Test
	public void testAcceptHeaderAndIgnoreMessage() throws Exception {
		pipe.setName("Response_To_Json_from_acceptSession");
		pipe.setInputFormatSessionKey("Accept");
		pipe.setOutputFormat(DocumentFormat.XML);
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
		pipe.setResponseRoot("BusinessPartner");
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		session.put("Accept", "json");

		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-withNamespace.xml");
		Message inputMessage = new Message(input, new MessageContext().with("Header.Accept", "application/pdf"));

		doPipe(inputMessage);

		assertEquals(DocumentFormat.JSON, pipe.getOutputFormat(session, true));
	}

	@Test
	public void testAcceptHeaderApplicationJson() throws Exception {
		String actual = setupAcceptHeaderTest("application/json");
		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-compact.json");
		assertEquals(expected, actual);
	}

	@Test
	public void testAcceptHeaderTextXML() throws Exception {
		String actual = setupAcceptHeaderTest("text/xml");
		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-withNamespace.xml");
		assertEquals(expected, actual);
	}

	@Test
	public void testAcceptHeaderApplicationXML() throws Exception {
		String actual = setupAcceptHeaderTest("application/xml");
		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-withNamespace.xml");
		assertEquals(expected, actual);
	}


	@Test
	public void testWithParameters() throws Exception {
		pipe.setName("RestGet");
		pipe.setRoot("Root");
		pipe.setOutputFormat(DocumentFormat.XML);
		pipe.setSchema("/Validation/Parameters/simple.xsd");
		pipe.setThrowException(true);

		pipe.addParameter(new Parameter("a", "param_a"));
		pipe.addParameter(ParameterBuilder.create().withName("b").withSessionKey("b_key"));
		pipe.addParameter(ParameterBuilder.create().withName("c").withSessionKey("c_key"));
		pipe.addParameter(ParameterBuilder.create().withName("d").withSessionKey("d_key"));
		pipe.configure();
		pipe.start();

		String input="";
		String expected = TestFileUtils.getTestFile("/Validation/Parameters/out.xml");

		session.put("b_key","b_value");
		// session variable "c_key is not present, so there should be no 'c' element in the result
		session.put("d_key","");

		PipeRunResult prr = doPipe(pipe, input,session);

		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testWithParametersNestedElement() throws Exception {
		pipe.setName("Find with Nested Element");
		pipe.setSchema("/Align/NestedValue/nestedValue.xsd");
		pipe.setRoot("NestedValue");
		pipe.setThrowException(true);

		pipe.addParameter(new Parameter("Id", "3242343"));

		pipe.setDeepSearch(true); // deepSearch is required to find element in optional branches of the document

		pipe.configure();
		pipe.start();

		String input="";
		String expected = TestFileUtils.getTestFile("/Align/NestedValue/nestedValue.xml");

		PipeRunResult prr = doPipe(pipe, input,session);

		String actualXml = Message.asString(prr.getResult());

		assertXmlEquals("converted XML does not match", expected, actualXml, true);
	}

	@Test
	public void testWithDoubleId() throws Exception {
		pipe.setName("testWithPerson");
		pipe.setSchema("/Align/DoubleId/Party.xsd");
		pipe.setRoot("Party");
		pipe.setThrowException(true);

		//pipe.setDeepSearch(true); // deepSearch is required to find element in optional branches of the document

		pipe.addParameter(new Parameter("Id", "24"));

		pipe.configure();
		pipe.start();

		String input    = TestFileUtils.getTestFile("/Align/DoubleId/Party-Template.json");
		String expected = TestFileUtils.getTestFile("/Align/DoubleId/Party.xml");

		PipeRunResult prr = doPipe(pipe, input,session);

		String actualXml = Message.asString(prr.getResult());

		assertXmlEquals("converted XML does not match", expected, actualXml, true);
	}

	@Test
	public void testnoParams() throws Exception {
		pipe.setName("testMultivaluedParameters");
		pipe.setSchema("/Align/Options/options.xsd");
		pipe.setRoot("Options");
		pipe.setThrowException(true);


		pipe.configure();
		pipe.start();

		String input    = "{ \"stringArray\" : [ \"xx\", \"yy\" ] }";
		String expected = TestFileUtils.getTestFile("/Align/Options/stringArray.xml");


		PipeRunResult prr = doPipe(pipe, input,session);

		String actualXml = Message.asString(prr.getResult());

		assertXmlEquals("converted XML does not match", expected, actualXml, true);
	}


	@Test
	public void testMultivaluedParameters() throws Exception {
		pipe.setName("testMultivaluedParameters");
		pipe.setSchema("/Align/Options/options.xsd");
		pipe.setRoot("Options");
		pipe.setThrowException(true);

		pipe.addParameter(new Parameter("singleInt", "33"));

		pipe.addParameter(new Parameter("singleString", "tja"));

		List<String> stringValues = Arrays.asList("aa","bb");
		session.put("StringValueList", stringValues);
		pipe.addParameter(ParameterBuilder.create().withName("stringArray2").withSessionKey("StringValueList"));

		List<String> intValues = Arrays.asList("11","22");
		session.put("IntValueList", intValues);
		pipe.addParameter(ParameterBuilder.create().withName("intArray").withSessionKey("IntValueList"));

		List<String> stringElem3elements = Arrays.asList("aa","bb");
		session.put("stringElem3elements", stringElem3elements);
		pipe.addParameter(ParameterBuilder.create().withName("stringElem3").withSessionKey("stringElem3elements"));

		pipe.configure();
		pipe.start();

		String input    = "{ \"stringArray\" : [ \"xx\", \"yy\" ] }";
		String expected = TestFileUtils.getTestFile("/Align/Options/allOptions.xml");

		PipeRunResult prr = doPipe(pipe, input,session);

		String actualXml = Message.asString(prr.getResult());

		assertXmlEquals("converted XML does not match", expected, actualXml, true);
	}

	@Test
	public void testMultivaluedParametersFindDocuments() throws Exception {
		pipe.setName("testMultivaluedParameters");
		pipe.setSchema("/Align/FindDocuments/findDocuments.xsd");
		pipe.setRoot("FindDocuments_Request");
		pipe.setThrowException(true);

		pipe.addParameter(new Parameter("schemaName", "NNPensioenen"));
		pipe.addParameter(new Parameter("requestUserId", "postman2"));
		pipe.addParameter(ParameterBuilder.create().withName("SearchAttributes/agreementNumber").withSessionKey("agreementNumbers"));

		pipe.configure();
		pipe.start();

		List<String> agreementNumbers = new ArrayList<>();
		agreementNumbers.add("12.12");
		agreementNumbers.add("33002118");

		session.put("agreementNumbers", agreementNumbers);

		String input    = "{}";
		String expected = TestFileUtils.getTestFile("/Align/FindDocuments/FindDocumentsRequest.xml");


		PipeRunResult prr = doPipe(pipe, input,session);

		String actualXml = Message.asString(prr.getResult());

		assertXmlEquals("converted XML does not match", expected, actualXml, true);
	}

	@Test
	public void testMultivaluedParametersFindDocumentsDeepSearch() throws Exception {
		pipe.setName("testMultivaluedParameters");
		pipe.setSchema("/Align/FindDocuments/findDocuments.xsd");
		pipe.setRoot("FindDocuments_Request");
		pipe.setDeepSearch(true);
		pipe.setThrowException(true);

		pipe.addParameter(new Parameter("schemaName", "NNPensioenen"));
		pipe.addParameter(new Parameter("requestUserId", "postman2"));
		pipe.addParameter(ParameterBuilder.create().withName("agreementNumber").withSessionKey("agreementNumbers"));

		pipe.configure();
		pipe.start();

		List<String> agreementNumbers = new ArrayList<>();
		agreementNumbers.add("12.12");
		agreementNumbers.add("33002118");

		session.put("agreementNumbers", agreementNumbers);

		String input    = "{}";
		String expected = TestFileUtils.getTestFile("/Align/FindDocuments/FindDocumentsRequest.xml");


		PipeRunResult prr = doPipe(pipe, input,session);

		String actualXml = Message.asString(prr.getResult());

		assertXmlEquals("converted XML does not match", expected, actualXml, true);
	}

	@Test
	public void testSingleValueInArrayListParam() throws Exception {
		pipe.setName("testMultivaluedParameters");
		pipe.setSchema("/Align/Options/options.xsd");
		pipe.setRoot("Options");
		pipe.setThrowException(true);

		List<String> intValues = Arrays.asList("44");
		session.put("IntValueList", intValues);
		pipe.addParameter(ParameterBuilder.create().withName("intArray").withSessionKey("IntValueList"));

		pipe.configure();
		pipe.start();

		String input    = "{  }";
		String expected = TestFileUtils.getTestFile("/Align/Options/singleValueInArray.xml");

		PipeRunResult prr = doPipe(pipe, input,session);

		String actualXml = Message.asString(prr.getResult());

		assertXmlEquals("converted XML does not match", expected, actualXml, true);
	}

	@Test
	public void testSingleValueInArrayStringParam() throws Exception {
		pipe.setName("testMultivaluedParameters");
		pipe.setSchema("/Align/Options/options.xsd");
		pipe.setRoot("Options");
		pipe.setThrowException(true);

		pipe.addParameter(new Parameter("intArray", "44"));

		pipe.configure();
		pipe.start();

		String input    = "{  }";
		String expected = TestFileUtils.getTestFile("/Align/Options/singleValueInArray.xml");


		PipeRunResult prr = doPipe(pipe, input,session);

		String actualXml = Message.asString(prr.getResult());

		assertXmlEquals("converted XML does not match", expected, actualXml, true);
	}

	public void testStoreRootElement(DocumentFormat outputFormat, String inputFile, boolean setRootElement) throws Exception {
		pipe.setName("testStoreRootElement");
		pipe.setSchema("/Align/Abc/abc.xsd");
		pipe.setRootElementSessionKey("rootElement");
		pipe.setOutputFormat(outputFormat);
		if (setRootElement) {
			pipe.setRoot("a");
		}

		pipe.registerForward(new PipeForward("failure",null));
		pipe.registerForward(new PipeForward(PipeForward.EXCEPTION_FORWARD_NAME,null));

		pipe.configure();
		pipe.start();

		String input    = TestFileUtils.getTestFile("/Align/Abc/"+inputFile);

		PipeRunResult prr = doPipe(pipe, input,session);

		String expectedForward = "success";
		String actualForward = prr.getPipeForward().getName();
		assertEquals(expectedForward, actualForward);

		assertEquals("a", session.get("rootElement"));
	}

	@Test
	public void testStoreRootElementXml2Json() throws Exception {
		testStoreRootElement(DocumentFormat.JSON,"abc.xml",false);
	}
	@Test
	public void testStoreRootElementJson2XmlFull() throws Exception {
		testStoreRootElement(DocumentFormat.XML,"abc-full.json",false);
	}
	@Test
	public void testStoreRootElementJson2XmlCompact() throws Exception {
		testStoreRootElement(DocumentFormat.XML,"abc-compact.json",true);
	}
	@Test
	public void testStoreRootElementJson2JsonFull() throws Exception {
		testStoreRootElement(DocumentFormat.JSON,"abc-full.json",false);
	}
	@Test
	public void testStoreRootElementJson2JsonCompact() throws Exception {
		testStoreRootElement(DocumentFormat.JSON,"abc-compact.json",true);
	}
	@Test
	public void testStoreRootElementXml2Xml() throws Exception {
		testStoreRootElement(DocumentFormat.XML,"abc.xml",false);
	}

	public void testRecoverableError(DocumentFormat outputFormat, boolean ignoreUndeclaredElements, String expectedForward, String expectedErrorMessage) throws Exception {
		pipe.setName("testValidWithWarnings");
		pipe.setSchema("/Align/Abc/abc.xsd");
		pipe.setOutputFormat(outputFormat);
		pipe.setRoot("a");
		pipe.setReasonSessionKey("reasons");
		pipe.setXmlReasonSessionKey("XmlReasons");
		pipe.setIgnoreUndeclaredElements(ignoreUndeclaredElements);

		pipe.registerForward(new PipeForward("failure",null));
		pipe.registerForward(new PipeForward("warnings",null));
		pipe.registerForward(new PipeForward(PipeForward.EXCEPTION_FORWARD_NAME,null));

		pipe.configure();
		pipe.start();

		String input          = TestFileUtils.getTestFile("/Align/Abc/abc-err"+ (outputFormat == DocumentFormat.XML ? ".json" : ".xml"));
		String expectedResult = TestFileUtils.getTestFile("/Align/Abc/abc"+(outputFormat == DocumentFormat.XML ? ".xml" : "-compact.json"));

		PipeRunResult prr = doPipe(pipe, input,session);

		assertEquals(expectedForward, prr.getPipeForward().getName());
		if (outputFormat == DocumentFormat.XML) {
			assertXmlEquals("recovered result", expectedResult, prr.getResult().asString(), true);
		} else {
			assertEquals(expectedResult, prr.getResult().asString());
		}

		assertThat((String)session.get("reasons"), containsString(expectedErrorMessage));
	}

	@Test
	public void testRecoverableErrorJson2Xml() throws Exception {
		testRecoverableError(DocumentFormat.XML, false, "failure", "Cannot find the declaration of element [d]");
	}
	@Test
	public void testValidWithWarningsJson2Xml() throws Exception {
		testRecoverableError(DocumentFormat.XML, true, "warnings", "Cannot find the declaration of element [d]");
	}
	@Test
	public void testRecoverableErrorXml2Json() throws Exception {
		testRecoverableError(DocumentFormat.JSON, false, "failure", "No typeDefinition found for element [d]");
	}
	@Test
	@Ignore("Cannot ignore XML validation failure")
	public void testValidWithWarningsXml2Json() throws Exception {
		testRecoverableError(DocumentFormat.JSON, true, "warnings", "No typeDefinition found for element [d]");
	}
}
