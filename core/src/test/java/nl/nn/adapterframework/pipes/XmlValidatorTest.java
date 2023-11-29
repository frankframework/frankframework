package nl.nn.adapterframework.pipes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import nl.nn.adapterframework.configuration.ApplicationWarnings;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.validation.AbstractXmlValidator;
import nl.nn.adapterframework.validation.AbstractXmlValidator.ValidationResult;
import nl.nn.adapterframework.validation.JavaxXmlValidator;
import nl.nn.adapterframework.validation.XercesXmlValidator;
import nl.nn.adapterframework.validation.XmlValidatorTestBase;

/**
 * @author Michiel Meeuwissen
 */
@RunWith(value = Parameterized.class)
public class XmlValidatorTest extends XmlValidatorTestBase {

	private Class<AbstractXmlValidator> implementation;

	private TestConfiguration configuration;

	@Before
	public void setup() {
		configuration = new TestConfiguration();
		ApplicationWarnings.removeInstance();
	}

	public XmlValidatorTest(Class<AbstractXmlValidator> implementation) {
		this.implementation = implementation;
	}

	@Parameterized.Parameters(name= "{0}")
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][]{
			{XercesXmlValidator.class}
			,{JavaxXmlValidator.class}
		};
		return Arrays.asList(data);
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
		validator.registerForward(createSuccessForward());
		validator.registerForward(createFailureForward());
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
		assertEquals(expectedNrOfWarnings + " ConfigurationWarnings expected, got " + configuration.getConfigurationWarnings().getWarnings(), expectedNrOfWarnings, configuration.getConfigurationWarnings().size());
		assertEquals("No ApplicationWarnings expected, got " + ApplicationWarnings.getWarningsList(), 0, ApplicationWarnings.getSize());
	}

	public static XmlValidator getValidator(String schemaLocation, boolean addNamespaceToSchema, Class<AbstractXmlValidator> implementation) throws ConfigurationException {
		XmlValidator validator=getUnconfiguredValidator(schemaLocation, addNamespaceToSchema, implementation);
		validator.configure();
		return validator;
	}

	public static XmlValidator getUnconfiguredValidator(String schemaLocation, Class<AbstractXmlValidator> implementation) throws ConfigurationException {
		return getUnconfiguredValidator(schemaLocation, false, implementation);
	}

	public static XmlValidator getUnconfiguredValidator(String schemaLocation, boolean addNamespaceToSchema, Class<AbstractXmlValidator> implementation) throws ConfigurationException {
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
		validator.registerForward(createSuccessForward());
		validator.setThrowException(true);
		validator.setFullSchemaChecking(true);
		return validator;
	}



	protected ValidationResult runAndEvaluate(XmlValidator validator, String inputfile, String[] expectedFailureReasons) throws IOException  {
		System.out.println("inputfile ["+inputfile+"]");
		String testXml=inputfile!=null?getTestXml(inputfile+".xml"):null;
		PipeLineSession session=new PipeLineSession();
		try {
			PipeRunResult result=validator.doPipe(new Message(testXml), session);
			PipeForward forward=result.getPipeForward();
			ValidationResult validationResult = forward.getName().equals("success") ? ValidationResult.VALID : ValidationResult.INVALID;
			evaluateResult(validationResult, session, null, expectedFailureReasons);
		} catch (Exception e) {
			evaluateResult(ValidationResult.INVALID, session, e, expectedFailureReasons);
			return ValidationResult.INVALID;
		}
		return null;
	}

	@Override
	public ValidationResult validate(String rootelement, String rootNamespace, String schemaLocation, boolean addNamespaceToSchema,
			boolean ignoreUnknownNamespaces, String inputfile, String[] expectedFailureReasons) throws IOException, ConfigurationException, PipeStartException {
		XmlValidator validator = getUnconfiguredValidator(schemaLocation, addNamespaceToSchema, implementation);
		if (rootelement!=null) validator.setRoot(rootelement);
		validator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
		validator.configure();
		validator.start();
		return runAndEvaluate(validator, inputfile, expectedFailureReasons);
	}

	@Test
	public void testSoapNamespaceFeature() throws ConfigurationException, IOException, PipeStartException {
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, null, NO_NAMESPACE_SOAP_MSGROOT);
		validator.setFullSchemaChecking(true);
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

	@Test
	public void testStoreRootElement() throws Exception {
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

	@Test
	public void testWrongRootElement() throws Exception {
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, SCHEMA_LOCATION_BASIC_A_OK,  "anotherElement");
		validator.setFullSchemaChecking(true);
		validator.setReasonSessionKey("reason");
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


	@Test
	public void testMultipleRootElement() throws Exception {
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

	@Test
	public void testRuntimeRootElement() throws Exception {
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

	@Test
	public void testWrongRuntimeRootElement() throws Exception {
		// Arrange
		String root = "A";
		XmlValidator validator = buildXmlValidator(configuration, SCHEMA_LOCATION_BASIC_A_OK, root);
		validator.setReasonSessionKey("reason");
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

	@Test
	public void testWithCircularReferenceInXsd1() throws Exception {
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

	@Test
	public void testWithCircularReferenceInXsd2() throws Exception {
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

	@Test
	public void testMultipleImport() throws Exception {
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

	@Test //copied from iaf-test /XmlValidator/scenario07a
	public void testImportIncludeOK() throws Exception {
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

	@Test
	public void testImportNestedIncludeOK() throws Exception {
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

	@Test //copied from iaf-test /XmlValidator/scenario07b
	public void testImportIncludeError() throws Exception {
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, "http://nn.nl/root /Validation/ImportInclude/xsd/root.xsd", "root");
		// Override throwing exception
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

	@Test //copied from iaf-test /XmlValidator/scenario05a, then simplified
	public void testIncludeOK() throws Exception {
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

	@Test //copied from iaf-test /XmlValidator/scenario05b, then simplified
	public void testIncludeError() throws Exception {
		// Arrange
		XmlValidator validator = buildXmlValidator(configuration, "http://www.frankframework.org/tom /Validation/Include/xsd/main.xsd", "GetParties");
		validator.setThrowException(false);

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

	@Test
	public void testElementFormDefaultUnqualified() throws Exception {
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
