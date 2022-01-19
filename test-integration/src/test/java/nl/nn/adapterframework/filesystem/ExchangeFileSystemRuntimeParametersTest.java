package nl.nn.adapterframework.filesystem;

import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.property.complex.Attachment;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.receivers.ExchangeMailListener;
import nl.nn.adapterframework.testutil.PropertyUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ExchangeFileSystemRuntimeParametersTest  extends MailFileSystemTestBase<EmailMessage, Attachment, ExchangeFileSystem>{

	//private String DEFAULT_URL = "https://outlook.office365.com/EWS/Exchange.asmx";

	private String url         = PropertyUtil.getProperty(PROPERTY_FILE, "url");
	private String mailaddress = PropertyUtil.getProperty(PROPERTY_FILE, "mailaddress");
	private String accessToken = PropertyUtil.getProperty(PROPERTY_FILE, "accessToken");
//	private String basefolder2 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder2");
//	private String basefolder3 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder3");

	private final String SEPARATOR = "|";


	private String nonExistingFileName = "AAMkAGNmZTczMWUwLWQ1MDEtNDA3Ny1hNjU4LTlmYTQzNjE0NjJmYgBGAAAAAAALFKqetECyQKQyuRBrRSzgBwDx14SZku4LS5ibCBco+nmXAAAAAAEMAADx14SZku4LS5ibCBco+nmXAABMFuwsAAA=";

	@Override
	protected ExchangeFileSystem createFileSystem() throws ConfigurationException {
		ExchangeFileSystem fileSystem = new ExchangeFileSystem();
		if (StringUtils.isNotEmpty(url)) fileSystem.setUrl(url);
		fileSystem.setAccessToken(accessToken);
		fileSystem.setUsername(username);
		fileSystem.setPassword(password);
		return fileSystem;
	}

	@Test
	public void testFolders() throws Exception {
		testFolders(constructFolderName(mailaddress, "Test folder A"));
	}

	@Test
	public void testFiles() throws Exception {
		testFiles(constructFolderName(mailaddress, "Test folder A"),
			constructFolderName(mailaddress, "Test folder B"), "messageFolder");
	}

	private String constructFolderName(String mailbox, String folderName){
		return mailbox + SEPARATOR + folderName;
	}
}
