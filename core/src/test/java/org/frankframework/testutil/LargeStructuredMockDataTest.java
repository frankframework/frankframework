package org.frankframework.testutil;

import static org.frankframework.testutil.TestAssertions.assertEqualsIgnoreRNTSpace;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import jakarta.json.Json;
import jakarta.json.JsonStructure;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.XmlWriter;

/**
 * Test for the test-harness, since the test-harness is not trivial.
 */
class LargeStructuredMockDataTest {

	@Test
	void getLargeJsonDataReader() throws IOException {
		Reader reader = LargeStructuredMockData.getLargeJsonDataReader(200);
		String result = StreamUtil.readerToString(reader, null);

		assertJsonResult(result);
	}

	@Test
	void getLargeXmlDataReader() throws IOException {
		Reader reader = LargeStructuredMockData.getLargeXmlDataReader(400);
		String result = StreamUtil.readerToString(reader, null);

		assertXmlResult(result);
	}

	@Test
	void getLargeJsonDataInputStream() throws IOException {
		InputStream inputStream = LargeStructuredMockData.getLargeJsonDataInputStream(200, StandardCharsets.UTF_8);
		String result = StreamUtil.streamToString(inputStream);

		assertJsonResult(result);
	}

	@Test
	void getLargeXmlDataInputStream() throws IOException {
		InputStream inputStream = LargeStructuredMockData.getLargeXmlDataInputStream(400, StandardCharsets.UTF_8);
		String result = StreamUtil.streamToString(inputStream);

		assertXmlResult(result);
	}

	@Test
	void parseLargeJsonDataReader() throws Exception {
		Reader reader = LargeStructuredMockData.getLargeJsonDataReader(200);
		JsonStructure jsonStructure = assertDoesNotThrow(() -> Json.createReader(reader).read());
		assertJsonResult(jsonStructure.toString());
	}

	@Test
	void parseLargeXmlDataReader() throws Exception {
		Reader reader = LargeStructuredMockData.getLargeXmlDataReader(400);
		StringWriter writer = new StringWriter();
		XmlWriter handler = new XmlWriter(writer);
		handler.setIncludeXmlDeclaration(true);
		handler.setNewlineAfterXmlDeclaration(true);
		assertDoesNotThrow(() -> XmlUtils.parseXml(reader, handler));
		String result = writer.toString();
		assertXmlResult(result);
	}

	@Test
	void parseLargeJsonDataInputStream() throws Exception {
		InputStream inputStream = LargeStructuredMockData.getLargeJsonDataInputStream(200, StandardCharsets.UTF_8);
		JsonStructure jsonStructure = assertDoesNotThrow(() -> Json.createReader(inputStream).read());
		assertJsonResult(jsonStructure.toString());
	}

	@Test
	void parseLargeXmlDataInputStream() throws Exception {
		InputStream inputStream = LargeStructuredMockData.getLargeXmlDataInputStream(400, StandardCharsets.UTF_8);
		StringWriter writer = new StringWriter();
		XmlWriter handler = new XmlWriter(writer);
		handler.setIncludeXmlDeclaration(true);
		handler.setNewlineAfterXmlDeclaration(true);
		assertDoesNotThrow(() -> XmlUtils.parseXml(new InputSource(inputStream), handler));
		String result = writer.toString();
		assertXmlResult(result);
	}

	private static void assertJsonResult(String result) {
		assertEqualsIgnoreRNTSpace(
				"""
					[
					  {
						"type": "/errors/",
						"title": "There Is Joy In Repetition",
						"status": "DATA_ERROR",
						"detail": "The Devil's In The Details",
						"instance": "/archiving/documents"
					  },
					  {
						"type": "/errors/",
						"title": "There Is Joy In Repetition",
						"status": "DATA_ERROR",
						"detail": "The Devil's In The Details",
						"instance": "/archiving/documents"
					  },
					  {
						"type": "One Two Buckle my Shoe",
						"title": "Three Four Knock at the Door",
						"status": "Five Six Pick up the Sticks",
						"detail": "Seven Eight Lay them Straight",
						"instance": "Nine Ten a Big Fat Hen"
					  }
					]
					""", result
		);
	}

	private static void assertXmlResult(String result) {
		assertEqualsIgnoreRNTSpace(
				"""
					<?xmlversion="1.0"encoding="UTF-8"?>
					<GetDocumentAttributes_Error>
						<errors>
							<error>
								<type>/errors/</type>
								<title>There Is Joy In Repetition</title>
								<status>One Two Buckle my Shoe</status>
								<detail>The Devil's In The Details</detail>
								<instance>/archiving/documents</instance>
							</error>
						</errors>
						<errors>
							<error>
								<type>/errors/</type>
								<title>There Is Joy In Repetition</title>
								<status>One Two Buckle my Shoe</status>
								<detail>The Devil's In The Details</detail>
								<instance>/archiving/documents</instance>
							</error>
						</errors>
					</GetDocumentAttributes_Error>
					""", result
		);
	}
}
