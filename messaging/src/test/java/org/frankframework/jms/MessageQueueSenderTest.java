package org.frankframework.jms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Map;

import jakarta.jms.ObjectMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mockrunner.mock.jms.MockQueue;

import org.frankframework.core.PipeLineSession;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.mock.MockRunnerConnectionFactoryFactory;
import org.frankframework.util.CloseUtils;

class MessageQueueSenderTest {
	private TestConfiguration configuration;
	private MockQueue mockQueue;

	private MessageQueueSender sender;
	private PipeLineSession senderSession;

	@BeforeEach
	void setUp() throws Exception {
		configuration = new TestConfiguration(false);

		senderSession = new PipeLineSession();

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
	}

	@AfterEach
	void tearDown() {
		configuration.stop();
		CloseUtils.closeSilently(senderSession, configuration);
	}

	@Test
	void testSendMessage() throws Exception {
		// Arrange

		// Session contains keys A, C, D. Session keys copied by Sender will be A, B, C
		senderSession.put("A", "valueA");
		senderSession.put("c", "valueC");
		senderSession.put("d", "valueD");
		senderSession.put(PipeLineSession.CORRELATION_ID_KEY, "myCorrelationID");

		Message input = Message.asMessage("data");

		// Act
		sender.sendMessage(input, senderSession);

		// Assert
		jakarta.jms.Message message = mockQueue.getMessage();

		ObjectMessage objectMessage = assertInstanceOf(ObjectMessage.class, message);
		Object object = objectMessage.getObject();

		MessageWrapper<?> messageWrapper = assertInstanceOf(MessageWrapper.class, object);
		Message result = messageWrapper.getMessage();

		assertEquals("data", result.asString());
		assertThat(messageWrapper.getContext())
				.contains(Map.entry("A", "valueA"))
				.contains(Map.entry("c" ,"valueC"))
				.doesNotContainKey("B")
				.doesNotContainKey("d")
				.contains(Map.entry(PipeLineSession.CORRELATION_ID_KEY, "myCorrelationID"));
	}
}
