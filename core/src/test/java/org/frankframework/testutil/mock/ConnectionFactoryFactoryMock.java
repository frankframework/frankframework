package org.frankframework.testutil.mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.mockito.Mockito;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.jms.IConnectionFactoryFactory;
import org.frankframework.jms.JmsTransactionalStorage;

public class ConnectionFactoryFactoryMock implements IConnectionFactoryFactory {
	private final Map<String, ConnectionFactory> objects = new ConcurrentHashMap<>();
	public static final String MOCK_CONNECTION_FACTORY_NAME = "dummyMockConnectionFactory";

	private static final ThreadLocal<MessageHandler> messageHandlers = new ThreadLocal<>();

	public ConnectionFactoryFactoryMock() throws JMSException {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		ConnectionMetaData connMetaData = mock(ConnectionMetaData.class);
		doReturn(connMetaData).when(connection).getMetaData();

		Session session = mock(Session.class);
		Queue destinationQueue = mock(Queue.class);
		doReturn(destinationQueue).when(session).createQueue(anyString());

		MessageHandler handler = getMessageHandler();
		doReturn(handler).when(session).createConsumer(isNull(), isNull());
		doReturn(handler).when(session).createConsumer(any(Destination.class), anyString());
		doReturn(handler).when(session).createProducer(any(Destination.class));

		QueueBrowser browser = new QueueBrowserMock(destinationQueue);
		doReturn(browser).when(session).createBrowser(any(Queue.class));
		doReturn(browser).when(session).createBrowser(any(Queue.class), anyString());
		doReturn(session).when(connection).createSession(anyBoolean(), anyInt());
		doReturn(TextMessageMock.newInstance()).when(session).createTextMessage();
		doReturn(connection).when(cf).createConnection();
		objects.put(MOCK_CONNECTION_FACTORY_NAME, cf);
	}

	public static MessageHandler getMessageHandler() {
		return getMessageHandler(false);
	}

	public static synchronized MessageHandler getMessageHandler(boolean createNewInstance) {
		MessageHandler handler = messageHandlers.get();
		if(handler == null || createNewInstance) {
			handler = MessageHandler.newInstance();
			messageHandlers.set(handler);
		}
		return handler;
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName) throws NamingException {
		return getConnectionFactory(connectionFactoryName, null);
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName, Properties jndiEnvironment) throws NamingException {
		return objects.get(connectionFactoryName);
	}

	@Override
	public List<String> getConnectionFactoryNames() {
		return new ArrayList<>(objects.keySet());
	}

	public class QueueBrowserMock implements QueueBrowser {
		private Queue queue;

		public QueueBrowserMock(Queue queue) {
			this.queue = queue;
		}

		@Override
		public Queue getQueue() throws JMSException {
			return queue;
		}

		@Override
		public String getMessageSelector() throws JMSException {
			return null;
		}

		@Override
		public Enumeration<Message> getEnumeration() throws JMSException {
			List<Message> messages = new ArrayList<>();
			Message jmsMessage = mock(Message.class);
			doReturn("dummyMessageId").when(jmsMessage).getJMSMessageID();
			doReturn((long) 12.34).when(jmsMessage).getJMSExpiration();
			doReturn((long) 45.56).when(jmsMessage).getJMSTimestamp();
			doReturn("dummy-hostname").when(jmsMessage).getStringProperty(eq(JmsTransactionalStorage.FIELD_HOST));
			doReturn("dummyCorrelationId").when(jmsMessage).getJMSCorrelationID();
			messages.add(jmsMessage);
			return Collections.enumeration(messages);
		}

		@Override
		public void close() throws JMSException {
			// Nothing to close
		}
	}

	//Use this to 'send' and 'receive' messages
	public abstract static class MessageHandler extends Mockito implements MessageProducer, MessageConsumer {
		private jakarta.jms.Message payload = null;
		public static MessageHandler newInstance() {
			return mock(MessageHandler.class, CALLS_REAL_METHODS);
		}

		@Override
		public void send(Message message) throws JMSException {
			send(null, message);
		}

		@Override
		public void send(Destination destination, Message message) throws JMSException {
			payload = message;
		}

		@Override
		public Message receive() throws JMSException {
			return receive(0);
		}

		@Override
		public Message receive(long timeout) throws JMSException {
			return payload == null ? TextMessageMock.newInstance() : payload;
		}
	}

	public abstract static class TextMessageMock extends Mockito implements TextMessage {
		private @Getter @Setter String text;
		public static TextMessageMock newInstance() {
			return mock(TextMessageMock.class, CALLS_REAL_METHODS);
		}

		@Override
		public Enumeration<String> getPropertyNames() throws JMSException {
			return Collections.emptyEnumeration();
		}
	}
}
