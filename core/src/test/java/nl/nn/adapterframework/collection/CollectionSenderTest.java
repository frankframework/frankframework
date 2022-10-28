package nl.nn.adapterframework.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import nl.nn.adapterframework.collection.CollectionActor.Action;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.stream.Message;

public class CollectionSenderTest extends SenderTestBase<CollectorSender> {

	@Override
	public CollectorSender createSender() throws ConfigurationException {
		return new CollectorSender() {

			@Override
			public Object openCollection(Message input, PipeLineSession session, ParameterValueList pvl) throws CollectionException {
				return new TestCollector(input, session, pvl);
			}
		};
	}


	@Test
	public void testOpen() throws Exception {
		sender.setAction(Action.OPEN);
		sender.configure();
		sender.open();

		String input = "testOpen";

		Message result = sendMessage(input);

		TestCollector collector = (TestCollector)session.get("collection");

		assertEquals(true, collector.open);
		assertEquals(input, collector.input.toString());
		assertEquals(session, collector.session);
	}

	@Test
	public void testClose() throws Exception {
		sender.setAction(Action.CLOSE);
		sender.configure();
		sender.open();

		TestCollector collector = new TestCollector();
		session.put("collection", collector);

		String input = "testClose";
		Message result = sendMessage(input);

		assertEquals(false, collector.open);
	}

	@Test
	public void testWrite() throws Exception {
		sender.setAction(Action.WRITE);
		sender.configure();
		sender.open();

		TestCollector collector = new TestCollector();
		session.put("collection", collector);

		String input = "testWrite";
		Message result = sendMessage(input);

		assertEquals(true, collector.open);
		assertEquals(input, collector.input.toString());
		assertEquals(session, collector.session);
		assertEquals(sender, collector.writingElement);
	}

	@Test
	public void testStream() throws Exception {
		sender.setAction(Action.STREAM);
		sender.configure();
		sender.open();

		TestCollector collector = new TestCollector();
		session.put("collection", collector);

		String input = "testStream";
		Message result = sendMessage(input);

		assertNotNull(result.asObject());
		assertEquals(collector.outputStream, result.asObject());

		assertEquals(true, collector.open);
		assertEquals(input, collector.input.toString());
		assertEquals(session, collector.session);
		assertEquals(sender, collector.writingElement);
	}

}
