package org.frankframework.filesystem.mail;

import jakarta.mail.Message;
import jakarta.mail.NoSuchProviderException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

import org.frankframework.filesystem.BasicFileSystemListenerTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.ImapFileSystem;
import org.frankframework.receivers.ImapListener;

@Disabled("these don't work")
public class ImapMailListenerTest extends BasicFileSystemListenerTest<Message, ImapFileSystem> {
	private static final String BASE_FOLDER = "inbox";
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
		greenMail.setUser(user.email, user.username, user.password);

		super.setUp();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new GreenmailImapTestFileSystemHelper(greenMail, user, BASE_FOLDER);
	}

	@Override
	public ImapListener createFileSystemListener() {

		ImapListener listener = new ImapListener() {
			@Override
			public ImapFileSystem createFileSystem() {
				return new ImapFileSystem() {
					@Override
					protected String getStoreName() throws NoSuchProviderException {
						return "imap";
					}
				};
			}
		};
		listener.setMessageIdPropertyKey("Subject"); // for the filename based tests to use the correct key

		listener.setPort(greenMail.getImap().getPort());
		listener.setHost("localhost");
		listener.setUsername(user.username);
		listener.setPassword(user.password);
		listener.setBaseFolder(BASE_FOLDER);
		listener.setReplyAddressFields("from,sender");

		return listener;
	}

	// Simple container with user information to pass around
	public record User(String email, String username, String password) {
	}
}
