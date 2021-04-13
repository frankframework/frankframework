/*
   Copyright 2020-2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.xml.SaxElementBuilder;

public class ImapFileSystem extends MailFileSystemBase<Message, MimeBodyPart, IMAPFolder> {
	protected Logger log = LogUtil.getLogger(this);

	private @Getter String host = "";
	private @Getter int port = 993;
	
	private Session emailSession = Session.getInstance(System.getProperties());

	
	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getHost())) {
			throw new ConfigurationException("attribute host needs to be specified");
		}
	}


	@Override
	protected IMAPFolder createConnection() throws FileSystemException {
		try {
			// emailSession.setDebug(true);
			CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
			Store store = emailSession.getStore("imaps");
			store.connect(getHost(), getPort(), cf.getUsername(), cf.getPassword());

			IMAPFolder inbox = (IMAPFolder)store.getFolder("INBOX");
			IMAPFolder folder;
			String baseFolder = getBaseFolder();
			if (StringUtils.isNotEmpty(baseFolder)) {
				folder = (IMAPFolder)inbox.getFolder(baseFolder);
				if (!folder.exists()) {
					folder = (IMAPFolder)store.getFolder(baseFolder);
				}
				if (!folder.exists()) {
					throw new FileSystemException("Could not find baseFolder ["+baseFolder+"]");
				}
			} else {
				folder = inbox;
			}
			return folder;
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	protected void closeConnection(IMAPFolder folder) throws FileSystemException {
		try (Store store = folder.getStore()) {
			if (folder.isOpen()) {
				folder.close();
			}
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	private IMAPFolder getFolder(IMAPFolder baseFolder, String name) throws MessagingException, FileSystemException {
		if (StringUtils.isNotEmpty(name)) {
			return (IMAPFolder)baseFolder.getFolder(name);
		}
		return baseFolder;
	}

	private String uidToName(long uid) {
		return Long.toHexString(uid);
	}

	private long nameToUid(String filename) {
		return Long.parseLong(filename, 16);
	}

	@Override
	public String getName(Message f) {
		UIDFolder folder = (UIDFolder) f.getFolder();
		try {
			return uidToName(folder.getUID(f));
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Message toFile(String filename) throws FileSystemException {
		return toFile(null, filename);
	}

	@Override
	public Message toFile(String defaultFolder, String filename) throws FileSystemException {
		IMAPFolder baseFolder = getConnection();
		try {
			IMAPFolder folder = getFolder(baseFolder, defaultFolder);
			if (!folder.isOpen()) {
				folder.open(Folder.READ_WRITE);
			}
			return folder.getMessageByUID(nameToUid(filename));
		} catch (MessagingException e) {
			invalidateConnection(baseFolder);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(baseFolder);
		}
	}

	@Override
	public boolean exists(Message f) throws FileSystemException {
		try {
			Folder folder = f.getFolder();
			folder.expunge();
			return !f.isExpunged() && !f.isSet(Flags.Flag.DELETED);
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean folderExists(String foldername) throws FileSystemException {
		IMAPFolder baseFolder = getConnection();
		try {
			IMAPFolder folder = getFolder(baseFolder, foldername);
			return folder.exists();
		} catch (MessagingException e) {
			invalidateConnection(baseFolder);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(baseFolder);
		}
	}

	@Override
	public int getNumberOfFilesInFolder(String foldername) throws FileSystemException {
		IMAPFolder baseFolder = getConnection();
		try {
			IMAPFolder folder = getFolder(baseFolder, foldername);
			if (!folder.isOpen()) {
				folder.open(Folder.READ_WRITE);
			}
			Message messages[] = folder.getMessages();
			return messages.length;
		} catch (MessagingException e) {
			invalidateConnection(baseFolder);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(baseFolder);
		}
	}

	@Override
	public DirectoryStream<Message> listFiles(String foldername) throws FileSystemException {
		IMAPFolder baseFolder = getConnection();
		try {
			IMAPFolder folder = getFolder(baseFolder, foldername);
			if (!folder.isOpen()) {
				folder.open(Folder.READ_WRITE);
			}
			Message messages[] = folder.getMessages();
			return FileSystemUtils.getDirectoryStream(Arrays.asList(messages));
		} catch (MessagingException e) {
			invalidateConnection(baseFolder);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(baseFolder);
		}
	}

	@Override
	public void deleteFile(Message f) throws FileSystemException {
		try {
			f.setFlag(Flags.Flag.DELETED, true);
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public Message moveFile(Message f, String destinationFolder, boolean createFolder) throws FileSystemException {
		IMAPFolder baseFolder = getConnection();
		try {
			AppendUID results[];
			try (IMAPFolder destination = getFolder(baseFolder, destinationFolder)) {
				Message messages[] = new Message[1];
				messages[0] = f;
				destination.open(Folder.READ_WRITE);
				IMAPFolder src = (IMAPFolder) f.getFolder();
				results = src.moveUIDMessages(messages, destination);
			}
			if (results[0] == null) {
				log.warn("could not find new name of message in folder [" + destinationFolder + "]");
				return null;
			}
			IMAPFolder destination = getFolder(baseFolder, destinationFolder);
			destination.open(Folder.READ_WRITE);
			return destination.getMessageByUID(results[0].uid);
		} catch (MessagingException e) {
			invalidateConnection(baseFolder);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(baseFolder);
		}
	}

	@Override
	public Message copyFile(final Message f, String destinationFolder, boolean createFolder) throws FileSystemException {
		IMAPFolder baseFolder = getConnection();
		try {
			AppendUID results[];
			try (IMAPFolder destination = getFolder(baseFolder, destinationFolder)) {
				Message messages[] = new Message[1];
				messages[0] = f;
				destination.open(Folder.READ_WRITE);
				IMAPFolder src = (IMAPFolder) f.getFolder();
				results = src.copyUIDMessages(messages, destination);
			}
			if (results[0] == null) {
				log.warn("could not find new name of message in folder [" + destinationFolder + "]");
				return null;
			}
			IMAPFolder destination = getFolder(baseFolder, destinationFolder);
			destination.open(Folder.READ_WRITE);
			return destination.getMessageByUID(results[0].uid);
		} catch (MessagingException e) {
			invalidateConnection(baseFolder);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(baseFolder);
		}
	}

	@Override
	public void createFolder(String folderName) throws FileSystemException {
		IMAPFolder baseFolder = getConnection();
		try {
			IMAPFolder folder = getFolder(baseFolder, folderName);
			if (!folder.create(Folder.HOLDS_FOLDERS + Folder.HOLDS_MESSAGES)) {
				throw new FileSystemException("Could not create folder [" + folderName + "]");
			}
		} catch (MessagingException e) {
			invalidateConnection(baseFolder);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(baseFolder);
		}
	}

	@Override
	public void removeFolder(String folderName, boolean removeNonEmptyFolder) throws FileSystemException {
		IMAPFolder baseFolder = getConnection();
		try {
			IMAPFolder folder = getFolder(baseFolder, folderName);
			if (folder == null) {
				throw new FileSystemException("Could not find folder object [" + folderName + "]");
			}
			if (!folder.delete(removeNonEmptyFolder)) {
				throw new FileSystemException("Could not delete folder [" + folderName + "]");
			}
		} catch (MessagingException e) {
			invalidateConnection(baseFolder);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(baseFolder);
		}
	}

	@Override
	public nl.nn.adapterframework.stream.Message readFile(Message f) throws FileSystemException, IOException {
		try {
			Object content = f.getContent();
			if (content instanceof MimeMultipart) {
				MimeMultipart mimeMultipart = (MimeMultipart) content;
				for (int i = 0; i < mimeMultipart.getCount(); i++) {
					MimeBodyPart bodyPart = (MimeBodyPart) mimeMultipart.getBodyPart(i);
					if (bodyPart.getContentType().startsWith("text/html")) {
						return new nl.nn.adapterframework.stream.Message(bodyPart.getInputStream());
					}
				}
				for (int i = 0; i < mimeMultipart.getCount(); i++) {
					BodyPart bodyPart = mimeMultipart.getBodyPart(i);
					if (bodyPart.getContentType().startsWith("text")) {
						return new nl.nn.adapterframework.stream.Message(bodyPart.getInputStream());
					}
				}
			}
			return new nl.nn.adapterframework.stream.Message(f.getInputStream());
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public Iterator<MimeBodyPart> listAttachments(Message f) throws FileSystemException {
		try {
			String contentType = f.getContentType();
			if (!contentType.contains("multipart")) {
				return null;
			}
			Multipart multiPart = (Multipart) f.getContent();
			Iterator<MimeBodyPart> result = new Iterator<MimeBodyPart>() {

				MimeBodyPart part = null;
				int i = 0;

				private void findPart() {
					try {
						while ((part == null) && i < multiPart.getCount()) {
							part = (MimeBodyPart) multiPart.getBodyPart(i++);
							if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
								return;
							}
							part = null;
						}
					} catch (MessagingException e) {
						log.warn(e);
					}
				}

				@Override
				public boolean hasNext() {
					findPart();
					return part != null;
				}

				@Override
				public MimeBodyPart next() {
					findPart();
					MimeBodyPart result = part;
					part = null;
					return result;
				}

			};
			return result;
		} catch (MessagingException | IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String getAttachmentName(MimeBodyPart a) throws FileSystemException {
		try {
			String name = a.getContentID();
			return name == null ? "" : name;
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public nl.nn.adapterframework.stream.Message readAttachment(MimeBodyPart a) throws FileSystemException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getAttachmentSize(MimeBodyPart a) throws FileSystemException {
		try {
			return a.getSize();
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String getAttachmentContentType(MimeBodyPart a) throws FileSystemException {
		try {
			return a.getContentType();
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String getAttachmentFileName(MimeBodyPart a) throws FileSystemException {
		try {
			return a.getFileName();
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public Message getFileFromAttachment(MimeBodyPart a) throws FileSystemException {
		try {
			Object content = a.getContent();
			if (content instanceof Message) {
				return (Message) content;
			}
			return null;
		} catch (MessagingException | IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public Map<String, Object> getAdditionalAttachmentProperties(MimeBodyPart a) throws FileSystemException {
		try {
			Map<String, Object> result = new LinkedHashMap<>();
			for (Enumeration<Header> headers = a.getAllHeaders(); headers.hasMoreElements();) {
				Header header = headers.nextElement();
				result.put(header.getName(), header.getValue());
			}
			return result;
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public long getFileSize(Message f) throws FileSystemException {
		try {
			return f.getSize();
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String getCanonicalName(Message f) throws FileSystemException {
		return getName(f);
	}

	@Override
	public Date getModificationTime(Message f) throws FileSystemException {
		return null;
	}

	private List<String> getRecipientsOfType(Message f, RecipientType type) throws MessagingException {
		InternetAddress[] recipients = (InternetAddress[]) f.getRecipients(type);
		if (recipients == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(recipients).stream().map(InternetAddress::toUnicodeString).collect(Collectors.toList());
	}

	private List<String> getReplyTo(Message f) throws MessagingException {
		InternetAddress[] recipients = (InternetAddress[]) f.getReplyTo();
		if (recipients == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(recipients).stream().map(InternetAddress::toUnicodeString).collect(Collectors.toList());
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(Message f) throws FileSystemException {
		try {
			Map<String, Object> result = new LinkedHashMap<String, Object>();
			result.put(IMailFileSystem.TO_RECEPIENTS_KEY, getRecipientsOfType(f, RecipientType.TO));
			result.put(IMailFileSystem.CC_RECEPIENTS_KEY, getRecipientsOfType(f, RecipientType.CC));
			result.put(IMailFileSystem.BCC_RECEPIENTS_KEY, getRecipientsOfType(f, RecipientType.BCC));
			result.put(IMailFileSystem.FROM_ADDRESS_KEY, InternetAddress.toUnicodeString(f.getFrom()));
			// result.put(IMailFileSystem.SENDER_ADDRESS_KEY, f.getS);
			result.put(IMailFileSystem.REPLY_TO_RECEPIENTS_KEY, getReplyTo(f));
			result.put(IMailFileSystem.DATETIME_SENT_KEY, f.getSentDate());
			result.put(IMailFileSystem.DATETIME_RECEIVED_KEY, f.getReceivedDate());
			for (Enumeration<Header> headerEnum = f.getAllHeaders(); headerEnum.hasMoreElements();) {
				Header header = headerEnum.nextElement();
				result.put(header.getName(), header.getValue());
			}
			result.put(IMailFileSystem.BEST_REPLY_ADDRESS_KEY, MailFileSystemUtils.findBestReplyAddress(result, getReplyAddressFields()));
			return result;
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void forwardMail(Message emailMessage, String destination) throws FileSystemException {
		try {
			Message forward = new MimeMessage(emailSession);
			// Fill in header
			forward.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destination));
			forward.setSubject("Fwd: " + emailMessage.getSubject());
			//forward.setFrom(new InternetAddress(emailMessage.get));
			// Create the message part
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			// Create a multipart message
			Multipart multipart = new MimeMultipart();
			// set content
			messageBodyPart.setContent(emailMessage, "message/rfc822");
			// Add part to multi part
			multipart.addBodyPart(messageBodyPart);
			// Associate multi-part with message
			forward.setContent(multipart);
			forward.saveChanges();
			// Send the message by authenticating the SMTP server
			// Create a Transport instance and call the sendMessage
			try (Transport t = emailSession.getTransport("smtp")) {
				CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
				// connect to the smpt server using transport instance
				t.connect(cf.getUsername(), cf.getPassword());
				t.sendMessage(forward, forward.getAllRecipients());
			}
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		URLName urlName = null;
		if (isOpen()) {
			try {
				IMAPFolder baseFolder = getConnection();
				urlName = baseFolder.getStore().getURLName();
				releaseConnection(baseFolder);
			} catch (FileSystemException e) {
				log.warn("cannot get urlName", e);
			}
		}
		String name = urlName == null ? "<no url>" : urlName.toString();
		return Misc.concatStrings(name," ", super.getPhysicalDestinationName());
	}

	@Override
	public String getSubject(Message emailMessage) throws FileSystemException {
		try {
			return emailMessage.getSubject();
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public nl.nn.adapterframework.stream.Message getMimeContent(Message emailMessage) throws FileSystemException {
		try {
			return new nl.nn.adapterframework.stream.Message(((IMAPMessage) emailMessage).getMimeStream());
		} catch (MessagingException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void extractEmail(Message emailMessage, SaxElementBuilder emailXml)
			throws FileSystemException, SAXException {
		MailFileSystemUtils.addEmailInfo(this, emailMessage, emailXml);
	}

	@Override
	public void extractAttachment(MimeBodyPart attachment, SaxElementBuilder attachmentsXml)
			throws FileSystemException, SAXException {
		MailFileSystemUtils.addAttachmentInfo(this, attachment, attachmentsXml);
	}

	@IbisDoc({ "1", "The hostname of the IMAP server" })
	public void setHost(String host) {
		this.host = host;
	}

	@IbisDoc({ "2", "The port of the IMAP server", "993" })
	public void setPort(int port) {
		this.port = port;
	}

}
