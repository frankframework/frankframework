package org.frankframework.pipes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ApplicationWarnings;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.validation.AbstractXmlValidator;
import org.frankframework.validation.AbstractXmlValidator.ValidationResult;
import org.frankframework.validation.JavaxXmlValidator;
import org.frankframework.validation.XercesXmlValidator;
import org.frankframework.validation.XmlValidatorTestBase;

/**
 * @author Michiel Meeuwissen
 */
public class XmlValidatorPipelineTest extends XmlValidatorTestBase {

	private TestConfiguration configuration;

	@BeforeEach
	public void setup() {
		configuration = new TestConfiguration();
		ApplicationWarnings.removeInstance();
	}

	public void initXmlValidatorTest(Class<? extends AbstractXmlValidator> implementation) {
		this.implementation = implementation;
	}

	protected static Stream<Arguments> data() {
		return Stream.of(
				Arguments.of(XercesXmlValidator.class),
				Arguments.of(JavaxXmlValidator.class)
		);
	}

	protected static PipeForward createSuccessForward() {
		PipeForward forward = new PipeForward();
		forward.setName("success");
		return forward;
	}

	protected static PipeForward createFailureForward() {
		PipeForward forward = new PipeForward();
		forward.setName("failure");
		return forward;
	}

	private static XmlValidator buildXmlValidator(TestConfiguration configuration, String schemaLocation, String root) throws ConfigurationException {
		Adapter adapter = configuration.createBean(Adapter.class);

		XmlValidator validator = configuration.createBean(XmlValidator.class);
		validator.setName("validator");
		validator.addForward(createSuccessForward());
		validator.setRoot(root);
		validator.setSchemaLocation(schemaLocation);
		validator.setThrowException(true);

		PipeLine pl = new PipeLine();
		pl.setInputValidator(validator);
		pl.addPipe(validator);
		pl.setFirstPipe("validator");
		adapter.setPipeLine(pl);
		return validator;
	}

	private static void assertNoWarnings(TestConfiguration configuration) {
		assertWarnings(configuration, 0);
	}

	private static void assertWarnings(TestConfiguration configuration, int expectedNrOfWarnings) {
		// NB: We should get ConfigurationWarnings not ApplicationWarnings, but we check both just to be sure because in the past logic has changed
		// in other places in the code, which silently made warnings go to one instead of the other, making checks on just one
		// of them useless (as the warnings would never show up there anymore).
		// By checking both we prevent this in the future.
		assertEquals(expectedNrOfWarnings, configuration.getConfigurationWarnings().size(), expectedNrOfWarnings + " ConfigurationWarnings expected, got " + configuration.getConfigurationWarnings().getWarnings());
		assertEquals(0, ApplicationWarnings.getSize(), "No ApplicationWarnings expected, got " + ApplicationWarnings.getWarningsList());
	}

	public static XmlValidator getUnconfiguredValidator(String schemaLocation, Class<AbstractXmlValidator> implementation) throws ConfigurationException {
		return getUnconfiguredValidator(schemaLocation, false, implementation);
	}

	public static XmlValidator getUnconfiguredValidator(String schemaLocation, boolean addNamespaceToSchema, Class<? extends AbstractXmlValidator> implementation) throws ConfigurationException {
		XmlValidator validator = new XmlValidator();
		try {
			validator.setImplementation(implementation);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		validator.setSchemaLocation(schemaLocation);
		if (addNamespaceToSchema) {
			validator.setAddNamespaceToSchema(addNamespaceToSchema);
		}
		validator.addForward(createSuccessForward());
		validator.setThrowException(true);
		validator.setFullSchemaChecking(true);
		return validator;
	}

	protected ValidationResult runAndEvaluate(XmlValidator validator, String inputFile, String[] expectedFailureReasons) throws IOException  {
		log.debug("inputFile [{}]", inputFile);
		String testXml = inputFile != null ? getTestXml(inputFile + ".xml") : null;
		PipeLineSession session = new PipeLineSession();
		try {
			PipeRunResult result=validator.doPipe(new Message(testXml), session);
			PipeForward forward=result.getPipeForward();
			ValidationResult validationResult = "success".equals(forward.getName()) ? ValidationResult.VALID : ValidationResult.INVALID;
			evaluateResult(validationResult, session, null, expectedFailureReasons);
		} catch (Exception e) {
			evaluateResult(ValidationResult.INVALID, session, e, expectedFailureReasons);
			return ValidationResult.INVALID;
		}
		return null;
	}

	@Override
	public ValidationResult validate(String rootelement, String rootNamespace, String schemaLocation, boolean addNamespaceToSchema,
			boolean ignoreUnknownNamespaces, String inputfile, String[] expectedFailureReasons) throws IOException, ConfigurationException {
		XmlValidator validator = getUnconfiguredValidator(schemaLocation, addNamespaceToSchema, implementation);
		if (rootelement!=null) validator.setRoot(rootelement);
		validator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
		validator.configure();
		validator.start();
		return runAndEvaluate(validator, inputfile, expectedFailureReasons);
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}")
	void testSoapNamespaceFeature(Class<AbstractXmlValidator> implementation) throws ConfigurationException, IOException {
		initXmlValidatorTest(implementation);
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, null, NO_NAMESPACE_SOAP_MSGROOT);
		validator.setFullSchemaChecking(true);
		validator.addForward(createFailureForward());
		try {
			validator.setImplementation(implementation);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		validator.setFullSchemaChecking(true);
		validator.setRoot(NO_NAMESPACE_SOAP_MSGROOT);
		validator.setSoapNamespace("http://www.w3.org/2003/05/soap-envelope");
		validator.setSchema(NO_NAMESPACE_SCHEMA);

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertNoWarnings(configuration);

		// Act / Assert 2
		assertNull(runAndEvaluate(validator, NO_NAMESPACE_SOAP_FILE, null));
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}")
	void testStoreRootElement(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, SCHEMA_LOCATION_BASIC_A_OK, "A");
		validator.setFullSchemaChecking(true);
		validator.setRootElementSessionKey("rootElement");

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertNoWarnings(configuration);

		// Arrange 2
		String testXml = getTestXml(INPUT_FILE_BASIC_A_OK + ".xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.doPipe(new Message(testXml), session);
		PipeForward forward = result.getPipeForward();

		// Assert 2
		assertEquals("A", session.get("rootElement"));
		assertEquals("success", forward.getName());
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}")
	void testWrongRootElement(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, SCHEMA_LOCATION_BASIC_A_OK,  "anotherElement");
		validator.setFullSchemaChecking(true);
		validator.setReasonSessionKey("reason");
		validator.addForward(createFailureForward());
		validator.setThrowException(false);

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertWarnings(configuration, 1);

		// Arrange 2
		String testXml = getTestXml(INPUT_FILE_BASIC_A_OK + ".xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.doPipe(new Message(testXml), session);
		PipeForward forward = result.getPipeForward();

		// Assert 2
		assertEquals("failure", forward.getName());
		assertThat((String)session.get("reason"), containsString("Illegal element 'A'. Element(s) 'anotherElement' expected."));
	}


	@MethodSource("data")
	@ParameterizedTest(name = "{0}")
	void testMultipleRootElement(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		String root = "A";
		// if multiple root elements are specified, in a comma separated list, the validation succeeds if one of these root elements is found
		XmlValidator validator = buildXmlValidator(configuration, SCHEMA_LOCATION_BASIC_A_OK, root + ",anotherElement");
		validator.setFullSchemaChecking(true);

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertNoWarnings(configuration);

		// Arrange 2
		String testXml = getTestXml(INPUT_FILE_BASIC_A_OK + ".xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.doPipe(new Message(testXml), session);
		PipeForward forward = result.getPipeForward();

		// Assert 2
		assertEquals("success", forward.getName());
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}")
	void testRuntimeRootElement(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		// if multiple root elements are specified, in a comma separated list, the validation succeeds if one of these root elements is found
		XmlValidator validator = buildXmlValidator(configuration, SCHEMA_LOCATION_BASIC_A_OK, "oneElement,anotherElement");
		validator.setFullSchemaChecking(true);

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertWarnings(configuration, 2);

		// Arrange 2
		String testXml = getTestXml(INPUT_FILE_BASIC_A_OK + ".xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.validate(new Message(testXml), session, "A");
		PipeForward forward = result.getPipeForward();

		// Assert 2
		assertEquals("success", forward.getName());
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}")
	void testWrongRuntimeRootElement(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		String root = "A";
		XmlValidator validator = buildXmlValidator(configuration, SCHEMA_LOCATION_BASIC_A_OK, root);
		validator.setReasonSessionKey("reason");
		validator.addForward(createFailureForward());
		validator.setThrowException(false);

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertNoWarnings(configuration);

		// Arrange 2
		String testXml = getTestXml(INPUT_FILE_BASIC_A_OK + ".xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.validate(new Message(testXml), session, "anotherElement");
		PipeForward forward = result.getPipeForward();

		// Assert 2
		assertEquals("failure", forward.getName());
		assertThat((String)session.get("reason"), containsString("Illegal element 'A'. Element(s) 'anotherElement' expected."));
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}")
	void testWithCircularReferenceInXsd1(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, "http://xmlns/a /Validation/Circular/AB/xsd/A.xsd", "A");

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertNoWarnings(configuration);

		// Arrange 2
		String testXml = getTestXml("/Validation/Circular/AB/A.xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.validate(new Message(testXml), session, "A");
		PipeForward forward = result.getPipeForward();

		// Assert 2
		assertEquals("success", forward.getName());
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}")
	void testWithCircularReferenceInXsd2(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, "http://www.egem.nl/StUF/sector/zkn/0310 /Validation/Circular/zds/xsd/zkn0310_msg_zs-dms_resolved2017.xsd", "Point");

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertNoWarnings(configuration);

		// Arrange 2
		String testXml = getTestXml("/Validation/Circular/zds/ontvangAsynchroon_CreeerZaak_input_example.xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.validate(new Message(testXml), session, "zakLk01");
		PipeForward forward = result.getPipeForward();

		// Arrange 2
		assertEquals("success", forward.getName());
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}")
	void testMultipleImport(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, "urn:frank/root /Validation/MultipleImport/xsd/root.xsd", "root");

		// Act
		validator.configure();
		validator.start();

		// There should not be the message: "sch-props-correct.2: A schema cannot contain two global components with the same name; this schema contains two occurrences of 'urn:frank/leaf01,leaf01'."
		// Assert
		assertNoWarnings(configuration);

		// Arrange 2
		String testXml = getTestXml("/Validation/MultipleImport/root-ok.xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.validate(new Message(testXml), session, "root");
		PipeForward forward = result.getPipeForward();

		// Assert 2
		assertEquals("success", forward.getName());
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}") //copied from iaf-test /XmlValidator/scenario07a
	void testImportIncludeOK(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, "http://nn.nl/root /Validation/ImportInclude/xsd/root.xsd", "root");

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertNoWarnings(configuration);

		// Arrange 2
		String testXml = getTestXml("/Validation/ImportInclude/root-ok.xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.validate(new Message(testXml), session, "root");
		PipeForward forward = result.getPipeForward();

		// Assert 2
		assertEquals("success", forward.getName());
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}")
	void testImportNestedIncludeOK(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, "http://nn.nl/root /Validation/ImportNestedInclude/xsd/root.xsd", "root");

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertNoWarnings(configuration);

		// Arrange 2
		String testXml = getTestXml("/Validation/ImportNestedInclude/root-ok.xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.validate(new Message(testXml), session, "root");
		PipeForward forward = result.getPipeForward();

		assertEquals("success", forward.getName());
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}") //copied from iaf-test /XmlValidator/scenario07b
	void testImportIncludeError(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, "http://nn.nl/root /Validation/ImportInclude/xsd/root.xsd", "root");
		// Override throwing exception
		validator.addForward(createFailureForward());
		validator.setThrowException(false);

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertNoWarnings(configuration);

		// Arrange 2
		String testXml = getTestXml("/Validation/ImportInclude/root-err.xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.validate(new Message(testXml), session, "root");
		PipeForward forward = result.getPipeForward();

		// Assert 2
		assertEquals("failure", forward.getName());
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}") //copied from iaf-test /XmlValidator/scenario07b
	void testIncludeErrorDupNSPrefixes(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, "http://www.frankframework.org/test XSDTest/MultipleIncludesClashingPrefixes/root1.xsd", "cc");
		// Override throwing exception
		validator.addForward(createFailureForward());
		validator.setThrowException(false);

		// Act
		ConfigurationException exception = assertThrows(ConfigurationException.class, validator::configure);

		// Assert
		assertThat(exception.getMessage(), Matchers.containsString("Prefix [dup] defined in multiple files with different namespaces"));
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}") //copied from iaf-test /XmlValidator/scenario05a, then simplified
	void testIncludeOK(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, "http://www.frankframework.org/tom /Validation/Include/xsd/main.xsd", "GetParties");

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertNoWarnings(configuration);

		// Arrange 2
		String testXml = getTestXml("/Validation/Include/in-ok.xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.validate(new Message(testXml), session, "GetParties");
		PipeForward forward = result.getPipeForward();

		// Assert 2
		assertEquals("success", forward.getName());
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}") //copied from iaf-test /XmlValidator/scenario05b, then simplified
	void testIncludeError(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, "http://www.frankframework.org/tom /Validation/Include/xsd/main.xsd", "GetParties");
		validator.setThrowException(false);
		validator.addForward(createFailureForward());

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertNoWarnings(configuration);

		// Arrange 2
		String testXml = getTestXml("/Validation/Include/in-err.xml");
		PipeLineSession session = new PipeLineSession();

		// Act 2
		PipeRunResult result = validator.validate(new Message(testXml), session, "GetParties");
		PipeForward forward = result.getPipeForward();

		// Assert 2
		assertEquals("failure", forward.getName());
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}")
	void testThrowExceptionAndFailureForward(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		// Arrange
		// throwException=true and a failure forward both exist.
		XmlValidator validator = buildXmlValidator(configuration, "http://nn.nl/root /Validation/ImportNestedInclude/xsd/root.xsd", "root");

		validator.setThrowException(true);
		validator.addForward(createFailureForward());

		// Act
		validator.configure();
		validator.start();

		// Assert
		assertWarnings(configuration, 1);
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{0}")
	void testElementFormDefaultUnqualified(Class<AbstractXmlValidator> implementation) throws Exception {
		initXmlValidatorTest(implementation);
		XmlValidator validator = getUnconfiguredValidator(ELEMENT_FORM_DEFAULT_UNQUALIFIED_NAMESPACE +" "+ ELEMENT_FORM_DEFAULT_UNQUALIFIED_SCHEMA, false, implementation);
		validator.setIgnoreUnknownNamespaces(true);
		validator.configure();
		validator.start();

		String testXml = getTestXml(ELEMENT_FORM_DEFAULT_UNQUALIFIED_INPUT);
		PipeLineSession session = new PipeLineSession();
		PipeRunResult result = validator.validate(new Message(testXml), session, ELEMENT_FORM_DEFAULT_UNQUALIFIED_MSGROOT);
		PipeForward forward = result.getPipeForward();

		assertEquals("success", forward.getName());
	}
}
