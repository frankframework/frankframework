/*
   Copyright 2019-2025 WeAreFrank!

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
package org.frankframework.filesystem.smb;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.msfscc.fileinformation.FileStandardInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.Factory;
import com.hierynomus.protocol.commons.buffer.Buffer.BufferException;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.auth.Authenticator;
import com.hierynomus.smbj.auth.NtlmAuthenticator;
import com.hierynomus.smbj.auth.SpnegoAuthenticator;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskEntry;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.filesystem.AbstractFileSystem;
import org.frankframework.filesystem.FileAlreadyExistsException;
import org.frankframework.filesystem.FileSystemException;
import org.frankframework.filesystem.FileSystemUtils;
import org.frankframework.filesystem.FolderAlreadyExistsException;
import org.frankframework.filesystem.FolderNotFoundException;
import org.frankframework.filesystem.IWritableFileSystem;
import org.frankframework.filesystem.TypeFilter;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.CredentialFactory;

/**
 *
 * Uses the (newer) SMB 2 and 3 protocol.
 *
 * Possible error codes:
 * <br/>
 * Pre-authentication information was invalid (24) / Idenitfier doesn't match expected value (906):  login information is incorrect
 * Server not found in Kerberos database (7): Verify that the hostname is the FQDN and the server is using a valid SPN.
 *
 * @author Ali Sihab
 * @author Niels Meijer
 *
 */
@Log4j2
public class Samba2FileSystem extends AbstractFileSystem<SmbFileRef> implements IWritableFileSystem<SmbFileRef> {
	private final @Getter String domain = "SMB";

	private @Getter Samba2AuthType authType = Samba2AuthType.SPNEGO;
	private @Getter String share = null;
	private String hostname;
	private int port = 445;

	private @Getter String domainName = null;
	private @Getter String kdc = null;
	private @Getter String realm = null;

	private @Getter String username = null;
	private @Getter String password = null;
	private @Getter String authAlias = null;

	private @Getter boolean listHiddenFiles = false;

	private SMBClient client = null;
	private Connection connection;
	private Session session;
	private DiskShare diskShare;

	public enum Samba2AuthType {
		NTLM, SPNEGO, ANONYMOUS
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getShare())) {
			throw new ConfigurationException("server share endpoint is required");
		}

		switch (authType) {
		case NTLM:
			if(StringUtils.isBlank(domainName)) {
				throw new ConfigurationException("attribute domainName is required for NTLM authentication");
			}
			break;
		case SPNEGO:
			if(StringUtils.isBlank(kdc) || StringUtils.isBlank(realm)) {
				throw new ConfigurationException("attribute kdc and realm are both required for SPNEGO authentication");
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void open() throws FileSystemException {
		try {
			List<Factory.Named<Authenticator>> authenticators = new ArrayList<>();
			authenticators.add(new SpnegoAuthenticator.Factory());
			authenticators.add(new NtlmAuthenticator.Factory());
			SmbConfig config = SmbConfig.builder().withAuthenticators(authenticators).withTimeout(30, TimeUnit.SECONDS).build();
			client = new SMBClient(config);

			connection = client.connect(hostname, port);
			if(connection.isConnected()) {
				log.debug("successfully created connection to [{}]", connection::getRemoteHostname);
			}

			AuthenticationContext authContext = createAuthenticationContext();
			log.debug("creating connection using authentication context [{}]", authContext::getClass);
			session = connection.authenticate(authContext);
			if(session == null) {
				throw new FileSystemException("Cannot create session for " + authContext);
			}

			diskShare = (DiskShare) session.connectShare(getShare());
			if(diskShare == null) {
				throw new FileSystemException("Cannot connect to the share ["+ getShare() +"]");
			}
		} catch (IOException e) {
			throw new FileSystemException("Cannot connect to samba server", e);
		}
		super.open();
	}

	@Override
	public void close() throws FileSystemException {
		CloseUtils.closeSilently(diskShare, client);

		diskShare = null;
		session = null;
		connection = null;
		client = null;

		super.close();
		log.debug("closed connection to [{}] for Samba2FS", hostname);
	}

	private @Nonnull AuthenticationContext createAuthenticationContext() throws FileSystemException {
		CredentialFactory credentialFactory = new CredentialFactory(authAlias, username, password);
		if(StringUtils.isNotEmpty(credentialFactory.getUsername())) {
			switch (authType) {
			case NTLM:
				String cfPassword = credentialFactory.getPassword();
				char[] passwordChars = cfPassword != null ? cfPassword.toCharArray() : new char[0];
				return new AuthenticationContext(credentialFactory.getUsername(), passwordChars, getDomainName());
			case SPNEGO:
				if(!StringUtils.isEmpty(kdc) && !StringUtils.isEmpty(realm)) {
					System.setProperty("java.security.krb5.kdc", kdc);
					System.setProperty("java.security.krb5.realm", realm);
				}

				return SambaFileSystemUtils.createGSSAuthenticationContext(credentialFactory);
			case ANONYMOUS:
				return AuthenticationContext.anonymous();
			}
		}
		return AuthenticationContext.anonymous();
	}

	@Override
	public SmbFileRef toFile(@Nullable String filename) throws FileSystemException {
		return toFile(null, filename);
	}

	@Override
	public SmbFileRef toFile(@Nullable String folder, @Nullable String filename) throws FileSystemException {
		return new SmbFileRef(filename != null ? filename : "", folder);
	}

	@Override
	public DirectoryStream<SmbFileRef> list(String folder, TypeFilter filter) throws FileSystemException {
		return FileSystemUtils.getDirectoryStream(new FilesIterator(folder, filter, diskShare.list(folder)));
	}

	@Override
	public boolean exists(SmbFileRef f) {
		return diskShare.fileExists(f.getName());
	}

	@Override
	public boolean isFolder(SmbFileRef smbFileRef) {
		return FilesIterator.isDirectoryAndAccessible(smbFileRef);
	}

	@Override
	public OutputStream createFile(SmbFileRef f) throws FileSystemException, IOException {
		Set<AccessMask> accessMask = new HashSet<>(EnumSet.of(AccessMask.FILE_ADD_FILE));
		Set<SMB2CreateOptions> createOptions = new HashSet<>(
				EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_WRITE_THROUGH));

		final File file = diskShare.openFile(f.getName(), accessMask, null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF, createOptions);
		return wrapOutputStream(file, file.getOutputStream());
	}

	@Override
	public OutputStream appendFile(SmbFileRef f) throws FileSystemException, IOException {
		final File file = getFile(f, AccessMask.FILE_APPEND_DATA, SMB2CreateDisposition.FILE_OPEN_IF);
		return wrapOutputStream(file, file.getOutputStream(true));
	}

	private static OutputStream wrapOutputStream(Closeable file, OutputStream stream) {
		return new FilterOutputStream(stream) {

			boolean isOpen = true;
			@Override
			public void close() throws IOException {
				if(isOpen) {
					super.close();
					isOpen=false;
				}
				file.close();
			}
		};
	}

	private static InputStream wrapInputStream(File file) {
		return new FilterInputStream(file.getInputStream()) {

			boolean isOpen = true;
			@Override
			public void close() throws IOException {
				if(isOpen) {
					super.close();
					isOpen=false;
				}
				file.close();
			}
		};
	}

	@Override
	public Message readFile(SmbFileRef filename, String charset) throws FileSystemException, IOException {
		File file = getFile(filename, AccessMask.GENERIC_READ, SMB2CreateDisposition.FILE_OPEN);
		MessageContext context = FileSystemUtils.getContext(this, filename, charset);
		return new Message(wrapInputStream(file), context);
	}

	@Override
	public void deleteFile(SmbFileRef f) throws FileSystemException {
		try {
			diskShare.rm(f.getName());
		} catch (SMBApiException e) {
			throw new FileSystemException("Could not delete file [" + getCanonicalNameOrErrorMessage(f) + "]: " + e.getMessage());
		}
	}

	@Override
	public SmbFileRef renameFile(SmbFileRef source, SmbFileRef destination) throws FileSystemException {
		try (File file = getFile(source, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_OPEN)) {
			file.rename(destination.getName(), true);
		}
		return destination;
	}

	@Override
	public SmbFileRef moveFile(SmbFileRef f, String destinationFolder, boolean createFolder) throws FileSystemException {
		try (File file = getFile(f, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_OPEN)) {
			SmbFileRef destination = toFile(destinationFolder, f.getName());
			if(exists(destination)) {
				throw new FileAlreadyExistsException("target already exists");
			}

			file.rename(destination.getName(), false);
			return destination;
		} catch (SMBApiException e) {
			throw new FileSystemException("unable to move file", e);
		}
	}

	@Override
	public SmbFileRef copyFile(SmbFileRef f, String destinationFolder, boolean createFolder) throws FileSystemException {
		if(createFolder && !folderExists(destinationFolder)) {
			createFolder(destinationFolder);
		}

		try (File file = getFile(f, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_OPEN)) {
			SmbFileRef destination = toFile(destinationFolder, f.getFilename());
			try (File destinationFile = getFile(destination, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_SUPERSEDE)) {
				file.remoteCopyTo(destinationFile);
			} catch (TransportException | BufferException | SMBApiException e) {
				throw new FileSystemException("cannot copy file ["+f+"] to ["+destinationFolder+"]",e);
			}
			return destination;
		}
	}

	@Override
	@Nullable
	public Map<String, Object> getAdditionalFileProperties(SmbFileRef f) {
		Map<String, Object> attributes = new HashMap<>();
		FileAllInformation attrs = getFileAttributes(f);
		if(attrs != null) {
			attributes.put("ctime", attrs.getBasicInformation().getCreationTime());
			attributes.put("atime", attrs.getBasicInformation().getLastAccessTime());
			attributes.put("fileAttributes", attrs.getBasicInformation().getFileAttributes());
			attributes.put("nameInformation", attrs.getNameInformation());
			attributes.put("rawListing", attrs.toString());
		}
		return attributes;
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		try {
			return diskShare.folderExists(folder);
		} catch (SMBApiException e) {
			if(NtStatus.STATUS_OBJECT_NAME_COLLISION == NtStatus.valueOf(e.getStatusCode())) {
				return false;
			}
			throw e;
		}
	}

	@Override
	public void createFolder(String folder) throws FileSystemException {
		if (folderExists(folder)) {
			throw new FolderAlreadyExistsException("Create directory for [" + folder + "] has failed. Directory already exists.");
		}
		diskShare.mkdir(folder);
	}

	@Override
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException {
		if (!folderExists(folder)) {
			throw new FolderNotFoundException("Cannot remove folder [" + folder + "]. Directory does not exist.");
		}
		try {
			diskShare.rmdir(folder, removeNonEmptyFolder);
		} catch(SMBApiException e) {
			throw new FileSystemException("Cannot remove folder [" + folder + "]", e);
		}
	}

	private File getFile(SmbFileRef file, AccessMask accessMask, SMB2CreateDisposition createDisposition) {
		Set<SMB2CreateOptions> createOptions = new HashSet<>();
		createOptions.add(SMB2CreateOptions.FILE_WRITE_THROUGH);

		return diskShare.openFile(file.getName(), EnumSet.of(accessMask), null, SMB2ShareAccess.ALL, createDisposition, createOptions);
	}

	@Override
	public long getFileSize(SmbFileRef f) {
		getFileAttributes(f);
		return f.getAttributes().getStandardInformation().getEndOfFile();
	}

	private FileAllInformation getFileAttributes(SmbFileRef f) {
		if(f.getAttributes() == null) {
			try {
				f.setAttributes(getAttributes(f));
			} catch (SMBApiException e) {
				log.warn("unable to fetch file attributes for [{}]", f.getName(), e);
			}
		}
		return f.getAttributes();
	}

	private FileAllInformation getAttributes(SmbFileRef file) throws SMBApiException {
		Set<AccessMask> accessMaskSet = new HashSet<>();
		accessMaskSet.add(AccessMask.FILE_READ_ATTRIBUTES);

		try (DiskEntry entry = diskShare.open(file.getName(), accessMaskSet, null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
			return entry.getFileInformation();
		}
	}

	@Override
	public String getName(SmbFileRef file) {
		String name = file.getFilename();
		if(StringUtils.isNotEmpty(name)) {
			return name;
		}
		String folder = file.getFolder();
		if (folder != null) { // Folder: only take part before last slash
			int lastSlashPos = folder.lastIndexOf('\\', folder.length() - 2);
			return folder.substring(lastSlashPos + 1);
		}

		return null;
	}

	@Override
	public String getParentFolder(SmbFileRef f) {
		return f.getFolder();
	}

	@Override
	public String getCanonicalName(SmbFileRef f) {
		return f.getName(); //Should include folder structure if known
	}

	@Override
	public Date getModificationTime(SmbFileRef f) {
		getFileAttributes(f);
		return f.getAttributes().getBasicInformation().getChangeTime().toDate();
	}

	@Override
	public String getPhysicalDestinationName() {
		return "host "+authType.name()+":["+hostname+"/"+getShare()+"]";
	}

	/**
	 * May not contain '\\' characters. The destination share, aka smb://xxx/yyy share.
	 * @ff.optional
	 */
	public void setShare(String share) {
		this.share = share;
	}

	/** the smb share username */
	public void setUsername(String username) {
		this.username = username;
	}

	/** the smb share password */
	public void setPassword(String password) {
		this.password = password;
	}

	/** alias used to obtain credentials for the smb share */
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	/** NTLM only: logon/authentication domain, in case the user account is bound to a domain such as Active Directory. */
	public void setDomainName(String domain) {
		this.domainName = domain;
	}

	/**
	 * Type of the authentication either 'NTLM' or 'SPNEGO'.
	 * When setting SPNEGO, the host must use the FQDN, and must be registered on the KDC with a valid SPN.
	 * @ff.default SPNEGO
	 */
	public void setAuthType(Samba2AuthType authType) {
		this.authType = authType;
	}

	/** SPNEGO only:
	 * Key Distribution Center, typically hosted on a domain controller.
	 * Stored in <code>java.security.krb5.kdc</code>
	 */
	public void setKdc(String kdc) {
		this.kdc = kdc;
	}

	/**
	 * SPNEGO only:
	 * Kerberos Realm, case sensitive. Typically upper case and the same as the domain name.
	 * An Active Directory domain acts as a Kerberos Realm.
	 * Stored in <code>java.security.krb5.realm</code>
	 */
	public void setRealm(String realm) {
		this.realm = realm;
	}

	/**
	 * Hostname of the SMB share.
	 * @ff.mandatory
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * Port to connect to.
	 * @ff.default 445
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Controls if hidden files are seen
	 * @ff.default false
	 */
	public void setListHiddenFiles(boolean listHiddenFiles) {
		this.listHiddenFiles = listHiddenFiles;
	}

	class FilesIterator implements Iterator<SmbFileRef> {

		private final List<SmbFileRef> files;
		private int i = 0;

		public FilesIterator(String folder, TypeFilter filter, List<FileIdBothDirectoryInformation> list) {
			files = new ArrayList<>();
			for (FileIdBothDirectoryInformation info : list) {
				if (!StringUtils.equals(".", info.getFileName()) && !StringUtils.equals("..", info.getFileName())) {
					SmbFileRef file = new SmbFileRef(info.getFileName(), folder);
					try {
						FileAllInformation fileInfo = getAttributes(file);
						file.setAttributes(fileInfo);
						if (!allowHiddenFile(file)) continue;

						if (filter.includeFiles() && isFileAndAccessible(file)) {
							files.add(file);
						}
						if (filter.includeFolders() && isDirectoryAndAccessible(file)) {
							files.add(file);
						}
					} catch (SMBApiException e) {
						if (NtStatus.STATUS_DELETE_PENDING == NtStatus.valueOf(e.getStatusCode())) {
							log.debug("delete pending for file [{}]", file.getName());
						} else {
							throw e;
						}
					}
				}
			}
		}

		static boolean isFileAndAccessible(SmbFileRef file) {
			FileStandardInformation fsi = file.getAttributes().getStandardInformation();
			boolean accessible = !fsi.isDeletePending();
			boolean isDirectory = fsi.isDirectory();
			return accessible && !isDirectory;
		}

		static boolean isDirectoryAndAccessible(SmbFileRef file) {
			FileStandardInformation fsi = file.getAttributes().getStandardInformation();
			boolean accessible = !fsi.isDeletePending();
			boolean isDirectory = fsi.isDirectory();
			return accessible && isDirectory;
		}

		private boolean allowHiddenFile(SmbFileRef file) {
			boolean isHidden = EnumWithValue.EnumUtils.isSet(file.getAttributes().getBasicInformation().getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_HIDDEN);
			return isListHiddenFiles() || !isHidden;
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.size();
		}

		@Override
		public SmbFileRef next() {
			if(!hasNext()) {
				throw new NoSuchElementException();
			}
			return files.get(i++);
		}

		@Override
		public void remove() {
			SmbFileRef file = files.get(i++);
			try {
				deleteFile(file);
			} catch (FileSystemException e) {
				log.warn("unable to remove file [{}]: {}", getCanonicalNameOrErrorMessage(file), e.getMessage());
			}
		}
	}

}
