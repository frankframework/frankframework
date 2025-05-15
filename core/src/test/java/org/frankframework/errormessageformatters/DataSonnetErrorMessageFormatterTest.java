package org.frankframework.errormessageformatters;

import static org.frankframework.testutil.MatchUtils.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasName;
import org.frankframework.stream.Message;

class DataSonnetErrorMessageFormatterTest {

	private DataSonnetErrorMessageFormatter formatter;
	private Message originalMessage;
	private Exception exception;
	private String errorMessage;
	private HasName location;
	private String messageId;
	private Instant receivedTs;

	@BeforeEach
	void setUp() throws ConfigurationException {
		formatter = new DataSonnetErrorMessageFormatter();
		formatter.setStyleSheetName("ErrorMessageFormatters/errormessage.jsonnet");
		formatter.configure();

		originalMessage = Message.asMessage("dummy-message");
		exception = null;
		errorMessage = "dummy-error-message";
		location = new ErrorMessageFormatterTest.MyLocation();
		messageId = "dummy-message-id";
		receivedTs = Instant.now();
	}

	@Test
	void format() throws IOException {
		// Act
		Message error = formatter.format(errorMessage, exception, location, originalMessage, messageId, receivedTs.toEpochMilli());

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
}
