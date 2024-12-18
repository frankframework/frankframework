package org.frankframework.filesystem.exchange;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;

import org.frankframework.filesystem.FileSystemSenderTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.senders.ExchangeFileSystemSender;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.PropertyLoader;

public class ExchangeFileSystemSenderTest extends FileSystemSenderTest<ExchangeFileSystemSender, MailItemId, ExchangeFileSystem> {

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
	public ExchangeFileSystemSender createFileSystemSender() {
		ExchangeFileSystemSender fileSystem = configuration.createBean(ExchangeFileSystemSender.class);
		fileSystem.setClientId(clientId);
		fileSystem.setClientSecret(clientSecret);
		fileSystem.setTenantId(tenantId);
		fileSystem.setMailAddress(mailAddress);
		fileSystem.setBaseFolder(baseFolder);

		return fileSystem;
	}
}
