package org.frankframework.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.frankframework.collection.AbstractCollectorPipe.Action;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;

public class CollectorPipeTest extends PipeTestBase<AbstractCollectorPipe<TestCollector, TestCollectorPart>> {
	private final TestCollector collector = new TestCollector();

	@Override
	public AbstractCollectorPipe<TestCollector, TestCollectorPart> createPipe() throws ConfigurationException {
		return new AbstractCollectorPipe<>() {

			@Override
			protected TestCollector createCollector(Message input, PipeLineSession session) throws CollectionException {
				return collector;
			}

		};
	}

	@Test
	public void testOpen() throws Exception {
		pipe.setAction(Action.OPEN);
		configureAndStartPipe();

		PipeRunResult prr = doPipe("testOpen");

		assertEquals("success", prr.getPipeForward().getName());

		assertTrue(collector.open);
		assertEquals("", collector.getInput());
	}

	@Test
	public void testClose() throws Exception {
		pipe.setAction(Action.OPEN);
		pipe.doAction(Action.OPEN, null, session);
		pipe.setAction(Action.CLOSE);
		configureAndStartPipe();

		PipeRunResult prr = doPipe("testClose");

		assertEquals("success", prr.getPipeForward().getName());
		assertFalse(collector.open);
	}

	@Test
	public void testWrite() throws Exception {
		pipe.setAction(Action.OPEN);
		pipe.doAction(Action.OPEN, null, session);
		pipe.setAction(Action.WRITE);
		configureAndStartPipe();

		PipeRunResult prr = doPipe("testWrite");

		assertEquals("success", prr.getPipeForward().getName());
		assertEquals("testWrite", collector.getInput());
		assertNull(prr.getResult().asString());
		assertTrue(collector.open);
	}

	@Test
	public void testLast() throws Exception {
		pipe.setAction(Action.OPEN);
		pipe.doAction(Action.OPEN, null, session);
		pipe.setAction(Action.WRITE);
		configureAndStartPipe();

		PipeRunResult prr = doPipe("testWrite");

		assertEquals("success", prr.getPipeForward().getName());
		assertEquals("testWrite", collector.getInput());
		assertNull(prr.getResult().asString());
		assertTrue(collector.open);
	}
}
