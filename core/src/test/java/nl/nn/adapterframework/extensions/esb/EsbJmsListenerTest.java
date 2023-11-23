package nl.nn.adapterframework.extensions.esb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.stream.Stream;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.mockrunner.mock.jms.MockBytesMessage;
import com.mockrunner.mock.jms.MockTextMessage;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.testutil.mock.ConnectionFactoryFactoryMock;

class EsbJmsListenerTest {

	private EsbJmsListener jmsListener;

	public static Stream<Arguments> testExtractMessageProperties() throws JMSException {
		TextMessage textMessage = new MockTextMessage();
		textMessage.setText("<test><value>someValue</value></test>");
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
		jmsListener = new EsbJmsListener();
		jmsListener.setQueueConnectionFactoryName(ConnectionFactoryFactoryMock.MOCK_CONNECTION_FACTORY_NAME);
		jmsListener.setConnectionFactoryFactory(new ConnectionFactoryFactoryMock());
		IListenerConnector<Message> jmsConnectorMock = mock(IListenerConnector.class);
		jmsListener.setJmsConnector(jmsConnectorMock);
		jmsListener.setDestinationName("jms/dest_fake");
		jmsListener.setLookupDestination(false);
	}

	@AfterEach
	void tearDown() {
		jmsListener.close();
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
	void testConfigureWithFFProtocol() throws ConfigurationException {
		jmsListener.setMessageProtocol(EsbJmsListener.MessageProtocol.FF);
		jmsListener.configure();

		assertFalse(jmsListener.getForceMessageIdAsCorrelationId());
	}

	@ParameterizedTest
	@MethodSource
	public void testExtractMessageProperties(Message message) throws Exception {
		// Arrange
		jmsListener.setCopyAEProperties(true);
		jmsListener.setxPathLoggingKeys("value");
		jmsListener.configure();

		// Act
		Map<String, Object> messageProperties = jmsListener.extractMessageProperties(message);

		// Assert
		assertEquals("testValue", messageProperties.get("ae_testKeyValue"));
		assertEquals("someValue", messageProperties.get("value"));
	}
}
