package org.frankframework.filesystem.exchange;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.frankframework.filesystem.FileSystemSenderTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.senders.ExchangeFileSystemSender;

public class ExchangeFileSystemSenderTest extends FileSystemSenderTest<ExchangeFileSystemSender, MailItemId, ExchangeFileSystem> {

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
	public ExchangeFileSystemSender createFileSystemSender() {
		return ExchangeConnectionCache.getExchangeFileSystemSender();
	}
}
