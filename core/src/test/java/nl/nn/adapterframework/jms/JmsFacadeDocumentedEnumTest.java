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
		assertEquals(AcknowledgeMode.AUTO_ACKNOWLEDGE, jms.getAckModeEnum());
		assertEquals(DestinationType.QUEUE, jms.getDestinationTypeEnum());
		assertEquals(SubscriberType.DURABLE, jms.getSubscriberTypeEnum());
	}

	@Test
	public void testAckMode() {
		JMSFacade jms = new JMSFacade();
		jms.setAckMode(0);
		assertEquals(AcknowledgeMode.NOT_SET, jms.getAckModeEnum());
		jms.setAcknowledgeMode("dups");
		assertEquals(AcknowledgeMode.DUPS_OK_ACKNOWLEDGE, jms.getAckModeEnum());
		jms.setAcknowledgeMode("client_acknowledge");
		assertEquals(AcknowledgeMode.CLIENT_ACKNOWLEDGE, jms.getAckModeEnum());
		jms.setAcknowledgeMode("");
		assertEquals(AcknowledgeMode.NOT_SET, jms.getAckModeEnum());
	}

	@Test
	public void testDestinationType() {
		JMSFacade jms = new JMSFacade();
		assertFalse(jms.isUseTopicFunctions());
		jms.setDestinationType("topic");
		assertEquals(DestinationType.TOPIC, jms.getDestinationTypeEnum());
		assertTrue(jms.isUseTopicFunctions());
	}

	@Test
	public void testSubscriberType() {
		JMSFacade jms = new JMSFacade();
		jms.setSubscriberType("tranSieNT");
		assertEquals(SubscriberType.TRANSIENT, jms.getSubscriberTypeEnum());
	}

	@Test
	public void testDeliveryMode() {
		JmsSender jms = new JmsSender();
		assertEquals(DeliveryMode.NOT_SET, jms.getDeliveryModeEnum()); //Default
		jms.setDeliveryMode("persistent");
		assertEquals(DeliveryMode.PERSISTENT, jms.getDeliveryModeEnum());
		assertEquals(DeliveryMode.NON_PERSISTENT, DeliveryMode.parse(1)); //Tests parsing programmatic setter
	}
}
