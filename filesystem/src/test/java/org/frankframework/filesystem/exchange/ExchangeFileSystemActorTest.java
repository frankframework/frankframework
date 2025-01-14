package org.frankframework.filesystem.exchange;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.FileSystemActorTest;
import org.frankframework.filesystem.IFileSystemTestHelper;

@Disabled("Disabled for now, running too many tests in CI causes problems")
public class ExchangeFileSystemActorTest extends FileSystemActorTest<MailItemId, ExchangeFileSystem> {

	@BeforeAll
	public static void beforeAll() {
		assumeTrue(ExchangeConnectionCache.validateCredentials());
	}

	@AfterAll
	public static void afterAll() {
		ExchangeConnectionCache.close();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		setWaitMillis(ExchangeFileSystemTestHelper.WAIT_MILLIS);

		return ExchangeConnectionCache.getExchangeFileSystemTestHelper();
	}

	@Override
	public ExchangeFileSystem createFileSystem() {
		return ExchangeConnectionCache.getExchangeFileSystem();
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
