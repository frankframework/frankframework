package org.frankframework.filesystem;

import java.io.InputStream;
import java.io.OutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.filesystem.mock.MockFile;
import org.frankframework.senders.MailSender;
import org.frankframework.stream.Message;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlBuilder;

public class MailSendingTestHelper implements IFileSystemTestHelper {

	private String senderDestination="";
	private String senderSmtpHost="";
	private String senderUserId="";
	private String senderPassword="";

	private MailSender mailSender;

	private MockFile currentFile;

	public MailSendingTestHelper(String senderDestination, String senderSmtpHost, int senderSmtpPort, boolean senderSsl, String senderUserId, String senderPassword) {
		this.senderDestination=senderDestination;
		this.senderSmtpHost=senderSmtpHost;
		this.senderUserId=senderUserId;
		this.senderPassword=senderPassword;
	}

	@BeforeEach
	public void setUp() throws ConfigurationException, SenderException {
//		mailSender=new SendGridSender();
		mailSender=new MailSender();
		mailSender.setName("MailSendingTestHelper");
		mailSender.setSmtpHost(senderSmtpHost);
		//mailSender.setProperties(properties);
		mailSender.setUserId(senderUserId);
		mailSender.setPassword(senderPassword);
		mailSender.setDefaultMessageType("text/plain");
		mailSender.configure();
		mailSender.open();
	}


	@AfterEach
	public void tearDown() throws SenderException {
		mailSender.close();
	}


	@Override
	public boolean _fileExists(String folder, String filename) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean _folderExists(String folderName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void _deleteFile(String folder, String filename) {
		// TODO Auto-generated method stub

	}

	@Override
	public OutputStream _createFile(String folder, String filename) {
		currentFile=new MockFile(filename, null);
		return currentFile.getOutputStream(true);
	}

	@Override
	public InputStream _readFile(String folder, String filename) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void _createFolder(String foldername) {
		// TODO Auto-generated method stub

	}

	@Override
	public void _deleteFolder(String folderName) {
		// TODO Auto-generated method stub

	}

//	<email>
//    <recipients>
//       <recipient type="to">***@hotmail.com</recipient>
//       <recipient type="cc">***@gmail.com</recipient>
//    </recipients>
//    <from>***@yahoo.com</from>
//    <subject>This is the subject</subject>
//    <threadTopic>subject</threadTopic>
//    <message>This is the message</message>
//    <messageType>text/plain</messageType><!-- Optional -->
//    <messageBase64>false</messageBase64><!-- Optional -->
//    <charset>UTF-8</charset><!-- Optional -->
//    <attachments>
//       <attachment name="filename1.txt">This is the first attachment</attachment>
//       <attachment name="filename2.pdf" base64="true">JVBERi0xLjQKCjIgMCBvYmoKPDwvVHlwZS9YT2JqZWN0L1N1YnR5cGUvSW1...vSW5mbyA5IDAgUgo+PgpzdGFydHhyZWYKMzQxNDY2CiUlRU9GCg==</attachment>
//       <attachment name="filename3.pdf" url="file:/c:/filename3.pdf"/>
//       <attachment name="filename4.pdf" sessionKey="fileContent"/>
//    </attachments><!-- Optional -->
//</email>

	public void _commitFile() throws Exception {
		XmlBuilder email=new XmlBuilder("email");
		XmlBuilder recipients=new XmlBuilder("recipients");
		email.addSubElement(recipients);
		XmlBuilder recipient=new XmlBuilder("recipient");
		recipients.addSubElement(recipient);
		recipient.addAttribute("type", "to");
		recipient.setValue(senderDestination);

		XmlBuilder from=new XmlBuilder("from");
		email.addSubElement(from);
		from.setValue("gerrit@integrationparners.nl");

		XmlBuilder subject=new XmlBuilder("subject");
		email.addSubElement(subject);
		subject.setValue(currentFile.getName());

		XmlBuilder threadTopic=new XmlBuilder("threadTopic");
		email.addSubElement(threadTopic);
		threadTopic.setValue("integrationtests");

		XmlBuilder message=new XmlBuilder("message");
		email.addSubElement(message);
		message.setValue(StreamUtil.streamToString(currentFile.getInputStream()));

		Message msg = email.asMessage();
		System.out.println("email: ["+msg+"]");
		mailSender.sendMessageOrThrow(msg, null);
		Thread.sleep(5000);
	}
}
