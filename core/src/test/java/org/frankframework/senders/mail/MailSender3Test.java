package org.frankframework.senders.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

import org.frankframework.core.SenderException;
import org.frankframework.parameters.Parameter;
import org.frankframework.senders.MailSender;
import org.frankframework.senders.SenderTestBase;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;

public class MailSender3Test extends SenderTestBase<MailSender> {

	private final String toAddress="testUser@localhost";
	private final String testUser="testUser";
	private final String testPassword="testPassword";

	private final String examplesMainFolder = "/MailSender/";

	private static final ServerSetup serverSetup = ServerSetupTest.SMTP;

	@RegisterExtension
	static GreenMailExtension greenMail = new GreenMailExtension(serverSetup);

	static {
		// Increase the timeout for the GreenMail server to start; default of 2000L fails on GitHub Actions
		serverSetup.setServerStartupTimeout(5_000L);
		serverSetup.dynamicPort();
	}

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
		return mailSender;
	}

	@Test
	public void testCompleteXMLFile() throws Exception {
		sendMailXml(examplesMainFolder + "emailSample.xml");
	}

	@Test
	public void testEmptySubject() throws Exception {
		sendMailXml(examplesMainFolder + "emailSampleEmptySubject.xml");
	}

	@Test
	public void testEmptySubjectUseDefault() throws Exception {
		sender.setDefaultSubject("subject ");
		sendMailXml(examplesMainFolder + "emailSampleEmptySubject.xml");
	}

	@Test
	public void testEmptyFrom() throws Exception {
		assertThrows(SenderException.class, () -> sendMailXml(examplesMainFolder + "emailSampleEmptyFrom.xml"));
	}

	@Test
	public void testEmptyMessage() throws Exception {
		assertThrows(SenderException.class, () -> sendMailXml(examplesMainFolder + "emailSampleEmptyMessage.xml"));
	}

	@Test
	public void testHeader() throws Exception {
		sendMailXml(examplesMainFolder + "emailSampleWithHeaders.xml");
	}

	@Test
	public void testEmptyRecipients() throws Exception {
		SenderException thrown = assertThrows(SenderException.class, () -> sendMailXml(examplesMainFolder + "emailSampleEmptyRecipients.xml"));
		assertTrue(thrown.getMessage().contains("no recipients for message"));
	}

	@Test
	public void testWithAttachments() throws Exception {
		sendMailXml(examplesMainFolder + "emailSampleWithAttachment.xml");
	}

	@Test
	public void testNullXMLFile() throws Exception {
		Parameter pFrom = new Parameter();
		pFrom.setName("from");
		pFrom.setValue("dummy@dummy.com");
		sender.addParameter(pFrom);

		Parameter pSubject = new Parameter();
		pSubject.setName("subject");
		pSubject.setDefaultValue("subject of the email");
		sender.addParameter(pSubject);

		Parameter pMessage = new Parameter();
		pMessage.setName("message");
		pMessage.setValue("message");
		sender.addParameter(pMessage);

		Parameter pRecip = new Parameter();
		pRecip.setName("recipients");
		pRecip.setValue("<recipient type=\"to\">john@doe.com</recipient>");
		sender.addParameter(pRecip);

		Parameter pMessageType = new Parameter();
		pMessageType.setName("messageType");
		pMessageType.setValue("text/plain");
		sender.addParameter(pMessageType);

		Parameter pMessageBase64 = new Parameter();
		pMessageBase64.setName("messageBase64");
		pMessageBase64.setValue("false");
		sender.addParameter(pMessageBase64);

		Parameter pCharSet = new Parameter();
		pCharSet.setName("charset");
		pCharSet.setValue("UTF-8");
		sender.addParameter(pCharSet);

		Parameter pThreadTopic = new Parameter();
		pThreadTopic.setName("threadTopic");
		pThreadTopic.setValue("subject");
		sender.addParameter(pThreadTopic);

		Parameter pAttachments = new Parameter();
		pAttachments.setName("attachments");
		pAttachments.setValue("<attachment name=\"filename1\" type=\"text/plain\">This is the first attachment</attachment>");
		sender.addParameter(pAttachments);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(new Message("<dummy><a>s</a></dummy>"), session).asString();
		assertEquals(testMessageId, result);
	}

	@Test
	public void testParametersEmptyRecipients() throws Exception {

		Parameter pFrom = new Parameter();
		pFrom.setName("from");
		pFrom.setDefaultValue("dummy@dummy.com");
		sender.addParameter(pFrom);

		Parameter pSubject = new Parameter();
		pSubject.setName("subject");
		pSubject.setDefaultValue("subject of the email");
		sender.addParameter(pSubject);

		Parameter pMessage = new Parameter();
		pMessage.setName("message");
		pMessage.setValue("message");
		sender.addParameter(pMessage);

		Parameter pRecip = new Parameter();
		pRecip.setName("recipients");
		pRecip.setValue("");
		sender.addParameter(pRecip);

		Parameter pMessageType = new Parameter();
		pMessageType.setName("messageType");
		pMessageType.setValue("text/plain");
		sender.addParameter(pMessageType);

		Parameter pMessageBase64 = new Parameter();
		pMessageBase64.setName("messageBase64");
		pMessageBase64.setValue("false");
		sender.addParameter(pMessageBase64);

		Parameter pCharSet = new Parameter();
		pCharSet.setName("charset");
		pCharSet.setValue("UTF-8");
		sender.addParameter(pCharSet);

		Parameter pThreadTopic = new Parameter();
		pThreadTopic.setName("threadTopic");
		pThreadTopic.setValue("subject");
		sender.addParameter(pThreadTopic);

		Parameter pAttachments = new Parameter();
		pAttachments.setName("attachments");
		pAttachments.setValue("<attachment name=\"filename1\" type=\"text/plain\">This is the first attachment</attachment>");
		sender.addParameter(pAttachments);

		sender.configure();
		sender.start();

		assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(null, session));
	}

	@Test
	public void testAttachmentsFromSessionKey() throws Exception {

		Parameter pFrom = new Parameter();
		pFrom.setName("from");
		pFrom.setDefaultValue("dummy@dummy.com");
		sender.addParameter(pFrom);

		Parameter pSubject = new Parameter();
		pSubject.setName("subject");
		pSubject.setDefaultValue("subject of the email");
		sender.addParameter(pSubject);

		Parameter pMessage = new Parameter();
		pMessage.setName("message");
		pMessage.setValue("message");
		sender.addParameter(pMessage);

		Parameter pRecip = new Parameter();
		pRecip.setName("recipients");
		pRecip.setValue("<recipient type=\"to\">john@doe.com</recipient>");
		sender.addParameter(pRecip);

		Parameter pMessageType = new Parameter();
		pMessageType.setName("messageType");
		pMessageType.setValue("text/plain");
		sender.addParameter(pMessageType);

		Parameter pMessageBase64 = new Parameter();
		pMessageBase64.setName("messageBase64");
		pMessageBase64.setValue("false");
		sender.addParameter(pMessageBase64);

		Parameter pCharSet = new Parameter();
		pCharSet.setName("charset");
		pCharSet.setValue("UTF-8");
		sender.addParameter(pCharSet);

		Parameter pThreadTopic = new Parameter();
		pThreadTopic.setName("threadTopic");
		pThreadTopic.setValue("subject");
		sender.addParameter(pThreadTopic);

		session.put("attachment", TestFileUtils.getTestFileURL(examplesMainFolder + "emailSample.xml").openStream());
		Parameter pAttachments = new Parameter();
		pAttachments.setName("attachments");
		pAttachments.setValue("<attachment name=\"filename1\" type=\"text/plain\">This is the first attachment</attachment>"
				+ "<attachment name=\"email.xml\" sessionKey=\"attachment\"> </attachment>");
		sender.addParameter(pAttachments);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(null, session).asString();
		assertEquals(testMessageId, result);
	}

	@Test
	public void testAttachmentsContentBase64() throws Exception {

		Parameter pFrom = new Parameter();
		pFrom.setName("from");
		pFrom.setDefaultValue("dummy@dummy.com");
		sender.addParameter(pFrom);

		Parameter pSubject = new Parameter();
		pSubject.setName("subject");
		pSubject.setDefaultValue("subject of the email");
		sender.addParameter(pSubject);

		Parameter pMessage = new Parameter();
		pMessage.setName("message");
		pMessage.setValue("message");
		sender.addParameter(pMessage);

		Parameter pRecip = new Parameter();
		pRecip.setName("recipients");
		pRecip.setValue("<recipient type=\"to\">john@doe.com</recipient>");
		sender.addParameter(pRecip);

		Parameter pMessageType = new Parameter();
		pMessageType.setName("messageType");
		pMessageType.setValue("text/plain");
		sender.addParameter(pMessageType);

		Parameter pMessageBase64 = new Parameter();
		pMessageBase64.setName("messageBase64");
		pMessageBase64.setValue("false");
		sender.addParameter(pMessageBase64);

		Parameter pAttachments = new Parameter();
		pAttachments.setName("attachments");
		//This is the first attachment base64 encoded
		pAttachments.setValue("<attachment name=\"filename1\" base64=\"true\" type=\"text/plain\">VGhpcyBpcyB0aGUgZmlyc3QgYXR0YWNobWVudA==</attachment>");
		sender.addParameter(pAttachments);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(null, session).asString();
		assertEquals(testMessageId, result);
	}

	public void sendMailXml(String filePath) throws Exception {
		sender.configure();
		sender.start();
		Message sampleMailXML = MessageTestUtils.getMessage(filePath);
		String result = sender.sendMessageOrThrow(sampleMailXML, session).asString();
		assertEquals(testMessageId, result);
	}
}
