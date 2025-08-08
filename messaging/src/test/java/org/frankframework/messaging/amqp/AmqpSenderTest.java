package org.frankframework.messaging.amqp;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.extensions.messaging.MessageProtocol;
import org.frankframework.jdbc.datasource.ResourceObjectLocator;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.UUIDUtil;

@Log4j2
@Tag("slow")
public abstract class AmqpSenderTest {
	private static final String QUEUE_EXCHANGE_NAME = "test:testQueueExchange";
	private static final String TOPIC_EXCHANGE_NAME = "test:testTopicExchange";
	private AmqpConnectionFactoryFactory factory;
	private AmqpSender sender;
	private PipeLineSession session;

	@BeforeEach
	void setUp() throws Exception {
		factory = createAmqpConnectionFactory();
		sender = new AmqpSender();
		sender.setAddress(AmqpSenderTest.QUEUE_EXCHANGE_NAME);
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
	void sendMessageToQueueNoReply(boolean sendStreaming) throws Exception {
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
		Message result = getMessage(AmqpSenderTest.QUEUE_EXCHANGE_NAME, AddressType.QUEUE);

		assertNotNull(result);
		log.info(result);
		String r = result.asString();
		assertEquals(requestValue, r);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void sendMessageToQueueNoReplyReceiveStreaming(boolean sendStreaming) throws Exception {
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
		Message result = getStreamingMessage(AmqpSenderTest.QUEUE_EXCHANGE_NAME, AddressType.QUEUE);

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

		String testData = "test-" + UUIDUtil.createRandomUUID();
		Message message = new Message(testData);

		CompletableFuture<Message> receivedRequest = startBackgroundRRReceiver(QUEUE_EXCHANGE_NAME, "my reply");

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
		assertEquals(testData, rr);
	}

	@Test
	void sendMessageFFTopic() throws ConfigurationException, SenderException, TimeoutException {
		// Arrange
		sender.setMessageProtocol(MessageProtocol.FF);
		sender.setAddressType(AddressType.TOPIC);
		sender.setAddress(TOPIC_EXCHANGE_NAME);

		sender.configure();
		sender.start();

		String testData = "test-" + UUIDUtil.createRandomUUID();
		Message message = new Message(testData);

		int nrOfReceivers = 5;
		Phaser syncPoint  = new Phaser(nrOfReceivers + 1);
		List<Map.Entry<Integer, CompletableFuture<Message>>> futureResults = IntStream.range(0, nrOfReceivers)
				.mapToObj(i -> Map.entry(i, startBackgroundFFReceiver(syncPoint, i, AddressType.TOPIC, TOPIC_EXCHANGE_NAME)))
				.toList();

		// Wait until all receivers are open before sending the message
		syncPoint.arriveAndAwaitAdvance();

		// Act
		sender.sendMessage(message, session);

		// Assert
		List<Executable> resultAssertions = futureResults.stream()
				.map(entry -> Map.entry(entry.getKey(), entry.getValue().join()))
				.map(f -> (Executable) () -> assertEquals(testData, f.getValue().asString(), "Topic Receiver %d did not receive expected message; got '%s' instead of '%s".formatted(f.getKey(), f.getValue().asString(), testData)))
				.toList();

		assertAll(resultAssertions);
	}

	protected @Nonnull CompletableFuture<Message> startBackgroundFFReceiver(Phaser syncPoint, int receiverNr, AddressType addressType, String receiverAddress) {
		CompletableFuture<Message> future = new CompletableFuture<>();

		return future.completeAsync(() -> {
			ReceiverOptions receiverOptions = new ReceiverOptions();
			receiverOptions.sourceOptions().capabilities(addressType.getCapabilityName());
			try (Connection connection = factory.getConnectionFactory(getResourceName()).getConnection();
				 Receiver receiver = connection.openReceiver(receiverAddress, receiverOptions)) {
				syncPoint.arrive();
				Delivery request = receiver.receive(60, TimeUnit.SECONDS);
				if (request != null) {
					org.apache.qpid.protonj2.client.Message<Object> received = request.message();
					log.info("Receiver {}: Received message with body: {}", receiverNr, received.body());
					Message ffRequest = Amqp1Helper.convertAmqpMessageToFFMessage(received);
					log.info("Receiver {}: Delivery converted to Frank!Framework message: {}", receiverNr, ffRequest);
					return ffRequest;
				} else {
					log.warn("Receiver {} Failed to read a message within 60 seconds.", receiverNr);
					return Message.asMessage("Receiver %d failed to receive a message within 60 seconds".formatted(receiverNr));
				}
			} catch (RuntimeException | ClientException | IOException e) {
				log.warn(() -> "Receiver %d: Exception receiving message".formatted(receiverNr), e);
				return Message.asMessage("Receiver %d: Exception receiving message: %s".formatted(receiverNr, e.getMessage()));
			}
		});
	}

	protected @Nonnull CompletableFuture<Message> startBackgroundRRReceiver(String rrQueue, String replyToSend) {

		// Start a RR - receiver in the background to receive a message and send a reply
		CompletableFuture<Message> future = new CompletableFuture<>();

		return future.completeAsync(() -> {
			ReceiverOptions receiverOptions = new ReceiverOptions();
			receiverOptions.sourceOptions().capabilities(AddressType.QUEUE.getCapabilityName());
			try (Connection connection = factory.getConnectionFactory(getResourceName()).getConnection();
				 Receiver receiver = connection.openReceiver(rrQueue, receiverOptions)) {
				Delivery request = receiver.receive(60, TimeUnit.SECONDS);
				if (request != null) {
					org.apache.qpid.protonj2.client.Message<Object> received = request.message();
					log.info("Received message with body: {}", received.body());
					Message ffRequest = Amqp1Helper.convertAmqpMessageToFFMessage(received);
					log.info("Delivery converted to Frank!Framework message:: {}", ffRequest);
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
