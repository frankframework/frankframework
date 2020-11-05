package nl.nn.adapterframework.filesystem;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import microsoft.exchange.webservices.data.core.service.item.Item;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.PropertyUtil;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class ExchangeFileSystemTest extends SelfContainedBasicFileSystemTest<Item, ExchangeFileSystem>{

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
		Item item = getFirstFileFromFolder(null);
		Message message = fileSystem.extractEmailMessage(item, null, false, null);
		String expected = TestFileUtils.getTestFile("/ExchangeMailNormal.xml");
		MatchUtils.assertXmlEquals(expected, message.asString());
	}

	@Test
	public void testExtractNormalMessageSimple() throws Exception {
		Item item = getFirstFileFromFolder(null);
		Message message = fileSystem.extractEmailMessage(item, null, true, null);
		String expected = TestFileUtils.getTestFile("/ExchangeMailNormalSimple.xml");
		MatchUtils.assertXmlEquals(expected, message.asString());
	}

	@Test
	public void testExtractProblematicMessage() throws Exception {
		Item item = getFirstFileFromFolder("XmlProblem");
		Message message = fileSystem.extractEmailMessage(item, null, false, null);
		String expected = TestFileUtils.getTestFile("/ExchangeMailProblem.xml");
		MatchUtils.assertXmlEquals(expected, message.asString());
	}
}
