package org.frankframework.soap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.validation.ValidatorTestBase;

class SoapValidatorTest extends PipeTestBase<SoapValidator> {

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
	void validate1Basic() throws Exception {
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
	void validate12Explicitversion() throws Exception {
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

	@Test
	void validate12Invalid() throws Exception {
		configureSoapValidator();
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapVersion(SoapVersion.SOAP12);
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_INVALID_SOAP;
		Message input = MessageTestUtils.getMessage(inputFile);

		assertThrows(PipeRunException.class, () -> doPipe(input));
	}

	@Test
	void validate12InvalidBody() throws Exception {
		configureSoapValidator();
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapVersion(SoapVersion.SOAP11);
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_INVALID_SOAP_BODY;
		Message input = MessageTestUtils.getMessage(inputFile);

		assertThrows(PipeRunException.class, () -> doPipe(input));
	}

	@Test
	void validate12UnknownNamespaceBody() throws Exception {
		configureSoapValidator();
		pipe.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
		pipe.setSoapVersion(SoapVersion.SOAP11);
		pipe.setSoapBody("Request");
		pipe.configure();
		pipe.start();
		String inputFile = INPUT_FILE_GPBDB_UNKNOWN_NAMESPACE_SOAP_BODY;
		Message input = MessageTestUtils.getMessage(inputFile);

		assertThrows(PipeRunException.class, () -> doPipe(input));
	}

	@Test
	void validateMultiSoapBody() throws Exception {
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
	void validateMultiSoapBodyOnMultipleLines1() throws Exception {
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
	void validateMultiSoapBodyOnMultipleLines2() throws Exception {
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
	void issue4183CharsetProblemInXSD() throws Exception {
		configureSoapValidator(false);
		pipe.setSchemaLocation("urn:namespacer Validation/CharsetProblem/non-utf8.xsd");

		assertDoesNotThrow(pipe::configure);
		assertDoesNotThrow(pipe::start);
	}

	private void configureSoapValidator() throws ConfigurationException {
		configureSoapValidator(false);
	}

	private void configureSoapValidator(boolean addNamespaceToSchema) throws ConfigurationException {
		configureSoapValidator(addNamespaceToSchema, null);
	}

	private void configureSoapValidator(boolean addNamespaceToSchema, SoapVersion soapVersion) {
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
