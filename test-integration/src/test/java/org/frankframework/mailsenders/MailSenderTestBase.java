package org.frankframework.mailsenders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

org.frankframework.core.PipeLineSession;
		org.frankframework.core.SenderException;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;

/**
 *
 * @author alisihab
 *
 */
public abstract class MailSenderTestBase<M extends IMailSender> extends SenderTestBase<IMailSender> {
	private final String correlationID = "fakeCorrelationID";
	private final String examplesMainFolder = "/MailSender/";

	@Override
	public abstract M createSender();

	@Override
		public void setup() throws Exception {
			super.setup();
			session = new PipeLineSession();
			session.put("messageId", correlationID);
		}

	@Test
	public void testCompleteXMLFile() throws Exception {
		sendMessage(examplesMainFolder + "emailSample.xml");
	}

	@Test
	public void testEmptySubject() throws Exception {
		sendMessage(examplesMainFolder + "emailSampleEmptySubject.xml");
	}

	@Test
	public void testEmptySubjectUseDefault() throws Exception {
		sender.setDefaultSubject("subject ");
		sendMessage(examplesMainFolder + "emailSampleEmptySubject.xml");
	}

	@Test
	public void testEmptyFrom() throws Exception {
		assertThrows(SenderException.class, () -> sendMessage(examplesMainFolder + "emailSampleEmptyFrom.xml"));
	}

	@Test
	public void testEmptyMessage() throws Exception {
		assertThrows(SenderException.class, () -> sendMessage(examplesMainFolder + "emailSampleEmptyMessage.xml"));
	}

	@Test
	public void testHeader() throws Exception {
		sendMessage(examplesMainFolder + "emailSampleWithHeaders.xml");
	}

	@Test
	public void testEmptyRecipients() throws Exception {
		SenderException thrown = assertThrows(SenderException.class, () -> sendMessage(examplesMainFolder + "emailSampleEmptyRecipients.xml"));
		assertTrue(thrown.getMessage().contains("no recipients for message"));
	}

	@Test
	public void testWithAttachments() throws Exception {
		sendMessage(examplesMainFolder + "emailSampleWithAttachment.xml");
	}

	@Test
	public void testNullXMLFile() {
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
		sender.open();

		String result = sender.sendMessageOrThrow(new Message("<dummy><a>s</a></dummy>"), session).asString();
		assertEquals(correlationID, result);
	}

	@Test
	public void testParametersEmptyRecipients() {

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
		sender.open();

		assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(null, session));
	}

	@Test
	public void testAttachmentsFromSessionKey() {

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
		sender.open();

		String result = sender.sendMessageOrThrow(null, session).asString();
		assertEquals(correlationID, result);
	}

	@Test
	public void testAttachmentsContentBase64() {

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
		sender.open();

		String result = sender.sendMessageOrThrow(null, session).asString();
		assertEquals(correlationID, result);
	}

	public void sendMessage(String filePath) {
		sender.configure();
		sender.open();
		Message sampleMailXML = MessageTestUtils.getMessage(filePath);
		String result = sender.sendMessageOrThrow(sampleMailXML, session).asString();
		assertEquals(correlationID, result);
	}
}
