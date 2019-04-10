package nl.nn.adapterframework.senders;

import org.junit.Test;

import microsoft.exchange.webservices.data.core.service.item.Item;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.ExchangeFileSystem;

public class ExchangeFileSystemTest extends BasicFileSystemTestBase<Item, ExchangeFileSystem>{

	private String mailaddress = "gerrit@integrationpartners.nl";
	private String username    = mailaddress;
	private String password    = "fFDH7fBnUj";
	
	private String nonExistingFileName = "AAMkAGNmZTczMWUwLWQ1MDEtNDA3Ny1hNjU4LTlmYTQzNjE0NjJmYgBGAAAAAAALFKqetECyQKQyuRBrRSzgBwDx14SZku4LS5ibCBco+nmXAAAAAAEMAADx14SZku4LS5ibCBco+nmXAABMFuwsAAA=";
	
	@Override
	protected ExchangeFileSystem createFileSystem() throws ConfigurationException {
		ExchangeFileSystem fileSystem = new ExchangeFileSystem();
		fileSystem.setMailAddress(mailaddress);
		fileSystem.setUsername(username);
		fileSystem.setPassword(password);
		return fileSystem;
	}

	@Test
	public void fileSystemTestListFile() throws Exception {
		fileSystemTestListFile(2);
	}

	@Test
	public void fileSystemTestRandomFileShouldNotExist() throws Exception {
		fileSystemTestRandomFileShouldNotExist(nonExistingFileName);
	}

}
