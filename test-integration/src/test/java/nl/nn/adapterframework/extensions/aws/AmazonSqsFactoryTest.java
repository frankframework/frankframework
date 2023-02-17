package nl.nn.adapterframework.extensions.aws;

import static org.junit.Assert.assertNotNull;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazonaws.regions.Regions;

import nl.nn.adapterframework.jms.AmazonSqsFactory;
import nl.nn.adapterframework.testutil.PropertyUtil;

public class AmazonSqsFactoryTest {
	protected String PROPERTY_FILE = "AmazonS3.properties";

	protected String accessKey = PropertyUtil.getProperty(PROPERTY_FILE, "accessKey");
	protected String secretKey = PropertyUtil.getProperty(PROPERTY_FILE, "secretKey");
	private Regions clientRegion = Regions.EU_WEST_1;

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
	public void testSendMessage() throws JMSException {
		// arrange
		String queueName = "iaf-test-integration-queue2";
		SQSConnection connection = (SQSConnection) createConnection();
		sqsFactory.createQueues(connection, queueName);
		
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue = session.createQueue(queueName);
		MessageProducer producer = session.createProducer(queue);
		TextMessage message = session.createTextMessage("Hello World!");
		producer.send(message);
		System.out.println("JMS Message " + message.getJMSMessageID());
	}
}
