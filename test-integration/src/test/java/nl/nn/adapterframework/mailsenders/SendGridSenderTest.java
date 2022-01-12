package nl.nn.adapterframework.mailsenders;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

public class SendGridSenderTest extends MailSenderTestBase<SendGridSenderWrapper> {
	private String password = "";

	@Override
	public SendGridSenderWrapper createSender() {
		SendGridSenderWrapper ms = new SendGridSenderWrapper();
		ms.setPassword(password);
		return ms;
	}

	@Test
	@Override
	public void testEmptySubject() throws SenderException, TimeOutException, ConfigurationException, IOException {
		exception.expectMessage("subject is required");
		super.testEmptySubject();
	}

}
