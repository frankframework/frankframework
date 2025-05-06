package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.frankframework.pipes.EchoPipe;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.MessageTestUtils.MessageType;

/**
 * For now this simply tests the 2 processing flow's and not the {@link ResourceLimiter} it self.
 * Purely if all methods are called and if multiple messages may be processed.
 */
public class LimitingParallelExecutionPipeProcessorTest extends PipeProcessorTestBase {

	@Test
	void testNormalExecution() throws Exception {
		createPipe(EchoPipe.class, "my pipe", "success", pipe -> {
			pipe.setMaxThreads(0);
		});

		Message binaryInput = MessageTestUtils.getMessage(MessageType.BINARY);

		assertNotNull(processPipeLine(binaryInput));
		assertNotNull(processPipeLine(binaryInput));
		assertNotNull(processPipeLine(binaryInput));
	}

	@Test
	void testOneThreadExecution() throws Exception {
		createPipe(EchoPipe.class, "my pipe", "success", pipe -> {
			pipe.setMaxThreads(1);
		});

		Message binaryInput = MessageTestUtils.getMessage(MessageType.BINARY);

		assertNotNull(processPipeLine(binaryInput));
		assertNotNull(processPipeLine(binaryInput));
		assertNotNull(processPipeLine(binaryInput));
	}
}
