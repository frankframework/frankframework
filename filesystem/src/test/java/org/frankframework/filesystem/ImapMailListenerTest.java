package org.frankframework.filesystem;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeBodyPart;

import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

public class ImapMailListenerTest extends MailListenerTest<Message, MimeBodyPart, GreenmailImapTestFileSystem> {

	private static final ServerSetup serverSetup = ServerSetupTest.IMAP;
	static final String INPUT_FOLDER = "InputFolder";

	@RegisterExtension
	static GreenMailExtension greenMail = new GreenMailExtension(serverSetup);

	static {
		// Increase the timeout for the GreenMail server to start; default of 2000L fails on GitHub Actions
		serverSetup.setServerStartupTimeout(5_000L);
		serverSetup.dynamicPort();
	}

	public record User(String email, String username, String password){};

	User user = new User("test@example.org", "testUser", "testPassword");

	@Override
	public MailListener<Message, MimeBodyPart, GreenmailImapTestFileSystem> createFileSystemListener() {
		greenMail.setUser(user.email, user.username, user.password);

		MailListener<Message, MimeBodyPart, GreenmailImapTestFileSystem> listener = new MailListener<>() {
			@Override
			protected GreenmailImapTestFileSystem createFileSystem() {
				return getImapFileSystem();
			}
		};

		listener.setMessageIdPropertyKey("Subject"); // for the filename based tests to use the correct key
		listener.setInputFolder(INPUT_FOLDER); // For the tests in the FileSystemTestBase to succeed, make sure to set this
		listener.setCreateFolders(true);

		return listener;
	}

	private GreenmailImapTestFileSystem getImapFileSystem() {
		GreenmailImapTestFileSystem imapTestFileSystem = new GreenmailImapTestFileSystem();

		imapTestFileSystem.setSession(greenMail.getImap().createSession());
		imapTestFileSystem.setHost("localhost");
		imapTestFileSystem.setPort(greenMail.getImap().getPort());
		imapTestFileSystem.setUsername(user.username);
		imapTestFileSystem.setPassword(user.password);
		imapTestFileSystem.setBaseFolder("/");
		imapTestFileSystem.setReplyAddressFields("from,sender");

		return imapTestFileSystem;
	}

	@Override
	protected IFileSystemTestHelperFullControl getFileSystemTestHelper() {
		return new GreenmailImapTestFileSystemHelper(greenMail, user);
	}
}
