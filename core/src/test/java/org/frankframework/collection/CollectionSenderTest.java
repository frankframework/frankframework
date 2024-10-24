package org.frankframework.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.senders.SenderTestBase;

class CollectionSenderTest extends SenderTestBase<CollectorSenderBase<TestCollector, TestCollectorPart>> {

	private final TestCollector collector = new TestCollector();

	@Override
	public CollectorSenderBase<TestCollector, TestCollectorPart> createSender() {
		return new CollectorSenderBase<>() {
			@Override
			protected Collection<TestCollector, TestCollectorPart> getCollection(PipeLineSession session) {
				return new Collection<>(collector);
			}
		};
	}

	@Test
	void testWrite() throws Exception {
		sender.configure();
		sender.start();

		String input = "testWrite";
		sendMessage(input);

		assertTrue(collector.open);
		assertEquals(input, collector.getInput());
	}
}
