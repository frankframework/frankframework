package nl.nn.adapterframework.mailsenders;

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
}
