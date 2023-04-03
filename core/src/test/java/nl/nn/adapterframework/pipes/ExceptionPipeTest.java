package nl.nn.adapterframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.hamcrest.Matchers;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.stream.Message;

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
	public void throwsExceptionWithoutMessage() throws Exception {
		pipe.setThrowException(true);

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "", session));
		assertThat(e.getMessage(), Matchers.endsWith("ExceptionPipe under test"));
	}

	@Test
	public void throwsExceptionWithMessage() throws Exception {
		pipe.setThrowException(true);

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "exception thrown with a custom message", session));
		assertThat(e.getMessage(), Matchers.endsWith("exception thrown with a custom message"));

	}

}
