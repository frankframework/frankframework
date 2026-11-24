package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.frankframework.parameters.Parameter;

class SenderWrapperTest extends SenderTestBase<SenderWrapper> {

	private EchoSender testSender;

	@Override
	public SenderWrapper createSender() throws Exception {
		SenderWrapper senderWrapper = new SenderWrapper();
		testSender = new EchoSender();
		senderWrapper.setSender(testSender);
		senderWrapper.addParameter(new Parameter("test", "v1"));

		return senderWrapper;
	}

	@Test
	void testConfigureCopiesParameters() throws Exception {
		assertFalse(testSender.getParameterList().hasParameter("test"));

		sender.configure();

		assertTrue(testSender.getParameterList().hasParameter("test"));
	}
}
