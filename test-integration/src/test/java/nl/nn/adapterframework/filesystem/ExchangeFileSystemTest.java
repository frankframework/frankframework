package nl.nn.adapterframework.filesystem;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import microsoft.exchange.webservices.data.core.service.item.Item;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.testutil.PropertyUtil;

public class ExchangeFileSystemTest extends BasicFileSystemTestBase<Item, ExchangeFileSystem>{

	private String PROPERTY_FILE = "ExchangeMail.properties";
	
	//private String DEFAULT_URL = "https://outlook.office365.com/EWS/Exchange.asmx";
	
	private String url         = PropertyUtil.getProperty(PROPERTY_FILE, "url");
	private String mailaddress = PropertyUtil.getProperty(PROPERTY_FILE, "mailaddress");
	private String accessToken = PropertyUtil.getProperty(PROPERTY_FILE, "accessToken");
	
	
	private String nonExistingFileName = "AAMkAGNmZTczMWUwLWQ1MDEtNDA3Ny1hNjU4LTlmYTQzNjE0NjJmYgBGAAAAAAALFKqetECyQKQyuRBrRSzgBwDx14SZku4LS5ibCBco+nmXAAAAAAEMAADx14SZku4LS5ibCBco+nmXAABMFuwsAAA=";
	
	@Override
	protected ExchangeFileSystem createFileSystem() throws ConfigurationException {
		ExchangeFileSystem fileSystem = new ExchangeFileSystem();
		if (StringUtils.isNotEmpty(url)) fileSystem.setUrl(url);
		fileSystem.setMailAddress(mailaddress);
		fileSystem.setAccessToken(accessToken);
		return fileSystem;
	}

	@Test
	public void fileSystemTestListFile() throws Exception {
		fileSystemTestListFile(10);
	}

	@Test
	public void fileSystemTestRandomFileShouldNotExist() throws Exception {
		fileSystemTestRandomFileShouldNotExist(nonExistingFileName);
	}

}
