package nl.nn.adapterframework.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.senders.SenderTestBase;

class CollectionSenderTest extends SenderTestBase<CollectorSenderBase<TestCollector, TestCollectorPart>> {

	private final TestCollector collector = new TestCollector();

	@Override
	public CollectorSenderBase<TestCollector, TestCollectorPart> createSender() {
		return new CollectorSenderBase<TestCollector, TestCollectorPart>() {
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
