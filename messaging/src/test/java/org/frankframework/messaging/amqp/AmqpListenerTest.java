package org.frankframework.messaging.amqp;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.ListenerException;
import org.frankframework.extensions.messaging.MessageProtocol;
import org.frankframework.jdbc.datasource.ResourceObjectLocator;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.stream.Message;

@Log4j2
@Tag("slow")
abstract class AmqpListenerTest {
	private static final String QUEUE_EXCHANGE_NAME = "test:testQueueExchange";
	private static final String TOPIC_EXCHANGE_NAME = "test:testTopicExchange";
	private static final String DURABLE_TOPIC_EXCHANGE_NAME = "test:testDurableTopicExchange";
	private AmqpConnectionFactoryFactory factory;
	private AmqpListener listener;
	private AmqpListenerContainerManager containerManager;
	private ApplicationContext applicationContext;

	private final List<AmqpListenerContainer> containers = new ArrayList<>();
	private final List<Message> receivedMessages = new ArrayList<>();

	@BeforeEach
	void setUp() throws Exception {
		factory = createAmqpConnectionFactory();

		AutowireCapableBeanFactory beanFactory = mock();
		when(beanFactory.createBean(AmqpListenerContainer.class)).thenAnswer(
				invocation -> {
					AmqpListenerContainer listenerContainer = new AmqpListenerContainer();
					listenerContainer.setConnectionFactoryFactory(factory);
					listenerContainer.setTaskExecutor(new SimpleAsyncTaskExecutor("AmqpListenerContainer"));
					containers.add(listenerContainer);
					return listenerContainer;
				}
		);
		applicationContext = mock();
		when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(beanFactory);

		containerManager = new AmqpListenerContainerManager();
		containerManager.setApplicationContext(applicationContext);

		this.listener = createAmqpListener();
	}

	@Nonnull
	private AmqpListener createAmqpListener() throws ListenerException {
		AmqpListener amqpListener = new AmqpListener();
		amqpListener.setConnectionName(getResourceName());
		amqpListener.setApplicationContext(applicationContext);
		amqpListener.setListenerContainerManager(containerManager);

		IMessageHandler<org.apache.qpid.protonj2.client.Message<?>> messageHandler = mock();
		amqpListener.setHandler(messageHandler);
		when(messageHandler.processRequest(any(), any(), any())).thenAnswer(
				invocation -> {
					MessageWrapper<?> messageWrapper = invocation.getArgument(1);
					Message message = messageWrapper.getMessage();
					receivedMessages.add(message);
					return message;
				}
		);
		return amqpListener;
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
		// Close all listener-containers first, then wait to make sure each of
		// them is closed
		containers.forEach(container->{
			container.closeAllListeners(null);
		});
		containers.forEach(container->{
			await().atMost(10, TimeUnit.SECONDS)
							.until(() -> !container.isOpen());
		});
		containers.clear();
		receivedMessages.clear();
		try {
			factory.destroy();
		} catch (Exception e) {
			log.warn("Failed to destroy the connection factory", e);
		}
	}

	@Test
	void testStart() throws Exception {
		// Arrange
		listener.setDurable(false);
		listener.setAddress(QUEUE_EXCHANGE_NAME);
		listener.setMessageProtocol(MessageProtocol.FF);
		listener.setName("testListener testStart");
		listener.configure();

		// Act
		listener.start();

		// Assert
		assertEquals(1, containers.size());
	}

	@Test
	void testListenFFQueueReceiveText() throws Exception {
		// Arrange
		String addressToUse = QUEUE_EXCHANGE_NAME + "_1";
		listener.setDurable(false);
		listener.setAddress(addressToUse);
		listener.setMessageProtocol(MessageProtocol.FF);
		listener.setName("testListener testListenFFQueueReceiveText");
		listener.configure();
		listener.start();

		// Act
		Amqp1Helper.sendFFMessage(factory, getResourceName(), addressToUse, AddressType.QUEUE, new Message("test"));

		// Assert
		await().atMost(10, TimeUnit.SECONDS)
				.until(() -> receivedMessages.size() == 1);

		Message message = receivedMessages.get(0);
		assertNotNull(message);
		assertFalse(message.isBinary(), "Expected text message");
		assertEquals("test", message.asString());
	}

	@Test
	void testListenFFQueueReceiveBinary() throws Exception {
		// Arrange
		listener.setDurable(false);
		String addressToUse = QUEUE_EXCHANGE_NAME + "_2";
		listener.setAddress(addressToUse);
		listener.setMessageProtocol(MessageProtocol.FF);
		listener.setName("testListener testListenFFQueueReceiveBinary");
		listener.configure();
		listener.start();

		// Act
		Amqp1Helper.sendFFMessage(factory, getResourceName(), addressToUse, AddressType.QUEUE, new Message("test".getBytes()));

		// Assert
		await().atMost(10, TimeUnit.SECONDS)
				.until(() -> receivedMessages.size() == 1);

		Message message = receivedMessages.get(0);
		assertNotNull(message);
		assertTrue(message.isBinary(), "Expected binary message");
		assertEquals("test", message.asString());
	}

	@Test
	void testListenFFTopicReceiveText() throws Exception {
		// Arrange
		String addressToUse = TOPIC_EXCHANGE_NAME + "_1";

		// Now setup and start the listener
		listener.setDurable(false);
		listener.setAddressType(AddressType.TOPIC);
		listener.setAddress(addressToUse);
		listener.setMessageProtocol(MessageProtocol.FF);
		listener.setName("testListener testListenFFTopicReceiveText");
		listener.configure();
		listener.start();

		// Act
		Amqp1Helper.sendFFMessage(factory, getResourceName(), addressToUse, AddressType.TOPIC, new Message("test"));

		// Assert
		await().atMost(10, TimeUnit.SECONDS)
				.until(() -> receivedMessages.size() == 1);

		Message message = receivedMessages.get(0);
		assertNotNull(message);
		assertFalse(message.isBinary(), "Expected text message");
		assertEquals("test", message.asString());
	}

	@Test
	void testListenFFTopicReceiveBinary() throws Exception {
		// Arrange
		String addressToUse = TOPIC_EXCHANGE_NAME + "_2";

		// Now setup and start the listener
		listener.setDurable(false);
		listener.setAddressType(AddressType.TOPIC);
		listener.setAddress(addressToUse);
		listener.setSubscriptionName("test-listener-2");
		listener.setMessageProtocol(MessageProtocol.FF);
		listener.setName("testListener testListenFFTopicReceiveBinary");
		listener.configure();
		listener.start();

		// Act
		Amqp1Helper.sendFFMessage(factory, getResourceName(), addressToUse, AddressType.TOPIC, new Message("test".getBytes()));

		// Assert
		await().atMost(10, TimeUnit.SECONDS)
				.until(() -> receivedMessages.size() == 1);

		Message message = receivedMessages.get(0);
		assertNotNull(message);
		assertTrue(message.isBinary(), "Expected binary message");
		assertEquals("test", message.asString());
	}


	@Test
	void testListenFFDurableTopicReceiveText() throws Exception {
		// Arrange
		String addressToUse = DURABLE_TOPIC_EXCHANGE_NAME + "_1";

		// For ActiveMQ test to pass, we need to send message to the durable topic (thus creating it) before we start listening
		if (getResourceName().equals("ActiveMQ")) {
			Amqp1Helper.sendFFMessage(factory, getResourceName(), addressToUse, AddressType.TOPIC, new Message("test"));
		}

		// Now setup and start the listener
		listener.setDurable(true);
		listener.setAddress(addressToUse);
		listener.setSubscriptionName("test-listener-1");
		listener.setMessageProtocol(MessageProtocol.FF);
		listener.setName("testListener testListenFFTopicReceiveText");
		listener.configure();
		listener.start();

		// Act
		if (!getResourceName().equals("ActiveMQ")) {
			Amqp1Helper.sendFFMessage(factory, getResourceName(), addressToUse, AddressType.TOPIC, new Message("test"));
		}

		// Assert
		await().atMost(10, TimeUnit.SECONDS)
				.until(() -> receivedMessages.size() == 1);

		Message message = receivedMessages.get(0);
		assertNotNull(message);
		assertFalse(message.isBinary(), "Expected text message");
		assertEquals("test", message.asString());
	}

	@Test
	void testListenFFDurableTopicReceiveBinary() throws Exception {
		// Arrange
		String addressToUse = DURABLE_TOPIC_EXCHANGE_NAME + "_2";

		// For ActiveMQ test to pass, we need to send message to the durable topic (thus creating it) before we start listening
		if (getResourceName().equals("ActiveMQ")) {
			Amqp1Helper.sendFFMessage(factory, getResourceName(), addressToUse, AddressType.TOPIC, new Message("test".getBytes()));
		}

		// Now setup and start the listener
		listener.setDurable(true);
		listener.setAddress(addressToUse);
		listener.setSubscriptionName("test-listener-2");
		listener.setMessageProtocol(MessageProtocol.FF);
		listener.setName("testListener testListenFFTopicReceiveBinary");
		listener.configure();
		listener.start();

		// Act
		if (!getResourceName().equals("ActiveMQ")) {
			Amqp1Helper.sendFFMessage(factory, getResourceName(), addressToUse, AddressType.TOPIC, new Message("test".getBytes()));
		}

		// Assert
		await().atMost(10, TimeUnit.SECONDS)
				.until(() -> receivedMessages.size() == 1);

		Message message = receivedMessages.get(0);
		assertNotNull(message);
		assertTrue(message.isBinary(), "Expected binary message");
		assertEquals("test", message.asString());
	}

	@Test
	void testManyListenersOnSameConnection() throws Exception {
		// Arrange
		startListener(listener, AddressType.QUEUE, QUEUE_EXCHANGE_NAME, "-1");

		AmqpListener listener1 = createAmqpListener();
		startListener(listener1, AddressType.QUEUE, QUEUE_EXCHANGE_NAME, "-2");

		AmqpListener listener2 = createAmqpListener();
		startListener(listener2, AddressType.TOPIC, TOPIC_EXCHANGE_NAME, "-1");

		// Act 1
		Amqp1Helper.sendFFMessage(factory, getResourceName(), TOPIC_EXCHANGE_NAME + "-1", AddressType.TOPIC, new Message("topic message 1"));

		// Assert 1
		await().atMost(10, TimeUnit.SECONDS)
				.until(() -> receivedMessages.size() == 1);

		Message message = receivedMessages.get(0);
		assertEquals("topic message 1", message.asString());

		receivedMessages.clear();

		// Act 2

		for (int i = 1; i <= 10; i++) {
			Amqp1Helper.sendFFMessage(factory, getResourceName(), QUEUE_EXCHANGE_NAME + "-1", AddressType.QUEUE, new Message("test q1 " + i));
			Amqp1Helper.sendFFMessage(factory, getResourceName(), QUEUE_EXCHANGE_NAME + "-2", AddressType.QUEUE, new Message("test q2 " + i));
			Amqp1Helper.sendFFMessage(factory, getResourceName(), TOPIC_EXCHANGE_NAME + "-1", AddressType.TOPIC, new Message("test t1 " + i));
		}

		// Assert 2
		await().atMost(10, TimeUnit.SECONDS)
				.until(() -> receivedMessages.size() == 30);
	}

	private static void startListener(AmqpListener amqpListener, AddressType addressType, String baseAddress, String suffix) throws ConfigurationException {
		amqpListener.setAddressType(addressType);
		amqpListener.setAddress(baseAddress + suffix);
		amqpListener.setName(baseAddress + suffix);
		amqpListener.setMaxThreadCount(2);
		amqpListener.configure();
		amqpListener.start();
	}

	// TODO: Test with multiple threads per listener
	// TODO: Test with Request-Reply
	// TODO: Test with multiple connection names (which creates multiple listener-containers)
}
