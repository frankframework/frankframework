package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.AbstractPipe;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.receivers.ResourceLimiter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.MessageTestUtils.MessageType;
import org.frankframework.util.Locker;

/**
 * For now this simply tests the 2 processing flow's and not the {@link ResourceLimiter} it self.
 * Purely if all methods are called and if multiple messages may be processed.
 */
public class LockerPipeProcessorTest extends PipeProcessorTestBase {

	@Test
	void testNormalExecution() throws Exception {
		createPipe(EchoPipe.class, "my pipe", "success", pipe -> {
			pipe.setLocker(null);
		});

		Message binaryInput = MessageTestUtils.getMessage(MessageType.BINARY);

		assertNotNull(processPipeLine(binaryInput));
		assertNotNull(processPipeLine(binaryInput));
		assertNotNull(processPipeLine(binaryInput));
	}

	@Test
	void testWithLockerExecution() throws Exception {
		// Ensure only 1 process may execute at once, and that Locker.release is called
		Locker locker = mock(Locker.class);
		AtomicInteger guard = new AtomicInteger();
		doAnswer(e -> "id-" + guard.incrementAndGet()).when(locker).acquire();
		doAnswer(e -> guard.decrementAndGet()).when(locker).release(anyString());

		createPipe(EchoPipe.class, "my pipe", "success", pipe -> {
			pipe.setLocker(locker);
		});

		Message binaryInput = MessageTestUtils.getMessage(MessageType.BINARY);

		assertNotNull(processPipeLine(binaryInput));
		assertNotNull(processPipeLine(binaryInput));
		assertNotNull(processPipeLine(binaryInput));

		assertEquals(0, guard.get());
		verify(locker, times(3)).acquire();
		verify(locker, times(3)).release(anyString());
	}

	@Test
	void whenNoLockCanBeAquiredNoPipeExecution() throws Exception {
		Locker locker = mock(Locker.class);
		doThrow(IllegalStateException.class).when(locker).acquire();
		doReturn("name").when(locker).toString();

		createPipe(ThrowExceptionPipe.class, "my pipe", "success", pipe -> {
			pipe.setLocker(locker);
		});

		Message binaryInput = new Message("input");

		PipeRunException ex = assertThrows(PipeRunException.class, () -> processPipeLine(binaryInput));
		assertEquals("Pipe [my pipe] error while trying to obtain lock [name]: (IllegalStateException)", ex.getMessage());
	}

	@Test
	void whenEmptyLockCanBeAquiredNoPipeExecution() throws Exception {
		Locker locker = mock(Locker.class);
		doReturn(null).when(locker).acquire();
		doReturn("name").when(locker).toString();

		createPipe(ThrowExceptionPipe.class, "my pipe", "success", pipe -> {
			pipe.setLocker(locker);
		});

		Message binaryInput = new Message("input");

		PipeRunException ex = assertThrows(PipeRunException.class, () -> processPipeLine(binaryInput));
		assertEquals("Pipe [my pipe] could not obtain lock [name]", ex.getMessage());
	}

	private static class ThrowExceptionPipe extends AbstractPipe {

		@Override
		public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
			throw new PipeRunException(this, "this operation should not be called");
		}
	}
}
