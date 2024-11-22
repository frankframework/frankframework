package org.frankframework.filesystem;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeBodyPart;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.testutil.PropertyUtil;

public class ImapFileSystemTest extends MailFileSystemTestBase<Message, MimeBodyPart, ImapFileSystem>{

	private final String PROPERTY_FILE = "ExchangeMail.properties";
	private final int port = 993;
	private final String host = "outlook.office365.com";

	private final String username = PropertyUtil.getProperty(PROPERTY_FILE, "username");
	private final String password = PropertyUtil.getProperty(PROPERTY_FILE, "password");
	private final String basefolder1 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder1");


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
