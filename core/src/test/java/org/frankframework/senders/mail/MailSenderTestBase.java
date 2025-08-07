package org.frankframework.senders.mail;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.eclipse.angus.mail.smtp.SMTPMessage;
import org.junit.jupiter.api.Test;

import org.frankframework.core.ISenderWithParameters;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.parameters.Parameter;
import org.frankframework.senders.AbstractMailSender;
import org.frankframework.senders.AbstractMailSender.EMail;
import org.frankframework.senders.AbstractMailSender.MailSessionBase;
import org.frankframework.senders.MailSender;
import org.frankframework.senders.SenderTestBase;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAssertions;

public abstract class MailSenderTestBase<S extends AbstractMailSender> extends SenderTestBase<S> {

	@Test
	public void noRecipient() throws Exception {
		String mailInput = """
			<email>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">me@address.org</from>\
			<message>My Message Goes Here</message>\
			</email>\
			""";

		sender.configure();
		sender.start();

		SenderException e = assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(new Message(mailInput), session));
		assertThat(e.getMessage(), containsString("at least 1 recipient must be specified"));
	}

	private void validateAuthentication(Session session) {
		String user = session.getProperty("login.user");
		String pass = session.getProperty("login.pass");

		assertEquals("user123", user);
		assertEquals("secret321", pass);
	}

	public void compare(String file, MimeMessage message) throws Exception {
		assertNotNull(file);
		assertNotNull(message);

		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		message.writeTo(boas);
		String rawResult = new String(boas.toByteArray());
		String expected = getResource(file).asString();
		rawResult = rawResult.replace(message.getMessageID(), "MESSAGE-ID");
		rawResult = rawResult.replaceAll(" +\\r?\\n", "\n");
		rawResult = rawResult.replaceFirst("Date: (.+)", "Date: DATE");
		int i = rawResult.indexOf("boundary=\"----");
		if(i > 0) {
			Pattern pattern = Pattern.compile("(boundary=.+)\\w");
			Matcher matcher = pattern.matcher(rawResult);
			matcher.find();
			String boundary = matcher.group(0);
			boundary = boundary.substring(10, boundary.length()); //remove an additional 10 for 'boundary='
			rawResult = rawResult.replace(boundary, "BOUNDARY");
		}

		//Make sure there is always a newline, otherwise test assertions may sometimes fail
		TestAssertions.assertEqualsIgnoreCRLF(expected, rawResult.replace("/mixed; boundary", "/mixed;\n\tboundary"));
	}

	private void appendParameters(ISenderWithParameters sender) {

		String recipientsXml = """
				<recipient type="to" name="dummy">me@address.org</recipient>\
				<recipient type="to" name="two">me2@address.org</recipient>\
				""";
		sender.addParameter(new Parameter("from", "myself@address.org"));
		sender.addParameter(new Parameter("replyTo", "to@address.com"));
		sender.addParameter(new Parameter("subject", "My Subject"));
		sender.addParameter(new Parameter("message", "My Message Goes Here"));
		sender.addParameter(new Parameter("messageBase64", "false"));
		sender.addParameter(new Parameter("recipients", recipientsXml));
	}

	private void appendAttachmentParameter(ISenderWithParameters sender, String attachmentsXml) {
		sender.addParameter(new Parameter("attachments", attachmentsXml));
	}

	private void validateNDR(Session session, String ndr) {
		String from = session.getProperty("mail.smtp.from");
		log.debug("mail NDR address ["+from+"]");

		assertEquals(ndr, from);
	}

	@Test
	public void testExtract() throws Exception {
		String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			<recipient type="cc">cc@address.org</recipient>\
			<recipient type="bcc">bcc@address.org</recipient>\
			<recipient type="bcc" name="name">i@address.org</recipient>\
			<recipient type="bcc">"personalpart1" &lt;addresspart1@address.org&gt;</recipient>\
			<recipient type="bcc">personalpart2 &lt;addresspart2@address.org&gt;</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>My Message Goes Here</message>\
			<messageType>text/plain</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			<headers>\
			<header name="x-custom">TEST</header>\
			<header name="does-this-work">yes</header>\
			</headers>\
			</email>\
			""";
		sender.configure();
		MailSessionBase mailSession = sender.extract(new Message(mailInput), session);
		validateAddress(mailSession.getRecipientList().get(0),"to", "dummy","me@address.org");
		validateAddress(mailSession.getRecipientList().get(1),"cc", null,"cc@address.org");
		validateAddress(mailSession.getRecipientList().get(2),"bcc", null,"bcc@address.org");
		validateAddress(mailSession.getRecipientList().get(3),"bcc", "name","i@address.org");
		validateAddress(mailSession.getRecipientList().get(4),"bcc", "personalpart1","addresspart1@address.org");
		validateAddress(mailSession.getRecipientList().get(5),"bcc", "personalpart2","addresspart2@address.org");
		validateAddress(mailSession.getFrom(),"from", "Me, Myself and I","myself@address.org");
	}

	public void validateAddress(EMail address, String expectedType, String expectedPersonal, String expectedAddress) {
		assertEquals(expectedAddress, address.getAddress());
		assertEquals(expectedPersonal, address.getName());
		assertEquals(expectedType, address.getType());
	}

	@Test
	public void mailWithMultipleRecipients() throws Exception {
		String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			<recipient type="cc">cc@address.org</recipient>\
			<recipient type="bcc">bcc@address.org</recipient>\
			<recipient type="bcc" name="name">i@address.org</recipient>\
			<recipient type="bcc" name="fulladdress">personalpart1 &lt;addresspart1@address.org&gt;</recipient>\
			<recipient type="bcc">personalpart2 &lt;addresspart2@address.org&gt;</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>My Message Goes Here</message>\
			<messageType>text/plain</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			<headers>\
			<header name="x-custom">TEST</header>\
			<header name="does-this-work">yes</header>\
			</headers>\
			</email>\
			""";

		sender.configure();
		sender.start();

		sender.sendMessageOrThrow(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithMultipleRecipients.txt", message);
	}

	@Test
	public void mailWithIllegalContentType() throws Exception {
		String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			<recipient type="cc">cc@address.org</recipient>\
			<recipient type="bcc">bcc@address.org</recipient>\
			<recipient type="bcc" name="name">i@address.org</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>My Message Goes Here</message>\
			<messageType>MessageTypeWithoutASlash</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			<headers>\
			<header name="x-custom">TEST</header>\
			<header name="does-this-work">yes</header>\
			</headers>\
			</email>\
			""";

		sender.configure();
		sender.start();

		SenderException e = assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(new Message(mailInput), session));
		assertThat(e.getMessage(), containsString("messageType [MessageTypeWithoutASlash] must contain a forward slash ('/')"));
	}

	@Test
	public void mailWithBase64Message() throws Exception {
		String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>VGhpcyBpcyBhIHRlc3QgZmlsZS4=</message>\
			<messageType>text/plain</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			<messageBase64>true</messageBase64>\
			</email>\
			""";

		sender.configure();
		sender.start();

		sender.sendMessageOrThrow(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithBase64Message.txt", message);
	}

	@Test
	public void mailWithoutBase64Message() throws Exception {
		String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>*not base64 should return in an error*</message>\
			<messageType>text/plain</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			<messageBase64>true</messageBase64>\
			</email>\
			""";

		sender.configure();
		sender.start();

		assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(new Message(mailInput), session));
	}

	@Test
	public void mailWithAttachment() throws Exception {
		String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>My Message Goes Here</message>\
			<messageType>text/plain</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			<attachments>\
			<attachment name="test.txt">This is a test file.</attachment>\
			</attachments>\
			</email>\
			""";

		sender.configure();
		sender.start();

		sender.sendMessageOrThrow(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithAttachment.txt", message);
	}

	@Test
	public void mailWithBase64Attachment() throws Exception {
		String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>My Message Goes Here</message>\
			<messageType>text/plain</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			<messageBase64>false</messageBase64>\
			<attachments>\
			<attachment name="test.txt" base64="true">VGhpcyBpcyBhIHRlc3QgZmlsZS4=</attachment>\
			</attachments>\
			</email>\
			""";

		sender.configure();
		sender.start();

		sender.sendMessageOrThrow(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithBase64Attachment.txt", message);
	}

	@Test
	public void mailWithBase64MessageAndAttachment() throws Exception {
		String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>TXkgTWVzc2FnZSBHb2VzIEhlcmU=</message>\
			<messageType>text/plain</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			<messageBase64>true</messageBase64>\
			<attachments>\
			<attachment name="test.txt" base64="true">VGhpcyBpcyBhIHRlc3QgZmlsZS4=</attachment>\
			</attachments>\
			</email>\
			""";

		sender.configure();
		sender.start();

		sender.sendMessageOrThrow(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithBase64MessageAndAttachment.txt", message);
	}

	@Test
	public void mailWithBase64MessageAndAttachmentWithContentType() throws Exception {
		String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>TXkgTWVzc2FnZSBHb2VzIEhlcmU=</message>\
			<messageType>text/plain</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			<messageBase64>true</messageBase64>\
			<attachments>\
			<attachment name="test.txt" base64="true" type="abc/def">VGhpcyBpcyBhIHRlc3QgZmlsZS4=</attachment>\
			</attachments>\
			</email>\
			""";

		sender.configure();
		sender.start();

		sender.sendMessageOrThrow(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithBase64MessageAndAttachmentWithContentType.txt", message);
	}

	@Test
	public void mailWithBase64MessageAndAttachmentWithIllegalContentType() throws Exception {
		String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>TXkgTWVzc2FnZSBHb2VzIEhlcmU=</message>\
			<messageType>text/plain</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			<messageBase64>true</messageBase64>\
			<attachments>\
			<attachment name="test.txt" base64="true" type="messageTypeWithoutASlash">VGhpcyBpcyBhIHRlc3QgZmlsZS4=</attachment>\
			</attachments>\
			</email>\
			""";

		sender.configure();
		sender.start();

		SenderException e = assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(new Message(mailInput), session));
		assertThat(e.getMessage(), containsString("mimeType [messageTypeWithoutASlash] of attachment [test.txt] must contain a forward slash ('/')"));
	}

	@Test
	public void mailWithPRC() throws Exception {
		String mailInput = "<email/>";

		appendParameters(sender);

		sender.configure();
		sender.start();

		sender.sendMessageOrThrow(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithPRC.txt", message);
	}

	@Test
	public void mailWithPRCAttachments() throws Exception {
		String mailInput = "<email/>";

		appendParameters(sender);

		String attachmentsXml = """
				<attachment name="test1.txt">This is test file 1.</attachment>\
				<attachment name="test2.txt" base64="true">VGhpcyBpcyB0ZXN0IGZpbGUgMi4=</attachment>\
				""";
		appendAttachmentParameter(sender, attachmentsXml);

		sender.configure();
		sender.start();

		sender.sendMessageOrThrow(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithRPCAttachments.txt", message);
	}

	@Test
	public void mailWithPRCAttachmentsFromSession() throws Exception {
		String mailInput = "<email/>";

		appendParameters(sender);

		String attachmentsXml = """
				<attachment name="test1.txt" sessionKey="attachment1" />\
				<attachment name="test2.txt" sessionKey="attachment2" base64="true" />\
				<attachment name="test3.txt" sessionKey="attachment3" />\
				<attachment name="test4.txt" sessionKey="attachment4" base64="true" />\
				""";
		appendAttachmentParameter(sender, attachmentsXml);

		sender.configure();
		sender.start();

		session.put("attachment1", "This is a text message.");
		session.put("attachment2", "VGhpcyBpcyBhIHRlc3QgZmlsZS4=");
		byte[] bytes = "This is a test file too.".getBytes();
		byte[] base64Bytes = "VGhpcyBpcyBhIGJhc2U2NCB0ZXN0IGZpbGUh".getBytes();
		session.put("attachment3", new ByteArrayInputStream(bytes));
		session.put("attachment4", new ByteArrayInputStream(base64Bytes));

		sender.sendMessageOrThrow(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithPRCAttachmentsFromSession.txt", message);
	}

	@Test
	public void mailWithNDR() throws Exception {
		if(!(sender instanceof MailSender)) return;

		String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>My Message Goes Here</message>\
			<messageType>text/plain</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			</email>\
			""";

		sender.setBounceAddress("my@bounce.nl");

		sender.configure();
		sender.start();

		sender.sendMessageOrThrow(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		validateNDR(mailSession, "my@bounce.nl");
		compare("mailWithNDR.txt", message);
	}

	@Test
	public void mailWithDynamicNDR() throws Exception {
		if(!(sender instanceof MailSender)) return;

		String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>My Message Goes Here</message>\
			<messageType>text/plain</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			<bounceAddress>my@bounce.nl</bounceAddress>\
			</email>\
			""";

		sender.configure();
		sender.start();

		sender.sendMessageOrThrow(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		SMTPMessage message = (SMTPMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		assertEquals("my@bounce.nl", message.getEnvelopeFrom());
		compare("mailWithNDR.txt", message);
	}

	@Test
	public void parallelMailWithNDR() throws Throwable {
		if(!(sender instanceof MailSender)) return;

		final String mailInput = """
			<email>\
			<recipients>\
			<recipient type="to" name="dummy">me@address.org</recipient>\
			</recipients>\
			<subject>My Subject</subject>\
			<from name="Me, Myself and I">myself@address.org</from>\
			<message>My Message Goes Here</message>\
			<messageType>text/plain</messageType>\
			<replyTo>to@address.com</replyTo>\
			<charset>UTF-8</charset>\
			</email>\
			""";

		int threads = 50;
		ExecutorService service = Executors.newFixedThreadPool(10);
		Collection<Future<Session>> futures = new ArrayList<>(threads);
		for (int t = 0; t < threads; ++t) {
			Callable<Session> task = new Callable<>() {
				@Override
				public Session call() throws Exception {
					ISenderWithParameters sender2 = createSender();
					String bounce = "my"+Math.random()+"@bounce.com";
					((MailSender) sender2).setBounceAddress(bounce);

					sender2.configure();
					sender2.start();

					PipeLineSession session1 = new PipeLineSession();
					sender2.sendMessageOrThrow(new Message(mailInput), session1);
					Session mailSession1 = (Session) session1.get("mailSession");
					mailSession1.getProperties().setProperty("bounce", bounce);

					PipeLineSession session2 = new PipeLineSession();
					sender2.sendMessageOrThrow(new Message(mailInput), session2);
					Session mailSession2 = (Session) session2.get("mailSession");
					assertEquals(mailSession1, mailSession2, "same session should be used");
					validateNDR(mailSession1, bounce);
					validateNDR(mailSession2, bounce);

					return mailSession1;
				}
			};
			futures.add(service.submit(task));
		}

		List<Session> sessions = new ArrayList<>();
		for (Future<Session> sessionFuture : futures) {
			try {
				Session session = sessionFuture.get();
				assertFalse(sessions.contains(session), "session ["+session+"] should not already exist");

				validateAuthentication(session);
				validateNDR(session, session.getProperty("bounce"));
				MimeMessage message = (MimeMessage) session.getProperties().get("MimeMessage");
				compare("mailWithNDR.txt", message);

				sessions.add(session);
			} catch (Exception e) {
				throw e.getCause();
			}
		}
		assertEquals(threads, sessions.size());
	}
}
