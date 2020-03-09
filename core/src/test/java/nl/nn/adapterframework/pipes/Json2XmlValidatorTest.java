package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.hamcrest.core.StringContains;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class Json2XmlValidatorTest {

	@Test
	public void testNoNamespaceXml2Json() throws Exception {
		Json2XmlValidator validator = new Json2XmlValidator();
		
		validator.setName("Response_To_Json");
		validator.setOutputFormat("json");
		validator.setSchema("/Validation/NoNamespace/bp.xsd");
//		validator.setRoot("GetPartiesOnAgreement_Response");
//		validator.setTargetNamespace("http://nn.nl/XSD/CustomerAdministration/Party/1/GetPartiesOnAgreement/7");
		validator.setThrowException(true);
		validator.registerForward(new PipeForward("success",null));
		validator.configure();
		validator.start();
		
		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response.xml");
		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-compact.json");
		
		PipeLineSessionBase session = new PipeLineSessionBase();
		try {
			PipeRunResult prr = validator.doPipe(input,session);
			fail("expected to fail");
		} catch (PipeRunException e) {
			assertThat(e.getMessage(),StringContains.containsString("Cannot find the declaration of element 'BusinessPartner'"));
		}
	}

	@Test
	public void testNoNamespaceXml() throws Exception {
		XmlValidator validator = new XmlValidator();
		
		validator.setName("Response_Validator");
		validator.setSchema("/Validation/NoNamespace/bp.xsd");
//		validator.setRoot("GetPartiesOnAgreement_Response");
//		validator.setTargetNamespace("http://nn.nl/XSD/CustomerAdministration/Party/1/GetPartiesOnAgreement/7");
		validator.setThrowException(true);
		validator.registerForward(new PipeForward("success",null));
		validator.configure();
		validator.start();
		
		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response.xml");
		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-compact.json");
		
		PipeLineSessionBase session = new PipeLineSessionBase();
		try {
			PipeRunResult prr = validator.doPipe(input,session);
			fail("expected to fail");
		} catch (PipeRunException e) {
			assertThat(e.getMessage(),StringContains.containsString("Cannot find the declaration of element 'BusinessPartner'"));
		}
	}


	@Test
	public void testWithNamespace() throws Exception {
		Json2XmlValidator validator = new Json2XmlValidator();
		
		validator.setName("Response_To_Json");
		validator.setOutputFormat("json");
		validator.setSchema("/Validation/NoNamespace/bp.xsd");
//		validator.setRoot("GetPartiesOnAgreement_Response");
//		validator.setTargetNamespace("http://nn.nl/XSD/CustomerAdministration/Party/1/GetPartiesOnAgreement/7");
		validator.setThrowException(true);
		validator.registerForward(new PipeForward("success",null));
		validator.configure();
		validator.start();
		
		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-withNamespace.xml");
		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-compact.json");
		
		PipeLineSessionBase session = new PipeLineSessionBase();
		PipeRunResult prr = validator.doPipe(input,session);
		
		assertEquals(expected, prr.getResult());
	}

	@Test
	public void testWithParameters() throws Exception {
		Json2XmlValidator validator = new Json2XmlValidator();
		
		validator.setName("RestGet");
		validator.setRoot("Root");
		validator.setOutputFormat("xml");
		validator.setSchema("/Validation/Parameters/simple.xsd");
		validator.setThrowException(true);
		validator.registerForward(new PipeForward("success",null));
		Parameter param = new Parameter();
		param.setName("a");
		param.setValue("param_a");
		validator.addParameter(param);
		param = new Parameter();
		param.setName("b");
		param.setSessionKey("b_key");
		validator.addParameter(param);
		param = new Parameter();
		param.setName("c");
		param.setSessionKey("c_key");
		validator.addParameter(param);
		param = new Parameter();
		param.setName("d");
		param.setSessionKey("d_key");
		validator.addParameter(param);
		validator.configure();
		validator.start();
		
		String input="";
		String expected = TestFileUtils.getTestFile("/Validation/Parameters/out.xml");
		
		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("b_key","b_value");
		// session variable "c_key is not present, so there should be no 'c' element in the result
		session.put("d_key","");
		
		PipeRunResult prr = validator.doPipe(input,session);
		
		assertEquals(expected, prr.getResult());
	}
}
