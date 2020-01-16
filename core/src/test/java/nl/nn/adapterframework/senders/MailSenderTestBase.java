package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.TestAssertions;

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
		sender.sendMessage(null, mailInput);
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

		TestAssertions.assertEqualsIgnoreCRLF(expected, rawResult);
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

		ParameterResolutionContext prc = new ParameterResolutionContext(mailInput, session);
		sender.sendMessage(null, mailInput, prc);
		Session session = (Session) prc.getSession().get("mailSession");
		assertEquals("localhost", session.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) session.getProperties().get("MimeMessage");
		compare("mailWithMultipleRecipients.txt", message);
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

		ParameterResolutionContext prc = new ParameterResolutionContext(mailInput, session);
		sender.sendMessage(null, mailInput, prc);
		Session session = (Session) prc.getSession().get("mailSession");
		assertEquals("localhost", session.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) session.getProperties().get("MimeMessage");
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

		ParameterResolutionContext prc = new ParameterResolutionContext(mailInput, session);
		sender.sendMessage(null, mailInput, prc);
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

		ParameterResolutionContext prc = new ParameterResolutionContext(mailInput, session);
		sender.sendMessage(null, mailInput, prc);
		Session session = (Session) prc.getSession().get("mailSession");
		assertEquals("localhost", session.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) session.getProperties().get("MimeMessage");
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

		ParameterResolutionContext prc = new ParameterResolutionContext(mailInput, session);
		sender.sendMessage(null, mailInput, prc);
		Session session = (Session) prc.getSession().get("mailSession");
		assertEquals("localhost", session.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) session.getProperties().get("MimeMessage");
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

		ParameterResolutionContext prc = new ParameterResolutionContext(mailInput, session);
		sender.sendMessage(null, mailInput, prc);
		Session session = (Session) prc.getSession().get("mailSession");
		assertEquals("localhost", session.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) session.getProperties().get("MimeMessage");
		compare("mailWithBase64MessageAndAttachment.txt", message);
	}

	@Test
	public void mailWithPRC() throws Exception {
		String mailInput = "<email/>";

		appendParameters(sender);

		sender.configure();
		sender.open();

		ParameterResolutionContext prc = new ParameterResolutionContext(mailInput, session);
		sender.sendMessage(null, mailInput, prc);
		Session session = (Session) prc.getSession().get("mailSession");
		assertEquals("localhost", session.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) session.getProperties().get("MimeMessage");
		validateAuthentication(session);
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

		ParameterResolutionContext prc = new ParameterResolutionContext(mailInput, session);

		sender.sendMessage(null, mailInput, prc);
		Session session = (Session) prc.getSession().get("mailSession");
		assertEquals("localhost", session.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) session.getProperties().get("MimeMessage");
		validateAuthentication(session);
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

		ParameterResolutionContext prc = new ParameterResolutionContext(mailInput, session);
		session.put("attachment1", "This is a text message.");
		session.put("attachment2", "VGhpcyBpcyBhIHRlc3QgZmlsZS4=");
		byte[] bytes = "This is a test file too.".getBytes();
		byte[] base64Bytes = "VGhpcyBpcyBhIGJhc2U2NCB0ZXN0IGZpbGUh".getBytes();
		session.put("attachment3", new ByteArrayInputStream(bytes));
		session.put("attachment4", new ByteArrayInputStream(base64Bytes));

		sender.sendMessage(null, mailInput, prc);
		Session session = (Session) prc.getSession().get("mailSession");
		assertEquals("localhost", session.getProperty("mail.smtp.host"));

		MimeMessage message = (MimeMessage) session.getProperties().get("MimeMessage");
		compare("mailWithPRCAttachmentsFromSession.txt", message);
	}
}
