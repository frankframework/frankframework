package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;

public class EchoSenderTest extends SenderTestBase<EchoSender> {

	@Override
	public EchoSender createSender() {
		return new EchoSender();
	}

	@Test
	public void basic() throws SenderException, TimeoutException, ConfigurationException, IOException {
		sender.configure();
		sender.open();
		String input = "<dummy/>";
		Message message = new Message(input);
		String result = sender.sendMessage(message, session).asString();
		assertEquals(input, result);
	}
}
