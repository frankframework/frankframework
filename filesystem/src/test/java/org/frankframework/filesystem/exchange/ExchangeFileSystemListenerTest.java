package org.frankframework.filesystem.exchange;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import org.frankframework.filesystem.BasicFileSystemListenerTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.receivers.ExchangeMailListener;
import org.frankframework.testutil.TestAssertions;

@Tag("slow")
@Tag("unstable") // Relies on a remote API that is not always available for the tests
public class ExchangeFileSystemListenerTest extends BasicFileSystemListenerTest<MailItemId, ExchangeFileSystem> {

	@BeforeAll
	public static void beforeAll() {
		assumeFalse(TestAssertions.isTestRunningOnCI());
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
	public ExchangeMailListener createFileSystemListener() {
		return ExchangeConnectionCache.getExchangeMailListener();
	}
}
