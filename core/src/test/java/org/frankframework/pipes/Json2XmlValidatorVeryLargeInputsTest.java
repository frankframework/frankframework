package org.frankframework.pipes;

import static org.frankframework.testutil.TestAssertions.assertEqualsIgnoreWhitespaces;

import org.junit.jupiter.api.Test;

import org.frankframework.LargeStructuredMockDataReader;
import org.frankframework.core.PipeRunResult;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;


public class Json2XmlValidatorVeryLargeInputsTest extends PipeTestBase<Json2XmlValidator> {

	@Override
	public Json2XmlValidator createPipe() {
		return new Json2XmlValidator();
	}

	@Test
	public void testJsonArrayToJsonLargeInput() throws Exception {
		// Arrange
		pipe.setName("testJsonIntoDeepSearch");
		pipe.setSchema("/Validation/Json2Xml/ParameterSubstitution/Main.xsd");
		pipe.setThrowException(true);
		pipe.setOutputFormat(DocumentFormat.JSON);
		pipe.setRoot("GetDocumentAttributes_Error");
		pipe.setDeepSearch(true);

		pipe.configure();
		pipe.start();

		LargeStructuredMockDataReader reader = new LargeStructuredMockDataReader(Integer.MAX_VALUE / 2 - 100,
				"[\n",
				"""
										{
					"type": "/errors/",
					"title": "More than one document found",
					"status": "DATA_ERROR",
					"detail": "The Devil's In The Details",
					"instance": "/archiving/documents"
				}
				]
				""",
				"""
				{
					"type": "/errors/",
					"title": "More than one document found",
					"status": "DATA_ERROR",
					"detail": "The Devil's In The Details",
					"instance": "/archiving/documents"
				},
				""");
		Message input = Message.asMessage(reader);

		// Act
		PipeRunResult result = pipe.doPipe(input, session);

		// Assert
		String expectedResult = TestFileUtils.getTestFile("/Validation/Json2Xml/ParameterSubstitution/expected_output_multiple_occurrences.json");
		assertEqualsIgnoreWhitespaces(expectedResult, result.getResult().asString());
	}

	@Test
	public void testXmlToJsonLargeInput() throws Exception {
		// Arrange
		pipe.setName("testJsonIntoDeepSearch");
		pipe.setSchema("/Validation/Json2Xml/ParameterSubstitution/Main.xsd");
		pipe.setAcceptNamespacelessXml(true);
		pipe.setThrowException(true);
		pipe.setOutputFormat(DocumentFormat.JSON);
		pipe.setRoot("GetDocumentAttributes_Error");

		pipe.configure();
		pipe.start();

		LargeStructuredMockDataReader reader = new LargeStructuredMockDataReader(Integer.MAX_VALUE / 2 - 100,
				"""
						<?xml version="1.0" encoding="UTF-8"?>
						<GetDocumentAttributes_Error>
						""",
				"""
						</GetDocumentAttributes_Error>
						""",
				"""
						<errors>
							<error>
								<type>/errors/</type>
								<title>More than one document found</title>
								<status>DATA_ERROR</status>
								<detail>The Devil's In The Details</detail>
								<instance>/archiving/documents</instance>
							</error>
						</errors>
						""");
		Message input = Message.asMessage(reader);

		// Act
		PipeRunResult result = pipe.doPipe(input, session);

		// Assert
		String expectedResult = TestFileUtils.getTestFile("/Validation/Json2Xml/ParameterSubstitution/expected_output_multiple_occurrences.json");
		assertEqualsIgnoreWhitespaces(expectedResult, result.getResult().asString());
	}

	@Test
	public void testJsonArrayToJsonXLargeInput() throws Exception {
		// Arrange
		pipe.setName("testJsonIntoDeepSearch");
		pipe.setSchema("/Validation/Json2Xml/ParameterSubstitution/Main.xsd");
		pipe.setThrowException(true);
		pipe.setOutputFormat(DocumentFormat.JSON);
		pipe.setRoot("GetDocumentAttributes_Error");
		pipe.setDeepSearch(true);

		pipe.configure();
		pipe.start();

		LargeStructuredMockDataReader reader = new LargeStructuredMockDataReader(Integer.MAX_VALUE - 100,
				"[\n",
				"""
										{
					"type": "/errors/",
					"title": "More than one document found",
					"status": "DATA_ERROR",
					"detail": "The Devil's In The Details",
					"instance": "/archiving/documents"
				}
				]
				""",
				"""
				{
					"type": "/errors/",
					"title": "More than one document found",
					"status": "DATA_ERROR",
					"detail": "The Devil's In The Details",
					"instance": "/archiving/documents"
				},
				""");
		Message input = Message.asMessage(reader);

		// Act
		PipeRunResult result = pipe.doPipe(input, session);

		// Assert
		String expectedResult = TestFileUtils.getTestFile("/Validation/Json2Xml/ParameterSubstitution/expected_output_multiple_occurrences.json");
		assertEqualsIgnoreWhitespaces(expectedResult, result.getResult().asString());
	}

	@Test
	public void testXmlToJsonXLargeInput() throws Exception {
		// Arrange
		pipe.setName("testJsonIntoDeepSearch");
		pipe.setSchema("/Validation/Json2Xml/ParameterSubstitution/Main.xsd");
		pipe.setAcceptNamespacelessXml(true);
		pipe.setThrowException(true);
		pipe.setOutputFormat(DocumentFormat.JSON);
		pipe.setRoot("GetDocumentAttributes_Error");

		pipe.configure();
		pipe.start();

		LargeStructuredMockDataReader reader = new LargeStructuredMockDataReader(Integer.MAX_VALUE - 100,
				"""
						<?xml version="1.0" encoding="UTF-8"?>
						<GetDocumentAttributes_Error>
						""",
				"""
						</GetDocumentAttributes_Error>
						""",
				"""
						<errors>
							<error>
								<type>/errors/</type>
								<title>More than one document found</title>
								<status>DATA_ERROR</status>
								<detail>The Devil's In The Details</detail>
								<instance>/archiving/documents</instance>
							</error>
						</errors>
						""");
		Message input = Message.asMessage(reader);

		// Act
		PipeRunResult result = pipe.doPipe(input, session);

		// Assert
		String expectedResult = TestFileUtils.getTestFile("/Validation/Json2Xml/ParameterSubstitution/expected_output_multiple_occurrences.json");
		assertEqualsIgnoreWhitespaces(expectedResult, result.getResult().asString());
	}
}
