package org.frankframework.jms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mockrunner.mock.jms.MockQueue;

import org.frankframework.core.PipeLineSession;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.mock.MockRunnerConnectionFactoryFactory;
import org.frankframework.util.CloseUtils;

class MessageQueueSenderListenerTest {
	private TestConfiguration configuration;
	private MockQueue mockQueue;

	private MessageQueueSender sender;
	private MessageQueueListener listener;
	private PipeLineSession senderSession;
	private PipeLineSession listenerSession;

	@BeforeEach
	void setUp() throws Exception {
		configuration = new TestConfiguration(false);

		senderSession = new PipeLineSession();
		listenerSession = new PipeLineSession();

		MockRunnerConnectionFactoryFactory mockFactory = new MockRunnerConnectionFactoryFactory();
		mockQueue = mockFactory.getDestinationManager().createQueue("TestQueue");

		sender = new MessageQueueSender();
		sender.setQueueConnectionFactoryName("mock");
		sender.setConnectionFactoryFactory(mockFactory);
		sender.setDestinationName("TestQueue");
		sender.setConfigurationMetrics(configuration.getBean("configurationMetrics", MetricsInitializer.class));
		sender.setApplicationContext(configuration.getApplicationContext());
		sender.setSessionKeys("a,b,c");

		sender.configure();
		sender.start();

		listener = new MessageQueueListener();
		listener.setQueueConnectionFactoryName("mock");
		listener.setConnectionFactoryFactory(mockFactory);
		listener.setDestinationName("TestQueue");
		listener.setApplicationContext(configuration.getApplicationContext());

		listener.configure();
		listener.start();
	}

	@AfterEach
	void tearDown() {
		configuration.stop();
		CloseUtils.closeSilently(listenerSession, senderSession, configuration);
	}

	@Test
	void testSendReceiveMessage() throws Exception {
		// Arrange

		// Session contains keys A, C, D. Session keys copied by Sender will be A, B, C
		senderSession.put("A", "valueA");
		senderSession.put("c", "valueC");
		senderSession.put("d", "valueD");
		senderSession.put(PipeLineSession.CORRELATION_ID_KEY, "myCorrelationID");

		Message input = Message.asMessage("data".getBytes());

		Map<String, Object> threadContext = listener.openThread();

		// Act
		sender.sendMessage(input, senderSession);
		RawMessageWrapper<jakarta.jms.Message> rawMessage = listener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		Message result = listener.extractMessage(rawMessage, listenerSession);

		// Assert
		assertEquals("data", result.asString());
		assertThat(listenerSession)
				.contains(Map.entry("A", "valueA"))
				.contains(Map.entry("c" ,"valueC"))
				.doesNotContainKey("B")
				.doesNotContainKey("d")
				.contains(Map.entry(PipeLineSession.CORRELATION_ID_KEY, "myCorrelationID"));

		listener.closeThread(threadContext);
	}
}
