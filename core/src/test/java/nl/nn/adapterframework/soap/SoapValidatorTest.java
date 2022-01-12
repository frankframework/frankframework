package nl.nn.adapterframework.soap;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.validation.ValidatorTestBase;

/**
 * @author Michiel Meeuwissen
 */
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
	@Ignore("Don't know what it should test any more")
	public void basic() {
		pipe.setSchemaLocation("http://www.ing.com/pim test.xsd");
		pipe.setSoapBody("{http://www.ing.com/pim}a");
		System.out.println(pipe.getSoapBody());
		// WTF it was something with QName, it is a string. I have no idea whether and how that must be tested (I don't know what the string is meant to represent any more)
		// assertEquals(new QName("http://www.ing.com/pim", "a"), validator.getSoapBodyTags().iterator().next());
	}

	@Test
	@Ignore("Don't know what it should test any more")
	public void defaultNamespace() {
		pipe.setSchemaLocation("http://www.ing.com/pim test.xsd");
		pipe.setSoapBody("a");
//		WTF assertEquals(new QName("http://www.ing.com/pim", "a"), validator.getSoapBodyTags().iterator().next());
	}

	@Test
	public void validate1Basic() throws Exception {
		configureSoapValidator(true);
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_VALID_SOAP;
		Message input = TestFileUtils.getTestFileMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void validate12Explicitversion() throws Exception {
		configureSoapValidator(true, "1.2");
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_VALID_SOAP_1_2;
		Message input = TestFileUtils.getTestFileMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test(expected = PipeRunException.class)
	public void validate12Invalid() throws Exception {
		configureSoapValidator();
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapVersion("1.2");
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_INVALID_SOAP;
		Message input = TestFileUtils.getTestFileMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test(expected = PipeRunException.class)
	public void validate12Invalid_body() throws Exception {
		configureSoapValidator();
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapVersion("1.1");
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_INVALID_SOAP_BODY;
		Message input = TestFileUtils.getTestFileMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test(expected = PipeRunException.class)
	public void validate12Unknown_namespace_body() throws Exception {
		configureSoapValidator();
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapVersion("1.1");
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_UNKNOWN_NAMESPACE_SOAP_BODY;
		Message input = TestFileUtils.getTestFileMessage(inputFile);
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
		Message input = TestFileUtils.getTestFileMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void validateMultiSoapBodyOnMultipleLines1() throws Exception {
		configureSoapValidator(true);
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapBody("\nRequest,\nResponse\n");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_VALID_SOAP;
		Message input = TestFileUtils.getTestFileMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void validateMultiSoapBodyOnMultipleLines2() throws Exception {
		configureSoapValidator(true);
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapBody("\nResponse,\nRequest\n");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_VALID_SOAP;
		Message input = TestFileUtils.getTestFileMessage(inputFile);
		String expected = TestFileUtils.getTestFile(inputFile);
		PipeRunResult prr = doPipe(input);
		assertEquals(expected, prr.getResult().asString());
	}


	private void configureSoapValidator() throws ConfigurationException {
		configureSoapValidator(false);
	}

	private void configureSoapValidator(boolean addNamespaceToSchema) throws ConfigurationException {
		configureSoapValidator(addNamespaceToSchema, null);
	}

	private void configureSoapValidator(boolean addNamespaceToSchema, String soapVersion) throws ConfigurationException {
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
