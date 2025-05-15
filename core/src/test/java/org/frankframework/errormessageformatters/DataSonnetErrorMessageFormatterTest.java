package org.frankframework.errormessageformatters;

import static org.frankframework.testutil.MatchUtils.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasName;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.NumberParameter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.NumberParameterBuilder;
import org.frankframework.util.CloseUtils;

class DataSonnetErrorMessageFormatterTest {

	private DataSonnetErrorMessageFormatter formatter;
	private Message originalMessage;
	private Exception exception;
	private String errorMessage;
	private HasName location;
	private PipeLineSession session;

	@BeforeEach
	void setUp() throws ConfigurationException {
		formatter = new DataSonnetErrorMessageFormatter();
		formatter.setStyleSheetName("ErrorMessageFormatters/errormessage.jsonnet");
		formatter.configure();

		originalMessage = Message.asMessage("dummy-message");
		exception = null;
		errorMessage = "dummy-error-message";
		location = new ErrorMessageFormatterTest.MyLocation();
		session = new PipeLineSession();
		PipeLineSession.updateListenerParameters(session, "dummy-message-id", "dummy-cid", Instant.now(), Instant.now());
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(session);
	}

	@Test
	void formatNoParameters() throws IOException {
		// Act
		Message error = formatter.format(errorMessage, exception, location, originalMessage, session);

		// Assert
		String errorAsString = error.asString();
		assertNotNull(errorAsString);

		String expected = """
				{
					"error": "MyLocation [dummy-location] msgId [dummy-message-id]: dummy-error-message",
					"messageId": "dummy-message-id"
				}
				""";

		System.err.println(errorAsString);
		assertJsonEquals(expected, errorAsString);
	}

	@Test
	void formatWithParameters() throws Exception {
		// Arrange
		session.put("exitCode", 400);
		NumberParameter parameter = NumberParameterBuilder.create()
				.withName("exitCode")
				.withSessionKey("exitCode");
		formatter.addParameter(parameter);
		formatter.setStyleSheetName("ErrorMessageFormatters/errormessageWithParams.jsonnet");
		formatter.configure();

		// Act
		Message error = formatter.format(errorMessage, exception, location, originalMessage, session);

		// Assert
		String errorAsString = error.asString();
		assertNotNull(errorAsString);

		String expected = """
				{
					"error": "MyLocation [dummy-location] msgId [dummy-message-id]: dummy-error-message",
					"messageId": "dummy-message-id",
					"status": 400
				}
				""";

		System.err.println(errorAsString);
		assertJsonEquals(expected, errorAsString);
	}
}
