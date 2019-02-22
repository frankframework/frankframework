package nl.nn.adapterframework.mailsenders;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

public class MailSenderTest extends MailSenderTestBase<MailSenderWrapper> {
	private String smtpHost = "localhost"; // could be smtp.sendgrid.net
	private String userId = "";
	private String password = "";

	@Override
	public MailSenderWrapper createSender() {
		MailSenderWrapper ms = new MailSenderWrapper();
		ms.setSmtpHost(smtpHost);
		ms.setSmtpUserid(userId);
		ms.setSmtpPassword(password);
		return ms;
	}

	@Ignore("MailSender throws NullPointerException because of a bug")
	@Test
	@Override
	public void testParametersEmptyRecipients()
			throws SenderException, TimeOutException, ConfigurationException, IOException {
		super.testParametersEmptyRecipients();
	}

}
