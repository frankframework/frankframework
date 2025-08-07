package org.frankframework.messaging.amqp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.Delivery;
import org.apache.qpid.protonj2.client.Receiver;
import org.apache.qpid.protonj2.client.ReceiverOptions;
import org.apache.qpid.protonj2.client.Sender;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.extensions.messaging.MessageProtocol;
import org.frankframework.jdbc.datasource.ResourceObjectLocator;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

@Log4j2
public abstract class AmqpSenderTest {
	private static final String EXCHANGE_NAME = "test:testExchange";
	private AmqpConnectionFactoryFactory factory;
	private AmqpSender sender;
	private PipeLineSession session;

	@BeforeEach
	void setUp() throws Exception {
		factory = createAmqpConnectionFactory();
		sender = new AmqpSender();
		sender.setAddress(AmqpSenderTest.EXCHANGE_NAME);
		sender.setConnectionName(getResourceName());
		sender.setConnectionFactoryFactory(factory);

		session = new PipeLineSession();
	}

	protected AmqpConnectionFactoryFactory createAmqpConnectionFactory() throws Exception {

		System.setProperty("amqp.host", getHost());
		System.setProperty("amqp.port", getAmqpPort().toString());

		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("amqpResources.yml");
		locator.afterPropertiesSet();

		AmqpConnectionFactoryFactory connectionFactory = new AmqpConnectionFactoryFactory();
		connectionFactory.setObjectLocators(List.of(locator));
		connectionFactory.afterPropertiesSet();

		return connectionFactory;
	}

	protected abstract @Nonnull String getResourceName();

	protected abstract @Nonnull Integer getAmqpPort();

	protected abstract @Nonnull String getHost();

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(session);
		sender.stop();
		try {
			factory.destroy();
		} catch (Exception e) {
			log.warn("Failed to destroy the connection factory", e);
		}
	}

	@AfterAll
	static void tearDownClass() {
//		await().atMost(1, TimeUnit.DAYS).until(() -> false);
		System.clearProperty("amqp.host");
		System.clearProperty("amqp.port");
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void sendMessageNoReply(boolean sendStreaming) throws Exception {
		// Arrange
		sender.setStreamingMessages(sendStreaming);
		sender.setMessageProtocol(MessageProtocol.FF);
		sender.configure();
		sender.start();

		String requestValue = "test; sendStreaming=[%b] receiveStreaming=[false]".formatted(sendStreaming);
		Message message = new Message(requestValue);

		// Act
		SenderResult senderResult = assertDoesNotThrow(() -> sender.sendMessage(message, session));

		// Assert
		assertTrue(senderResult.isSuccess());

		// Check message on queue
		Message result = getMessage(AmqpSenderTest.EXCHANGE_NAME, AddressType.QUEUE);

		assertNotNull(result);
		log.info(result);
		String r = result.asString();
		assertEquals(requestValue, r);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void sendMessageNoReplyReceiveStreaming(boolean sendStreaming) throws Exception {
		// Arrange
		sender.setStreamingMessages(sendStreaming);
		sender.setMessageProtocol(MessageProtocol.FF);

		// To receive a message as streaming, it has to be sent as binary (Data section).
		sender.setMessageType(AmqpSender.MessageType.BINARY);
		sender.configure();
		sender.start();

		String requestValue = "test; sendStreaming=[%b] receiveStreaming=[true]".formatted(sendStreaming);
		Message message = new Message(requestValue);

		// Act
		SenderResult senderResult = assertDoesNotThrow(() -> sender.sendMessage(message, session));

		// Assert
		assertTrue(senderResult.isSuccess());

		// Check for a message on queue
		Message result = getStreamingMessage(AmqpSenderTest.EXCHANGE_NAME, AddressType.QUEUE);

		assertNotNull(result);
		log.info(result);
		String r = result.asString();
		assertEquals(requestValue, r);
	}

	@Test
	void sendMessageRRDynamicReplyQueue() throws Exception {
		// Arrange
		sender.setStreamingMessages(false);
		sender.setMessageProtocol(MessageProtocol.RR);
		sender.configure();
		sender.start();

		Message message = new Message("test");

		CompletableFuture<Message> receivedRequest = startBackgroundRRReceiver(EXCHANGE_NAME, "my reply");

		// Act
		SenderResult senderResult = assertDoesNotThrow(() -> sender.sendMessage(message, session));

		// Assert
		assertTrue(senderResult.isSuccess());
		Message result = senderResult.getResult();
		assertNotNull(result);
		log.info(result);
		String r = result.asString();
		assertEquals("my reply", r);

		Message request = receivedRequest.get(1, TimeUnit.MINUTES);
		assertNotNull(request);
		log.info(request);
		String rr = request.asString();
		assertEquals("test", rr);
	}

	protected @Nonnull CompletableFuture<Message> startBackgroundRRReceiver(String rrQueue, String replyToSend) {

		// Start a RR - receiver in the background to receive a message and send a reply
		CompletableFuture<Message> future = new CompletableFuture<>();

		return future.completeAsync(() -> {
			ReceiverOptions receiverOptions = new ReceiverOptions();
			receiverOptions.sourceOptions().capabilities("queue");
			try (Connection connection = factory.getConnectionFactory(getResourceName()).getConnection();
				 Receiver receiver = connection.openReceiver(rrQueue, receiverOptions)) {
				Delivery request = receiver.receive(60, TimeUnit.SECONDS);
				if (request != null) {
					org.apache.qpid.protonj2.client.Message<Object> received = request.message();
					log.info("Received message with body: " + received.body());
					Message ffRequest = Amqp1Helper.convertAmqpMessageToFFMessage(received);
					log.info("Received message with body: " + ffRequest);
					String replyAddress = received.replyTo();
					log.info("Sending reply to: " + replyAddress);
					if (replyAddress != null) {
						Sender amqpSender = connection.openSender(replyAddress);
						amqpSender.send(org.apache.qpid.protonj2.client.Message.create(replyToSend));
					}
					return ffRequest;
				} else {
					log.warn("Failed to read a message during the defined wait interval.");
					future.completeExceptionally(new TimeoutException("Did not receive request-message within 60 seconds."));
				}
			} catch (RuntimeException | ClientException | IOException e) {
				log.warn("Exception receiving message or sending reply", e);
				future.completeExceptionally(e);
			}
			return null;
		});
	}

	protected @Nullable Message getMessage(@Nonnull String address, @Nonnull AddressType addressType) throws ClientException, IOException {
		return Amqp1Helper.getMessage(factory, getResourceName() , address, addressType);
	}

	protected @Nullable Message getStreamingMessage(String address, @Nonnull AddressType addressType) throws ClientException, IOException {
		return Amqp1Helper.getStreamingMessage(factory, getResourceName(), address, addressType);
	}
}
