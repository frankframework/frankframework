package org.frankframework.filesystem.exchange;

import static org.junit.jupiter.api.Assertions.fail;
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
	private static PropertyLoader properties;

	private static String mailAddress;
	private static String clientId;
	private static String clientSecret;
	private static String tenantId;

	// Should ideally never be `inbox` as it removes all mail items!
	private String baseFolder = properties.getProperty("baseFolder", "Inbox/iafTest");

	@BeforeAll
	public static void beforeAll() {
		try {
			properties = new PropertyLoader("azure-credentials.properties");

			mailAddress = properties.getProperty("mailAddress");
			clientId = properties.getProperty("clientId");
			clientSecret = properties.getProperty("clientSecret");
			tenantId = properties.getProperty("tenantId");
		} catch (Exception e) {
			// file not found
		}
		assumeTrue(properties != null);
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
	public void fileSystemActorReadWithCharsetUseIncompatible() {
		// NO OP
		fail();
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorCopyActionTestWithExcludeWildCard() {
		// NO OP: Wildcards are not supported
		fail();
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorCopyActionTestWithWildCard() {
		// NO OP: Wildcards are not supported
		fail();
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorDeleteActionTestWithExcludeWildCard() {
		// NO OP: Wildcards are not supported
		fail();
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorDeleteActionTestWithWildCard() {
		// NO OP: Wildcards are not supported
		fail();
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorListActionTestInFolderWithBothWildCardAndExcludeWildCard() {
		// NO OP: Wildcards are not supported
		fail();
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorListActionTestInFolderWithExcludeWildCard() {
		// NO OP: Wildcards are not supported
		fail();
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorListActionTestInFolderWithWildCard() {
		// NO OP: Wildcards are not supported
		fail();
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorMoveActionTestWithExcludeWildCard() {
		// NO OP: Wildcards are not supported
		fail();
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void fileSystemActorMoveActionTestWithWildCard() {
		// NO OP: Wildcards are not supported
		fail();
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void migrated_localFileSystemTestListExcludeWildcard() {
		// NO OP: Wildcards are not supported
		fail();
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void migrated_localFileSystemTestListIncludeExcludeWildcard() {
		// NO OP: Wildcards are not supported
		fail();
	}

	@Test
	@Disabled("wildcards are not supported")
	@Override
	public void migrated_localFileSystemTestListWildcard() {
		// NO OP: Wildcards are not supported
		fail();
	}
}
