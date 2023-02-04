package nl.nn.adapterframework.validation;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.Json2XmlValidator;
import nl.nn.adapterframework.pipes.JsonPipe;
import nl.nn.adapterframework.pipes.JsonPipe.Direction;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.validation.AbstractXmlValidator.ValidationResult;

/**
 * @author Gerrit van Brakel
 */
@RunWith(value = Parameterized.class)
public class Json2XmlValidatorTest extends XmlValidatorTestBase {

	private Class<? extends AbstractXmlValidator> implementation;
	private AbstractXmlValidator validator;

	Json2XmlValidator instance;
	JsonPipe jsonPipe;

	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { { XercesXmlValidator.class }, { JavaxXmlValidator.class } };
		return Arrays.asList(data);
	}

	public Json2XmlValidatorTest(Class<? extends AbstractXmlValidator> implementation) {
		this.implementation = implementation;
	}

	protected void init() throws ConfigurationException  {
		jsonPipe=new JsonPipe();
		jsonPipe.setName("xml2json");
		jsonPipe.registerForward(new PipeForward("success",null));
		jsonPipe.setDirection(Direction.XML2JSON);
		jsonPipe.configure();
		try {
			validator = implementation.newInstance();
		} catch (IllegalAccessException e) {
			throw new ConfigurationException(e);
		} catch (InstantiationException e) {
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
				if (prr.getPipeForward().getName().equals("failure")) {
					event = ValidationResult.INVALID;
				} else if (prr.getPipeForward().getName().equals("warnings")) {
					event = ValidationResult.VALID_WITH_WARNINGS;
				} else if (prr.getPipeForward().getName().equals("parserError")) {
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
					ValidationResult validationResult = validator.validate(result, session, "check result", rootvalidations, null);
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

	@Test
	public void jsonStructs() throws Exception {
		validate(null, SCHEMA_LOCATION_ARRAYS, true, INPUT_FILE_SCHEMA_LOCATION_ARRAYS_COMPACT_JSON, null);
		validate(null, SCHEMA_LOCATION_ARRAYS, true, INPUT_FILE_SCHEMA_LOCATION_ARRAYS_FULL_JSON, null);
	}

	@Override
	@Ignore // check this later...
	public void unresolvableSchema() throws Exception {
	}

	@Override
	@Ignore // no such thing as unknown namespace, align() determines it from the schema
	public void step5ValidationErrorUnknownNamespace() throws Exception {
	}

	@Override
	@Ignore // no such thing as unknown namespace, align() determines it from the schema
	public void validationUnknownNamespaceSwitchedOff() throws Exception {
	}

	@Override
	@Ignore // no such thing as unknown namespace, align() determines it from the schema
	public void validationUnknownNamespaceSwitchedOn() throws Exception {
	}

	@Override
	@Ignore // no such thing as unknown namespace, align() determines it from the schema
	public void step5ValidationUnknownNamespaces() throws Exception {
	}

	@Test
	public void issue3973MissingLocalWarning() throws Exception {
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
		assertEquals("no config warning thrown by XercesValidationErrorHandler", 0, config.getConfigurationWarnings().size());
	}

	@Test
	public void nonExistingResourceImportFromSchemaWithoutNamespace() throws Exception {
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
			assertThat(thrown.getMessage(), startsWith("Cannot find ["));

			assertEquals("no config warning thrown by XercesValidationErrorHandler", 0, config.getConfigurationWarnings().size());
		} finally {
			Thread.currentThread().setContextClassLoader(appClassLoader);
		}
	}
}
