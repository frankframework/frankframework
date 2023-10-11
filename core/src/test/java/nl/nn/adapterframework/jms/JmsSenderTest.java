package nl.nn.adapterframework.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import javax.jms.BytesMessage;
import javax.jms.TextMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mockrunner.mock.jms.MockQueue;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.mock.MockRunnerConnectionFactoryFactory;
import nl.nn.adapterframework.util.AppConstants;

class JmsSenderTest {

	private MockRunnerConnectionFactoryFactory mockFactory;
	private MockQueue mockQueue;

	private JmsSender jmsSender;

	private PipeLineSession pipeLineSession;

	@BeforeEach
	void setUp() throws Exception {
		pipeLineSession = new PipeLineSession();
		AppConstants.getInstance().setProperty("jms.createDestination", true);
		jmsSender = new JmsSender();

		mockFactory = new MockRunnerConnectionFactoryFactory();
		mockFactory.init();

		mockQueue = mockFactory.getDestinationManager().createQueue("TestQueue");

		jmsSender.setQueueConnectionFactoryName("mock");
		jmsSender.setConnectionFactoryFactory(mockFactory);
		jmsSender.setDestinationName("TestQueue");

		jmsSender.configure();
		jmsSender.open();
	}

	@AfterEach
	void tearDown() {
		pipeLineSession.close();
		jmsSender.close();
	}

	@Test
	void testSendMessageModeAutoWithTextMessage() throws Exception {
		// Arrange
		Message message = Message.asMessage("A Textual Message");

		// Act
		jmsSender.sendMessage(message, pipeLineSession);

		// Assert
		javax.jms.Message jmsMessage = mockQueue.getMessage();
		assertNotNull(jmsMessage);
		assertTrue(jmsMessage instanceof TextMessage);

		TextMessage textMessage = (TextMessage) jmsMessage;
		assertEquals("A Textual Message", textMessage.getText());
	}

	@Test
	void testSendMessageModeAutoWithBinaryMessage() throws Exception {
		// Arrange
		Message message = Message.asMessage("A Textual Message".getBytes(StandardCharsets.UTF_8));

		// Act
		jmsSender.sendMessage(message, pipeLineSession);

		// Assert
		javax.jms.Message jmsMessage = mockQueue.getMessage();
		assertNotNull(jmsMessage);
		assertTrue(jmsMessage instanceof BytesMessage);

		BytesMessage bytesMessage = (BytesMessage) jmsMessage;
		byte[] data = new byte[(int) bytesMessage.getBodyLength()];
		bytesMessage.readBytes(data);
		assertEquals("A Textual Message", new String(data, StandardCharsets.UTF_8));
	}

	@Test
	void testSendMessageModeBytesWithTextMessage() throws Exception {
		// Arrange
		Message message = Message.asMessage("A Textual Message");
		jmsSender.setMessageClass(JMSFacade.MessageClass.BYTES);

		// Act
		jmsSender.sendMessage(message, pipeLineSession);

		// Assert
		javax.jms.Message jmsMessage = mockQueue.getMessage();
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
		Message message = Message.asMessage("A Textual Message".getBytes(StandardCharsets.UTF_8));
		jmsSender.setMessageClass(JMSFacade.MessageClass.TEXT);

		// Act
		jmsSender.sendMessage(message, pipeLineSession);

		// Assert
		javax.jms.Message jmsMessage = mockQueue.getMessage();
		assertNotNull(jmsMessage);
		assertTrue(jmsMessage instanceof TextMessage);

		TextMessage textMessage = (TextMessage) jmsMessage;
		assertEquals("A Textual Message", textMessage.getText());
	}
}
