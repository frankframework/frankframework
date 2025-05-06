package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.AbstractPipe;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.pipes.ExceptionPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.DateFormatUtils;

public class ExceptionHandlingPipeProcessorTest extends PipeProcessorTestBase {

	@Test
	void piperunExceptionWithoutExceptionForward() throws Exception {
		createPipe(EchoPipe.class, "Echo pipe", "Exception pipe");
		createPipe(ThrowExceptionPipe.class, "Exception pipe", "success");

		PipeRunException ex = assertThrows(PipeRunException.class, () -> processPipeLine(new Message("PipeRun")));

		assertEquals("Pipe [Exception pipe] PipeRun", ex.getMessage());
	}

	@Test
	void runtimeExceptionWithoutExceptionForward() throws Exception {
		createPipe(EchoPipe.class, "Echo pipe", "Exception pipe");
		createPipe(ThrowExceptionPipe.class, "Exception pipe", "success");

		PipeRunException ex = assertThrows(PipeRunException.class, () -> processPipeLine(new Message("Runtime")));

		assertEquals("Pipe [Exception pipe] uncaught runtime exception while executing pipe: (RuntimeException) Runtime", ex.getMessage());
	}

	@Test
	void exceptionWithExceptionForward() throws Exception {
		createPipe(EchoPipe.class, "Echo pipe", "Exception pipe");
		createPipe(ExceptionPipe.class, "Exception pipe", "success", pipe -> {
			// There is an exception forward, but since it's the ExceptionPipe it should directly throw
			PipeForward exceptionForward = new PipeForward("exception", "exit");
			pipe.addForward(exceptionForward);
		});

		// Should still trigger an exception as the ExceptionPipe should be ignored!
		PipeRunException ex = assertThrows(PipeRunException.class, () -> processPipeLine(new Message("i'm an exception!")));

		assertEquals("Pipe [Exception pipe] i'm an exception!", ex.getMessage());
	}

	@Test
	void testWithPipeRunExceptionExitAndNormalPipe() throws Exception {
		createPipe(EchoPipe.class, "Echo pipe", "Exception pipe");
		createPipe(ThrowExceptionPipe.class, "Exception pipe", "success", pipe -> {
			// There is an exception forward, but since it's the ExceptionPipe it should catch and convert
			PipeForward exceptionForward = new PipeForward("exception", "exit");
			pipe.addForward(exceptionForward);
		});

		Message result = processPipeLine(new Message("PipeRun"));

		assertEquals("""
				<errorMessage timestamp="IGNORE" originator="IAF " message="ThrowExceptionPipe [Exception pipe] msgId [fake-mid]">
					<location class="org.frankframework.processors.ExceptionHandlingPipeProcessorTest$ThrowExceptionPipe" name="Exception pipe"/>
					<originalMessage messageId="fake-mid">PipeRun</originalMessage>
				</errorMessage>""", result.asString().replaceAll("timestamp=\"[A-Za-z0-9: ]+\"", "timestamp=\"IGNORE\""));
	}

	@Test
	void testWithPipeRunExceptionExitAndNormalPipeAndTsReceived() throws Exception {
		Instant tsReceived = Instant.ofEpochMilli(1234567890L);
		session.put(PipeLineSession.TS_RECEIVED_KEY, DateFormatUtils.format(tsReceived, DateFormatUtils.FULL_GENERIC_FORMATTER));
		createPipe(EchoPipe.class, "Echo pipe", "Exception pipe");
		createPipe(ThrowExceptionPipe.class, "Exception pipe", "success", pipe -> {
			PipeForward exceptionForward = new PipeForward("exception", "exit");
			pipe.addForward(exceptionForward);
		});

		Message result = processPipeLine(new Message("PipeRun"));

		assertEquals("""
				<errorMessage timestamp="IGNORE" originator="IAF " message="ThrowExceptionPipe [Exception pipe] msgId [fake-mid]">
					<location class="org.frankframework.processors.ExceptionHandlingPipeProcessorTest$ThrowExceptionPipe" name="Exception pipe"/>
					<originalMessage messageId="fake-mid" receivedTime="Thu Jan 15 07:56:07 CET 1970">PipeRun</originalMessage>
				</errorMessage>""", result.asString().replaceAll("timestamp=\"[A-Za-z0-9: ]+\"", "timestamp=\"IGNORE\""));
	}

	@Test
	void testWithRuntimeExceptionExitAndNormalPipe() throws Exception {
		createPipe(EchoPipe.class, "Echo pipe", "Exception pipe");
		createPipe(ThrowExceptionPipe.class, "Exception pipe", "success", pipe -> {
			PipeForward exceptionForward = new PipeForward("exception", "exit");
			pipe.addForward(exceptionForward);
		});

		String pipelineResult = processPipeLine(new Message("Runtime")).asString();
		Pattern pattern = Pattern.compile("<details>.*?</details>", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(pipelineResult);
		String cleaned = matcher.replaceAll("<details>...</details>");
		String result = cleaned.replaceAll("timestamp=\"[A-Za-z0-9: ]+\"", "timestamp=\"IGNORE\"");

		assertEquals("""
				<errorMessage timestamp="IGNORE" originator="IAF " message="Adapter [Adapter Name] msgId [fake-mid]: Runtime">
					<location class="org.frankframework.core.Adapter" name="Adapter Name"/>
					<details>...</details>
					<originalMessage messageId="fake-mid">Runtime</originalMessage>
				</errorMessage>""", result);
	}

	private static class ThrowExceptionPipe extends AbstractPipe {

		@Override
		public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
			try {
				String messageString = message.asString();
				if ("PipeRun".equals(messageString)) {
					throw new PipeRunException(this, messageString);
				}
				throw new RuntimeException(messageString);
			} catch (IOException e) {
				// should never happen...
				throw new PipeRunException(this, "cannot open stream", e);
			}
		}
	}
}
