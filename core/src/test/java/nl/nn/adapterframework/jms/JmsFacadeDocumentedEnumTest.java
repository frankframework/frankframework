package nl.nn.adapterframework.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.jms.JMSFacade.AcknowledgeMode;
import nl.nn.adapterframework.jms.JMSFacade.DeliveryMode;
import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.jms.JMSFacade.SubscriberType;

public class JmsFacadeDocumentedEnumTest {

	@Test
	public void testDefaults() {
		JMSFacade jms = new JMSFacade();
		assertEquals(AcknowledgeMode.AUTO_ACKNOWLEDGE, jms.getAcknowledgeModeEnum());
		assertEquals(DestinationType.QUEUE, jms.getDestinationType());
		assertEquals(SubscriberType.DURABLE, jms.getSubscriberType());
	}

	@Test
	public void testAckMode() {
		JMSFacade jms = new JMSFacade();
		jms.setAcknowledgeMode(AcknowledgeMode.NOT_SET.name());
		assertEquals(AcknowledgeMode.NOT_SET, jms.getAcknowledgeModeEnum());
		jms.setAcknowledgeMode("dups");
		assertEquals(AcknowledgeMode.DUPS_OK_ACKNOWLEDGE, jms.getAcknowledgeModeEnum());
		jms.setAcknowledgeMode(AcknowledgeMode.CLIENT_ACKNOWLEDGE.name());
		assertEquals(AcknowledgeMode.CLIENT_ACKNOWLEDGE, jms.getAcknowledgeModeEnum());
		jms.setAcknowledgeMode("");
		assertEquals(AcknowledgeMode.NOT_SET, jms.getAcknowledgeModeEnum());
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
