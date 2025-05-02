package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.frankframework.pipes.EchoPipe;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.MessageTestUtils.MessageType;
import org.frankframework.testutil.TestAppender;

public class LogPipeProcessorTest extends PipeProcessorTestBase {

	@Test
	void testLogMessageAlwaysOnInfo() throws Exception {
		createPipe(EchoPipe.class, "pipe1", "pipe2", pipe -> {
			pipe.setLogIntermediaryResults("true"); // shouldn't this be a boolean?
		});
		createPipe(EchoPipe.class, "pipe2", "success", pipe -> {
			pipe.setLogIntermediaryResults("false"); // shouldn't this be a boolean?
		});

		Message binaryInput = MessageTestUtils.getMessage(MessageType.BINARY);

		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level %X{adapter,pipe} - %m").build()) {
			Message result = processPipeLine(binaryInput);

			assertNotNull(result);

			// Normally adapter is present in the ThreadContext, but this is not set by the pipe processors
			assertTrue(appender.contains("DEBUG {} - pipeline process is about to call pipe [pipe1] current result ["));
			assertTrue(appender.contains("INFO {} - pipeline process is about to call pipe [pipe2]"));
		}
	}
}
