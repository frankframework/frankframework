package org.frankframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Stream;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import jakarta.jms.JMSException;
import jakarta.mail.internet.AddressException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import oracle.jdbc.xa.OracleXAException;

public class IbisExceptionTest {

	@Test
	public void twoNestedExceptionsWithDifferentMessages() {
		IbisExceptionSubClass exception = new IbisExceptionSubClass("Some text here", new NullPointerException("some other text here"));
		String result = exception.getMessage();

		assertEquals("prefixSome text here: (NullPointerException) some other text here", result);
	}

	@Test
	public void twoNestedExceptionsWithTheSameMessage() {
		IbisExceptionSubClass exception = new IbisExceptionSubClass(new NullPointerException("Some other text here"));
		String result = exception.getMessage();

		assertEquals("prefix: (NullPointerException) Some other text here", result);
	}

	@Test
	public void testRecursiveExceptionMessageSearch() {
		String msg1 = "Pipe [StubbedSender] msgId [Test Tool correlation id] exceptionOnResult [[error]]";
		IbisException exception1 = new IbisException(msg1);
		String msg2 = "Pipe [StubbedSender] msgId [Test Tool correlation id] caught exception: " + exception1.getMessage();
		IbisException exception2 = new IbisException(msg2, exception1);
		String msg3 = "IbisLocalSender [CallAdapter-sender] exception calling JavaListener [TestErrorAdapter_Stub]: " + exception2.getMessage();
		IbisException exception3 = new IbisException(msg3, exception2);
		String msg4 = "Pipe [CallAdapter] msgId [Test Tool correlation id] caught exception: " + exception3.getMessage();
		IbisException exception4 = new IbisException(msg4, exception3);
		String msg5 = "IbisLocalSender [CallAdapter-sender] exception calling JavaListener [NestedAdapter_04]: " + exception4.getMessage();
		IbisException exception5 = new IbisException(msg5, exception4);
		String result = exception5.getMessage();

		assertEquals(msg5, result);
	}

	@Test
	public void noMessageInException() {
		IbisException exception = new IbisException(new IbisException());
		String result = exception.getMessage();

		assertTrue(result.contains("no message in exception: org.frankframework.core.IbisException"));
	}

	@Test
	public void exceptionWithSpecificDetailsEmpty() {
		IbisException exception = new IbisException(new AddressException("some text"));
		String result = exception.getMessage();

		assertEquals("(AddressException) some text", result);
	}

	@Test
	public void exceptionWithSpecificDetails() {
		IbisException exception = new IbisException(new AddressException("test", "ref", 14));
		String result = exception.getMessage();

		assertEquals("(AddressException) [ref] at column [15]: test", result);
	}

	private static Stream<Arguments> sqlExceptions() {
		return Stream.of(
				Arguments.of("text: (SQLException) sql reason: (SQLException) spice it up with a suppressed message", new SQLException("sql reason")),
				Arguments.of("text: (SQLException) sql reason: rootException", new SQLException("sql reason", new IbisException("rootException"))),
				Arguments.of("text: (SQLException) errorCode [1234]: sql reason: rootException", new SQLException("sql reason", null, 1234, new IbisException("rootException"))),
				Arguments.of("text: (SQLException) SQLState [xyz]: sql reason: rootException", new SQLException("sql reason", "xyz", new IbisException("rootException"))),
				Arguments.of("text: (SQLException) SQLState [xyz], errorCode [1234]: sql reason: rootException", new SQLException("sql reason", "xyz", 1234, new IbisException("rootException")))
		);
	}

	@ParameterizedTest
	@MethodSource("sqlExceptions")
	public void sqlExceptions(String expectedMessage, SQLException cause) {
		SQLException nextException = new SQLException("spice it up with a suppressed message");
		cause.setNextException(nextException); //This should be picked up by the 'ExceptionUtils.getCause(t)'
		// Arrange
		IbisException exception = new IbisException("text", cause);

		// Act
		String result = exception.getMessage();

		// Assert
		assertEquals(expectedMessage, result);
	}

	private static Stream<Arguments> oracleXaExceptions() {
		return Stream.of(
				Arguments.of("text: (OracleXAException) XAErr (0): Internal XA Error ORA-1234 SQLErr (0)", new OracleXAException(1234, 0)),
				Arguments.of("text: (OracleXAException) xaError [5678] xaErrorMessage [Internal XA Error]: XAErr (5678): Internal XA Error ORA-1234 SQLErr (0)", new OracleXAException(1234, 5678)),
				Arguments.of("text: (OracleXAException) xaError [1234] xaErrorMessage [Internal XA Error]: XAErr (1234): Internal XA Error ORA-1234 SQLErr (0): (SQLException) SQLState [xyz], errorCode [1234]: sql reason: rootException", new OracleXAException(new SQLException("sql reason", "xyz", 1234, new IbisException("rootException")), 1234))
		);
	}

	@ParameterizedTest
	@MethodSource("oracleXaExceptions")
	public void sqlExceptions(String expectedMessage, OracleXAException cause) {
		// Arrange
		IbisException exception = new IbisException("text", cause);

		// Act
		String result = exception.getMessage();

		// Assert
		assertEquals(expectedMessage, result);
	}

	@Test
	public void sqlExceptionWithSpecificDetailsAsTheMessageOfInnerException() {
		IbisException exception = new IbisException("SQLState1 [state1]", new SQLException("reason", "state", new IbisException("SQLState2 [state2]")));
		String result = exception.getMessage();

		assertEquals("SQLState1 [state1]: (SQLException) SQLState [state]: reason: SQLState2 [state2]", result);
	}

	@Test
	public void testRecursiveExceptionMessageSearchUseToString() {
		String rootMessage = "rootmsg";
		Exception rootException = new AddressException(rootMessage);
		String message2 = rootException.toString();
		Exception exception2 = new IbisException(message2, rootException);
		Exception exception3 = new IbisException("Message3: " + exception2.getMessage(), exception2);
		String result = exception3.getMessage();

		assertEquals("Message3: (AddressException) " + rootMessage, result);
	}

	@Test
	public void testRecursiveExceptionMessageSearchUseToStringShort() {
		String rootMessage = "rootmsg";
		Exception rootException = new AddressException(rootMessage);
		String message2 = rootException.toString();
		Exception exception2 = new IbisException(message2, rootException);
		String result = exception2.getMessage();

		assertEquals("(AddressException) " + rootMessage, result);
	}

	@Test
	public void testJmsException() {
		IOException root = new IOException("rootMsg");
		JMSException jmse = new JMSException("reason", "errorCode");
		jmse.setLinkedException(root); //This should be picked up by the 'ExceptionUtils.getCause(t)'
		Exception ibisException = new IbisException("wrapper", jmse);

		String result = ibisException.getMessage();

		assertEquals("wrapper: (JMSException) reason: (IOException) rootMsg", result);
	}

	private static Stream<Arguments> differentLocators() {
		return Stream.of(
				Arguments.of("wrapper: (SAXParseException) reason", new TestLocator("abc", null, -1, -1)),
				Arguments.of("wrapper: (SAXParseException) SystemId [xyz]: reason", new TestLocator("abc", "xyz", -1, -1)),
				Arguments.of("wrapper: (SAXParseException) line [13] column [37]: reason", new TestLocator("abc", null, 13, 37)),
				Arguments.of("wrapper: (SAXParseException) SystemId [xyz] line [13] column [37]: reason", new TestLocator("abc", "xyz", 13, 37))
		);
	}

	@ParameterizedTest
	@MethodSource("differentLocators")
	public void testSAXParseException(String expectedMessage, Locator locator) {
		// Arrange
		IOException root = new IOException("spice it up with a suppressed message");
		SAXParseException saxException = new SAXParseException("reason", locator);
		saxException.addSuppressed(root); //This should be picked up by the 'ExceptionUtils.getCause(t)'
		Exception ibisException = new IbisException("wrapper", saxException);

		// Act
		String result = ibisException.getMessage();

		// Assert
		assertEquals(expectedMessage + ": (IOException) spice it up with a suppressed message", result);
	}

	private static Stream<Arguments> differentSourceLocators() {
		return Stream.of(
				Arguments.of("wrapper: (TransformerException) reason", null),
				Arguments.of("wrapper: (TransformerException) reason", new TestLocator("abc", null, -1, -1)),
				Arguments.of("wrapper: (TransformerException) SystemId [xyz]: reason", new TestLocator("abc", "xyz", -1, -1)),
				Arguments.of("wrapper: (TransformerException) line [13] column [37]: reason", new TestLocator("abc", null, 13, 37)),
				Arguments.of("wrapper: (TransformerException) SystemId [xyz] line [13] column [37]: reason", new TestLocator("abc", "xyz", 13, 37))
		);
	}

	@ParameterizedTest
	@MethodSource("differentSourceLocators")
	public void testTransformerException(String expectedMessage, SourceLocator locator) {
		// Arrange
		IOException root = new IOException("spice it up with a suppressed message");
		TransformerException saxException = new TransformerException("reason", locator);
		saxException.addSuppressed(root); //This should be picked up by the 'ExceptionUtils.getCause(t)'
		Exception ibisException = new IbisException("wrapper", saxException);

		// Act
		String result = ibisException.getMessage();

		// Assert
		assertEquals(expectedMessage + ": (IOException) spice it up with a suppressed message", result);
	}

	private static class IbisExceptionSubClass extends IbisException {
		public IbisExceptionSubClass(Throwable t) {
			this("", t);
		}

		public IbisExceptionSubClass(String errMsg, Throwable t) {
			super("prefix" + errMsg, t);
		}
	}

	@AllArgsConstructor
	private static class TestLocator implements Locator, SourceLocator {
		@Getter String publicId;
		@Getter String systemId;
		@Getter int lineNumber;
		@Getter int columnNumber;
	}
}
