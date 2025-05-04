package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.frankframework.pipes.EchoPipe;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.MessageTestUtils.MessageType;
import org.frankframework.testutil.TestAppender;

public class CheckMessageSizePipeProcessorTest extends PipeProcessorTestBase {

	@Test
	void shouldLogMessageWhenMessageSizeTooLarge() throws Exception {
		configurePipeLine(pipeline -> {
			pipeline.setMessageSizeWarn("1kb");
		});

		createPipe(EchoPipe.class, "Echo pipe", "Exception pipe");

		Message binaryInput = MessageTestUtils.getMessage(MessageType.BINARY);

		try (TestAppender appender = TestAppender.newBuilder().build()) {
			Message result = processPipeLine(binaryInput);

			assertNotNull(result);
			assertTrue(appender.contains("input message size [25KiB] exceeds [1KiB]"));
			assertTrue(appender.contains("result message size [25KiB] exceeds [1KiB]"));
		}
	}

	@Test
	void shouldLogMessageWhenSizeStatisticsIsDisabled() throws Exception {
		configurePipeLine(pipeline -> {
			pipeline.setMessageSizeWarn("1kb");
		});

		createPipe(EchoPipe.class, "Echo pipe", "Exception pipe", pipe -> {
			pipe.setSizeStatistics(false);
		});

		Message binaryInput = MessageTestUtils.getMessage(MessageType.BINARY);

		try (TestAppender appender = TestAppender.newBuilder().build()) {
			Message result = processPipeLine(binaryInput);

			assertNotNull(result);
			assertTrue(appender.contains("input message size [25KiB] exceeds [1KiB]"));
			assertTrue(appender.contains("result message size [25KiB] exceeds [1KiB]"));
		}
	}

	@Test
	void shouldNotLogMessageWhenSizeSmallerThenAllowed() throws Exception {
		configurePipeLine(pipeline -> {
			pipeline.setMessageSizeWarn("1mb");
		});

		createPipe(EchoPipe.class, "Echo pipe", "Exception pipe");

		Message binaryInput = MessageTestUtils.getMessage(MessageType.BINARY);

		try (TestAppender appender = TestAppender.newBuilder().build()) {
			Message result = processPipeLine(binaryInput);

			assertNotNull(result);
			assertFalse(appender.contains(" exceeds [1MiB]"));
		}
	}

	@Test
	void shouldNotCauseExceptionsOrLogAnythingWhenNoSize() throws Exception {
		configurePipeLine(pipeline -> {
			pipeline.setMessageSizeWarn("1kb");
		});

		createPipe(EchoPipe.class, "Echo pipe", "Exception pipe");

		Message message = new Message("");
		message.getContext().put(MessageContext.METADATA_SIZE, -1L);

		try (TestAppender appender = TestAppender.newBuilder().build()) {
			Message result = processPipeLine(message);

			assertNotNull(result);
			assertFalse(appender.contains(" exceeds [1KiB]"));
		}
	}
}
