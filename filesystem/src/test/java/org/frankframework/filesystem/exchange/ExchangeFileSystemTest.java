package org.frankframework.filesystem.exchange;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.HelperedBasicFileSystemTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.testutil.TestConfiguration;

/**
 * @author Niels Meijer
 */
public class ExchangeFileSystemTest extends HelperedBasicFileSystemTest<MailItemId, ExchangeFileSystem> {
	private static TestConfiguration configuration = new TestConfiguration();

	private String baseFolder = "Inbox/iafTest"; // should never be `inbox` as it removes all mail items!

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
	public void basicFileSystemTestCopyFile() throws Exception {
		super.basicFileSystemTestCopyFile();
	}

	@Test
	@Override
	public void basicFileSystemTestMoveFile() throws Exception {
		super.basicFileSystemTestMoveFile();
	}

	@Test
	@Override
	@Disabled("test needs to be rewritten to deal with file id's")
	public void basicFileSystemTestListDirsAndOrFolders() throws Exception {
		super.basicFileSystemTestListDirsAndOrFolders();
	}
}
