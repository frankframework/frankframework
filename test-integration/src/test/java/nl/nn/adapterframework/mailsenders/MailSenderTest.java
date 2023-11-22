package nl.nn.adapterframework.mailsenders;

public class MailSenderTest extends MailSenderTestBase<MailSenderWrapper> {
	private final String smtpHost = "localhost"; // could be smtp.sendgrid.net
	private final String userId = "";
	private final String password = "";

	@Override
	public MailSenderWrapper createSender() {
		MailSenderWrapper ms = new MailSenderWrapper();
		ms.setSmtpHost(smtpHost);
		ms.setSmtpUserid(userId);
		ms.setSmtpPassword(password);
		return ms;
	}
}
