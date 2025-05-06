package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import org.frankframework.pipes.DelayPipe;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.MessageTestUtils.MessageType;
import org.frankframework.testutil.TestAppender;

/**
 * Most cases are already tested in the {@link ExceptionHandlingPipeProcessorTest}.
 */
public class MonitoringPipeProcessorTest extends PipeProcessorTestBase {

	@Test
	void processorShouldLogWhenDurationTooLong() throws Exception {
		createPipe(DelayPipe.class, "my pipe", "success", pipe -> {
			pipe.setDurationThreshold(1L);
			pipe.setDelayTime(20L);
		});

		Message binaryInput = MessageTestUtils.getMessage(MessageType.BINARY);

		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level %X{adapter,pipe} - %m").minLogLevel(Level.WARN).build()) {
			Message result = processPipeLine(binaryInput);

			assertNotNull(result);

			// Normally adapter is present in the ThreadContext, but this is not set by the pipe processors
			assertTrue(appender.contains("] ms exceeds maximum allowed threshold of [1]"));
		}
	}

	@Test
	void processorShouldNogLog() throws Exception {
		createPipe(EchoPipe.class, "my pipe", "success", pipe -> {
			pipe.setDurationThreshold(1000L);
			// EchoPipe should have next to no delay
		});

		Message binaryInput = MessageTestUtils.getMessage(MessageType.BINARY);

		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level %X{adapter,pipe} - %m").minLogLevel(Level.WARN).build()) {
			Message result = processPipeLine(binaryInput);

			assertNotNull(result);

			// Normally adapter is present in the ThreadContext, but this is not set by the pipe processors
			assertFalse(appender.contains("] ms exceeds maximum allowed threshold of [1000]"));
		}
	}
}
