package org.frankframework.amqp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderResult;
import org.frankframework.jdbc.datasource.ResourceObjectLocator;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

@Log4j2
public abstract class AmqpSenderTest {
	private static final String EXCHANGE_NAME = "testExchange";
	private AmqpConnectionFactory factory;
	private AmqpSender sender;
	private PipeLineSession session;

	@BeforeEach
	void setUp() throws Exception {
		factory = createAmqpConnectionFactory();
		sender = new AmqpSender();
		sender.setQueueName(AmqpSenderTest.EXCHANGE_NAME);
		sender.setConnectionName(getResourceName());
		sender.setConnectionFactory(factory);
		sender.configure();
		sender.start();

		session = new PipeLineSession();
	}

	protected AmqpConnectionFactory createAmqpConnectionFactory() throws Exception {

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
			factory.destroy();
		} catch (Exception e) {
			log.warn("Failed to destroy the connection factory", e);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void sendMessageNoReply(boolean sendStreaming) throws Exception {
		// Arrange
		sender.setSendStreaming(sendStreaming);
		Message message = new Message("test");

		// Act
		SenderResult senderResult = assertDoesNotThrow(() -> sender.sendMessage(message, session));

		// Assert
		assertTrue(senderResult.isSuccess());

		// Check message on queue
		Message result = getMessage(AmqpSenderTest.EXCHANGE_NAME);

		assertNotNull(result);
		log.info(result);
		String r = result.asString();
		assertEquals("test", r);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void sendMessageNoReplyReceiveStreaming(boolean sendStreaming) throws Exception {
		// Arrange
		sender.setSendStreaming(sendStreaming);
		Message message = new Message("test");

		// Act
		SenderResult senderResult = assertDoesNotThrow(() -> sender.sendMessage(message, session));

		// Assert
		assertTrue(senderResult.isSuccess());

		// Check message on queue
		Message result = getStreamingMessage(AmqpSenderTest.EXCHANGE_NAME);

		assertNotNull(result);
		log.info(result);
		String r = result.asString();
		assertEquals("test", r);
	}

	protected @Nullable Message getMessage(String queueName) throws ClientException, IOException {
		return Amqp1Helper.getMessage(factory, getResourceName(), queueName);
	}

	protected @Nullable Message getStreamingMessage(String queueName) throws ClientException, IOException {
		return Amqp1Helper.getStreamingMessage(factory, getResourceName(),  queueName);
	}
}
