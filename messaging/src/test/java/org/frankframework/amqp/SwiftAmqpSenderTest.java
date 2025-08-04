package org.frankframework.amqp;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.extensions.messaging.MessageProtocol;
import org.frankframework.jdbc.datasource.ResourceObjectLocator;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

@Log4j2
public abstract class SwiftAmqpSenderTest {
	private static final String EXCHANGE_NAME = "testExchange";
	private SwiftAmqpConnectionFactory swiftFactory;
	private AmqpConnectionFactory protonFactory;
	private SwiftAmqpSender sender;
	private PipeLineSession session;

	@BeforeEach
	void setUp() throws Exception {
		swiftFactory = createSwiftAmqpConnectionFactory();
		protonFactory = createProtonAmqpConnectionFactory();

		sender = new SwiftAmqpSender();
		sender.setQueueName(SwiftAmqpSenderTest.EXCHANGE_NAME);
		sender.setConnectionName(getResourceName());
		sender.setConnectionFactory(swiftFactory);

		session = new PipeLineSession();
	}

	protected SwiftAmqpConnectionFactory createSwiftAmqpConnectionFactory() throws Exception {

		System.setProperty("amqp.host", getHost());
		System.setProperty("amqp.port", getAmqpPort().toString());

		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("amqpResources.yml");
		locator.afterPropertiesSet();

		SwiftAmqpConnectionFactory factory = new SwiftAmqpConnectionFactory();
		factory.setObjectLocators(List.of(locator));
		factory.afterPropertiesSet();

		return factory;
	}

	protected AmqpConnectionFactory createProtonAmqpConnectionFactory() throws Exception {

		System.setProperty("amqp.host", getHost());
		System.setProperty("amqp.port", getAmqpPort().toString());

		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("amqpResources.yml");
		locator.afterPropertiesSet();

		AmqpConnectionFactory factory = new AmqpConnectionFactory();
		factory.setObjectLocators(List.of(locator));
		factory.afterPropertiesSet();

		return factory;
	}

	protected abstract @Nonnull String getResourceName();

	protected abstract @Nonnull Integer getAmqpPort();

	protected abstract @Nonnull String getHost();

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(session);
		sender.stop();
		System.clearProperty("amqp.host");
		System.clearProperty("amqp.port");
		try {
			swiftFactory.destroy();
		} catch (Exception e) {
			log.warn("Failed to destroy the connection factory", e);
		}
	}

	@Test
	void sendMessageNoReply() throws Exception {
		// Arrange
		sender.setMessageProtocol(MessageProtocol.FF);
		sender.configure();
		sender.start();

		String requestValue = "test; SwiftMQ";
		Message message = new Message(requestValue);

		// Act
		SenderResult senderResult = assertDoesNotThrow(() -> sender.sendMessage(message, session));

		// Assert
		assertTrue(senderResult.isSuccess());

		// Check message on queue
		Message result = getMessage(SwiftAmqpSenderTest.EXCHANGE_NAME);

		assertNotNull(result);
		log.info(result);
		String r = result.asString();
		assertEquals(requestValue, r);
	}

	@Test
	void sendMessageNoReplyReceiveStreaming() throws Exception {
		// Arrange
		sender.setMessageProtocol(MessageProtocol.FF);
		sender.configure();
		sender.start();

		String requestValue = "test;SwiftMQ";
		Message message = new Message(requestValue);

		// Act
		SenderResult senderResult = assertDoesNotThrow(() -> sender.sendMessage(message, session));

		// Assert
		assertTrue(senderResult.isSuccess());

		// Check message on queue
		Message result = getStreamingMessage(SwiftAmqpSenderTest.EXCHANGE_NAME);

		assertNotNull(result);
		log.info(result);
		String r = result.asString();
		assertEquals(requestValue, r);
	}

	@Test
	void sendMessageRR() throws Exception {
		// Arrange
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

		Message request = receivedRequest.get();
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
			try (Connection connection = protonFactory.getConnection(getResourceName());
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
			} catch (ClientException | IOException e) {
				future.completeExceptionally(e);
			}
			return null;
		});
	}

	protected @Nullable Message getMessage(String queueName) throws ClientException, IOException {
		return Amqp1Helper.getMessage(protonFactory, getResourceName(), queueName);
	}

	protected @Nullable Message getStreamingMessage(String queueName) throws ClientException, IOException {
		return Amqp1Helper.getStreamingMessage(protonFactory, getResourceName(),  queueName);
	}
}
