package nl.nn.adapterframework.mailsenders;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;

/**
 * MailSender can be replaced with MailSenderNew to test MailSenderNew vice versa
 * 
 * @author alisihab
 *
 */
public abstract class MailSenderTestBase<M extends IMailSender> extends SenderTestBase<IMailSender> {

	private String examplesMainFolder = "/MailSender/";
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Override
	public abstract M createSender();

	@Test
	public void testCompleteXMLFile() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sendMessage(examplesMainFolder + "emailSample.xml");
	}

	@Test
	public void testEmptySubject() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sendMessage(examplesMainFolder + "emailSampleEmptySubject.xml");
	}

	@Test
	public void testEmptySubjectUseDefault()
			throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setDefaultSubject("subject ");
		sendMessage(examplesMainFolder + "emailSampleEmptySubject.xml");
	}

	@Test
	public void testEmptyFrom() throws SenderException, TimeOutException, ConfigurationException, IOException {
		exception.expect(SenderException.class);
		sendMessage(examplesMainFolder + "emailSampleEmptyFrom.xml");
	}

	@Test
	public void testEmptyMessage() throws SenderException, TimeOutException, ConfigurationException, IOException {
		exception.expect(SenderException.class);
		sendMessage(examplesMainFolder + "emailSampleEmptyMessage.xml");
	}

	@Test
	public void testHeader() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sendMessage(examplesMainFolder + "emailSampleWithHeaders.xml");
	}

	@Test
	public void testEmptyRecipients() throws SenderException, TimeOutException, ConfigurationException, IOException {
		exception.expectMessage("no recipients for message");
		sendMessage(examplesMainFolder + "emailSampleEmptyRecipients.xml");
	}

	@Test
	public void testWithAttachments() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sendMessage(examplesMainFolder + "emailSampleWithAttachment.xml");
	}

	@Test
	public void testNullXMLFile() throws SenderException, TimeOutException, ConfigurationException, IOException {
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

		Parameter pAttachments = new Parameter();
		pAttachments.setName("attachments");
		pAttachments.setValue("<attachment name=\"filename1\" type=\"txt\">This is the first attachment</attachment>");
		sender.addParameter(pAttachments);

		PipeLineSession session = new PipeLineSession();
		sender.configure();
		sender.open();

		String correlationId = "FakeCorrelationId";
		String result = sender.sendMessage(new Message("<dummy><a>s</a></dummy>"), session).asString();
		assertEquals(correlationId, result);
	}

	@Test
	public void testParametersEmptyRecipients()
			throws SenderException, TimeOutException, ConfigurationException, IOException {
		exception.expect(SenderException.class);

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
		pAttachments.setValue("<attachment name=\"filename1\" type=\"txt\">This is the first attachment</attachment>");
		sender.addParameter(pAttachments);

		PipeLineSession session = new PipeLineSession();

		sender.configure();
		sender.open();

		String correlationID = "fakeCorrelationID";
		String result = sender.sendMessage(null, session).asString();
		assertEquals(correlationID, result);
	}

	@Test
	public void testAttachmentsFromSessionKey()
			throws SenderException, TimeOutException, ConfigurationException, IOException {

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

		PipeLineSession session = new PipeLineSession();
		session.put("attachment",
				new URL("file:///" + getClass().getResource(examplesMainFolder + "emailSample.xml").getPath())
						.openStream());
		Parameter pAttachments = new Parameter();
		pAttachments.setName("attachments");
		pAttachments.setValue("<attachment name=\"filename1\" type=\"txt\">This is the first attachment</attachment>"
				+ "<attachment name=\"email.xml\" sessionKey=\"attachment\"> </attachment>");
		sender.addParameter(pAttachments);

		sender.configure();
		sender.open();

		String correlationID = "fakeCorrelationID";
		String result = sender.sendMessage(null, session).asString();
		assertEquals(correlationID, result);
	}

	@Test
	public void testAttachmentsContentBase64()
			throws SenderException, TimeOutException, ConfigurationException, IOException {

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
		pAttachments.setValue(
				"<attachment name=\"filename1\" base64=\"true\" type=\"txt\">VGhpcyBpcyB0aGUgZmlyc3QgYXR0YWNobWVudA==</attachment>");
		sender.addParameter(pAttachments);

		PipeLineSession session = new PipeLineSession();
		sender.configure();
		sender.open();

		String correlationID = "fakeCorrelationID";
		String result = sender.sendMessage(null, session).asString();
		assertEquals(correlationID, result);
	}

	public void sendMessage(String filePath)
			throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.configure();
		sender.open();
		Reader fileReader = new FileReader(getClass().getResource(filePath).getFile());
		BufferedReader bufReader = new BufferedReader(fileReader);
		StringBuilder sb = new StringBuilder();
		String line = bufReader.readLine();
		while (line != null) {
			sb.append(line).append("\n");
			line = bufReader.readLine();
		}
		bufReader.close();
		String xml2String = sb.toString();
		Message sampleMailXML = new Message(xml2String);
		String correlationID = "fakeCorrelationID";
		String result = sender.sendMessage(sampleMailXML, session).asString();
		assertEquals(correlationID, result);
	}
}
