package nl.nn.adapterframework.management.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;

import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.mock.ConnectionFactoryFactoryMock;

public class TestSendJmsMessage extends BusTestBase {

	@Test
	public void noConnectionFactory() throws Exception {
		String payload = "<dummy message=\"true\" />";
		MessageBuilder<String> request = createRequestMessage(payload, BusTopic.QUEUE, BusAction.UPLOAD);
		request.setHeader("destination", "dummyDestination");
		request.setHeader("type", DestinationType.QUEUE.name());

		try {
			callAsyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals("a connectionFactory must be provided", be.getMessage());
		}
	}

	@Test
	public void noDestination() throws Exception {
		String payload = "<dummy message=\"true\" />";
		MessageBuilder<String> request = createRequestMessage(payload, BusTopic.QUEUE, BusAction.UPLOAD);
		request.setHeader("connectionFactory", ConnectionFactoryFactoryMock.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("type", DestinationType.QUEUE.name());

		try {
			callAsyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals("a destination must be provided", be.getMessage());
		}
	}

	@Test
	public void noDestinationType() throws Exception {
		String payload = "<dummy message=\"true\" />";
		MessageBuilder<String> request = createRequestMessage(payload, BusTopic.QUEUE, BusAction.UPLOAD);
		request.setHeader("connectionFactory", ConnectionFactoryFactoryMock.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("destination", "dummyDestination");

		try {
			callAsyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals("a DestinationType must be provided", be.getMessage());
		}
	}

	@Test
	public void putInputStreamMessageOnQueue() throws Exception {
		Message payload = new Message("<dummy message=\"true\" />");
		MessageBuilder<InputStream> request = createRequestMessage(payload.asInputStream(), BusTopic.QUEUE, BusAction.UPLOAD);
		request.setHeader("connectionFactory", ConnectionFactoryFactoryMock.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("destination", "dummyDestination");
		request.setHeader("type", DestinationType.QUEUE.name());

		assertEquals(payload.asString(), callSyncGateway(request).getPayload());

		javax.jms.Message jmsResponse = ConnectionFactoryFactoryMock.getMessageHandler().receive();
		assertNotNull("expected a response", jmsResponse);
		assertTrue(jmsResponse instanceof javax.jms.TextMessage);
		String responseMessage = ((javax.jms.TextMessage) jmsResponse).getText();
		assertEquals(payload.asString(), responseMessage);
	}

	@Test
	public void putMessageOnQueueSynchronous() throws Exception {
		String payload = "<dummy message=\"true\" />";
		MessageBuilder<String> request = createRequestMessage(payload, BusTopic.QUEUE, BusAction.UPLOAD);
		request.setHeader("connectionFactory", ConnectionFactoryFactoryMock.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("destination", "dummyDestination");
		request.setHeader("type", DestinationType.QUEUE.name());

		assertEquals(payload, callSyncGateway(request).getPayload());

		javax.jms.Message jmsResponse = ConnectionFactoryFactoryMock.getMessageHandler().receive();
		assertNotNull("expected a response", jmsResponse);
		assertTrue(jmsResponse instanceof javax.jms.TextMessage);
		String responseMessage = ((javax.jms.TextMessage) jmsResponse).getText();
		assertEquals(payload, responseMessage);
	}

	@Test
	public void putMessageOnQueueAsynchronous() throws Exception {
		String payload = "<dummy message=\"true\" />";
		MessageBuilder<String> request = createRequestMessage(payload, BusTopic.QUEUE, BusAction.UPLOAD);
		request.setHeader("connectionFactory", ConnectionFactoryFactoryMock.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("destination", "dummyDestination");
		request.setHeader("type", DestinationType.QUEUE.name());

		callAsyncGateway(request);

		javax.jms.Message jmsResponse = ConnectionFactoryFactoryMock.getMessageHandler().receive();
		assertNotNull("expected a response", jmsResponse);
		assertTrue(jmsResponse instanceof javax.jms.TextMessage);
		String responseMessage = ((javax.jms.TextMessage) jmsResponse).getText();
		assertEquals(payload, responseMessage);
	}
}
