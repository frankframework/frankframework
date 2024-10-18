package org.frankframework.filesystem;

import java.util.Arrays;
import java.util.Optional;

import jakarta.annotation.Nullable;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.SubjectTerm;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.angus.mail.imap.IMAPFolder;

import org.frankframework.configuration.ConfigurationException;

/**
 * Slightly modified implementation of ImapFileSystem to be able to use greenmail as a backend.
 */
public class GreenmailImapTestFileSystem extends ImapFileSystem {
	private Session session;

	@Override
	protected IMAPFolder createConnection() throws FileSystemException {
		try {
			Store store = session.getStore("imap");
			store.connect(getUsername(), getPassword());
			Folder inbox = store.getFolder("INBOX");

			return (IMAPFolder) inbox;
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		if (session == null) {
			throw new ConfigurationException("session needs to be set");
		}
	}

	@Override
	public String getName(Message message) {
		try {
			// Use the subject as name, since the uid is generated
			return message.getSubject();
		} catch (MessagingException e) {
			return null;
		}
	}

	// uid stuff as defined in ImapMailListener didn't work with greenmail
	@Override
	public Message toFile(@Nullable String defaultFolder, @Nullable String filename) throws FileSystemException {
		IMAPFolder baseFolder = getConnection();
		boolean invalidateConnectionOnRelease = false;
		try {
			IMAPFolder folder = getFolder(baseFolder, defaultFolder);
			if (!folder.isOpen()) {
				folder.open(Folder.READ_WRITE);
			}

			return findMatchingFile(filename, folder)
					.orElse(getNotExistingMessage(filename));
		} catch (MessagingException e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException(e);
		} finally {
			releaseConnection(baseFolder, invalidateConnectionOnRelease);
		}
	}

	private Message getNotExistingMessage(String filename) throws MessagingException {
		MimeMessage message = new MimeMessage(session);
		message.setSubject(StringEscapeUtils.escapeJava(filename));

		return message;
	}

	private Optional<Message> findMatchingFile(String filename, IMAPFolder folder) throws MessagingException {
		return Arrays.stream(folder.getMessages())
				.filter(message -> isMatch(filename, message))
				.findFirst();
	}

	private boolean isMatch(String filename, Message message) {
		try {
			return message.match(new SubjectTerm(filename));
		} catch (MessagingException e) {
			return false;
		}
	}

	public void setSession(Session session) {
		this.session = session;
	}
}
