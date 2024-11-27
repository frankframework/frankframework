package org.frankframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import org.frankframework.pipes.FixedResultPipe;

public class ListenerExceptionTest {

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
	public void testListenerNullPipeWithCause() {
		String rootMessage = "rootmsg";
		Exception rootException = new NoSuchElementException(rootMessage);
		Exception exception2 = new PipeRunException(null, "Caught Exception", rootException);
		Exception exception3 = new ListenerException(exception2);
		String result = exception3.getMessage();

		assertEquals("Caught Exception: ("+rootException.getClass().getSimpleName()+") "+rootMessage, result);
	}

	@Test
	public void testListenerNullPipeNoCause() {
		Exception exception2 = new PipeRunException(null, "Caught Exception");
		Exception exception3 = new ListenerException(exception2);
		String result = exception3.getMessage();

		assertEquals("Caught Exception", result);
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
