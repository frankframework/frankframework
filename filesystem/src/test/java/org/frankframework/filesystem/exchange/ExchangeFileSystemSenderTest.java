package org.frankframework.filesystem.exchange;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import org.frankframework.filesystem.FileSystemSenderTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.senders.ExchangeFileSystemSender;
import org.frankframework.testutil.TestAssertions;

@Tag("slow")
public class ExchangeFileSystemSenderTest extends FileSystemSenderTest<ExchangeFileSystemSender, MailItemId, ExchangeFileSystem> {

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
	public ExchangeFileSystemSender createFileSystemSender() {
		return ExchangeConnectionCache.getExchangeFileSystemSender();
	}
}
