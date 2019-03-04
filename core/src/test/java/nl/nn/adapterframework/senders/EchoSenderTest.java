package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.EchoSender;

public class EchoSenderTest extends SenderTestBase<EchoSender> {

	@Override
	public EchoSender createSender() {
		return new EchoSender();
	}

	@Test
	public void basic() throws SenderException, TimeOutException, ConfigurationException {
		sender.configure();
		sender.open();
		String input = "<dummy/>";
		String result = sender.sendMessage(null, input);
		assertEquals(input, result);
	}
}
