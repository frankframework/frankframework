package org.frankframework.jms;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.apache.logging.log4j.Logger;
import org.frankframework.testutil.PropertyUtil;
import org.frankframework.util.LogUtil;
import software.amazon.awssdk.regions.Region;

class AmazonSqsFactoryTest {
	protected Logger log = LogUtil.getLogger(this);

	protected String PROPERTY_FILE = "AmazonS3.properties";

	protected String accessKey = PropertyUtil.getProperty(PROPERTY_FILE, "accessKey");
	protected String secretKey = PropertyUtil.getProperty(PROPERTY_FILE, "secretKey");
	private final Region clientRegion = Region.EU_WEST_1;

	private AmazonSqsFactory sqsFactory;

	@BeforeEach
	public void setup() {
		sqsFactory = new AmazonSqsFactory();
		sqsFactory.setAccessKey(accessKey);
		sqsFactory.setSecretKey(secretKey);
		sqsFactory.setClientRegion(clientRegion);
	}

	@Test
	void testCreateConnectionFactory() {
		// Arrange / Act
		ConnectionFactory cf = sqsFactory.createConnectionFactory();

		// Assert
		assertNotNull(cf);
	}

	private Connection createConnection() throws JMSException {
		ConnectionFactory cf = sqsFactory.createConnectionFactory();
		return cf.createConnection();
	}

	@Test
	void testGetSession() throws JMSException {
		// Arrange
		Connection connection = createConnection();
		assertNotNull(connection);

		// Act
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		// Assert
		assertNotNull(session);
		session.close();
		connection.close();
	}
}
