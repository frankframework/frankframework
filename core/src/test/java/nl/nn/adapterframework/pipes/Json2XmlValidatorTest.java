package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.hamcrest.core.StringContains;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class Json2XmlValidatorTest extends PipeTestBase<Json2XmlValidator> {

	@Override
	public Json2XmlValidator createPipe() {
		return new Json2XmlValidator();
	}

	@Test
	public void testNoNamespaceXml2Json() throws Exception {
		pipe.setName("Response_To_Json");
		pipe.setOutputFormat("json");
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
//		pipe.setRoot("GetPartiesOnAgreement_Response");
//		pipe.setTargetNamespace("http://nn.nl/XSD/CustomerAdministration/Party/1/GetPartiesOnAgreement/7");
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response.xml");
		TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-compact.json");

		PipeLineSessionBase session = new PipeLineSessionBase();
		try {
			doPipe(pipe, input,session);
			fail("expected to fail");
		} catch (PipeRunException e) {
			assertThat(e.getMessage(),StringContains.containsString("Cannot find the declaration of element 'BusinessPartner'"));
		}
	}

	@Test
	public void testNoNamespaceXml() throws Exception {
		XmlValidator pipe = new XmlValidator();
		
		pipe.setName("Response_Validator");
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
//		pipe.setRoot("GetPartiesOnAgreement_Response");
//		pipe.setTargetNamespace("http://nn.nl/XSD/CustomerAdministration/Party/1/GetPartiesOnAgreement/7");
		pipe.setThrowException(true);
		pipe.registerForward(new PipeForward("success",null));
		pipe.configure();
		pipe.start();
		
		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response.xml");
		TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-compact.json");
		
		PipeLineSessionBase session = new PipeLineSessionBase();
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
		pipe.setOutputFormat("json");
		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
//		pipe.setRoot("GetPartiesOnAgreement_Response");
//		pipe.setTargetNamespace("http://nn.nl/XSD/CustomerAdministration/Party/1/GetPartiesOnAgreement/7");
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();
		
		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-withNamespace.xml");
		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-compact.json");
		
		PipeLineSessionBase session = new PipeLineSessionBase();
		PipeRunResult prr = doPipe(pipe, input,session);
		
		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testWithParameters() throws Exception {
		pipe.setName("RestGet");
		pipe.setRoot("Root");
		pipe.setOutputFormat("xml");
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
		
		PipeLineSessionBase session = new PipeLineSessionBase();
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
		
		PipeLineSessionBase session = new PipeLineSessionBase();
		
		PipeRunResult prr = doPipe(pipe, input,session);
		
		String actualXml = Message.asString(prr.getResult());
		
		MatchUtils.assertXmlEquals("converted XML does not match", expected, actualXml, true);
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
		
		PipeLineSessionBase session = new PipeLineSessionBase();
		
		PipeRunResult prr = doPipe(pipe, input,session);
		
		String actualXml = Message.asString(prr.getResult());
		
		MatchUtils.assertXmlEquals("converted XML does not match", expected, actualXml, true);
	}


	public void testStoreRootElement(String outputFormat, String inputFile, boolean setRootElement) throws Exception {
		pipe.setName("testStoreRootElement");
		pipe.setSchema("/Align/Abc/abc.xsd");
		pipe.setRootElementSessionKey("rootElement");
		pipe.setOutputFormat(outputFormat);
		if (setRootElement) {
			pipe.setRoot("a");
		}

		pipe.registerForward(new PipeForward("failure",null));
		pipe.registerForward(new PipeForward("exception",null));
		
		pipe.configure();
		pipe.start();
		
		String input    = TestFileUtils.getTestFile("/Align/Abc/"+inputFile);
		
		PipeLineSessionBase session = new PipeLineSessionBase();
		
		PipeRunResult prr = doPipe(pipe, input,session);
		
		String expectedForward = "success";
		String actualForward = prr.getPipeForward().getName();
		assertEquals(expectedForward, actualForward);
		
		assertEquals("a", (String)session.get("rootElement"));
	}

	@Test
	public void testStoreRootElementXml2Json() throws Exception {
		testStoreRootElement("json","abc.xml",false);
	}
	@Test
	public void testStoreRootElementJson2XmlFull() throws Exception {
		testStoreRootElement("xml","abc-full.json",false);
	}
	@Test
	public void testStoreRootElementJson2XmlCompact() throws Exception {
		testStoreRootElement("xml","abc-compact.json",true);
	}
	@Test
	public void testStoreRootElementJson2JsonFull() throws Exception {
		testStoreRootElement("json","abc-full.json",false);
	}
	@Test
	public void testStoreRootElementJson2JsonCompact() throws Exception {
		testStoreRootElement("json","abc-compact.json",true);
	}
	@Test
	public void testStoreRootElementXml2Xml() throws Exception {
		testStoreRootElement("xml","abc.xml",false);
	}
}
