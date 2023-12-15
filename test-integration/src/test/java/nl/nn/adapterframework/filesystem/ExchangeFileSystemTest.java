package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.receivers.ExchangeMailListener;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.testutil.PropertyUtil;

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


	public ExchangeMailListener getConfiguredListener(String sourceFolder, String inProcessFolder) {
		ExchangeMailListener listener = new ExchangeMailListener();
		autowireByName(listener);
		if (StringUtils.isNotEmpty(url)) listener.setUrl(url);
		listener.setMailAddress(mailaddress);
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

		ExchangeMessageReference orgMsg = prepareFolderAndGetFirstMessage(sourceFolder, null);
		if (!fileSystem.folderExists(inProcessFolder)) {
			fileSystem.createFolder(inProcessFolder);
		}
		String messageId = (String)fileSystem.getAdditionalFileProperties(orgMsg).get("Message-ID");

		ExchangeMailListener listener1 = getConfiguredListener(sourceFolder, null);
		ExchangeMailListener listener2 = getConfiguredListener(sourceFolder, inProcessFolder);

		Map<String,Object> threadContext1 = new HashMap<>();
		Map<String,Object> threadContext2 = new HashMap<>();

		RawMessageWrapper<ExchangeMessageReference> msg1 = listener1.getRawMessage(threadContext1);
		Map<String,Object> messageContext1 = listener1.populateContextFromMessage(msg1.getRawMessage(), threadContext1);
		String msgId1 = (String) messageContext1.get(PipeLineSession.MESSAGE_ID_KEY);
		System.out.println("1st msgid ["+msgId1+"], filename ["+fileSystem.getName(msg1)+"]");


		RawMessageWrapper<ExchangeMessageReference> msg2 = listener2.getRawMessage(threadContext2);
		Map<String,Object> messageContext2 = listener2.populateContextFromMessage(msg2.getRawMessage(), threadContext2);
		String msgId2 = (String) messageContext2.get(PipeLineSession.MESSAGE_ID_KEY);
		System.out.println("2nd msgid ["+msgId2+"], filename ["+fileSystem.getName(msg2)+"]");

		assertEquals(msgId1, msgId2);
		assertEquals(msg1.getId(), msgId1);

		assertEquals(messageId, msgId2);
	}

}
