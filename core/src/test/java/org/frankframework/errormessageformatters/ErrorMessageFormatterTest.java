package org.frankframework.errormessageformatters;

import static org.frankframework.testutil.MatchUtils.assertJsonEquals;
import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.HasName;
import org.frankframework.core.PipeLineSession;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

class ErrorMessageFormatterTest {

	private ErrorMessageFormatter formatter;
	private Message originalMessage;
	private Exception exception;
	private String errorMessage;
	private HasName location;
	private PipeLineSession session;

	@BeforeEach
	void setUp() {
		formatter = new ErrorMessageFormatter();
		originalMessage = Message.asMessage("dummy-message");
		exception = null;
		errorMessage = "dummy-error-message";
		location = new MyLocation();
		session = new PipeLineSession();
		PipeLineSession.updateListenerParameters(session, "dummy-message-id", "dummy-cid", Instant.now(), Instant.now());
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(session);
	}

	@Test
	void formatAsXml() throws Exception {
		formatter.setMessageFormat(DocumentFormat.XML);

		// Act
		Message error = formatter.format(errorMessage, exception, location, originalMessage, session);

		// Assert
		String errorAsString = error.asString();
		assertNotNull(errorAsString);

		String expected = """
				<errorMessage timestamp="-timestamp-" originator="IAF " message="MyLocation [dummy-location] msgId [dummy-message-id]: dummy-error-message">
					<location class="org.frankframework.errormessageformatters.MyLocation" name="dummy-location"/>
					<originalMessage messageId="dummy-message-id" receivedTime="-timestamp-">dummy-message</originalMessage>
				</errorMessage>
				""";

		assertXmlEquals(applyIgnores(expected), applyIgnores(errorAsString));
	}

	@Test
	void formatAsJson() throws Exception {
		formatter.setMessageFormat(DocumentFormat.JSON);

		// Act
		Message error = formatter.format(errorMessage, exception, location, originalMessage, session);

		// Assert
		String errorAsString = error.asString();
		assertNotNull(errorAsString);

		String expected = """
				{
					"errorMessage": {
						"timestamp": "-timestamp-",
						"originator": "IAF ",
						"message": "MyLocation [dummy-location] msgId [dummy-message-id]: dummy-error-message",
						"location": {
							"class": "org.frankframework.errormessageformatters.MyLocation",
							"name": "dummy-location"
						},
						"originalMessage": {
							"messageId": "dummy-message-id",
							"receivedTime": "-timestamp-",
							"message": "dummy-message"
						}
					}
				}
				""";

		System.err.println(errorAsString);
		assertJsonEquals(applyIgnores(expected), applyIgnores(errorAsString));
	}

	public static String applyIgnores(String input) {
		// Ignore timestamps formatted in the formats:
		// - 2025-05-14 16:26:41
		// - Wed May 14 16:26:41 CEST 2025
		return input
				.replaceAll("\"\\w{3} \\w{3} \\d{1,2} \\d{1,2}:\\d{2}:\\d{2} \\w+ \\d{4}\"", "\"-timestamp-\"")
				.replaceAll("\"\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\"", "\"-timestamp-\"")
				;
	}
}
