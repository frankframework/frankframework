package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;

public class TimeoutGuardPipeTest extends PipeTestBase<TimeoutGuardPipe> {
	private static final String SUCCESS_MESSAGE = "did not timeout!";

	public class GuardTestPipe extends TimeoutGuardPipe {
		public GuardTestPipe() {
			setTimeout(1); //set default on 1 second
		}

		@Override
		public PipeRunResult doPipeWithTimeoutGuarded(Message input, PipeLineSession session) throws PipeRunException {
			try {
				long timeout = Long.parseLong(input.asString());
				Thread.sleep(timeout);
				return new PipeRunResult(getSuccessForward(), new Message(SUCCESS_MESSAGE));
			} catch (NumberFormatException | IOException e) {
				throw new PipeRunException(this, "error parsing input", e);
			} catch (InterruptedException e) {
				//Verify that pipe run thread has been aborted.
//				throw new PipeRunException(this, "timed out");
			}
			fail("this should not happen");
			throw new PipeRunException(this, "not aborted");
		}
	}

	@Override
	public GuardTestPipe createPipe() {
		return new GuardTestPipe();
	}

	@Test
	void doesNotTimeout() throws Exception {
		configureAndStartPipe();
		Message input = new Message("500");
		PipeRunResult result = doPipe(input);

		assertEquals(SUCCESS_MESSAGE, result.getResult().asString());
	}

	@Test
	void doesTimeoutWithException() throws Exception {
		configureAndStartPipe();
		Message input = new Message("1500");
		try {
			doPipe(input);

			fail("an exception should occur!");
		} catch(PipeRunException e) {
			assertTrue(e.getCause() instanceof TimeoutException);
		}
	}

	@Test
	void doesTimeoutWithoutException() throws Exception {
		pipe.setThrowException(false);
		configureAndStartPipe();
		Message input = new Message("1500");
		PipeRunResult result = doPipe(input);

		String errorMessage = "<error><![CDATA[TimeOutException: exceeds timeout of [1] s, interupting]]></error>";
		assertEquals(errorMessage, result.getResult().asString());
	}
}
