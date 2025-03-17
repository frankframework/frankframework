package org.frankframework.extensions.esb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Stream;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.mockrunner.mock.jms.MockBytesMessage;
import com.mockrunner.mock.jms.MockTextMessage;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IListenerConnector;
import org.frankframework.receivers.Receiver;
import org.frankframework.soap.SoapWrapper;
import org.frankframework.testutil.mock.ConnectionFactoryFactoryMock;
import org.frankframework.util.AppConstants;

class EsbJmsListenerTest {

	public static final String TEST_MESSAGE_DATA = "<test><value>someValue</value></test>";
	private EsbJmsListener jmsListener;

	public static Stream<Arguments> testExtractMessageProperties() throws JMSException {
		TextMessage textMessage = new MockTextMessage();
		textMessage.setText(TEST_MESSAGE_DATA);
		textMessage.setStringProperty("ae_testKeyValue", "testValue");

		BytesMessage bytesMessage = new MockBytesMessage();
		bytesMessage.writeBytes("<test><value>someValue</value></test>".getBytes());
		bytesMessage.setStringProperty("ae_testKeyValue", "testValue");
		bytesMessage.reset();

		return Stream.of(
				Arguments.of(textMessage), Arguments.of(bytesMessage)
		);
	}

	@BeforeEach
	void setUp() throws Exception {
		Receiver<Message> receiver = mock(Receiver.class);
		when(receiver.isTransacted()).thenReturn(false);
		jmsListener = new EsbJmsListener();
		jmsListener.setQueueConnectionFactoryName(ConnectionFactoryFactoryMock.MOCK_CONNECTION_FACTORY_NAME);
		jmsListener.setConnectionFactoryFactory(new ConnectionFactoryFactoryMock());
		IListenerConnector<Message> jmsConnectorMock = mock(IListenerConnector.class);
		jmsListener.setJmsConnector(jmsConnectorMock);
		jmsListener.setDestinationName("jms/dest_fake");
		jmsListener.setLookupDestination(false);
		jmsListener.setReceiver(receiver);
	}

	@AfterEach
	void tearDown() {
		jmsListener.stop();
	}

	@Test
	void testConfigureWithRRProtocol_Default() throws ConfigurationException {
		jmsListener.setMessageProtocol(EsbJmsListener.MessageProtocol.RR);
		jmsListener.configure();

		assertTrue(jmsListener.getForceMessageIdAsCorrelationId());
	}

	@Test
	void testConfigureWithRRProtocol_ForceMessageId() throws ConfigurationException {
		jmsListener.setMessageProtocol(EsbJmsListener.MessageProtocol.RR);
		jmsListener.setForceMessageIdAsCorrelationId(true);
		jmsListener.configure();

		assertTrue(jmsListener.getForceMessageIdAsCorrelationId());
	}

	@Test
	void testConfigureWithRRProtocol_NoForceMessageIdAsCorrelationId() throws ConfigurationException {
		jmsListener.setMessageProtocol(EsbJmsListener.MessageProtocol.RR);
		jmsListener.setForceMessageIdAsCorrelationId(false);
		jmsListener.configure();

		assertFalse(jmsListener.getForceMessageIdAsCorrelationId());
	}

	@Test
	void testConfigureWithRRProtocol_DefaultSetForceMessageIdAsCorrelationId() throws Exception {
		AppConstants.getInstance().put(EsbJmsListener.JMS_RR_FORCE_MESSAGE_KEY, "false");
		setUp(); // Reset the listener to re-read the AppConstants
		jmsListener.setMessageProtocol(EsbJmsListener.MessageProtocol.RR);
		jmsListener.configure();

		assertFalse(jmsListener.getForceMessageIdAsCorrelationId());
		AppConstants.getInstance().remove(EsbJmsListener.JMS_RR_FORCE_MESSAGE_KEY);
	}

	@Test
	void testConfigureWithFFProtocol() throws ConfigurationException {
		jmsListener.setMessageProtocol(EsbJmsListener.MessageProtocol.FF);
		jmsListener.configure();

		assertFalse(jmsListener.getForceMessageIdAsCorrelationId());
	}

	@ParameterizedTest
	@MethodSource
	public void testExtractMessageProperties(Message jmsMessage) throws Exception {
		// Arrange
		jmsListener.setCopyAEProperties(true);
		jmsListener.setxPathLoggingKeys("value");
		jmsListener.configure();

		// Act
		Map<String, Object> messageProperties = jmsListener.extractMessageProperties(jmsMessage);

		// Assert
		assertEquals("testValue", messageProperties.get("ae_testKeyValue"));
		assertEquals("someValue", messageProperties.get("value"));

		// Check that the message contents can still be read after the properties have been extracted.
		org.frankframework.stream.Message extractedMessage = jmsListener.extractMessage(jmsMessage, messageProperties, false, "soapHeader", SoapWrapper.getInstance());
		assertNotNull(extractedMessage);
		assertEquals(TEST_MESSAGE_DATA, extractedMessage.asString());
	}
}
