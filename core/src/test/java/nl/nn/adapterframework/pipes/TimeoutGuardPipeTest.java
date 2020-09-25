package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.stream.Message;

public class TimeoutGuardPipeTest extends PipeTestBase<TimeoutGuardPipe> {
	private final static String SUCCESS_MESSAGE = "did not timeout!";

	public class GuardTestPipe extends TimeoutGuardPipe {
		public GuardTestPipe() {
			setTimeout(1); //set default on 1 second
		}

		@Override
		public PipeRunResult doPipeWithTimeoutGuarded(Message input, IPipeLineSession session) throws PipeRunException {
			try {
				long timeout = Long.parseLong(input.asString());
				Thread.sleep(timeout);
				return new PipeRunResult(getForward(), new Message(SUCCESS_MESSAGE));
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
	public void doesNotTimeout() throws Exception {
		configureAndStartPipe();
		Message input = new Message("500");
		PipeRunResult result = doPipe(input);

		assertEquals(SUCCESS_MESSAGE, result.getResult().asString());
	}

	@Test
	public void doesTimeoutWithException() throws Exception {
		configureAndStartPipe();
		Message input = new Message("1500");
		try {
			doPipe(input);

			fail("an exception should occur!");
		} catch(PipeRunException e) {
			assertTrue(e.getCause() instanceof TimeOutException);
		}
	}

	@Test
	public void doesTimeoutWithoutException() throws Exception {
		pipe.setThrowException(false);
		configureAndStartPipe();
		Message input = new Message("1500");
		PipeRunResult result = doPipe(input);

		String errorMessage = "<error><![CDATA[TimeOutException: exceeds timeout of [1] s, interupting]]></error>";
		assertEquals(errorMessage, result.getResult().asString());
	}
}
