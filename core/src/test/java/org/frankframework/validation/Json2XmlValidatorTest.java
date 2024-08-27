package org.frankframework.validation;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.Json2XmlValidator;
import org.frankframework.pipes.JsonPipe;
import org.frankframework.pipes.JsonPipe.Direction;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.ClassUtils;
import org.frankframework.validation.AbstractXmlValidator.ValidationResult;

/**
 * @author Gerrit van Brakel
 */
public class Json2XmlValidatorTest extends XmlValidatorTestBase {

	private AbstractXmlValidator validator;

	Json2XmlValidator instance;
	JsonPipe jsonPipe;

	protected static Stream<Arguments> data() {
		return Stream.of(
				Arguments.of(XercesXmlValidator.class),
				Arguments.of(JavaxXmlValidator.class)
		);
	}

	public void initJson2XmlValidatorTest(Class<? extends AbstractXmlValidator> implementation) {
		this.implementation = implementation;
	}

	protected void init() throws ConfigurationException  {
		jsonPipe=new JsonPipe();
		jsonPipe.setName("xml2json");
		jsonPipe.registerForward(new PipeForward("success",null));
		jsonPipe.setDirection(Direction.XML2JSON);
		jsonPipe.configure();
		try {
			validator = ClassUtils.newInstance(implementation);
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
		validator.setThrowException(true);
		validator.setFullSchemaChecking(true);

		instance=new Json2XmlValidator();
		instance.registerForward(new PipeForward("success",null));
		instance.setSoapNamespace(null);
		instance.setFailOnWildcards(false);
	}

	@Override
	public ValidationResult validate(String rootelement, String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, boolean ignoreUnknownNamespaces, String inputFile, String[] expectedFailureReasons) throws Exception {
		init();
		PipeLineSession session = new PipeLineSession();
		// instance.setSchemasProvider(getSchemasProvider(schemaLocation,
		// addNamespaceToSchema));
		instance.setSchemaLocation(schemaLocation);
		instance.setAddNamespaceToSchema(addNamespaceToSchema);
		instance.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
//        instance.registerForward("success");
		instance.setThrowException(true);
		instance.setFullSchemaChecking(true);
		instance.setTargetNamespace(rootNamespace);
		instance.registerForward(new PipeForward("warnings", null));
		instance.registerForward(new PipeForward("failure", null));
		instance.registerForward(new PipeForward("parserError", null));
		if (rootelement != null) {
			instance.setRoot(rootelement);
		}
		instance.configure();
		instance.start();
		validator.setSchemasProvider(instance);
		validator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
		validator.configure(null);
		validator.start();

		String testXml = inputFile != null ? TestFileUtils.getTestFile(inputFile + ".xml") : null;
		log.debug("testXml [" + inputFile + ".xml] contents [" + testXml + "]");
		String xml2json = null;
		try {
			xml2json = jsonPipe.doPipe(new Message(testXml), session).getResult().asString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.debug("testXml [" + inputFile + ".xml] to json [" + xml2json + "]");
		String testJson = inputFile != null ? TestFileUtils.getTestFile(inputFile + ".json") : null;
		log.debug("testJson ["+testJson+"]");

		try {
			PipeRunResult prr = instance.doPipe(new Message(testJson), session);
			String result = prr.getResult().asString();
			log.debug("result [" + ToStringBuilder.reflectionToString(prr) + "]");
			ValidationResult event;
			if (prr.isSuccessful()) {
				event = ValidationResult.VALID;
			} else {
				if ("failure".equals(prr.getPipeForward().getName())) {
					event = ValidationResult.INVALID;
				} else if ("warnings".equals(prr.getPipeForward().getName())) {
					event = ValidationResult.VALID_WITH_WARNINGS;
				} else if ("parserError".equals(prr.getPipeForward().getName())) {
					event = ValidationResult.PARSER_ERROR;
				} else {
					event = null;
				}
			}
			evaluateResult(event, session, null, expectedFailureReasons);
			if (event != ValidationResult.PARSER_ERROR) {
				try {
					RootValidations rootvalidations = null;
					if (rootelement != null) {
						rootvalidations = new RootValidations("Envelope", "Body", rootelement);
					}
					ValidationResult validationResult = validator.validate(result, session, rootvalidations, null);
					evaluateResult(validationResult, session, null, expectedFailureReasons);
					return validationResult;
				} catch (Exception e) {
					fail("result XML must be valid: " + e.getMessage());
				}
			}

			return event;
		} catch (PipeRunException pre) {
			evaluateResult(ValidationResult.INVALID, session, pre, expectedFailureReasons);
		}
		return null;
	}

	@Override
	public String getExpectedErrorForPlainText() {
		return "Message is not XML or JSON";
	}

	@MethodSource("data")
	@ParameterizedTest
	void jsonStructs(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		initJson2XmlValidatorTest(implementation);
		validate(null, SCHEMA_LOCATION_ARRAYS, true, INPUT_FILE_SCHEMA_LOCATION_ARRAYS_COMPACT_JSON, null);
		validate(null, SCHEMA_LOCATION_ARRAYS, true, INPUT_FILE_SCHEMA_LOCATION_ARRAYS_FULL_JSON, null);
	}

	@Override
	@ParameterizedTest
	@Disabled("check this later...")
	@MethodSource("data")
	public void unresolvableSchema(Class<? extends AbstractXmlValidator> implementation) {
		initJson2XmlValidatorTest(implementation);
	}

	@Override
	@ParameterizedTest
	@Disabled("no such thing as unknown namespace, align() determines it from the schema")
	@MethodSource("data")
	public void step5ValidationErrorUnknownNamespace(Class<? extends AbstractXmlValidator> implementation) {
		initJson2XmlValidatorTest(implementation);
	}

	@Override
	@ParameterizedTest
	@Disabled("no such thing as unknown namespace, align() determines it from the schema")
	@MethodSource("data")
	public void validationUnknownNamespaceSwitchedOff(Class<? extends AbstractXmlValidator> implementation) {
		initJson2XmlValidatorTest(implementation);
	}

	@Override
	@ParameterizedTest
	@Disabled("no such thing as unknown namespace, align() determines it from the schema")
	@MethodSource("data")
	public void validationUnknownNamespaceSwitchedOn(Class<? extends AbstractXmlValidator> implementation) {
		initJson2XmlValidatorTest(implementation);
	}

	@Override
	@ParameterizedTest
	@Disabled("no such thing as unknown namespace, align() determines it from the schema")
	@MethodSource("data")
	public void step5ValidationUnknownNamespaces(Class<? extends AbstractXmlValidator> implementation) {
		initJson2XmlValidatorTest(implementation);
	}

	@MethodSource("data")
	@ParameterizedTest
	void issue3973MissingLocalWarning(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		initJson2XmlValidatorTest(implementation);
		TestConfiguration config = new TestConfiguration();

		Json2XmlValidator json2xml = config.createBean(Json2XmlValidator.class);
		json2xml.setDeepSearch(true);
		json2xml.setSchema(BASE_DIR_VALIDATION+"/IncludeWithoutNamespace/main.xsd");
		json2xml.setRoot("GetDocument_Error");
		json2xml.setOutputFormat(DocumentFormat.JSON);

		json2xml.addParameter(new Parameter("type", "aaa"));
		json2xml.addParameter(new Parameter("title", "bbb"));
		json2xml.addParameter(new Parameter("status", "ccc"));
		json2xml.addParameter(new Parameter("detail", "ddd"));
		json2xml.addParameter(new Parameter("instance", "eee"));

		json2xml.setThrowException(true);

		json2xml.registerForward(new PipeForward("success",null));
		json2xml.configure();
		json2xml.start();
		PipeLineSession pipeLineSession = new PipeLineSession();

		PipeRunResult prr = json2xml.doPipe(new Message("{}"),pipeLineSession);
		String expected = TestFileUtils.getTestFile(BASE_DIR_VALIDATION+"/IncludeWithoutNamespace/out.json");
		assertEquals(expected, prr.getResult().asString());
		assertEquals(0, config.getConfigurationWarnings().size(), "no config warning thrown by XercesValidationErrorHandler");
	}

	@MethodSource("data")
	@ParameterizedTest
	void nonExistingResourceImportFromSchemaWithoutNamespace(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		initJson2XmlValidatorTest(implementation);
		ClassLoader appClassLoader = Thread.currentThread().getContextClassLoader();
		TestConfiguration config = new TestConfiguration();

		Json2XmlValidator json2xml = config.createBean(Json2XmlValidator.class);
		json2xml.setSchema(BASE_DIR_VALIDATION+"/IncludeNonExistingResource/main.xsd");
		json2xml.setRoot("GetDocument_Request");
		json2xml.setResponseRoot("GetDocument_Error");
		json2xml.setOutputFormat(DocumentFormat.JSON);
		json2xml.setDeepSearch(true);
		json2xml.setProduceNamespacelessXml(true);

		json2xml.addParameter(new Parameter("documentId", "aaa"));
		json2xml.addParameter(new Parameter("externalDocumentId", "bbb"));
		json2xml.addParameter(new Parameter("requestUserId", "ccc"));
		json2xml.addParameter(new Parameter("authorizedTo", "ddd"));
		json2xml.addParameter(new Parameter("idType", "documentId"));

		json2xml.setThrowException(true);

		json2xml.registerForward(new PipeForward("success", null));

		try {
			Thread.currentThread().setContextClassLoader(new ClassLoader(null) {}); //No parent classloader, getResource and getResources will not fall back

			// Should pass because the ScopeProvider is set during class initialization
			ConfigurationException thrown = assertThrows(ConfigurationException.class, json2xml::configure);
			assertThat(thrown.getMessage(), startsWith("Cannot find resource ["));

			assertEquals(0, config.getConfigurationWarnings().size(), "no config warning thrown by XercesValidationErrorHandler");
		} finally {
			Thread.currentThread().setContextClassLoader(appClassLoader);
		}
	}
}
