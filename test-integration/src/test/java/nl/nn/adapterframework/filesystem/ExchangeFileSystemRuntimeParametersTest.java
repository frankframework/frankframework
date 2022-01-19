package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.property.complex.Attachment;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.receivers.ExchangeMailListener;
import nl.nn.adapterframework.testutil.PropertyUtil;

public class ExchangeFileSystemTest extends MailFileSystemTestBase<EmailMessage, Attachment, ExchangeFileSystem>{

	//private String DEFAULT_URL = "https://outlook.office365.com/EWS/Exchange.asmx";
	
	private String url         = PropertyUtil.getProperty(PROPERTY_FILE, "url");
	private String mailaddress = PropertyUtil.getProperty(PROPERTY_FILE, "mailaddress");
	private String accessToken = PropertyUtil.getProperty(PROPERTY_FILE, "accessToken");
//	private String basefolder2 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder2");
//	private String basefolder3 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder3");
	
	
	private String nonExistingFileName = "AAMkAGNmZTczMWUwLWQ1MDEtNDA3Ny1hNjU4LTlmYTQzNjE0NjJmYgBGAAAAAAALFKqetECyQKQyuRBrRSzgBwDx14SZku4LS5ibCBco+nmXAAAAAAEMAADx14SZku4LS5ibCBco+nmXAABMFuwsAAA=";
	
	@Override
	protected ExchangeFileSystem createFileSystem() throws ConfigurationException {
		ExchangeFileSystem fileSystem = new ExchangeFileSystem();
		if (StringUtils.isNotEmpty(url)) fileSystem.setUrl(url);
		fileSystem.setMailAddress(mailaddress);
		fileSystem.setAccessToken(accessToken);
		fileSystem.setUsername(username);
		fileSystem.setPassword(password);
		fileSystem.setBaseFolder(basefolder1);
		return fileSystem;
	}

	@Test
	public void fileSystemTestRandomFileShouldNotExist() throws Exception {
		fileSystemTestRandomFileShouldNotExist(nonExistingFileName);
	}

	
	public ExchangeMailListener getConfiguredListener(String sourceFolder, String inProcessFolder) throws Exception {
		ExchangeMailListener listener = new ExchangeMailListener();
		if (StringUtils.isNotEmpty(url)) listener.setUrl(url);
		listener.setMailAddress(mailaddress);
		listener.setAccessToken(accessToken);
		listener.setUsername(username);
		listener.setPassword(password);
		listener.setBaseFolder(basefolder1);
		listener.setInputFolder(sourceFolder);
		if (inProcessFolder!=null) listener.setInProcessFolder(inProcessFolder);
		listener.configure();
		listener.open();
		return listener;
	}
	
	@Test
	public void testMessageIdDoesNotChangeWhenMessageIsMoved() throws Exception {
		String sourceFolder = "SourceFolder";
		String inProcessFolder = "InProcessFolder";

		EmailMessage orgMsg = prepareFolderAndGetFirstMessage(sourceFolder, null);
		if (!fileSystem.folderExists(inProcessFolder)) {
			fileSystem.createFolder(inProcessFolder);
		}
		String messageId = (String)fileSystem.getAdditionalFileProperties(orgMsg).get("Message-ID");

		ExchangeMailListener listener1 = getConfiguredListener(sourceFolder, null);
		ExchangeMailListener listener2 = getConfiguredListener(sourceFolder, inProcessFolder);
		
		Map<String,Object> threadContext1 = new HashMap<>();
		Map<String,Object> threadContext2 = new HashMap<>();
		
		EmailMessage msg1 = listener1.getRawMessage(threadContext1);
		String msgId1 = listener1.getIdFromRawMessage(msg1, threadContext1);
		System.out.println("1st msgid ["+msgId1+"], filename ["+fileSystem.getName(msg1)+"]");
		
		
		EmailMessage msg2 = listener2.getRawMessage(threadContext2);
		String msgId2 = listener2.getIdFromRawMessage(msg2, threadContext2);
		System.out.println("2nd msgid ["+msgId2+"], filename ["+fileSystem.getName(msg2)+"]");
		
		assertEquals(msgId1, msgId2);
		
		assertEquals(messageId, msgId2);
	}
	
}
