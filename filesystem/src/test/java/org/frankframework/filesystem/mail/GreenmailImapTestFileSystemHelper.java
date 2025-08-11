package org.frankframework.filesystem.mail;

import java.io.InputStream;

import jakarta.annotation.Nonnull;
import jakarta.mail.Address;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.SubjectTerm;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.angus.mail.imap.IMAPFolder;

import com.icegreen.greenmail.junit5.GreenMailExtension;

import org.frankframework.filesystem.IFileSystemTestHelper;

/**
 * Greenmail based mail file system helper.
 * <br>
 * Greenmail is a simple mail backend to send and retrieve e-mails, to mock smtp, imap, etc.
 *
 * @see "https://github.com/greenmail-mail-test/greenmail/tree/master/greenmail-core/src/test/java/com/icegreen/greenmail/examples"
 */
public class GreenmailImapTestFileSystemHelper implements IFileSystemTestHelper {

	private final GreenMailExtension greenMail;
	private final ImapMailListenerTest.User user;
	private final String baseFolder;
	private Folder inbox;

	public GreenmailImapTestFileSystemHelper(GreenMailExtension greenMail, ImapMailListenerTest.User user, String baseFolder) {
		this.greenMail = greenMail;
		this.user = user;
		this.baseFolder = baseFolder;
	}

	@Override
	public void setUp() throws Exception {
		Store store = greenMail.getImap().createStore();
		store.connect(user.username(), user.password());

		inbox = store.getFolder(baseFolder);
		if (!inbox.exists()) {
			inbox.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
		}

		inbox.open(Folder.READ_ONLY);
	}

	@Override
	public void tearDown() throws Exception {
		this.inbox.close();
	}

	@Override
	public boolean _fileExists(String folder, String filename) throws Exception {
		Folder containingFolder = inbox.getFolder(getFolderToUse(folder));
		containingFolder.open(Folder.READ_ONLY);

		Message[] search = containingFolder.search(new SubjectTerm(filename));
		return search != null && search.length > 0;
	}

	@Override
	public boolean _folderExists(String folderName) throws Exception {
		Folder folderToCheck = inbox.getFolder(folderName);

		return folderToCheck.exists();
	}

	@Override
	public void _deleteFile(String folderName, String filename) throws Exception {
		// not implemented
	}

	@Override
	public String createFile(String folder, String filename, String contents) throws Exception {
		Session session = greenMail.getImap().createSession();

		greenMail.getUserManager().getUser("testuser");
		IMAPFolder newFolder = (IMAPFolder) inbox.getFolder(getFolderToUse(folder));

		if (!newFolder.exists()) {
			newFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
		}

		// Construct a message
		MimeMessage message = new MimeMessage(session);
		message.setSubject(StringEscapeUtils.escapeJava(filename));
		message.setText(StringEscapeUtils.escapeJava(contents));

		setRecipients(message, Message.RecipientType.TO, "to", 1, 2);
		message.setFrom(new InternetAddress("from2@localhost"));
		message.setFlag(Flags.Flag.ANSWERED, true);

		// But return an OutputStream which will persist the message on calling close()

		Message[] toBeAppended = new Message[1];
		toBeAppended[0] = message;

		newFolder.appendMessages(toBeAppended);
		return message.getMessageID();
	}

	// the tests in FileSystemTestBase assume that the input folder is used
	private @Nonnull String getFolderToUse(String folderName) {
		return StringUtils.isNotBlank(folderName) ? folderName : baseFolder;
	}

	private void setRecipients(MimeMessage message, Message.RecipientType recipientType, String prefix, int... indexes)
			throws MessagingException {
		Address[] arr = new InternetAddress[indexes.length];

		for (int i = 0; i < arr.length; i++) {
			arr[i] = new InternetAddress(prefix + indexes[i] + "@localhost");
		}

		message.setRecipients(recipientType, arr);
	}

	@Override
	public InputStream _readFile(String folder, String filename) throws Exception {
		Folder containingFolder = inbox.getFolder(folder);
		containingFolder.open(Folder.READ_ONLY);

		Message[] search = containingFolder.search(new SubjectTerm(filename));
		Message message = search[0];

		return message.getInputStream();
	}

	@Override
	public void _createFolder(String folder) throws Exception {
		Folder containingFolder = inbox.getFolder(folder);
		containingFolder.create(Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS);
	}

	@Override
	public void _deleteFolder(String folder) throws Exception {
		Folder containingFolder = inbox.getFolder(folder);
		containingFolder.delete(true);
	}
}
