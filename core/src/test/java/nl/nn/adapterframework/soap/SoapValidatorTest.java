package nl.nn.adapterframework.soap;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.MessageTestUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.validation.ValidatorTestBase;

public class SoapValidatorTest extends PipeTestBase<SoapValidator> {

	public String SCHEMALOCATION_SET_GPBDB = ValidatorTestBase.SCHEMA_LOCATION_GPBDB_MESSAGE+" "+
			ValidatorTestBase.SCHEMA_LOCATION_GPBDB_GPBDB+" "+
			ValidatorTestBase.SCHEMA_LOCATION_GPBDB_RESPONSE+" "+
			ValidatorTestBase.SCHEMA_LOCATION_GPBDB_REQUEST;
	public String INPUT_FILE_GPBDB_VALID_SOAP					=ValidatorTestBase.BASE_DIR_VALIDATION+"/Tibco/Soap/valid_soap.xml";
	public String INPUT_FILE_GPBDB_VALID_SOAP_1_2				=ValidatorTestBase.BASE_DIR_VALIDATION+"/Tibco/Soap/valid_soap_1.2.xml";
	public String INPUT_FILE_GPBDB_INVALID_SOAP					=ValidatorTestBase.BASE_DIR_VALIDATION+"/Tibco/Soap/invalid_soap.xml";
	public String INPUT_FILE_GPBDB_INVALID_SOAP_BODY			=ValidatorTestBase.BASE_DIR_VALIDATION+"/Tibco/Soap/invalid_soap_body.xml";
	public String INPUT_FILE_GPBDB_UNKNOWN_NAMESPACE_SOAP_BODY	=ValidatorTestBase.BASE_DIR_VALIDATION+"/Tibco/Soap/unknown_namespace_soap_body.xml";

	@Override
	public SoapValidator createPipe() {
		return new SoapValidator();
	}

	@Test
	public void validate1Basic() throws Exception {
		configureSoapValidator(true);
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_VALID_SOAP;
		Message input = MessageTestUtils.getMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		MatchUtils.assertXmlEquals(expected, prr.getResult().asString());
	}

	@Test
	public void validate12Explicitversion() throws Exception {
		configureSoapValidator(true, SoapVersion.SOAP12);
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_VALID_SOAP_1_2;
		Message input = MessageTestUtils.getMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		MatchUtils.assertXmlEquals(expected, prr.getResult().asString());
	}

	@Test(expected = PipeRunException.class)
	public void validate12Invalid() throws Exception {
		configureSoapValidator();
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapVersion(SoapVersion.SOAP12);
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_INVALID_SOAP;
		Message input = MessageTestUtils.getMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test(expected = PipeRunException.class)
	public void validate12Invalid_body() throws Exception {
		configureSoapValidator();
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapVersion(SoapVersion.SOAP11);
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_INVALID_SOAP_BODY;
		Message input = MessageTestUtils.getMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test(expected = PipeRunException.class)
	public void validate12Unknown_namespace_body() throws Exception {
		configureSoapValidator();
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapVersion(SoapVersion.SOAP11);
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_UNKNOWN_NAMESPACE_SOAP_BODY;
		Message input = MessageTestUtils.getMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void validateMultiSoapBody() throws Exception {
		configureSoapValidator(true);
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapBody("Request,Response");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_VALID_SOAP;
		Message input = MessageTestUtils.getMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		MatchUtils.assertXmlEquals(expected, prr.getResult().asString());
	}

	@Test
	public void validateMultiSoapBodyOnMultipleLines1() throws Exception {
		configureSoapValidator(true);
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapBody("\nRequest,\nResponse\n");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_VALID_SOAP;
		Message input = MessageTestUtils.getMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		MatchUtils.assertXmlEquals(expected, prr.getResult().asString());
	}

	@Test
	public void validateMultiSoapBodyOnMultipleLines2() throws Exception {
		configureSoapValidator(true);
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapBody("\nResponse,\nRequest\n");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_VALID_SOAP;
		Message input = MessageTestUtils.getMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		MatchUtils.assertXmlEquals(expected, prr.getResult().asString());
	}

	@Test
	public void issue4183CharsetProblemInXSD() throws Exception {
		configureSoapValidator(false);
		pipe.setSchemaLocation("urn:namespacer Validation/CharsetProblem/non-utf8.xsd");
		pipe.configure();
		pipe.start();
	}

	private void configureSoapValidator() throws ConfigurationException {
		configureSoapValidator(false);
	}

	private void configureSoapValidator(boolean addNamespaceToSchema) throws ConfigurationException {
		configureSoapValidator(addNamespaceToSchema, null);
	}

	private void configureSoapValidator(boolean addNamespaceToSchema, SoapVersion soapVersion) throws ConfigurationException {
		if (addNamespaceToSchema) {
			pipe.setAddNamespaceToSchema(addNamespaceToSchema);
		}
		if (soapVersion != null) {
			pipe.setSoapVersion(soapVersion);
		}
		pipe.setSoapHeader("MessageHeader");
		pipe.setThrowException(true);
		pipe.setFullSchemaChecking(true);
	}
}
