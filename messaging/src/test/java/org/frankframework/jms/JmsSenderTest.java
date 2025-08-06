package org.frankframework.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import jakarta.jms.BytesMessage;
import jakarta.jms.TextMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.mockrunner.mock.jms.MockQueue;

import org.frankframework.core.PipeLineSession;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.mock.MockRunnerConnectionFactoryFactory;
import org.frankframework.util.AppConstants;
import org.frankframework.util.EnumUtils;

class JmsSenderTest {
	private TestConfiguration configuration;
	private MockRunnerConnectionFactoryFactory mockFactory;
	private MockQueue mockQueue;

	private JmsSender jmsSender;

	private PipeLineSession pipeLineSession;

	@BeforeEach
	void setUp() throws Exception {
		configuration = new TestConfiguration(false);

		pipeLineSession = new PipeLineSession();
		jmsSender = new JmsSender();

		mockFactory = new MockRunnerConnectionFactoryFactory();

		mockQueue = mockFactory.getDestinationManager().createQueue("TestQueue");

		jmsSender.setQueueConnectionFactoryName("mock");
		jmsSender.setConnectionFactoryFactory(mockFactory);
		jmsSender.setDestinationName("TestQueue");
		jmsSender.setConfigurationMetrics(configuration.getBean("configurationMetrics", MetricsInitializer.class));
		jmsSender.setApplicationContext(configuration.getApplicationContext());

		jmsSender.configure();
		jmsSender.start();
	}

	@AfterEach
	void tearDown() {
		pipeLineSession.close();
		jmsSender.stop();
	}

	@Test
	void testJmsSenderCopiesDefaultMessageClass() {
		// Assure that for the test, the default message class is not "AUTO"
		JMSFacade.MessageClass defaultMessageClass = EnumUtils.parse(JMSFacade.MessageClass.class, AppConstants.getInstance().getProperty("jms.messageClass.default"));

		assertNotNull(defaultMessageClass);
		assertNotEquals(JMSFacade.MessageClass.AUTO, defaultMessageClass);

		// Assert that the sender has same messageClass as the default is
		assertEquals(defaultMessageClass, jmsSender.getMessageClass());
	}

	@Test
	void testSendMessageModeAutoWithTextMessage() throws Exception {
		// Arrange
		Message message = new Message("A Textual Message");
		jmsSender.setMessageClass(JMSFacade.MessageClass.AUTO);

		// Act
		jmsSender.sendMessage(message, pipeLineSession);

		// Assert
		jakarta.jms.Message jmsMessage = mockQueue.getMessage();
		assertNotNull(jmsMessage);
		assertInstanceOf(TextMessage.class, jmsMessage);

		TextMessage textMessage = (TextMessage) jmsMessage;
		assertEquals("A Textual Message", textMessage.getText());
	}

	static Stream<Arguments> testSendMessageModeAutoWithBinaryMessage() {
		return Stream.of(
				Arguments.of(JmsSenderTest.class.getResource("/Documents/utf8-with-bom.txt")),
				Arguments.of(JmsSenderTest.class.getResource("/Documents/iso-8859-1.txt")),
				Arguments.of(JmsSenderTest.class.getResource("/Documents/doc001.pdf"))
			);
	}

	@ParameterizedTest
	@MethodSource
	void testSendMessageModeAutoWithBinaryMessage(URL messageUrl) throws Exception {
		// Arrange
		Message message = MessageTestUtils.getMessage(messageUrl);
		jmsSender.setMessageClass(JMSFacade.MessageClass.AUTO);

		// Act
		jmsSender.sendMessage(message, pipeLineSession);

		// Assert
		jakarta.jms.Message jmsMessage = mockQueue.getMessage();
		assertNotNull(jmsMessage);
		assertInstanceOf(BytesMessage.class, jmsMessage);

		BytesMessage bytesMessage = (BytesMessage) jmsMessage;
		byte[] data = new byte[(int) bytesMessage.getBodyLength()];
		bytesMessage.readBytes(data);
		try (Message result = new Message(data)) {
			assertEquals(message.asString(), result.asString());
		}
	}

	@Test
	void testSendMessageModeBytesWithTextMessage() throws Exception {
		// Arrange
		Message message = new Message("A Textual Message");
		jmsSender.setMessageClass(JMSFacade.MessageClass.BYTES);

		// Act
		jmsSender.sendMessage(message, pipeLineSession);

		// Assert
		jakarta.jms.Message jmsMessage = mockQueue.getMessage();
		assertNotNull(jmsMessage);
		assertTrue(jmsMessage instanceof BytesMessage);

		BytesMessage bytesMessage = (BytesMessage) jmsMessage;
		byte[] data = new byte[(int) bytesMessage.getBodyLength()];
		bytesMessage.readBytes(data);
		assertEquals("A Textual Message", new String(data, StandardCharsets.UTF_8));
	}

	@Test
	void testSendMessageModeTextWithBinaryMessage() throws Exception {
		// Arrange
		Message message = new Message("A Textual Message".getBytes(StandardCharsets.UTF_8));
		jmsSender.setMessageClass(JMSFacade.MessageClass.TEXT);

		// Act
		jmsSender.sendMessage(message, pipeLineSession);

		// Assert
		jakarta.jms.Message jmsMessage = mockQueue.getMessage();
		assertNotNull(jmsMessage);
		assertTrue(jmsMessage instanceof TextMessage);

		TextMessage textMessage = (TextMessage) jmsMessage;
		assertEquals("A Textual Message", textMessage.getText());
	}
}
