package org.frankframework.filesystem.mail;

import java.util.HashMap;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeBodyPart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

import org.frankframework.filesystem.AbstractMailListener;
import org.frankframework.filesystem.BasicFileSystemListenerTest;
import org.frankframework.filesystem.GreenmailImapTestFileSystem;
import org.frankframework.filesystem.IFileSystemTestHelper;

public class ImapMailListenerTest extends BasicFileSystemListenerTest<Message, GreenmailImapTestFileSystem> {
	public static final String INPUT_FOLDER = "InputFolder";
	private static final ServerSetup serverSetup = ServerSetupTest.IMAP;
	@RegisterExtension
	static GreenMailExtension greenMail = new GreenMailExtension(serverSetup);

	static {
		// Increase the timeout for the GreenMail server to start; default of 2000L fails on GitHub Actions
		serverSetup.setServerStartupTimeout(5_000L);
		serverSetup.dynamicPort();
	}

	User user = new User("test@example.org", "testUser", "testPassword");

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		fileSystemListener = createFileSystemListener();
		super.setUp();

		autowireByName(fileSystemListener);
		threadContext = new HashMap<>();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new GreenmailImapTestFileSystemHelper(greenMail, user, INPUT_FOLDER);
	}

	@Override
	public AbstractMailListener<Message, MimeBodyPart, GreenmailImapTestFileSystem> createFileSystemListener() {
		greenMail.setUser(user.email, user.username, user.password);

		AbstractMailListener<Message, MimeBodyPart, GreenmailImapTestFileSystem> listener = new AbstractMailListener<>() {
			@Override
			protected GreenmailImapTestFileSystem createFileSystem() {
				return getImapFileSystem();
			}
		};

		listener.setMessageIdPropertyKey("Subject"); // for the filename based tests to use the correct key
		listener.setInputFolder(INPUT_FOLDER); // For the tests in the FileSystemTestBase to succeed, make sure to set this

		return listener;
	}

	private GreenmailImapTestFileSystem getImapFileSystem() {
		GreenmailImapTestFileSystem imapTestFileSystem = new GreenmailImapTestFileSystem();

		imapTestFileSystem.setSession(greenMail.getImap().createSession());
		imapTestFileSystem.setPort(greenMail.getImap().getPort());
		imapTestFileSystem.setHost("localhost");
		imapTestFileSystem.setUsername(user.username);
		imapTestFileSystem.setPassword(user.password);
		imapTestFileSystem.setBaseFolder("/");
		imapTestFileSystem.setReplyAddressFields("from,sender");

		return imapTestFileSystem;
	}

	// Simple container with user information to pass around
	public record User(String email, String username, String password) {
	}
}
