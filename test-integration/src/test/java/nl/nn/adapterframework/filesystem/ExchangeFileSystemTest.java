package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.receivers.ExchangeMailListener;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.PropertyUtil;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.xml.SaxElementBuilder;

public class ExchangeFileSystemTest extends SelfContainedBasicFileSystemTest<EmailMessage, ExchangeFileSystem>{

	private String PROPERTY_FILE = "ExchangeMail.properties";
	
	//private String DEFAULT_URL = "https://outlook.office365.com/EWS/Exchange.asmx";
	
	private String url         = PropertyUtil.getProperty(PROPERTY_FILE, "url");
	private String mailaddress = PropertyUtil.getProperty(PROPERTY_FILE, "mailaddress");
	private String accessToken = PropertyUtil.getProperty(PROPERTY_FILE, "accessToken");
	private String username    = PropertyUtil.getProperty(PROPERTY_FILE, "username");
	private String password    = PropertyUtil.getProperty(PROPERTY_FILE, "password");
	private String basefolder1 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder1");
	private String basefolder2 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder2");
	private String basefolder3 = PropertyUtil.getProperty(PROPERTY_FILE, "basefolder3");
	
	
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
	public void fileSystemTestListFileFromInbox() throws Exception {
		fileSystemTestListFile(1, null);
	}

	@Test
	public void fileSystemTestRandomFileShouldNotExist() throws Exception {
		fileSystemTestRandomFileShouldNotExist(nonExistingFileName);
	}

	@Test
	public void fileSystemTestListFileWithXmlProblem() throws Exception {
		fileSystemTestListFile(1, "XmlProblem");
	}

	@Test
	public void testExtractNormalMessage() throws Exception {
		EmailMessage emailMessage = getFirstFileFromFolder(null);
		SaxElementBuilder xml = new SaxElementBuilder("email");
		fileSystem.extractEmail(emailMessage, xml);
		xml.close();
		String expected = TestFileUtils.getTestFile("/ExchangeMailNormal.xml");
		MatchUtils.assertXmlEquals(expected, xml.toString());
	}

	@Test
	public void testExtractNormalMessageSimple() throws Exception {
		EmailMessage emailMessage = getFirstFileFromFolder(null);
		SaxElementBuilder xml = new SaxElementBuilder("email");
		FileSystemUtils.addEmailInfoSimple(fileSystem, emailMessage, xml);
		xml.close();
		String expected = TestFileUtils.getTestFile("/ExchangeMailNormalSimple.xml");
		MatchUtils.assertXmlEquals(expected, xml.toString());
	}

	@Test
	public void testExtractProblematicMessage() throws Exception {
		EmailMessage emailMessage = getFirstFileFromFolder("XmlProblem");
		SaxElementBuilder xml = new SaxElementBuilder("email");
		fileSystem.extractEmail(emailMessage, xml);
		xml.close();
		String expected = TestFileUtils.getTestFile("/ExchangeMailProblem.xml");
		MatchUtils.assertXmlEquals(expected,xml.toString());
	}
	
	@Test
	public void testExtractMessageWithMessageAttached() throws Exception {
		EmailMessage emailMessage = getFirstFileFromFolder("AttachedMessage");
		SaxElementBuilder xml = new SaxElementBuilder("email");
		fileSystem.extractEmail(emailMessage, xml);
		xml.close();
		String expected = TestFileUtils.getTestFile("/ExchangeMailAttachedMessage.xml");
		MatchUtils.assertXmlEquals(expected,xml.toString());
	}
	
	private EmailMessage prepareFolderAndGetFirstMessage(String folderName, String sourceFolder) throws Exception {
		if (!fileSystem.folderExists(folderName)) {
			fileSystem.createFolder(folderName);
		}
		EmailMessage orgItem = getFirstFileFromFolder(folderName);
		if (orgItem == null) {
			EmailMessage seedItem = getFirstFileFromFolder(sourceFolder);
			orgItem = fileSystem.copyFile(seedItem, folderName, false);
		}
		return orgItem;
	}
	
	@Test
	public void testGetMessageRace() throws Exception {
		String folderName1 = "RaceFolder1";
		String folderName2 = "RaceFolder2";

		EmailMessage orgItem = prepareFolderAndGetFirstMessage(folderName1, null);
		System.out.println("Item original ["+fileSystem.getName(orgItem));

		System.out.println("moving item...");
		EmailMessage movedItem1 = fileSystem.moveFile(orgItem, folderName2, true);
		System.out.println("Item original ["+fileSystem.getName(orgItem));
		System.out.println("Item moved 1  ["+fileSystem.getName(movedItem1));

		System.out.println("tring to move same item again...");
		try {
			EmailMessage movedItem2 = fileSystem.moveFile(orgItem, folderName2, true);
			System.out.println("Item original ["+fileSystem.getName(orgItem));
			System.out.println("Item moved 1  ["+fileSystem.getName(movedItem1));
			System.out.println("Item moved 1  ["+fileSystem.getName(movedItem2));
			fail("Expected second move to fail");
		} catch (FileSystemException e) {
			log.debug("second move failed as expected", e);
		}
	}
	
	ExchangeMailListener getConfiguredListener(String sourceFolder, String inProcessFolder) throws Exception {
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
	}
	
}
