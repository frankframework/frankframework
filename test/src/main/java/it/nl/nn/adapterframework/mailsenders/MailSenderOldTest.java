package it.nl.nn.adapterframework.mailsenders;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

public class MailSenderOldTest extends MailSenderTestBase<MailSenderOldWrapper> {
	private String smtpHost = "localhost"; // could be smtp.sendgrid.net
	private String userId = "";
	private String password = "";

	@Override
	public MailSenderOldWrapper createSender() {
		MailSenderOldWrapper ms = new MailSenderOldWrapper();
		ms.setSmtpHost(smtpHost);
		ms.setSmtpUserid(userId);
		ms.setSmtpPassword(password);
		return ms;
	}

	@Ignore("MailSenderOld throws NullPointerException because of a bug")
	@Test
	@Override
	public void testParametersEmptyRecipients()
			throws SenderException, TimeOutException, ConfigurationException, IOException {
		super.testParametersEmptyRecipients();
	}
}
