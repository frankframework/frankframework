package nl.nn.adapterframework.pipes;

import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.core.StringContains;
import org.junit.Test;

import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class Json2XmlValidatorTest extends PipeTestBase<Json2XmlValidator> {

	@Override
	public Json2XmlValidator createPipe() {
		return new Json2XmlValidator();
	}

	@Test
	public void testNoNamespaceXml2Json() throws Exception {
		pipe.setName("Response_To_Json");
		pipe.setOutputFormat(DocumentFormat.JSON);
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
//		pipe.setRoot("GetPartiesOnAgreement_Response");
//		pipe.setTargetNamespace("http://nn.nl/XSD/CustomerAdministration/Party/1/GetPartiesOnAgreement/7");
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response.xml");

		try {
			doPipe(pipe, input,session);
			fail("expected to fail");
		} catch (PipeRunException e) {
			assertThat(e.getMessage(),StringContains.containsString("Cannot find the declaration of element 'BusinessPartner'"));
		}
	}

	@Test
	public void testNoNamespaceXml() throws Exception {
		pipe.setName("Response_Validator");
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
//		pipe.setRoot("GetPartiesOnAgreement_Response");
//		pipe.setTargetNamespace("http://nn.nl/XSD/CustomerAdministration/Party/1/GetPartiesOnAgreement/7");
		pipe.setThrowException(true);
		pipe.registerForward(new PipeForward("success",null));
		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response.xml");

		try {
			pipe.doPipe(Message.asMessage(input),session); // cannot use this.doPipe(), because pipe is a XmlValidator, not a Json2XmlValidator
			fail("expected to fail");
		} catch (PipeRunException e) {
			assertThat(e.getMessage(),StringContains.containsString("Cannot find the declaration of element 'BusinessPartner'"));
		}
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

		doPipe(pipe, input,session); // first run the request validation ...
		
		IValidator validator = pipe.getResponseValidator();
		PipeRunResult prr_response = validator.doPipe(new Message(input), session);

		return prr_response.getResult().asString();
		
	}
	
	@Test
	public void testAcceptHeaderTextJson() throws Exception {
		String actual = setupAcceptHeaderTest("text/Json");
		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-compact.json");
		assertEquals(expected, actual);
	}

	@Test
	public void testAcceptHeaderTextXML() throws Exception {
		String actual = setupAcceptHeaderTest("text/Xml");
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
		Parameter param = new Parameter();
		param.setName("a");
		param.setValue("param_a");
		pipe.addParameter(param);
		param = new Parameter();
		param.setName("b");
		param.setSessionKey("b_key");
		pipe.addParameter(param);
		param = new Parameter();
		param.setName("c");
		param.setSessionKey("c_key");
		pipe.addParameter(param);
		param = new Parameter();
		param.setName("d");
		param.setSessionKey("d_key");
		pipe.addParameter(param);
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

		Parameter param = new Parameter();
		param.setName("Id");
		param.setValue("3242343");
		pipe.addParameter(param);
		
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
		
		Parameter param = new Parameter();
		param.setName("Id");
		param.setValue("24");
		pipe.addParameter(param);
		
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
		
		Parameter param1 = new Parameter();
		param1.setName("singleInt");
		param1.setValue("33");
		pipe.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("singleString");
		param2.setValue("tja");
		pipe.addParameter(param2);

		Parameter param3 = new Parameter();
		param3.setName("stringArray2");
		List<String> stringValues = Arrays.asList("aa","bb");
		session.put("StringValueList", stringValues);
		param3.setSessionKey("StringValueList");
		pipe.addParameter(param3);
		
		Parameter param4 = new Parameter();
		param4.setName("intArray");
		List<String> intValues = Arrays.asList("11","22");
		session.put("IntValueList", intValues);
		param4.setSessionKey("IntValueList");
		pipe.addParameter(param4);
		
		Parameter param5 = new Parameter();
		param5.setName("stringElem3");
		List<String> stringElem3elements = Arrays.asList("aa","bb");
		session.put("stringElem3elements", stringElem3elements);
		param5.setSessionKey("stringElem3elements");
		pipe.addParameter(param5);
		
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
		
		Parameter param1 = new Parameter();
		param1.setName("schemaName");
		param1.setValue("NNPensioenen");
		pipe.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("requestUserId");
		param2.setValue("postman2");
		pipe.addParameter(param2);

		Parameter param3 = new Parameter();
		param3.setName("SearchAttributes/agreementNumber");
		param3.setSessionKey("agreementNumbers");
		pipe.addParameter(param3);
		
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
		
		Parameter param1 = new Parameter();
		param1.setName("schemaName");
		param1.setValue("NNPensioenen");
		pipe.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("requestUserId");
		param2.setValue("postman2");
		pipe.addParameter(param2);

		Parameter param3 = new Parameter();
		param3.setName("agreementNumber");
		param3.setSessionKey("agreementNumbers");
		pipe.addParameter(param3);
		
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
		
		Parameter param = new Parameter();
		param.setName("intArray");
		List<String> intValues = Arrays.asList("44");
		session.put("IntValueList", intValues);
		param.setSessionKey("IntValueList");
		pipe.addParameter(param);
		
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
		
		Parameter param = new Parameter();
		param.setName("intArray");
		param.setValue("44");
		pipe.addParameter(param);
		
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
}
