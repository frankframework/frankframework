package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.frankframework.core.PipeRunException;
import org.frankframework.stream.Message;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * ExceptionPipe Tester.
 *
 * @author <Sina Sen>
 */

public class ExceptionPipeTest extends PipeTestBase<ExceptionPipe> {

	@Override
	public ExceptionPipe createPipe() {
		return new ExceptionPipe();
	}

	@Test
	public void testDoesntThrowException() throws Exception {
		pipe.setThrowException(false);
		Message m = new Message("no exception");
		assertEquals("no exception", doPipe(pipe, m, session).getResult().asString());
	}

	@Test
	public void throwsExceptionWithoutMessage() {
		pipe.setThrowException(true);

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "", session));
		assertThat(e.getMessage(), Matchers.endsWith("ExceptionPipe under test"));
	}

	@Test
	public void throwsExceptionWithMessage() {
		pipe.setThrowException(true);

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "exception thrown with a custom message", session));
		assertThat(e.getMessage(), Matchers.endsWith("exception thrown with a custom message"));
	}
}
