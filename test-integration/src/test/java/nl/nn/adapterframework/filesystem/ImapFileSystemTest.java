package nl.nn.adapterframework.filesystem;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeBodyPart;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.testutil.PropertyUtil;

public class ImapFileSystemTest  extends MailFileSystemTestBase<Message, MimeBodyPart, ImapFileSystem>{

	private String PROPERTY_FILE = "ExchangeMail.properties";
	private int port = 993;
	private String host = "outlook.office365.com";

	private String username    = PropertyUtil.getProperty(PROPERTY_FILE, "username");
	private String password    = PropertyUtil.getProperty(PROPERTY_FILE, "password");
	private String basefolder1 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder1");


	@Override
	protected ImapFileSystem createFileSystem() throws ConfigurationException {
		ImapFileSystem fileSystem = new ImapFileSystem();
		fileSystem.setHost(host);
		fileSystem.setPort(port);
		fileSystem.setUsername(username);
		fileSystem.setPassword(password);
		fileSystem.setBaseFolder(basefolder1);
		return fileSystem;
	}

}
