package org.frankframework.align;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.JsonStructure;
import jakarta.json.stream.JsonParser;

import org.apache.commons.lang3.StringUtils;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeForward;
import org.frankframework.pipes.Json2XmlValidator;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.StreamUtil;

/*
 * @see: https://github.com/networknt/json-schema-validator
 * @see: https://swagger.io/specification
 * @see: https://json-schema.org/understanding-json-schema/reference/
 */
@Log4j2
public class TestXmlSchema2JsonSchema extends AlignTestBase {

	@Override
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems, String expectedFailureReason) throws Exception {
		boolean expectValidRoundTrip = false;
		testXml2JsonSchema(schemaFile, namespace, inputFile, rootElement, false, false, expectValidRoundTrip, expectedFailureReason);
//		testXml2JsonSchema(schemaFile, namespace, inputFile, rootElement, true, false, expectValidRoundTrip, expectedFailureReason);
//		testXml2JsonSchema(schemaFile, namespace, inputFile, rootElement, false, true, expectValidRoundTrip, expectedFailureReason);
		testXml2JsonSchema(schemaFile, namespace, inputFile, rootElement, true, true, expectValidRoundTrip,	expectedFailureReason);
	}

	public void testXml2JsonSchema(String schemaFile, String namespace, String inputFile, String rootElement, boolean compactArrays, boolean skipJsonRootElements, boolean expectValidRoundTrip, String expectedFailureReason) throws Exception {

		String jsonSchemaFile = schemaFile.replace(".xsd", (skipJsonRootElements ? "-compact-" : "-full-") + rootElement + ".jsd");
		URL jsonSchemaUrl = getSchemaURL(jsonSchemaFile);
		String expectedJsonSchema = jsonSchemaUrl == null ? null : StreamUtil.streamToString(jsonSchemaUrl.openStream(), "\n", "utf-8");
		String jsonString = getTestFile(inputFile + (skipJsonRootElements ? "-compact" : "-full") + ".json");

		Json2XmlValidator validator = new Json2XmlValidator();
		validator.addForward(new PipeForward("success", null));
		validator.setThrowException(true);
		if (StringUtils.isNotEmpty(namespace)) {
			validator.setSchemaLocation(namespace + " " + BASEDIR + schemaFile);
		} else {
			validator.setSchema(BASEDIR + schemaFile);
		}
		validator.setJsonWithRootElements(!skipJsonRootElements);
		validator.setCompactJsonArrays(compactArrays);
		validator.setRoot(rootElement);
		validator.configure();
		validator.start();

		boolean expectValid = expectedFailureReason == null;

//    	// check the validity of the input XML
//    	if (expectValid) {
//    		assertEquals("valid XML", expectValid, Utils.validate(schemaUrl, xmlString));
//    	}
//
		JsonStructure jsonschema = validator.createJsonSchema(rootElement);
		//JsonStructure jsonschema = validator.createJsonSchema();

		if (jsonschema == null) {
			fail("no schema generated for [" + rootElement + "]");
		}
		String jsonSchemaContent = Misc.jsonPretty(jsonschema.toString());
		log.info("result compactArrays [{}] skipJsonRootElements [{}] json:\n{}", compactArrays, skipJsonRootElements, jsonSchemaContent);
		if (StringUtils.isEmpty(jsonSchemaContent)) {
			fail("json schema is empty");
		}
		// compare generated schema to reference
		if (compactArrays == skipJsonRootElements) {

			//String schemaPretty = jsonPrettyPrint("{" + jsonSchemaContent + "}");
			//schemaPretty = schemaPretty.substring(1, schemaPretty.length() - 1).trim();

			log.debug("expected [{}]", expectedJsonSchema);
			log.debug("actual [{}]", jsonSchemaContent);
			MatchUtils.assertJsonEquals("generated does not match [" + jsonSchemaFile + "]", expectedJsonSchema, jsonSchemaContent);

//	    	if (!expectValid) {
//				fail("expected to fail with reason ["+ expectedFailureReason +"]");
//			}

		}

		// validate the json against the generated schema
		if (compactArrays==skipJsonRootElements) {
			if (StringUtils.isNotEmpty(jsonString)) {
				validateJson(jsonString,jsonSchemaContent);
			}
		}

	}

	public void validateJson(String jsonString, String jsonSchemaContent) {
		JsonValidationService service = JsonValidationService.newInstance();
		JsonSchema schema = service.readSchema(new StringReader(jsonSchemaContent));
		final List<String> problems = new ArrayList<>();
		// Problem handler which will print problems found.
		ProblemHandler handler = service.createProblemPrinter(problems::add);

		try (JsonParser parser = service.createParser(new StringReader(jsonString), schema, handler)) {
			while (parser.hasNext()) {
				parser.next();
				// Could do something useful here, like posting the event on a JsonEventHandler.
			}
		}

		log.debug("jsonString: {}", jsonString);
		log.debug("problems: {}", problems);
		assertEquals(0, problems.size(), problems.toString());
	}
}
