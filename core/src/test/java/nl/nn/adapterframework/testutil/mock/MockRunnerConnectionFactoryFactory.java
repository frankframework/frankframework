package nl.nn.adapterframework.testutil.mock;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.QueueConnectionFactory;
import javax.naming.NamingException;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.JMSMockObjectFactory;

import lombok.Getter;
import nl.nn.adapterframework.jms.IConnectionFactoryFactory;

public class MockRunnerConnectionFactoryFactory implements IConnectionFactoryFactory {
	private JMSMockObjectFactory mockFactory;
	private @Getter DestinationManager destinationManager;
	private ConfigurationManager configurationManager;
	private QueueConnectionFactory queueConnectionFactory;

	public void init() {
		mockFactory = new JMSMockObjectFactory();
		destinationManager = mockFactory.getDestinationManager();
		configurationManager = mockFactory.getConfigurationManager();

		queueConnectionFactory = mockFactory.getMockQueueConnectionFactory();
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName) throws NamingException {
		return queueConnectionFactory;
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName, Properties jndiEnvironment) throws NamingException {
		return queueConnectionFactory;
	}

	@Override
	public List<String> getConnectionFactoryNames() {
		return Collections.singletonList("MockTestQCF");
	}
}
