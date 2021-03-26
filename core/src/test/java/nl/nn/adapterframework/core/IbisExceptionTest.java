package nl.nn.adapterframework.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import javax.mail.internet.AddressException;

import org.junit.Test;

public class IbisExceptionTest {

	@Test
	public void twoNestedExceptionsWithDifferentMessages() {
		SenderException exception = new SenderException("Some text here", new NullPointerException("some other text here"));
		String result = exception.getMessage();

		assertEquals("Some text here: (NullPointerException) some other text here", result);
	}

	@Test
	public void twoNestedExceptionsWithTheSameMessage() {
		SenderException exception = new SenderException("Some text here", new NullPointerException("Some text here"));
		String result = exception.getMessage();

		assertEquals("(NullPointerException) Some text here", result);
	}

	@Test
	public void testRecursiveExceptionMessageSearch() {
		SenderException exception = new SenderException("IbisLocalSender [CallAdapter-sender] exception calling JavaListener [NestedAdapter_04]: Pipe [CallAdapter] msgId [Test Tool correlation id] caught exception: IbisLocalSender [CallAdapter-sender] exception calling JavaListener [TestErrorAdapter_Stub]: Pipe [StubbedSender] msgId [Test Tool correlation id] caught exception: Pipe [StubbedSender] msgId [Test Tool correlation id] exceptionOnResult [[error]]", 
				new SenderException("Pipe [CallAdapter] msgId [Test Tool correlation id] caught exception: IbisLocalSender [CallAdapter-sender] exception calling JavaListener [TestErrorAdapter_Stub]: Pipe [StubbedSender] msgId [Test Tool correlation id] caught exception: Pipe [StubbedSender] msgId [Test Tool correlation id] exceptionOnResult [[error]]", 
				new SenderException("IbisLocalSender [CallAdapter-sender] exception calling JavaListener [TestErrorAdapter_Stub]: Pipe [StubbedSender] msgId [Test Tool correlation id] caught exception: Pipe [StubbedSender] msgId [Test Tool correlation id] exceptionOnResult [[error]]",
				new SenderException("Pipe [StubbedSender] msgId [Test Tool correlation id] caught exception: Pipe [StubbedSender] msgId [Test Tool correlation id] exceptionOnResult [[error]]", 
				new SenderException("Pipe [StubbedSender] msgId [Test Tool correlation id] exceptionOnResult [[error]]")))));
		String result = exception.getMessage();

		assertEquals("IbisLocalSender [CallAdapter-sender] exception calling JavaListener [NestedAdapter_04]: Pipe [CallAdapter] msgId [Test Tool correlation id] caught exception: IbisLocalSender [CallAdapter-sender] exception calling JavaListener [TestErrorAdapter_Stub]: Pipe [StubbedSender] msgId [Test Tool correlation id] caught exception: Pipe [StubbedSender] msgId [Test Tool correlation id] exceptionOnResult [[error]]", result);
	}
	
	@Test
	public void noMessageInException() {
		SenderException exception = new SenderException(new SenderException());
		String result = exception.getMessage();

		assertTrue(result.contains("no message, fields of this exception: nl.nn.adapterframework.core.SenderException"));
	}

	@Test
	public void exceptionWithSpecificDetailsEmpty() {
		SenderException exception = new SenderException(new AddressException("some text"));
		String result = exception.getMessage();

		assertEquals("(AddressException) some text", result);
	}
	
	@Test
	public void exceptionWithSpecificDetails() {
		SenderException exception = new SenderException(new AddressException("test","ref",14));
		String result = exception.getMessage();

		assertEquals("(AddressException) [ref] at column [15]: test", result);
	}
	
	@Test
	public void sqlExceptionWithSpecificDetails() {
		SenderException exception = new SenderException("text" ,new SQLException("reason", "state", new SenderException("test")));
		String result = exception.getMessage();

		assertEquals("text: (SQLException) SQLState [state]: reason: test", result);
	}
	
	@Test
	public void sqlExceptionWithSpecificDetailsAsTheMessageOfInnerException() {
		SenderException exception = new SenderException("SQLState [state]" ,new SQLException("reason", "state", new SenderException("SQLState [state]")));
		String result = exception.getMessage();

		assertEquals("SQLState [state]: (SQLException) reason: SQLState [state]", result);
	}

	@Test
	public void testRecursiveExceptionMessageSearchUseToString() {
		SenderException exception = new SenderException("IbisLocalSender [CallAdapter-sender] exception calling JavaListener [NestedAdapter_04]: Pipe [CallAdapter] msgId [Test Tool correlation id] caught exception: IbisLocalSender [CallAdapter-sender] exception calling JavaListener [TestErrorAdapter_Stub]: Pipe [StubbedSender] msgId [Test Tool correlation id] caught exception: Pipe [StubbedSender] msgId [Test Tool correlation id] exceptionOnResult [[error]]", 
				new SenderException("javax.mail.internet.AddressException: Pipe [StubbedSender] msgId [Test Tool correlation id] exceptionOnResult [[error]]", 
				new AddressException("Pipe [StubbedSender] msgId [Test Tool correlation id] exceptionOnResult [[error]]")));
		String result = exception.getMessage();

		assertEquals("IbisLocalSender [CallAdapter-sender] exception calling JavaListener [NestedAdapter_04]: Pipe [CallAdapter] msgId [Test Tool correlation id] caught exception: IbisLocalSender [CallAdapter-sender] exception calling JavaListener [TestErrorAdapter_Stub]: Pipe [StubbedSender] msgId [Test Tool correlation id] caught exception: (AddressException) Pipe [StubbedSender] msgId [Test Tool correlation id] exceptionOnResult [[error]]", result);
	}
}
