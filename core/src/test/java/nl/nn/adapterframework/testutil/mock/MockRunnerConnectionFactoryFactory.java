package nl.nn.adapterframework.testutil.mock;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;
import javax.naming.NamingException;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.JMSMockObjectFactory;
import com.mockrunner.mock.jms.MockMessage;
import com.mockrunner.mock.jms.MockQueue;
import com.mockrunner.mock.jms.MockSession;

import lombok.Getter;
import nl.nn.adapterframework.jms.IConnectionFactoryFactory;

public class MockRunnerConnectionFactoryFactory implements IConnectionFactoryFactory {
	public static final String MOCK_CONNECTION_FACTORY_NAME = "MockConnectionFactory";

	private final @Getter DestinationManager destinationManager;
	private final ConfigurationManager configurationManager;
	private final ConnectionFactory connectionFactory;

	public MockRunnerConnectionFactoryFactory() {
		JMSMockObjectFactory mockFactory = new JMSMockObjectFactory();
		destinationManager = mockFactory.getDestinationManager();
		configurationManager = mockFactory.getConfigurationManager();

		connectionFactory = mockFactory.getMockQueueConnectionFactory();
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName) throws NamingException {
		return connectionFactory;
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName, Properties jndiEnvironment) throws NamingException {
		return connectionFactory;
	}

	@Override
	public List<String> getConnectionFactoryNames() {
		return Collections.singletonList(MOCK_CONNECTION_FACTORY_NAME);
	}

	public javax.jms.Message getLastMessageFromQueue(String queueName) {
		@SuppressWarnings("unchecked")
		List<MockMessage> receivedMessageList = getDestinationManager().getQueue(queueName).getReceivedMessageList();
		return receivedMessageList.get(receivedMessageList.size() - 1);
	}

	public void addEchoReceiverOnQueue(String queueName) throws Exception {
		MockQueue queue = getDestinationManager().getQueue(queueName);
		MockSession session = (MockSession) getConnectionFactory(null).createConnection()
				.createSession(false, 0);
		queue.addSession(session);
		MessageListener ml = message -> {
			try {
				MockQueue replyQueue = (MockQueue) message.getJMSReplyTo();
				if (replyQueue != null) {
					replyQueue.addMessage(message);
				}
			} catch (Exception e) {
				fail(e);
			}
		};
		session.setMessageListener(ml);
	}
}
