package org.frankframework.pipes;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.frankframework.testutil.TestAssertions.assertEqualsIgnoreWhitespaces;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import org.frankframework.core.IValidator;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestFileUtils;


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
		pipe.addForward(new PipeForward("success",null));
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
		assertEquals(MediaType.APPLICATION_JSON, prr.getResult().getContext().get(MessageContext.METADATA_MIMETYPE));
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
		assertEquals(MediaType.APPLICATION_XML, prr.getResult().getContext().get(MessageContext.METADATA_MIMETYPE));
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
	public void testDefaultAcceptHeaderFromMessage() throws Exception {
		testDefaultAcceptHeaderFromEmptyMessage(DocumentFormat.XML, DocumentFormat.XML, "*/*");
		testDefaultAcceptHeaderFromXmlInputMessage(DocumentFormat.XML, DocumentFormat.XML, "*/*");
	}

	@Test
	public void testDefaultAcceptHeaderFromMessageWithFaultyAcceptHeader() throws Exception {
		testDefaultAcceptHeaderFromEmptyMessage(DocumentFormat.XML, DocumentFormat.XML, "*/*;text/html");
		testDefaultAcceptHeaderFromXmlInputMessage(DocumentFormat.XML, DocumentFormat.XML, "*/*;text/html");
	}

	@Test
	public void testDefaultAcceptHeaderFromMessageJSON() throws Exception {
		testDefaultAcceptHeaderFromEmptyMessage(DocumentFormat.JSON, DocumentFormat.JSON, "*/*");
		testDefaultAcceptHeaderFromXmlInputMessage(DocumentFormat.JSON, DocumentFormat.JSON, "*/*");
	}

	@Test
	public void testMultipleAcceptHeaderValuesFromMessage() throws Exception {
		testDefaultAcceptHeaderFromEmptyMessage(DocumentFormat.XML, DocumentFormat.JSON, "application/json    ,application/xml,     */*");
		testDefaultAcceptHeaderFromXmlInputMessage(DocumentFormat.XML, DocumentFormat.JSON, "application/json    ,application/xml,     */*");
	}

	@Test
	public void testLongAcceptHeaderFromMessage() throws Exception {
		testDefaultAcceptHeaderFromEmptyMessage(DocumentFormat.JSON, DocumentFormat.XML, "text/html,application/xml,*/*");
		testDefaultAcceptHeaderFromXmlInputMessage(DocumentFormat.JSON, DocumentFormat.XML, "text/html,application/xml,*/*");
	}

	@Test
	public void testAcceptHeaderShouldIgnoreQFactor() throws Exception {
		testDefaultAcceptHeaderFromEmptyMessage(DocumentFormat.XML, DocumentFormat.JSON, "*/*;q=0.8,text/html,application/json;q=0.9");
		testDefaultAcceptHeaderFromXmlInputMessage(DocumentFormat.XML, DocumentFormat.JSON, "*/*;q=0.8,text/html,application/json;q=0.9");
	}

	private void testDefaultAcceptHeaderFromEmptyMessage(DocumentFormat defaultFormat, DocumentFormat expectedFormat, String acceptHeader) throws Exception {
		pipe.setName("Response_To_Json_from_acceptHeader");
		pipe.setRoot("Root");
		pipe.setResponseRoot("Root");
		pipe.setOutputFormat(defaultFormat);
		pipe.setSchema("/Validation/Parameters/simple.xsd");
		pipe.setThrowException(true);

		pipe.addParameter(new Parameter("a", "param_a"));
		pipe.addParameter(ParameterBuilder.create().withName("b").withSessionKey("b_key"));
		pipe.addParameter(ParameterBuilder.create().withName("c").withSessionKey("c_key"));
		pipe.addParameter(ParameterBuilder.create().withName("d").withSessionKey("d_key"));
		pipe.configure();
		pipe.start();

		// Get request with no content, when no (valid) accept header, fall back to the default.
		Message inputMessage = new Message("", new MessageContext().with("Header.Accept", acceptHeader));

		PipeRunResult inResult = doPipe(inputMessage);
		PipeRunResult outResult = pipe.getResponseValidator().validate(inputMessage, session, "Root");

		assertEquals(expectedFormat, pipe.getOutputFormat(session, true));
		assertEquals(defaultFormat == DocumentFormat.JSON ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML, inResult.getResult().getContext().get(MessageContext.METADATA_MIMETYPE));
		assertEquals(expectedFormat == DocumentFormat.JSON ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML, outResult.getResult().getContext().get(MessageContext.METADATA_MIMETYPE));
	}

	private void testDefaultAcceptHeaderFromXmlInputMessage(DocumentFormat defaultFormat, DocumentFormat expectedFormat, String acceptHeader) throws Exception {
		pipe.setName("Response_To_Json_from_acceptHeader");
		pipe.setRoot("Root");
		pipe.setResponseRoot("Root");
		pipe.setOutputFormat(defaultFormat);
		pipe.setSchema("/Validation/Parameters/simple.xsd");
		pipe.setThrowException(true);

		pipe.configure();
		pipe.start();

		// Get request with no content, when no (valid) accept header, fall back to the default.
		Message inputMessage = new Message("<Root><a/></Root>", new MessageContext().with("Header.Accept", acceptHeader));

		PipeRunResult inResult = doPipe(inputMessage);
		PipeRunResult outResult = pipe.getResponseValidator().validate(inputMessage, session, "Root");

		assertEquals(expectedFormat, pipe.getOutputFormat(session, true));
		assertEquals(defaultFormat == DocumentFormat.JSON ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML, inResult.getResult().getContext().get(MessageContext.METADATA_MIMETYPE));
		assertEquals(expectedFormat == DocumentFormat.JSON ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML, outResult.getResult().getContext().get(MessageContext.METADATA_MIMETYPE));
	}

	@Test
	public void testInputFormatSessionKeyAndIgnoreMessage() throws Exception {
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

		String actualXml = prr.getResult().asString();

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

		String actualXml = prr.getResult().asString();

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

		String actualXml = prr.getResult().asString();

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

		String actualXml = prr.getResult().asString();

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

		String actualXml = prr.getResult().asString();

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

		String actualXml = prr.getResult().asString();

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

		String actualXml = prr.getResult().asString();

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

		String actualXml = prr.getResult().asString();

		assertXmlEquals("converted XML does not match", expected, actualXml, true);
	}

	@Test
	public void testAttributeValue() throws Exception {
		pipe.setName("testAttributeValue");
		pipe.setSchema("/Align/TextAndAttributes/schema.xsd");
		pipe.setRoot("Root");
		pipe.setThrowException(true);

		pipe.addParameter(new Parameter("intArray", "44"));

		pipe.configure();
		pipe.start();

		String input    = TestFileUtils.getTestFile("/Align/TextAndAttributes/input-compact.json");
		String expected = TestFileUtils.getTestFile("/Align/TextAndAttributes/input.xml");

		PipeRunResult prr = doPipe(pipe, input,session);
		String actualXml = prr.getResult().asString();
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

		pipe.addForward(new PipeForward("failure",null));
		pipe.addForward(new PipeForward(PipeForward.EXCEPTION_FORWARD_NAME,null));

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

		pipe.addForward(new PipeForward("failure",null));
		pipe.addForward(new PipeForward("warnings",null));
		pipe.addForward(new PipeForward(PipeForward.EXCEPTION_FORWARD_NAME,null));

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
	@Disabled("Cannot ignore XML validation failure")
	public void testValidWithWarningsXml2Json() throws Exception {
		testRecoverableError(DocumentFormat.JSON, true, "warnings", "No typeDefinition found for element [d]");
	}

	@ParameterizedTest
	@CsvSource({
			"false, none",
			"false, all",
			"false, partial1",
			"false, partial2",
			"false, partial3",
			"false, partial4",
			"false, partial5",
			"true, none",
			"true, all",
			"true, partial1",
			"true, partial2",
			"true, partial3",
			"true, partial4",
			"true, partial5",
	})
//	@ValueSource(strings = { "partial2"})
	public void issue7146AttributesOnMultipleLevels(boolean deepSearch, String input) throws Exception {
		// Arrange
		pipe.setSchema("/Validation/AttributesOnDifferentLevels/MultipleOptionalElements.xsd");
		pipe.setRoot("Root");
		pipe.setDeepSearch(deepSearch);
		pipe.setProduceNamespacelessXml(true);

		pipe.setThrowException(true);

		pipe.addForward(new PipeForward("success", null));
		configureAndStartPipe();

		URL json = TestFileUtils.getTestFileURL("/Validation/AttributesOnDifferentLevels/input-"+input+".json");
		UrlMessage message = new UrlMessage(json);
		PipeLineSession session = new PipeLineSession();

		// Act / Assert
		PipeRunResult result = assertDoesNotThrow(() -> pipe.validate(message, session, "Case"));

		System.err.println(result.getResult().asString());
		String expected = TestFileUtils.getTestFile("/Validation/AttributesOnDifferentLevels/output-" + input + ".xml");
		assertXmlEquals(expected, result.getResult().asString());
	}

	@Test
	public void testExpandParameters() throws Exception {
		// Arrange
		pipe.setName("testExpandParameters");
		pipe.setSchema("/Validation/Json2Xml/ParameterSubstitution/Main.xsd");
		pipe.setThrowException(true);
		pipe.setOutputFormat(DocumentFormat.JSON);
		pipe.setRoot("GetDocumentAttributes_Error");
		pipe.setDeepSearch(true);

		pipe.addParameter(new Parameter("type", "/errors/"));
		pipe.addParameter(ParameterBuilder.create().withName("title").withSessionKey("errorReason"));
		pipe.addParameter(ParameterBuilder.create().withName("status").withSessionKey("errorCode"));
		pipe.addParameter(ParameterBuilder.create().withName("detail").withSessionKey("errorDetailText"));
		pipe.addParameter(new Parameter("instance", "/archiving/documents"));

		pipe.configure();
		pipe.start();

		session.put("errorReason", "More than one document found");
		session.put("errorCode", "DATA_ERROR");
		session.put("errorDetailText", "The Devil's In The Details");

		// Act
		PipeRunResult result = pipe.doPipe(Message.asMessage("{}"), session);

		// Assert
		String expectedResult = TestFileUtils.getTestFile("/Validation/Json2Xml/ParameterSubstitution/expected_output.json");
		assertEqualsIgnoreWhitespaces(expectedResult, result.getResult().asString());
	}

	@ParameterizedTest(name = "With DeepSearch={0} Case={1}")
	@DisplayName("Same Element-Name At Different Levels")
	@CsvSource(value = {
			"true, ChildTypeFirstInXsdMissingInInput",
			"false, ChildTypeFirstInXsdMissingInInput",
			"true, ChildTypeFirstInXsdPresentInInput",
			"false, ChildTypeFirstInXsdPresentInInput",
			"true, ChildTypeLastInXsd",
			"false, ChildTypeLastInXsd",
			"true, ParentNotRootChildMissing",
			"false, ParentNotRootChildMissing",
			"true, WithIntermediateLevelChildMissing",
			"false, WithIntermediateLevelChildMissing",
	})
	public void testSameNameDifferentLevels(boolean deepSearch, String testCase) throws Exception {
		// Arrange
		pipe.setName("testSameNameDifferentLevelsDeepSearch=" + deepSearch);
		pipe.setSchema("/Validation/Json2Xml/DeepSearch/" + testCase + "/Test.xsd");
		pipe.setThrowException(true);
		pipe.setRoot("root");
		pipe.setDeepSearch(deepSearch);

		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/Validation/Json2Xml/DeepSearch/" + testCase + "/Test-Input.json");

		// Act
		PipeRunResult result = pipe.doPipe(Message.asMessage(input), session);

		// Assert
		String expectedResult = TestFileUtils.getTestFile("/Validation/Json2Xml/DeepSearch/" + testCase + "/ExpectedOutput.xml");
		assertXmlEquals(expectedResult, result.getResult().asString());
	}

	@Test
	public void testSameNameDifferentLevelsFailingCase() throws Exception {
		// Test for quickly testing a failing case from the above parameterized test

		// Arrange
		final String testCase;
		testCase = "WithIntermediateLevelChildMissing";
//		testCase = "ChildTypeFirstInXsdMissingInInput";
		pipe.setName("testSameNameDifferentLevelsDeepSearch=true");
		pipe.setSchema("/Validation/Json2Xml/DeepSearch/" + testCase + "/Test.xsd");
		pipe.setThrowException(true);
		pipe.setRoot("root");
		pipe.setDeepSearch(true);

		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/Validation/Json2Xml/DeepSearch/" + testCase + "/Test-Input.json");

		// Act
		PipeRunResult result = pipe.doPipe(Message.asMessage(input), session);

		// Assert
		String expectedResult = TestFileUtils.getTestFile("/Validation/Json2Xml/DeepSearch/" + testCase + "/ExpectedOutput.xml");
		assertXmlEquals(expectedResult, result.getResult().asString());
	}
}
