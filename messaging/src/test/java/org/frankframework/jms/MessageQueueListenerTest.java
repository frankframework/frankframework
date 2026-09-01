package org.frankframework.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Map;
import java.util.stream.Stream;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.StreamMessage;
import jakarta.jms.TextMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.mockrunner.mock.jms.MockBytesMessage;
import com.mockrunner.mock.jms.MockObjectMessage;
import com.mockrunner.mock.jms.MockQueue;
import com.mockrunner.mock.jms.MockStreamMessage;
import com.mockrunner.mock.jms.MockTextMessage;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.mock.MockRunnerConnectionFactoryFactory;
import org.frankframework.util.CloseUtils;

class MessageQueueListenerTest {
	private TestConfiguration configuration;
	private MockQueue mockQueue;

	private MessageQueueListener listener;
	private PipeLineSession listenerSession;
	private Map<String, Object> threadContext;

	@BeforeEach
	void setUp() throws Exception {
		configuration = new TestConfiguration(false);

		listenerSession = new PipeLineSession();

		MockRunnerConnectionFactoryFactory mockFactory = new MockRunnerConnectionFactoryFactory();
		mockQueue = mockFactory.getDestinationManager().createQueue("TestQueue");

		listener = new MessageQueueListener();
		listener.setQueueConnectionFactoryName("mock");
		listener.setConnectionFactoryFactory(mockFactory);
		listener.setDestinationName("TestQueue");
		listener.setApplicationContext(configuration.getApplicationContext());

		listener.configure();
		listener.start();

		threadContext = listener.openThread();
	}

	@AfterEach
	void tearDown() {
		try {
			listener.closeThread(threadContext);
		} catch (ListenerException e) {
			// Ignore this exception
		}
		configuration.stop();
		CloseUtils.closeSilently(listenerSession, configuration);
	}

	public static Stream<Arguments> testReceiveMessage() throws JMSException {
		ObjectMessage objectMessage = new MockObjectMessage();
		objectMessage.setObject("data");

		BytesMessage bytesMessage = new MockBytesMessage();
		bytesMessage.writeBytes("data".getBytes());
		bytesMessage.reset();

		StreamMessage streamMessage = new MockStreamMessage();
		streamMessage.writeObject("data");
		streamMessage.reset();

		TextMessage textMessage = new MockTextMessage();
		textMessage.setText("data");

		return Stream.of(
				arguments(objectMessage),
				arguments(bytesMessage),
				arguments(streamMessage),
				arguments(textMessage)
		);
	}

	@ParameterizedTest
	@MethodSource
	void testReceiveMessage(jakarta.jms.Message message) throws Exception {
		// Arrange
		mockQueue.addMessage(message);

		// Act
		RawMessageWrapper<Message> rawMessage = listener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		org.frankframework.stream.Message result = listener.extractMessage(rawMessage, listenerSession);

		// Assert
		assertEquals("data", result.asString());
	}
}
