package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;
import org.junit.jupiter.api.Test;

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
		String result = sender.sendMessageOrThrow(message, session).asString();
		assertEquals(input, result);
	}
}
