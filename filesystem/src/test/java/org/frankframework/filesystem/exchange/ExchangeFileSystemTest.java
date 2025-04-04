package org.frankframework.filesystem.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.HelperedBasicFileSystemTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.util.JacksonUtils;

/**
 * @author Niels Meijer
 */
@Tag("slow")
public class ExchangeFileSystemTest extends HelperedBasicFileSystemTest<MailItemId, ExchangeFileSystem> {

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
