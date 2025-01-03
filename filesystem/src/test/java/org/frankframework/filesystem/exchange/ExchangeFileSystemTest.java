package org.frankframework.filesystem.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URL;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.HelperedBasicFileSystemTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.JacksonUtils;
import org.frankframework.util.PropertyLoader;

/**
 * @author Niels Meijer
 */
public class ExchangeFileSystemTest extends HelperedBasicFileSystemTest<MailItemId, ExchangeFileSystem> {
	private static TestConfiguration configuration = new TestConfiguration();
	private static PropertyLoader properties;

	private static String mailAddress;
	private static String clientId;
	private static String clientSecret;
	private static String tenantId;

	// Should ideally never be `inbox` as it removes all mail items!
	private String baseFolder = properties.getProperty("baseFolder", ExchangeFileSystemTestHelper.DEFAULT_BASE_FOLDER);

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

		assumeTrue(StringUtils.isNoneEmpty(mailAddress, clientId, clientSecret, tenantId));
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		setWaitMillis(ExchangeFileSystemTestHelper.WAIT_MILLIS);
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
	public void basicFileSystemTestListDirsAndOrFolders() {
		// NO OP
		fail();
	}

	@Test
	@Override
	@Disabled("test never fails because we ignore the charset attribute completely")
	public void basicFileSystemTestReadSpecialCharsFails() {
		// NO OP
		fail();
	}

	@Test
	public void testMailMapping() throws Exception {
		URL mailJson = this.getClass().getResource("/ms-graph-mail.json");
		assertNotNull(mailJson, "unable to find file");
		MailMessageResponse dto = JacksonUtils.convertToDTO(mailJson.openStream(), MailMessageResponse.class);
		List<MailMessage> messages = dto.messages;
		assertEquals(1, messages.size());
		MailMessage message = messages.get(0);
		assertEquals("Test Message Contents", message.getBody().getContent());
		assertEquals("EmailAddress: yes-reply <yes-reply@hidden.domain>", message.getSender().toString());
		assertEquals("EmailAddress: no-reply <no-reply@hidden.domain>", message.getFrom().toString());
		assertEquals("[EmailAddress: Karel Appel <karel@appel.domain>]", message.getToRecipients().toString());
	}
}
