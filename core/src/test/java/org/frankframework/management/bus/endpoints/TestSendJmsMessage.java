package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;

import org.frankframework.jms.JMSFacade;
import org.frankframework.jms.JMSFacade.DestinationType;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.stream.Message;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.mock.MockRunnerConnectionFactoryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
public class TestSendJmsMessage extends BusTestBase {

	public static final String DUMMY_DESTINATION = "dummyDestination";

	@Test
	public void noConnectionFactory() {
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
	public void noDestination() {
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
	public void noDestinationType() {
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

		jakarta.jms.Message jmsResponse = mockConnectionFactoryFactory.getLastMessageFromQueue(DUMMY_DESTINATION);
		assertNotNull(jmsResponse, "expected a response");
		assertTrue(jmsResponse instanceof jakarta.jms.BytesMessage);
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

		jakarta.jms.Message jmsResponse = mockConnectionFactoryFactory.getLastMessageFromQueue(DUMMY_DESTINATION);
		assertNotNull(jmsResponse, "expected a response");
		assertTrue(jmsResponse instanceof jakarta.jms.TextMessage);
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

		jakarta.jms.Message jmsResponse = mockConnectionFactoryFactory.getLastMessageFromQueue(DUMMY_DESTINATION);
		assertNotNull(jmsResponse, "expected a response");
		assertTrue(jmsResponse instanceof jakarta.jms.TextMessage);
		String responseMessage = ((jakarta.jms.TextMessage) jmsResponse).getText();
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

		jakarta.jms.Message jmsResponse = mockConnectionFactoryFactory.getLastMessageFromQueue(DUMMY_DESTINATION);
		assertNotNull(jmsResponse, "expected a response");
		assertTrue(jmsResponse instanceof jakarta.jms.TextMessage);
		String responseMessage = ((jakarta.jms.TextMessage) jmsResponse).getText();
		assertEquals(payload, responseMessage);
	}
}
