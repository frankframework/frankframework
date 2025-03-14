package org.frankframework.jms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.jms.JMSException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.receivers.Receiver;
import org.frankframework.testutil.mock.ConnectionFactoryFactoryMock;

class JmsListenerBaseTest {

	private static class JmsListenerBaseImpl extends AbstractJmsListener {
	}

	private JmsListenerBaseImpl jmsListener;

	@BeforeEach
	void setUp() throws JMSException {
		Receiver<jakarta.jms.Message> receiver = mock(Receiver.class);
		when(receiver.isTransacted()).thenReturn(false);
		jmsListener = new JmsListenerBaseImpl();
		jmsListener.setQueueConnectionFactoryName(ConnectionFactoryFactoryMock.MOCK_CONNECTION_FACTORY_NAME);
		jmsListener.setConnectionFactoryFactory(new ConnectionFactoryFactoryMock());
		jmsListener.setDestinationName("jms/dest_fake");
		jmsListener.setLookupDestination(false);
		jmsListener.setReceiver(receiver);
	}

	@AfterEach
	void tearDown() {
		jmsListener.stop();
	}

	@Test
	void testConfigure_Default() throws ConfigurationException {
		jmsListener.configure();
		assertFalse(jmsListener.getForceMessageIdAsCorrelationId());
	}

	@Test
	void testConfigure_ForceMessageId() throws ConfigurationException {
		jmsListener.setForceMessageIdAsCorrelationId(true);
		jmsListener.configure();

		assertTrue(jmsListener.getForceMessageIdAsCorrelationId());
	}
}
