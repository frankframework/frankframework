package nl.nn.adapterframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import javax.jms.JMSException;

import org.junit.jupiter.api.Test;

import jakarta.mail.internet.AddressException;
import nl.nn.adapterframework.pipes.FixedResultPipe;

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
		String msg1 = "Pipe [StubbedSender] msgId [Test Tool correlation id] exceptionOnResult [[error]]";
		SenderException exception1 = new SenderException(msg1);
		String msg2 = "Pipe [StubbedSender] msgId [Test Tool correlation id] caught exception: "+exception1.getMessage();
		SenderException exception2 = new SenderException(msg2, exception1);
		String msg3 = "IbisLocalSender [CallAdapter-sender] exception calling JavaListener [TestErrorAdapter_Stub]: "+exception2.getMessage();
		SenderException exception3 = new SenderException(msg3, exception2);
		String msg4 = "Pipe [CallAdapter] msgId [Test Tool correlation id] caught exception: "+exception3.getMessage();
		SenderException exception4 = new SenderException(msg4, exception3);
		String msg5 = "IbisLocalSender [CallAdapter-sender] exception calling JavaListener [NestedAdapter_04]: "+exception4.getMessage();
		SenderException exception5 = new SenderException(msg5, exception4);
		String result = exception5.getMessage();

		assertEquals(msg5, result);
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
		String rootMessage = "rootmsg";
		Exception rootException = new AddressException(rootMessage);
		String message2 = rootException.toString();
		Exception exception2 = new IbisException(message2, rootException);
		String message3base = "Message3: ";
		Exception exception3 = new IbisException(message3base+exception2.getMessage(), exception2);
		String result = exception3.getMessage();

		assertEquals(message3base + "(AddressException) "+rootMessage, result);
	}

	@Test
	public void testRecursiveExceptionMessageSearchUseToStringShort() {
		String rootMessage = "rootmsg";
		Exception rootException = new AddressException(rootMessage);
		String message2 = rootException.toString();
		Exception exception2 = new IbisException(message2, rootException);
		String result = exception2.getMessage();

		assertEquals("(AddressException) "+rootMessage, result);
	}

	@Test
	public void testJmsException() {
		IOException root = new IOException("rootMsg");
		JMSException jmse = new JMSException("reason", "errorCode");
		jmse.setLinkedException(root);
		Exception ibisException = new IbisException("wrapper", jmse);

		String result = ibisException.getMessage();

		assertEquals("wrapper: (JMSException) reason: (IOException) rootMsg", result);
	}

	@Test
	public void testListenerPipeRunOther() {
		IPipe pipe = new FixedResultPipe();
		String rootMessage = "rootmsg";
		Exception rootException = new NoSuchElementException(rootMessage);
		String message2 = "Caught Exception";
		Exception exception2 = new PipeRunException(pipe, message2, rootException);
		Exception exception3 = new ListenerException(exception2);
		String result = exception3.getMessage();

		assertEquals("Pipe [null] "+message2+": ("+rootException.getClass().getSimpleName()+") "+rootMessage, result);
	}

	@Test
	public void testListenerPipeRun() {
		IPipe pipe = new FixedResultPipe();
		String rootMessage = "rootmsg";
		Exception rootException = new PipeRunException(pipe, rootMessage);
		String message2 = "Caught Exception";
		Exception exception2 = new ListenerException(message2, rootException);
		String result = exception2.getMessage();

		assertEquals(message2+": Pipe [null] "+rootMessage, result);
	}
}
