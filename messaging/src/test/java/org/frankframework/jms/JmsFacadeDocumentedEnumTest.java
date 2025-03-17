package org.frankframework.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.frankframework.jms.JMSFacade.AcknowledgeMode;
import org.frankframework.jms.JMSFacade.DeliveryMode;
import org.frankframework.jms.JMSFacade.DestinationType;
import org.frankframework.jms.JMSFacade.SubscriberType;

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
		jms.setAcknowledgeMode(AcknowledgeMode.NOT_SET);
		assertEquals(AcknowledgeMode.NOT_SET, jms.getAcknowledgeMode());
		jms.setAcknowledgeMode(AcknowledgeMode.DUPS_OK_ACKNOWLEDGE);
		assertEquals(AcknowledgeMode.DUPS_OK_ACKNOWLEDGE, jms.getAcknowledgeMode());
		jms.setAcknowledgeMode(AcknowledgeMode.CLIENT_ACKNOWLEDGE);
		assertEquals(AcknowledgeMode.CLIENT_ACKNOWLEDGE, jms.getAcknowledgeMode());
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
