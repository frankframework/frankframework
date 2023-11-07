package nl.nn.adapterframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.jms.JMSFacade;
import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.mock.MockRunnerConnectionFactoryFactory;

public class TestSendJmsMessage extends BusTestBase {

	public static final String DUMMY_DESTINATION = "dummyDestination";

	@Test
	public void noConnectionFactory() throws Exception {
		String payload = "<dummy message=\"true\" />";
		MessageBuilder<String> request = createRequestMessage(payload, BusTopic.QUEUE, BusAction.UPLOAD);
		request.setHeader("destination", DUMMY_DESTINATION);
		request.setHeader("type", DestinationType.QUEUE.name());

		try {
			callAsyncGateway(request);
		} catch (Exception e) {
			assertInstanceOf(BusException.class, e.getCause());
			BusException be = (BusException) e.getCause();
			assertEquals("a connectionFactory must be provided", be.getMessage());
		}
	}

	@Test
	public void noDestination() throws Exception {
		String payload = "<dummy message=\"true\" />";
		MessageBuilder<String> request = createRequestMessage(payload, BusTopic.QUEUE, BusAction.UPLOAD);
		request.setHeader("connectionFactory", MockRunnerConnectionFactoryFactory.MOCK_CONNECTION_FACTORY_NAME);
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
		request.setHeader("connectionFactory", MockRunnerConnectionFactoryFactory.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("destination", DUMMY_DESTINATION);

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
		request.setHeader("connectionFactory", MockRunnerConnectionFactoryFactory.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("destination", DUMMY_DESTINATION);
		request.setHeader("type", DestinationType.QUEUE.name());

		mockConnectionFactoryFactory.addEchoReceiverOnQueue(DUMMY_DESTINATION);

		assertNotNull(callSyncGateway(request).getPayload());

		javax.jms.Message jmsResponse = mockConnectionFactoryFactory.getLastMessageFromQueue(DUMMY_DESTINATION);
		assertNotNull(jmsResponse, "expected a response");
		assertTrue(jmsResponse instanceof javax.jms.BytesMessage);
		String responseMessage = readBytesMessageToString((BytesMessage) jmsResponse);
		assertEquals(payload.asString(), responseMessage);
	}

	@Test
	public void putInputStreamMessageOnQueueSendTextMessage() throws Exception {
		Message payload = new Message("<dummy message=\"true\" />");
		MessageBuilder<InputStream> request = createRequestMessage(payload.asInputStream(), BusTopic.QUEUE, BusAction.UPLOAD);
		request.setHeader("connectionFactory", MockRunnerConnectionFactoryFactory.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("destination", DUMMY_DESTINATION);
		request.setHeader("type", DestinationType.QUEUE.name());
		request.setHeader("messageClass", JMSFacade.MessageClass.TEXT.name());

		mockConnectionFactoryFactory.addEchoReceiverOnQueue(DUMMY_DESTINATION);

		assertEquals(payload.asString(), callSyncGateway(request).getPayload());

		javax.jms.Message jmsResponse = mockConnectionFactoryFactory.getLastMessageFromQueue(DUMMY_DESTINATION);
		assertNotNull(jmsResponse, "expected a response");
		assertTrue(jmsResponse instanceof javax.jms.TextMessage);
		String responseMessage = ((TextMessage) jmsResponse).getText();
		assertEquals(payload.asString(), responseMessage);
	}

	private static String readBytesMessageToString(final BytesMessage jmsResponse) throws JMSException {
		byte[] data = new byte[(int)jmsResponse.getBodyLength()];
		jmsResponse.readBytes(data);
		return new String(data);
	}

	@Test
	public void putMessageOnQueueSynchronous() throws Exception {
		String payload = "<dummy message=\"true\" />";
		MessageBuilder<String> request = createRequestMessage(payload, BusTopic.QUEUE, BusAction.UPLOAD);
		request.setHeader("connectionFactory", MockRunnerConnectionFactoryFactory.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("destination", DUMMY_DESTINATION);
		request.setHeader("type", DestinationType.QUEUE.name());

		mockConnectionFactoryFactory.addEchoReceiverOnQueue(DUMMY_DESTINATION);

		assertEquals(payload, callSyncGateway(request).getPayload());

		javax.jms.Message jmsResponse = mockConnectionFactoryFactory.getLastMessageFromQueue(DUMMY_DESTINATION);
		assertNotNull(jmsResponse, "expected a response");
		assertTrue(jmsResponse instanceof javax.jms.TextMessage);
		String responseMessage = ((javax.jms.TextMessage) jmsResponse).getText();
		assertEquals(payload, responseMessage);
	}

	@Test
	public void putMessageOnQueueAsynchronous() throws Exception {
		String payload = "<dummy message=\"true\" />";
		MessageBuilder<String> request = createRequestMessage(payload, BusTopic.QUEUE, BusAction.UPLOAD);
		request.setHeader("connectionFactory", MockRunnerConnectionFactoryFactory.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("destination", DUMMY_DESTINATION);
		request.setHeader("type", DestinationType.QUEUE.name());

		callAsyncGateway(request);

		javax.jms.Message jmsResponse = mockConnectionFactoryFactory.getLastMessageFromQueue(DUMMY_DESTINATION);
		assertNotNull(jmsResponse, "expected a response");
		assertTrue(jmsResponse instanceof javax.jms.TextMessage);
		String responseMessage = ((javax.jms.TextMessage) jmsResponse).getText();
		assertEquals(payload, responseMessage);
	}
}
