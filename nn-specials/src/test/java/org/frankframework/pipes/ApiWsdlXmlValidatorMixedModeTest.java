package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import org.frankframework.core.IDualModeValidator;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.extensions.api.ApiWsdlXmlValidator;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.validation.ValidatorTestBase;


/**
  * @author Gerrit van Brakel
 */
public class ApiWsdlXmlValidatorMixedModeTest {

	private static final String WSDL	 = ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/GetPolicyDetails/GetPolicyDetails.wsdl";
	private static final String REQUEST  = ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/GetPolicyDetails/GetPolicyDetails-Request.xml";
	private static final String RESPONSE = ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/GetPolicyDetails/GetPolicyDetails-Response.xml";

	private static final String REQUEST_SOAP_BODY  = "GetPolicyDetails_Request";
	private static final String RESPONSE_SOAP_BODY  = "GetPolicyDetails_Response";

	private final PipeLineSession session = new PipeLineSession();

	private final TestConfiguration configuration = new TestConfiguration();

	public WsdlXmlValidator getInputValidator() throws Exception {
		WsdlXmlValidator val = configuration.createBean(ApiWsdlXmlValidator.class);
		val.setWsdl(WSDL);
		val.setSoapBody(REQUEST_SOAP_BODY);
		val.setThrowException(true);
		val.setSchemaLocation("http://frankframework.org/XSD/Generic/MessageHeader/2 schema1 http://api.frankframework.org/GetPolicyDetails schema2");
		val.addForward(new PipeForward("success", null));
		val.configure();
		val.start();
		return val;
	}
	public WsdlXmlValidator getOutputValidator() throws Exception {
		WsdlXmlValidator val = configuration.createBean(ApiWsdlXmlValidator.class);
		val.setWsdl(WSDL);
		val.setSoapBody(RESPONSE_SOAP_BODY);
		val.setThrowException(true);
		val.setSchemaLocation("http://frankframework.org/XSD/Generic/MessageHeader/2 schema1 http://api.frankframework.org/GetPolicyDetails schema2");
		val.addForward(new PipeForward("success", null));
		val.configure();
		val.start();
		return val;
	}
	public WsdlXmlValidator getMixedValidator() throws Exception  {
		WsdlXmlValidator val = configuration.createBean(ApiWsdlXmlValidator.class);
		val.setWsdl(WSDL);
		val.setSoapBody(REQUEST_SOAP_BODY);
		val.setOutputSoapBody(RESPONSE_SOAP_BODY);
		val.setThrowException(true);
		val.setSchemaLocation("http://frankframework.org/XSD/Generic/MessageHeader/2 schema1 http://api.frankframework.org/GetPolicyDetails schema2");
		val.addForward(new PipeForward("success", null));
		val.configure();
		val.getResponseValidator().configure();
		val.start();
		val.getResponseValidator().start();
		return val;
	}

	protected Message getTestXml(String testxml) throws IOException {
		BufferedReader buf = new BufferedReader(new InputStreamReader(XmlValidator.class.getResourceAsStream(testxml)));
		StringBuilder string = new StringBuilder();
		String line = buf.readLine();
		while (line != null) {
			string.append(line);
			line = buf.readLine();
		}
		return new Message(new StringReader(string.toString()));
	}


	protected void validate(IPipe val, String msg, String failureReason) throws Exception {
		Message messageToValidate = getTestXml(msg);
		if (failureReason!=null) {
			assertThrows(Exception.class, () -> val.doPipe(messageToValidate, session), failureReason);
		} else {
			val.doPipe(messageToValidate, session);
		}
	}

	public void testPipeLineProcessorProcessOutputValidation(IPipe inputValidator, IPipe outputValidator, String msg, String failureReason) throws Exception {
		IPipe responseValidator;
		if (inputValidator!=null && outputValidator==null && inputValidator instanceof IDualModeValidator validator) {
			responseValidator=validator.getResponseValidator();
		} else {
			responseValidator=outputValidator;
		}
		if (responseValidator != null) {
			validate(responseValidator,msg,failureReason);
		}
	}

	@Test
	void testInputValidator() throws Exception {
		WsdlXmlValidator val = getInputValidator();
		validate(val,REQUEST,null);
		validate(val,RESPONSE,"Illegal element");
	}

	@Test
	void testOutputValidator() throws Exception {
		WsdlXmlValidator val = getOutputValidator();
		validate(val,RESPONSE,null);
		validate(val,REQUEST,"Illegal element");
	}

	@Test
	void testMixedValidator() throws Exception {
		WsdlXmlValidator val = getMixedValidator();
		IPipe outputValidator =val.getResponseValidator();
		validate(val,REQUEST,null);
		validate(val,RESPONSE,"Illegal element");
		validate(outputValidator,RESPONSE,null);
		validate(outputValidator,REQUEST,"Illegal element");
		validate(val,REQUEST,null);
		validate(val,RESPONSE,"Illegal element");
	}


	@Test
	void testPipelineProcessorInputValidator() throws Exception {
		WsdlXmlValidator inputValidator = getInputValidator();
		WsdlXmlValidator outputValidator = null;
		testPipeLineProcessorProcessOutputValidation(inputValidator,outputValidator,REQUEST,null);
		testPipeLineProcessorProcessOutputValidation(inputValidator,outputValidator,RESPONSE,null);
	}

	@Test
	void testPipelineProcessorOutputValidator() throws Exception {
		WsdlXmlValidator inputValidator = null;
		WsdlXmlValidator outputValidator = getOutputValidator();
		testPipeLineProcessorProcessOutputValidation(inputValidator,outputValidator,REQUEST,"Illegal element");
		testPipeLineProcessorProcessOutputValidation(inputValidator,outputValidator,RESPONSE,null);
	}

	@Test
	void testPipelineProcessorMixedValidator() throws Exception {
		WsdlXmlValidator inputValidator = getMixedValidator();
		WsdlXmlValidator outputValidator = null;
		testPipeLineProcessorProcessOutputValidation(inputValidator,outputValidator,REQUEST,"Illegal element");
		testPipeLineProcessorProcessOutputValidation(inputValidator,outputValidator,RESPONSE,null);
	}

	@Test
	void testPipelineProcessorMixedPlusOutputValidator() throws Exception {
		WsdlXmlValidator inputValidator = getMixedValidator();
		WsdlXmlValidator outputValidator = getInputValidator(); // use input to make the difference visible
		testPipeLineProcessorProcessOutputValidation(inputValidator,outputValidator,REQUEST,null);
		testPipeLineProcessorProcessOutputValidation(inputValidator,outputValidator,RESPONSE,"Illegal element");
	}
}
