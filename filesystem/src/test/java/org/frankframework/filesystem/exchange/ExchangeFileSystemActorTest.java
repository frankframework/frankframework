package org.frankframework.filesystem.exchange;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.FileSystemActorTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.PropertyLoader;

public class ExchangeFileSystemActorTest extends FileSystemActorTest<MailItemId, ExchangeFileSystem> {
	private static TestConfiguration configuration = new TestConfiguration();
	private static PropertyLoader PROPERTIES;

	private static String mailAddress;
	private static String clientId;
	private static String clientSecret;
	private static String tenantId;

	// Should ideally never be `inbox` as it removes all mail items!
	private String baseFolder = PROPERTIES.getProperty("baseFolder", "Inbox/iafTest");

	@BeforeAll
	public static void beforeAll() {
		try {
			PROPERTIES = new PropertyLoader("azure-credentials.properties");

			mailAddress = PROPERTIES.getProperty("mailAddress");
			clientId = PROPERTIES.getProperty("clientId");
			clientSecret = PROPERTIES.getProperty("clientSecret");
			tenantId = PROPERTIES.getProperty("tenantId");
		} catch (Exception e) {
			// file not found
		}
		assumeTrue(PROPERTIES != null);
	}

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
	@Disabled("charset cannot be wrong")
	@Override
	public void fileSystemActorReadWithCharsetUseIncompatible() throws Exception {
		// NO OP
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorCopyActionTestWithExcludeWildCard() throws Exception {
		// NO OP: Wildcards are not supported
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorCopyActionTestWithWildCard() throws Exception {
		// NO OP: Wildcards are not supported
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorDeleteActionTestWithExcludeWildCard() throws Exception {
		// NO OP: Wildcards are not supported
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorDeleteActionTestWithWildCard() throws Exception {
		// NO OP: Wildcards are not supported
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorListActionTestInFolderWithBothWildCardAndExcludeWildCard() throws Exception {
		// NO OP: Wildcards are not supported
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorListActionTestInFolderWithExcludeWildCard() throws Exception {
		// NO OP: Wildcards are not supported
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorListActionTestInFolderWithWildCard() throws Exception {
		// NO OP: Wildcards are not supported
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorMoveActionTestWithExcludeWildCard() throws Exception {
		// NO OP: Wildcards are not supported
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorMoveActionTestWithWildCard() throws Exception {
		// NO OP: Wildcards are not supported
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void migrated_localFileSystemTestListExcludeWildcard() throws Exception {
		// NO OP: Wildcards are not supported
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void migrated_localFileSystemTestListIncludeExcludeWildcard() throws Exception {
		// NO OP: Wildcards are not supported
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void migrated_localFileSystemTestListWildcard() throws Exception {
		// NO OP: Wildcards are not supported
	}
}
