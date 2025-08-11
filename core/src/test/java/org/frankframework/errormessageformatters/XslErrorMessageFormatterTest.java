package org.frankframework.errormessageformatters;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import org.frankframework.util.TimeProvider;

class XslErrorMessageFormatterTest {

	private XslErrorMessageFormatter formatter;
	private Message originalMessage;
	private Exception exception;
	private String errorMessage;
	private HasName location;
	private PipeLineSession session;

	@BeforeEach
	void setUp() {
		formatter = new XslErrorMessageFormatter();

		originalMessage = Message.asMessage("dummy-message");
		exception = null;
		errorMessage = "dummy-error-message";
		location = new MyLocation();
		session = new PipeLineSession();
		PipeLineSession.updateListenerParameters(session, "dummy-message-id", "dummy-cid", TimeProvider.now(), TimeProvider.now());
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(session);
	}

	@Test
	void configureWithStylesheet() {
		// Arrange
		formatter.setStyleSheetName("ErrorMessageFormatters/errormessage.xsl");

		// Act / Assert
		assertDoesNotThrow(formatter::configure);
	}

	@Test
	void configureWithXPath() {
		// Arrange
		formatter.setXpathExpression("/ErrorMessages/Error");

		// Act / Assert
		assertDoesNotThrow(formatter::configure);
	}

	@Test
	void configureWithStylesheetAndXPath() {
		// Arrange
		formatter.setStyleSheetName("ErrorMessageFormatters/errormessage.xsl");
		formatter.setXpathExpression("/ErrorMessages/Error");

		// Act / Assert
		assertThrows(ConfigurationException.class, formatter::configure);
	}

	@Test
	void configureWithoutStylesheetAndXPath() {
		// Act / Assert
		assertThrows(ConfigurationException.class, formatter::configure);
	}

	@Test
	void formatWithStylesheet() throws Exception {
		// Arrange
		session.put("exitCode", 400);
		NumberParameter parameter = NumberParameterBuilder.create()
				.withName("exitCode")
				.withSessionKey("exitCode");
		formatter.addParameter(parameter);
		formatter.setStyleSheetName("ErrorMessageFormatters/errormessage.xsl");
		formatter.configure();

		// Act
		Message error = formatter.format(errorMessage, exception, location, originalMessage, session);

		// Assert
		String errorAsString = error.asString();
		assertNotNull(errorAsString);

		String expected = """
				<result>
					<error id="1">
					 	<exitCode>400</exitCode>
					</error>
				</result>
				""";

		assertXmlEquals(expected, errorAsString);
	}
}
