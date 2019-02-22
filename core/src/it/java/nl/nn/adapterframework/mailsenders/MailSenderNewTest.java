package nl.nn.adapterframework.mailsenders;

public class MailSenderNewTest extends MailSenderTestBase<MailSenderNewWrapper> {
	private String smtpHost = "localhost"; // could be smtp.sendgrid.net
	private String userId = "";
	private String password = "";

	@Override
	public MailSenderNewWrapper createSender() {
		MailSenderNewWrapper ms = new MailSenderNewWrapper();
		ms.setSmtpHost(smtpHost);
		ms.setSmtpUserid(userId);
		ms.setSmtpPassword(password);
		return ms;
	}
}
