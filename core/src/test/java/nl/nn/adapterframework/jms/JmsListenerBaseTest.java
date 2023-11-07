package nl.nn.adapterframework.jms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.jms.JMSException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.testutil.mock.ConnectionFactoryFactoryMock;

class JmsListenerBaseTest {

	private static class JmsListenerBaseImpl extends JmsListenerBase {
	}

	private JmsListenerBaseImpl jmsListener;

	@BeforeEach
	void setUp() throws JMSException {
		jmsListener = new JmsListenerBaseImpl();
		jmsListener.setQueueConnectionFactoryName(ConnectionFactoryFactoryMock.MOCK_CONNECTION_FACTORY_NAME);
		jmsListener.setConnectionFactoryFactory(new ConnectionFactoryFactoryMock());
		jmsListener.setDestinationName("jms/dest_fake");
		jmsListener.setLookupDestination(false);
	}

	@AfterEach
	void tearDown() {
		jmsListener.close();
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
