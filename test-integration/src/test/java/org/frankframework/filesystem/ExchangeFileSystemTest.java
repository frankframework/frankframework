package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.receivers.ExchangeMailListener;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.testutil.PropertyUtil;

public class ExchangeFileSystemTest extends MailFileSystemTestBase<ExchangeMessageReference, ExchangeAttachmentReference, ExchangeFileSystem>{

	//private String DEFAULT_URL = "https://outlook.office365.com/EWS/Exchange.asmx";

	private final String url = PropertyUtil.getProperty(PROPERTY_FILE, "url");
	private final String mailaddress = PropertyUtil.getProperty(PROPERTY_FILE, "mailaddress");
//	private String basefolder2 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder2");
//	private String basefolder3 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder3");
	private final String client_id = PropertyUtil.getProperty(PROPERTY_FILE, "client_id");
	private final String client_secr = PropertyUtil.getProperty(PROPERTY_FILE, "client_secret");
	private final String tenantId = PropertyUtil.getProperty(PROPERTY_FILE, "tenant_id");


	private final String nonExistingFileName = "AAMkAGNmZTczMWUwLWQ1MDEtNDA3Ny1hNjU4LTlmYTQzNjE0NjJmYgBGAAAAAAALFKqetECyQKQyuRBrRSzgBwDx14SZku4LS5ibCBco+nmXAAAAAAEMAADx14SZku4LS5ibCBco+nmXAABMFuwsAAA=";

	@Override
	protected ExchangeFileSystem createFileSystem() throws ConfigurationException {
		ExchangeFileSystem fileSystem = new ExchangeFileSystem();
		if (StringUtils.isNotEmpty(url)) fileSystem.setUrl(url);
		fileSystem.setMailAddress(mailaddress);
		fileSystem.setBaseFolder(basefolder1);
		fileSystem.setClientId(client_id);
		fileSystem.setClientSecret(client_secr);
		fileSystem.setTenantId(tenantId);
		return fileSystem;
	}

	@Test
	public void fileSystemTestRandomFileShouldNotExist() throws Exception {
		fileSystemTestRandomFileShouldNotExist(nonExistingFileName);
	}

	public ExchangeMailListener getConfiguredListener(String sourceFolder, String inProcessFolder) throws ConfigurationException {
		ExchangeMailListener listener = new ExchangeMailListener();
		autowireByName(listener);
		if (StringUtils.isNotEmpty(url)) listener.setUrl(url);
		listener.setClientId(client_id);
		listener.setClientSecret(client_secr);
		listener.setTenantId(tenantId);
		listener.setMailAddress(mailaddress);
		listener.setBaseFolder(basefolder1);
		listener.setInputFolder(sourceFolder);
		if (inProcessFolder!=null) listener.setInProcessFolder(inProcessFolder);
		listener.configure();
		listener.start();
		return listener;
	}

	@Test
	public void testMessageIdDoesNotChangeWhenMessageIsMoved() throws Exception {
		String sourceFolder = "SourceFolder";
		String inProcessFolder = "InProcessFolder";

		ExchangeMessageReference orgMsg = prepareFolderAndGetFirstMessage(sourceFolder);
		if (!fileSystem.folderExists(inProcessFolder)) {
			fileSystem.createFolder(inProcessFolder);
		}
		String messageId = (String)fileSystem.getAdditionalFileProperties(orgMsg).get("Message-ID");

		ExchangeMailListener listener1 = getConfiguredListener(sourceFolder, null);
		ExchangeMailListener listener2 = getConfiguredListener(sourceFolder, inProcessFolder);

		Map<String,Object> threadContext1 = new HashMap<>();
		Map<String,Object> threadContext2 = new HashMap<>();

		RawMessageWrapper<ExchangeMessageReference> msg1 = listener1.getRawMessage(threadContext1);
		String originalFilename = (String) threadContext1.get("originalFilename");
		Map<String,Object> messageContext1 = listener1.extractMessageProperties(msg1.getRawMessage(), originalFilename);
		String msgId1 = (String) messageContext1.get(PipeLineSession.MESSAGE_ID_KEY);
		System.out.println("1st msgid ["+msgId1+"], filename ["+fileSystem.getName(msg1.getRawMessage())+"]");


		RawMessageWrapper<ExchangeMessageReference> msg2 = listener2.getRawMessage(threadContext2);
		Map<String,Object> messageContext2 = listener2.extractMessageProperties(msg2.getRawMessage(), originalFilename);
		String msgId2 = (String) messageContext2.get(PipeLineSession.MESSAGE_ID_KEY);
		System.out.println("2nd msgid ["+msgId2+"], filename ["+fileSystem.getName(msg2.getRawMessage())+"]");

		assertEquals(msgId1, msgId2);
		assertEquals(msg1.getId(), msgId1);

		assertEquals(messageId, msgId2);
	}

}
