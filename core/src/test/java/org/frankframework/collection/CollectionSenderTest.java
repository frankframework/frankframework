package org.frankframework.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.frankframework.core.PipeLineSession;
import org.frankframework.senders.SenderTestBase;
import org.junit.jupiter.api.Test;

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
		sender.open();

		String input = "testWrite";
		sendMessage(input);

		assertEquals(true, collector.open);
		assertEquals(input, collector.getInput());
	}
}
