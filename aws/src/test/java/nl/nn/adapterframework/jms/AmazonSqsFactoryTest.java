package nl.nn.adapterframework.jms;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazonaws.regions.Regions;

import nl.nn.adapterframework.testutil.PropertyUtil;
import nl.nn.adapterframework.util.LogUtil;

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
	public void testCreateConnectionFactory() {
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
	public void testGetConnection() throws JMSException {
		// Arrange / Act
		Connection connection = createConnection();

		// Assert
		assertNotNull(connection);
	}

	@Test
	public void testGetSession() throws JMSException {
		// Arrange
		Connection connection = createConnection();

		// Act
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		// Assert
		assertNotNull(session);
	}
}
