package nl.nn.adapterframework.extensions.esb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import javax.jms.JMSException;
import javax.jms.Message;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.testutil.mock.ConnectionFactoryFactoryMock;

class EsbJmsListenerTest {

	private EsbJmsListener jmsListener;

	@BeforeEach
	void setUp() throws JMSException {
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

}
