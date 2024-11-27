package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;

/**
 * ExceptionPipe Tester.
 *
 * @author <Sina Sen>
 */

public class ExceptionPipeTest extends PipeTestBase<ExceptionPipe> {

	@Override
	public ExceptionPipe createPipe() {
		var pipe = new ExceptionPipe();

		pipe.addForward(new PipeForward("success", "success"));

		return pipe;
	}

	@Test
	public void testDoesntThrowException() throws Exception {
		pipe.setThrowException(false);
		pipe.configure();

		PipeRunResult result = doPipe(pipe, new Message("no exception"), session);
		assertEquals("no exception", result.getResult().asString());
		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, result.getPipeForward().getName());
	}

	@Test
	public void throwsExceptionWithoutMessage() throws ConfigurationException {
		pipe.setThrowException(true);
		pipe.configure();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "", session));
		assertThat(e.getMessage(), Matchers.endsWith("ExceptionPipe under test"));
	}

	@Test
	public void throwsExceptionWithMessage() throws ConfigurationException {
		pipe.setThrowException(true);
		pipe.configure();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "exception thrown with a custom message", session));
		assertThat(e.getMessage(), Matchers.endsWith("exception thrown with a custom message"));
	}
}
