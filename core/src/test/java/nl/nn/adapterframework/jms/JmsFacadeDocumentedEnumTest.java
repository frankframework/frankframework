package nl.nn.adapterframework.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.nn.adapterframework.jms.JMSFacade.AcknowledgeMode;
import nl.nn.adapterframework.jms.JMSFacade.DeliveryMode;
import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.jms.JMSFacade.SubscriberType;

public class JmsFacadeDocumentedEnumTest {

	@Test
	public void testDefaults() {
		JMSFacade jms = new JMSFacade();
		assertEquals(AcknowledgeMode.AUTO_ACKNOWLEDGE, jms.getAcknowledgeMode());
		assertEquals(DestinationType.QUEUE, jms.getDestinationType());
		assertEquals(SubscriberType.DURABLE, jms.getSubscriberType());
	}

	@Test
	public void testAckMode() {
		JMSFacade jms = new JMSFacade();
		jms.setAckMode(0);
		assertEquals(AcknowledgeMode.NOT_SET, jms.getAcknowledgeMode());
		jms.setAcknowledgeMode("dups");
		assertEquals(AcknowledgeMode.DUPS_OK_ACKNOWLEDGE, jms.getAcknowledgeMode());
		jms.setAcknowledgeMode("client_acknowledge");
		assertEquals(AcknowledgeMode.CLIENT_ACKNOWLEDGE, jms.getAcknowledgeMode());
		jms.setAcknowledgeMode("");
		assertEquals(AcknowledgeMode.NOT_SET, jms.getAcknowledgeMode());
	}

	@Test
	public void testDestinationType() {
		JMSFacade jms = new JMSFacade();
		assertFalse(jms.isUseTopicFunctions());
		jms.setDestinationType(DestinationType.TOPIC);
		assertEquals(DestinationType.TOPIC, jms.getDestinationType());
		assertTrue(jms.isUseTopicFunctions());
	}


	@Test
	public void testDeliveryMode() {
		JmsSender jms = new JmsSender();
		assertEquals(DeliveryMode.NOT_SET, jms.getDeliveryMode()); //Default
		jms.setDeliveryMode(DeliveryMode.PERSISTENT);
		assertEquals(DeliveryMode.PERSISTENT, jms.getDeliveryMode());
		assertEquals(DeliveryMode.NON_PERSISTENT, DeliveryMode.parse(1)); //Tests parsing programmatic setter
	}
}
