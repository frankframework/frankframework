package org.frankframework.testutil.mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageListener;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.JMSMockObjectFactory;
import com.mockrunner.mock.jms.MockMessage;
import com.mockrunner.mock.jms.MockQueue;
import com.mockrunner.mock.jms.MockSession;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.jms.IConnectionFactoryFactory;

@Log4j2
public class MockRunnerConnectionFactoryFactory implements IConnectionFactoryFactory {
	public static final String MOCK_CONNECTION_FACTORY_NAME = "dummyMockConnectionFactory";
	private static final JMSMockObjectFactory MOCK_FACTORY = new JMSMockObjectFactory();

	private final @Getter DestinationManager destinationManager;
	private final ConfigurationManager configurationManager;
	private final ConnectionFactory connectionFactory;

	public MockRunnerConnectionFactoryFactory() {
		destinationManager = MOCK_FACTORY.getDestinationManager();
		configurationManager = MOCK_FACTORY.getConfigurationManager();
		connectionFactory = MOCK_FACTORY.getMockQueueConnectionFactory();
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

	public jakarta.jms.Message getLastMessageFromQueue(String queueName) {
		MockQueue queue = getDestinationManager().getQueue(queueName);
		@SuppressWarnings("unchecked")
		List<MockMessage> receivedMessageList = queue.getReceivedMessageList();
		log.debug("{} messages on queue [{}]", receivedMessageList.size(), queueName);
		assertFalse(receivedMessageList.isEmpty(), "No messages received, expected at least a single message on queue [" + queueName + "]");
		return receivedMessageList.get(receivedMessageList.size() - 1);
	}

	public void addEchoReceiverOnQueue(String queueName) throws Exception {
		MockQueue queue = getDestinationManager().getQueue(queueName);
		MockSession session = (MockSession) getConnectionFactory(null).createConnection()
				.createSession(false, 0);
		queue.addSession(session);
		MessageListener ml = message -> {
			try {
				log.debug("received message on queue [{}]", queueName);
				MockQueue replyQueue = (MockQueue) message.getJMSReplyTo();
				log.debug("reply queue = [{}]", replyQueue);
				if (replyQueue != null) {
					replyQueue.addMessage(message);
				}
			} catch (Exception e) {
				fail(e);
			}
		};
		log.debug("Add message listener to queue [{}]", queueName);
		session.setMessageListener(ml);
	}

	public void addMessageOnQueue(String queueName, jakarta.jms.Message message) throws JMSException {
		destinationManager.getQueue(queueName).addMessage(message);
	}
}
