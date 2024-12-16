package org.frankframework.filesystem.exchange;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.HelperedBasicFileSystemTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.PropertyLoader;

/**
 * @author Niels Meijer
 */
public class ExchangeFileSystemTest extends HelperedBasicFileSystemTest<MailItemId, ExchangeFileSystem> {
	private static TestConfiguration configuration = new TestConfiguration();
	private static final PropertyLoader PROPERTIES = new PropertyLoader("azure-credentials.properties");

	private String mailAddress = PROPERTIES.getProperty("mailAddress");
	private String clientId = PROPERTIES.getProperty("clientId");
	private String clientSecret = PROPERTIES.getProperty("clientSecret");
	private String tenantId = PROPERTIES.getProperty("tenantId");

	// Should ideally never be `inbox` as it removes all mail items!
	private String baseFolder = PROPERTIES.getProperty("baseFolder", "Inbox/iafTest");

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		setWaitMillis(250);
		return new ExchangeFileSystemTestHelper(clientId, clientSecret, tenantId, mailAddress, baseFolder);
	}

	@Override
	public ExchangeFileSystem createFileSystem() {
		ExchangeFileSystem fileSystem = configuration.createBean(ExchangeFileSystem.class);
		fileSystem.setClientId(clientId);
		fileSystem.setClientSecret(clientSecret);
		fileSystem.setTenantId(tenantId);
		fileSystem.setMailAddress(mailAddress);
		fileSystem.setBaseFolder(baseFolder);

		return fileSystem;
	}

	@Test
	@Override
	@Disabled("test needs to be rewritten to deal with file id's")
	public void basicFileSystemTestListDirsAndOrFolders() throws Exception {
		// NO OP
	}

	@Test
	@Override
	@Disabled("test never fails because we ignore the charset attribute completely")
	public void basicFileSystemTestReadSpecialCharsFails() throws Exception {
		// NO OP
	}
}
