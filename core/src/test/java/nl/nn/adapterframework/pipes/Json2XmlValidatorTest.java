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
		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-compact.json");
		
		PipeLineSessionBase session = new PipeLineSessionBase();
		try {
			PipeRunResult prr = doPipe(pipe, input,session);
			fail("expected to fail");
		} catch (PipeRunException e) {
			assertThat(e.getMessage(),StringContains.containsString("Cannot find the declaration of element 'BusinessPartner'"));
		}
	}

//	@Test
//	public void testNoNamespaceXml() throws Exception {
//		XmlValidator pipe = new XmlValidator();
//		
//		pipe.setName("Response_Validator");
//		pipe.setSchema("/Validation/NoNamespace/bp.xsd");
////		pipe.setRoot("GetPartiesOnAgreement_Response");
////		pipe.setTargetNamespace("http://nn.nl/XSD/CustomerAdministration/Party/1/GetPartiesOnAgreement/7");
//		pipe.setThrowException(true);
//		pipe.registerForward(new PipeForward("success",null));
//		pipe.configure();
//		pipe.start();
//		
//		String input = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response.xml");
//		String expected = TestFileUtils.getTestFile("/Validation/NoNamespace/bp-response-compact.json");
//		
//		PipeLineSessionBase session = new PipeLineSessionBase();
//		try {
//			PipeRunResult prr = doPipe(pipe, input,session);
//			fail("expected to fail");
//		} catch (PipeRunException e) {
//			assertThat(e.getMessage(),StringContains.containsString("Cannot find the declaration of element 'BusinessPartner'"));
//		}
//	}


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
		
		assertEquals(expected, prr.getResult());
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
		
		assertEquals(expected, prr.getResult());
	}
}
