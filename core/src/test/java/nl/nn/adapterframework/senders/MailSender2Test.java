package nl.nn.adapterframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Enumeration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import nl.nn.adapterframework.stream.Message;

class MailSender2Test extends SenderTestBase<MailSender> {

	private final String toAddress="testUser@localhost";
	private final String testUser="testUser";
	private final String testPassword="testPassword";
	private final String domainWhitelist="localhost,frankframework.org";

	@RegisterExtension
	static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

	@BeforeEach
	public void setup() {
		greenMail.setUser(testUser, testPassword);
	}

	@Override
	public MailSender createSender() throws Exception {
		MailSender mailSender = new MailSender();
		mailSender.setSmtpHost("localhost");
		mailSender.setSmtpPort(greenMail.getSmtp().getPort());
		mailSender.setUserId(testUser);
		mailSender.setPassword(testPassword);
		mailSender.setDomainWhitelist(domainWhitelist);
		return mailSender;
	}

	@Test
	void testSendMessageBasic() throws Exception {
		// For this test disable the whitelist to verify that the empty value lets through all recipients
		sender.setDomainWhitelist("");

		String subject = "My Subject";
		String body = "My Message Goes Here";

		String mailInput = "<email>"
				+ "<recipients>"
				+ "<recipient type=\"to\" name=\"dummy\">" + toAddress + "</recipient>"
				+ "</recipients>"
				+ "<subject>" + subject + "</subject>"
				+ "<from name=\"Me, Myself and I\">me@address.org</from>"
				+ "<message>" + body + "</message>"
			+ "</email>";

		sender.configure();
		sender.open();
		sender.sendMessageOrThrow(new Message(mailInput), session);

		MimeMessage[] messages = greenMail.getReceivedMessages();
		assertEquals(1, messages.length);

		MimeMessage message = messages[0];
		assertEquals(subject, message.getSubject());
		assertEquals(body, message.getContent().toString().trim());

		assertMailHeader(message, "From", "\"Me, Myself and I\" <me@address.org>");
		assertMailHeader(message, "Return-Path", "<me@address.org>");
	}

	@Test
	void testSendMessageWithBounceAddress() throws Exception {
		// For this test disable the whitelist to verify that the empty value lets through all recipients
		sender.setDomainWhitelist("");
		sender.setBounceAddress("my@bounce.nl");

		String subject = "My Subject";
		String body = "My Message Goes Here";

		String mailInput = "<email>"
				+ "<recipients>"
				+ "<recipient type=\"to\" name=\"dummy\">" + toAddress + "</recipient>"
				+ "</recipients>"
				+ "<subject>" + subject + "</subject>"
				+ "<from name=\"Me, Myself and I\">me@address.org</from>"
				+ "<message>" + body + "</message>"
			+ "</email>";

		sender.configure();
		sender.open();
		sender.sendMessageOrThrow(new Message(mailInput), session);

		MimeMessage[] messages = greenMail.getReceivedMessages();
		assertEquals(1, messages.length);

		MimeMessage message = messages[0];
		assertEquals(subject, message.getSubject());
		assertEquals(body, message.getContent().toString().trim());
		assertMailHeader(message, "Return-Path", "<my@bounce.nl>");
	}

	@Test
	void testSendMessageWithReplyToAndBounceAddress() throws Exception {
		// For this test disable the whitelist to verify that the empty value lets through all recipients
		sender.setDomainWhitelist("");
		sender.setBounceAddress("my@bounce.nl");

		String subject = "My Subject";
		String body = "My Message Goes Here";

		String mailInput = "<email>"
				+ "<recipients>"
				+ "<recipient type=\"to\" name=\"dummy\">" + toAddress + "</recipient>"
				+ "</recipients>"
				+ "<subject>" + subject + "</subject>"
				+ "<from name=\"Me, Myself and I\">me@address.org</from>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<message>" + body + "</message>"
			+ "</email>";

		sender.configure();
		sender.open();
		sender.sendMessageOrThrow(new Message(mailInput), session);

		MimeMessage[] messages = greenMail.getReceivedMessages();
		assertEquals(1, messages.length);

		MimeMessage message = messages[0];
		assertEquals(subject, message.getSubject());
		assertEquals(body, message.getContent().toString().trim());

		assertMailHeader(message, "Return-Path", "<my@bounce.nl>");
		assertMailHeader(message, "Reply-To", "to@address.com");
	}

	private static void assertMailHeader(MimeMessage message, String headerName, String headerValue) throws MessagingException {
		Enumeration<String> returnPathHeaders = message.getMatchingHeaderLines(new String[]{headerName});
		assertTrue(returnPathHeaders.hasMoreElements(), "Expected at least 1 header " + headerName);
		String returnPathHeader = returnPathHeaders.nextElement().trim();
		assertEquals(headerName + ": " + headerValue, returnPathHeader);
	}

	@Test
	void testSendMessageDomainWhitelist() throws Exception {
		String subject = "My Subject";
		String body = "My Message Goes Here";

		String mailInput = "<email>"
				+ "<recipients>"
				+ "<recipient type=\"to\" name=\"dummy\">" + toAddress + "</recipient>"
				+ "<recipient type=\"to\" name=\"test\">test@frankframework.org</recipient>"
				+ "<recipient type=\"to\" name=\"notwhitelisted\">test@notwhitelisted</recipient>"
				+ "</recipients>"
				+ "<subject>" + subject + "</subject>"
				+ "<from name=\"Me, Myself and I\">me@address.org</from>"
				+ "<message>" + body + "</message>"
			+ "</email>";

		sender.configure();
		sender.open();
		sender.sendMessageOrThrow(new Message(mailInput), session);

		MimeMessage[] messages = greenMail.getReceivedMessages();
		assertEquals(2, messages.length);
	}

	@Test
	void testSendMessageNoRecipientOnWhitelist() throws Exception {
		String subject = "My Subject";
		String body = "My Message Goes Here";

		String mailInput = "<email>"
				+ "<recipients>"
				+ "<recipient type=\"to\" name=\"notwhitelisted\">test@notwhitelisted</recipient>"
				+ "</recipients>"
				+ "<subject>" + subject + "</subject>"
				+ "<from name=\"Me, Myself and I\">me@address.org</from>"
				+ "<message>" + body + "</message>"
			+ "</email>";

		sender.configure();
		sender.open();
		sender.sendMessageOrThrow(new Message(mailInput), session);

		MimeMessage[] messages = greenMail.getReceivedMessages();
		assertEquals(0, messages.length);
	}
}
