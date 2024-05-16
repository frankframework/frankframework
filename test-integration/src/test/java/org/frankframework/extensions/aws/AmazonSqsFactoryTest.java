package org.frankframework.extensions.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazonaws.regions.Regions;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.logging.log4j.Logger;
import org.frankframework.jms.AmazonSqsFactory;
import org.frankframework.testutil.PropertyUtil;
import org.frankframework.util.LogUtil;

public class AmazonSqsFactoryTest {
	protected Logger log = LogUtil.getLogger(this);

	protected String PROPERTY_FILE = "AmazonS3.properties";

	protected String accessKey = PropertyUtil.getProperty(PROPERTY_FILE, "accessKey");
	protected String secretKey = PropertyUtil.getProperty(PROPERTY_FILE, "secretKey");
	private final Regions clientRegion = Regions.EU_WEST_1;

	private AmazonSqsFactory sqsFactory;

	@BeforeEach
	public void setup() {
		sqsFactory = new AmazonSqsFactory();
		sqsFactory.setAccessKey(accessKey);
		sqsFactory.setSecretKey(secretKey);
		sqsFactory.setClientRegion(clientRegion.getName());
	}

	@Test
	public void testCreateConnectionFactory() throws JMSException {
		// arrange / act
		ConnectionFactory cf = sqsFactory.createConnectionFactory();

		// assert
		assertNotNull(cf);
	}

	private Connection createConnection() throws JMSException {
		ConnectionFactory cf = sqsFactory.createConnectionFactory();
		return cf.createConnection();
	}

	@Test
	public void testGetConnection() throws JMSException {
		// arrange / act
		Connection connection = createConnection();

		// assert
		assertNotNull(connection);
	}

	@Test
	public void testGetSession() throws JMSException {
		// arrange
		Connection connection = createConnection();

		// act
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		// assert
		assertNotNull(session);
	}

	@Test
	public void testSendAndReceiveMessage() throws JMSException {
		// arrange
		String queueName = "iaf-test-integration-queue2";
		SQSConnection connection = (SQSConnection) createConnection();
		sqsFactory.createQueues(connection, queueName);

		Session senderSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue senderQueue = senderSession.createQueue(queueName);
		log.info("queue ["+senderQueue+"]");

		log.debug("reading all messages from queue");
		TextMessage message;
		do {
			message=readMessage(queueName);
		} while (message!=null);

		log.debug("sending message");
		MessageProducer producer = senderSession.createProducer(senderQueue);
		TextMessage sent = senderSession.createTextMessage("Hello World!");
		producer.send(sent);
		String messageId = sent.getJMSMessageID();
		assertNotNull(messageId);
		senderSession.close();
		connection.close();


		log.debug("reading message");
		connection = (SQSConnection) createConnection();
		connection.start();

		Session receiverSsession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue receiverQueue = receiverSsession.createQueue(queueName);
		MessageConsumer consumer = receiverSsession.createConsumer(receiverQueue);
		TextMessage received = (TextMessage)consumer.receive(1000);

		assertNotNull(received);

		assertEquals(sent.getJMSMessageID(), received.getJMSMessageID());
		assertEquals(sent.getText(), received.getText());

		log.debug("rereading message");
		received = (TextMessage)consumer.receive(1000);
		assertNull(received);
		receiverSsession.close();
		connection.close();
	}

	@Test
	public void testSendAndReceiveMessageClientAcknowledge() throws JMSException, InterruptedException {
		// arrange
		String queueName = "iaf-test-integration-queue2";
		SQSConnection connection = (SQSConnection) createConnection();
		sqsFactory.createQueues(connection, queueName);

		Session senderSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue senderQueue = senderSession.createQueue(queueName);
		log.info("queue ["+senderQueue+"]");

		log.debug("reading all messages from queue");
		TextMessage message;
		do {
			message=readMessage(queueName);
		} while (message!=null);

		log.debug("sending message");
		MessageProducer producer = senderSession.createProducer(senderQueue);
		TextMessage sent = senderSession.createTextMessage("Hello World!");
		producer.send(sent);
		String messageId = sent.getJMSMessageID();
		assertNotNull(messageId);
		senderSession.close();
		connection.close();


		log.debug("reading message");
		connection = (SQSConnection) createConnection();
		connection.start();

		Session receiverSsession = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		Queue receiverQueue = receiverSsession.createQueue(queueName);
		MessageConsumer consumer = receiverSsession.createConsumer(receiverQueue);
		TextMessage received = (TextMessage)consumer.receive(1000);

		assertNotNull(received);

		assertEquals(sent.getJMSMessageID(), received.getJMSMessageID());
		assertEquals(sent.getText(), received.getText());

		log.debug("sleep 30 seconds");
		Thread.sleep(30000);

		log.debug("rereading message");
		// we did not acknowledge the message, it should be visible again after the visibility timeout expires
		received = (TextMessage)consumer.receive(1000);
		assertNotNull(received);
		assertEquals(sent.getJMSMessageID(), received.getJMSMessageID());
		assertEquals(sent.getText(), received.getText());
		received.acknowledge();
		receiverSsession.close();
		connection.close();
	}

	private TextMessage readMessage(String queueName) throws JMSException {
		SQSConnection connection = (SQSConnection) createConnection();
		connection.start();

		Session receiverSsession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue receiverQueue = receiverSsession.createQueue(queueName);
		MessageConsumer consumer = receiverSsession.createConsumer(receiverQueue);
		TextMessage received = (TextMessage)consumer.receive(1000);
		if (received!=null) {
			log.debug("read messageid ["+received.getJMSMessageID()+"]");
		}
		receiverSsession.close();
		connection.close();
		return received;
	}

}
