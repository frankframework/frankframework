package nl.nn.adapterframework.senders.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

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

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

import com.sun.mail.smtp.SMTPMessage;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.MailSender;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

public abstract class MailSenderTestBase<S extends ISenderWithParameters> extends SenderTestBase<S> {

	protected abstract String getTestRootFolder();

	@Test(expected = SenderException.class)
	public void noRecipient() throws Exception {
		String mailInput = "<email>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">me@address.org</from>"
				+ "<message>My Message Goes Here</message>"
			+ "</email>";

		sender.configure();
		sender.open();
		sender.sendMessage(new Message(mailInput), session);
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
		String expected = TestFileUtils.getTestFile(getTestRootFolder()+file);
		assertNotNull("expected folder ["+getTestRootFolder()+"] file ["+file+"] not found", expected);
		rawResult = rawResult.replace(message.getMessageID(), "MESSAGE-ID");
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
		TestAssertions.assertEqualsIgnoreCRLF(expected, rawResult.replace("/mixed; boundary", "/mixed; \n\tboundary"));
	}

	private void appendParameters(ISenderWithParameters sender) {

		Parameter parameterFrom = new Parameter();
		parameterFrom.setName("from");
		parameterFrom.setValue("myself@address.org");
		sender.addParameter(parameterFrom);

		Parameter parameterSubject = new Parameter();
		parameterSubject.setName("subject");
		parameterSubject.setValue("My Subject");
		sender.addParameter(parameterSubject);

		Parameter parameterMessage = new Parameter();
		parameterMessage.setName("message");
		parameterMessage.setValue("My Message Goes Here");
		sender.addParameter(parameterMessage);

		Parameter parameterMessageBase64 = new Parameter();
		parameterMessageBase64.setName("messageBase64");
		parameterMessageBase64.setValue("false");
		sender.addParameter(parameterMessageBase64);

		String recipientsXml = "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
				+ "<recipient type=\"to\" name=\"two\">me2@address.org</recipient>";
		Parameter parameterRecipients = new Parameter();
		parameterRecipients.setName("recipients");
		parameterRecipients.setValue(recipientsXml);
		sender.addParameter(parameterRecipients);
	}

	private void appendAttachmentParameter(ISenderWithParameters sender, String attachmentsXml) {
		Parameter parameter = new Parameter();
		parameter.setName("attachments");
		parameter.setValue(attachmentsXml);
		sender.addParameter(parameter);
	}

	private void validateNDR(Session session, String ndr) {
		String from = session.getProperty("mail.smtp.from");
		log.debug("mail NDR address ["+from+"]");

		assertEquals(ndr, from);
	}

	@Test
	public void mailWithMultipleRecipients() throws Exception {
		String mailInput = "<email>"
				+ "<recipients>"
					+ "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
					+ "<recipient type=\"cc\">cc@address.org</recipient>"
					+ "<recipient type=\"bcc\">bcc@address.org</recipient>"
					+ "<recipient type=\"bcc\" name=\"name\">i@address.org</recipient>"
				+ "</recipients>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">myself@address.org</from>"
				+ "<message>My Message Goes Here</message>"
				+ "<messageType>text/plain</messageType>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<charset>UTF-8</charset>"
				+ "<headers>"
					+ "<header name=\"x-custom\">TEST</header>"
					+ "<header name=\"does-this-work\">yes</header>"
				+ "</headers>"
			+ "</email>";

		sender.configure();
		sender.open();

		sender.sendMessage(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithMultipleRecipients.txt", message);
	}

	@Test
	public void mailWithIllegalContentType() throws Exception {
		String mailInput = "<email>"
				+ "<recipients>"
					+ "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
					+ "<recipient type=\"cc\">cc@address.org</recipient>"
					+ "<recipient type=\"bcc\">bcc@address.org</recipient>"
					+ "<recipient type=\"bcc\" name=\"name\">i@address.org</recipient>"
				+ "</recipients>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">myself@address.org</from>"
				+ "<message>My Message Goes Here</message>"
				+ "<messageType>MessageTypeWithoutASlash</messageType>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<charset>UTF-8</charset>"
				+ "<headers>"
					+ "<header name=\"x-custom\">TEST</header>"
					+ "<header name=\"does-this-work\">yes</header>"
				+ "</headers>"
			+ "</email>";

		sender.configure();
		sender.open();

		exception.expectMessage("messageType [MessageTypeWithoutASlash] must contain a forward slash ('/')");
		sender.sendMessage(new Message(mailInput), session);
	}

	@Test
	public void mailWithBase64Message() throws Exception {
		String mailInput = "<email>"
				+ "<recipients>"
					+ "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
				+ "</recipients>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">myself@address.org</from>"
				+ "<message>VGhpcyBpcyBhIHRlc3QgZmlsZS4=</message>"
				+ "<messageType>text/plain</messageType>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<charset>UTF-8</charset>"
				+ "<messageBase64>true</messageBase64>"
			+ "</email>";

		sender.configure();
		sender.open();

		sender.sendMessage(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithBase64Message.txt", message);
	}

	@Test(expected = SenderException.class)
	public void mailWithoutBase64Message() throws Exception {
		String mailInput = "<email>"
				+ "<recipients>"
					+ "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
				+ "</recipients>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">myself@address.org</from>"
				+ "<message>not base64 should return in an error</message>"
				+ "<messageType>text/plain</messageType>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<charset>UTF-8</charset>"
				+ "<messageBase64>true</messageBase64>"
			+ "</email>";

		sender.configure();
		sender.open();

		sender.sendMessage(new Message(mailInput), session);
	}

	@Test
	public void mailWithAttachment() throws Exception {
		String mailInput = "<email>"
				+ "<recipients>"
					+ "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
				+ "</recipients>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">myself@address.org</from>"
				+ "<message>My Message Goes Here</message>"
				+ "<messageType>text/plain</messageType>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<charset>UTF-8</charset>"
				+ "<attachments>"
					+ "<attachment name=\"test.txt\">This is a test file.</attachment>"
				+ "</attachments>"
			+ "</email>";

		sender.configure();
		sender.open();

		sender.sendMessage(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithAttachment.txt", message);
	}

	@Test
	public void mailWithBase64Attachment() throws Exception {
		String mailInput = "<email>"
				+ "<recipients>"
					+ "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
				+ "</recipients>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">myself@address.org</from>"
				+ "<message>My Message Goes Here</message>"
				+ "<messageType>text/plain</messageType>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<charset>UTF-8</charset>"
				+ "<messageBase64>false</messageBase64>"
				+ "<attachments>"
					+ "<attachment name=\"test.txt\" base64=\"true\">VGhpcyBpcyBhIHRlc3QgZmlsZS4=</attachment>"
				+ "</attachments>"
			+ "</email>";

		sender.configure();
		sender.open();

		sender.sendMessage(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithBase64Attachment.txt", message);
	}

	@Test
	public void mailWithBase64MessageAndAttachment() throws Exception {
		String mailInput = "<email>"
				+ "<recipients>"
					+ "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
				+ "</recipients>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">myself@address.org</from>"
				+ "<message>TXkgTWVzc2FnZSBHb2VzIEhlcmU=</message>"
				+ "<messageType>text/plain</messageType>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<charset>UTF-8</charset>"
				+ "<messageBase64>true</messageBase64>"
				+ "<attachments>"
					+ "<attachment name=\"test.txt\" base64=\"true\">VGhpcyBpcyBhIHRlc3QgZmlsZS4=</attachment>"
				+ "</attachments>"
			+ "</email>";

		sender.configure();
		sender.open();

		sender.sendMessage(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithBase64MessageAndAttachment.txt", message);
	}

	@Test
	public void mailWithBase64MessageAndAttachmentWithContentType() throws Exception {
		String mailInput = "<email>"
				+ "<recipients>"
					+ "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
				+ "</recipients>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">myself@address.org</from>"
				+ "<message>TXkgTWVzc2FnZSBHb2VzIEhlcmU=</message>"
				+ "<messageType>text/plain</messageType>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<charset>UTF-8</charset>"
				+ "<messageBase64>true</messageBase64>"
				+ "<attachments>"
					+ "<attachment name=\"test.txt\" base64=\"true\" type=\"abc/def\">VGhpcyBpcyBhIHRlc3QgZmlsZS4=</attachment>"
				+ "</attachments>"
			+ "</email>";

		sender.configure();
		sender.open();

		sender.sendMessage(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithBase64MessageAndAttachmentWithContentType.txt", message);
	}
	
	@Test
	public void mailWithBase64MessageAndAttachmentWithIllegalContentType() throws Exception {
		String mailInput = "<email>"
				+ "<recipients>"
					+ "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
				+ "</recipients>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">myself@address.org</from>"
				+ "<message>TXkgTWVzc2FnZSBHb2VzIEhlcmU=</message>"
				+ "<messageType>text/plain</messageType>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<charset>UTF-8</charset>"
				+ "<messageBase64>true</messageBase64>"
				+ "<attachments>"
					+ "<attachment name=\"test.txt\" base64=\"true\" type=\"messageTypeWithoutASlash\">VGhpcyBpcyBhIHRlc3QgZmlsZS4=</attachment>"
				+ "</attachments>"
			+ "</email>";

		sender.configure();
		sender.open();

		exception.expectMessage("mimeType [messageTypeWithoutASlash] of attachment [test.txt] must contain a forward slash ('/')");
		sender.sendMessage(new Message(mailInput), session);
	}
	
	@Test
	public void mailWithPRC() throws Exception {
		String mailInput = "<email/>";

		appendParameters(sender);

		sender.configure();
		sender.open();

		sender.sendMessage(new Message(mailInput), session);
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

		String attachmentsXml = "<attachment name=\"test1.txt\">This is test file 1.</attachment>"
				+ "<attachment name=\"test2.txt\" base64=\"true\">VGhpcyBpcyB0ZXN0IGZpbGUgMi4=</attachment>";
		appendAttachmentParameter(sender, attachmentsXml);

		sender.configure();
		sender.open();

		sender.sendMessage(new Message(mailInput), session);
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

		String attachmentsXml = "<attachment name=\"test1.txt\" sessionKey=\"attachment1\" />"
				+ "<attachment name=\"test2.txt\" sessionKey=\"attachment2\" base64=\"true\" />"
				+ "<attachment name=\"test3.txt\" sessionKey=\"attachment3\" />"
				+ "<attachment name=\"test4.txt\" sessionKey=\"attachment4\" base64=\"true\" />";
		appendAttachmentParameter(sender, attachmentsXml);

		sender.configure();
		sender.open();

		session.put("attachment1", "This is a text message.");
		session.put("attachment2", "VGhpcyBpcyBhIHRlc3QgZmlsZS4=");
		byte[] bytes = "This is a test file too.".getBytes();
		byte[] base64Bytes = "VGhpcyBpcyBhIGJhc2U2NCB0ZXN0IGZpbGUh".getBytes();
		session.put("attachment3", new ByteArrayInputStream(bytes));
		session.put("attachment4", new ByteArrayInputStream(base64Bytes));

		sender.sendMessage(new Message(mailInput), session);
		Session mailSession = (Session) session.get("mailSession");
		assertEquals("localhost", mailSession.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) mailSession.getProperties().get("MimeMessage");
		validateAuthentication(mailSession);
		compare("mailWithPRCAttachmentsFromSession.txt", message);
	}

	@Test
	public void mailWithNDR() throws Exception {
		if(!(sender instanceof MailSender)) return;

		String mailInput = "<email>"
				+ "<recipients>"
					+ "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
				+ "</recipients>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">myself@address.org</from>"
				+ "<message>My Message Goes Here</message>"
				+ "<messageType>text/plain</messageType>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<charset>UTF-8</charset>"
			+ "</email>";

		if(sender instanceof MailSender) {
			((MailSender) sender).setBounceAddress("my@bounce.nl");
		}

		sender.configure();
		sender.open();

		sender.sendMessage(new Message(mailInput), session);
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

		String mailInput = "<email>"
				+ "<recipients>"
					+ "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
				+ "</recipients>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">myself@address.org</from>"
				+ "<message>My Message Goes Here</message>"
				+ "<messageType>text/plain</messageType>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<charset>UTF-8</charset>"
				+ "<bounceAddress>my@bounce.nl</bounceAddress>"
			+ "</email>";

		sender.configure();
		sender.open();

		sender.sendMessage(new Message(mailInput), session);
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

		final String mailInput = "<email>"
				+ "<recipients>"
					+ "<recipient type=\"to\" name=\"dummy\">me@address.org</recipient>"
				+ "</recipients>"
				+ "<subject>My Subject</subject>"
				+ "<from name=\"Me, Myself and I\">myself@address.org</from>"
				+ "<message>My Message Goes Here</message>"
				+ "<messageType>text/plain</messageType>"
				+ "<replyTo>to@address.com</replyTo>"
				+ "<charset>UTF-8</charset>"
			+ "</email>";

		int threads = 50;
		ExecutorService service = Executors.newFixedThreadPool(10);
		Collection<Future<Session>> futures = new ArrayList<>(threads);
		for (int t = 0; t < threads; ++t) {
			Callable<Session> task = new Callable<Session>() {
				@Override
				public Session call() throws Exception {
					ISenderWithParameters sender2 = createSender();
					String bounce = "my"+Math.random()+"@bounce.com";
					((MailSender) sender2).setBounceAddress(bounce);

					sender2.configure();
					sender2.open();

					IPipeLineSession session1 = new PipeLineSessionBase();
					sender2.sendMessage(new Message(mailInput), session1);
					Session mailSession1 = (Session) session1.get("mailSession");
					mailSession1.getProperties().setProperty("bounce", bounce);

					IPipeLineSession session2 = new PipeLineSessionBase();
					sender2.sendMessage(new Message(mailInput), session2);
					Session mailSession2 = (Session) session2.get("mailSession");
					assertEquals("same session should be used", mailSession1, mailSession2);
					validateNDR(mailSession1, bounce);
					validateNDR(mailSession2, bounce);

					return mailSession1;
				}
			};
			futures.add(service.submit(task));
		}

		List<Session> sessions = new ArrayList<Session>();
		for (Future<Session> sessionFuture : futures) {
			try {
				Session session = sessionFuture.get();
				assertFalse("session ["+session+"] should not already exist", sessions.contains(session));

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
