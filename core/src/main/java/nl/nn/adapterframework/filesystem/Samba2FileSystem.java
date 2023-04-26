/*
   Copyright 2019-2022 WeAreFrank!

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

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginContext;

import org.apache.commons.lang3.StringUtils;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.msfscc.fileinformation.FileStandardInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer.BufferException;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.auth.GSSAuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.Directory;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;

/**
 *
 * @author alisihab
 *
 */
public class Samba2FileSystem extends FileSystemBase<String> implements IWritableFileSystem<String> {
	private final @Getter(onMethod = @__(@Override)) String domain = "SMB";

	private static final String SPNEGO_OID = "1.3.6.1.5.5.2";
	private static final String KERBEROS5_OID = "1.2.840.113554.1.2.2";

	private @Getter Samba2AuthType authType = Samba2AuthType.SPNEGO;
	private @Getter String share = null;
	private @Getter String authenticationDomain = null;
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
		NTLM,
		SPNEGO
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getShare())) {
			throw new ConfigurationException("server share endpoint is required");
		}
	}

	@Override
	public void open() throws FileSystemException {
		try {
			AuthenticationContext auth = authenticate();
			client = new SMBClient();
			connection = client.connect(authenticationDomain); //TODO: can this be null!?
			if(connection.isConnected()) {
				log.debug("successfully created connection to ["+connection.getRemoteHostname()+"]");
			}
			session = connection.authenticate(auth);
			if(session == null) {
				throw new FileSystemException("Cannot create session for alias [" +
						authAlias + "] / user ["+username+"] on domain ["+authenticationDomain+"]");
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
		super.close();
		try {
			if(diskShare != null) {
				diskShare.close();
			}
			if(session != null) {
				session.close();
			}
			if(connection != null) {
				connection.close();
			}
			if(client != null) {
				client.close();
			}
			diskShare = null;
			session = null;
			connection = null;
			client = null;
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	private AuthenticationContext authenticate() throws FileSystemException {
		CredentialFactory credentialFactory = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if (StringUtils.isNotEmpty(credentialFactory.getUsername())) {
			switch(authType) {
				case NTLM:
					String cfPassword = credentialFactory.getPassword();
					char[] passwordChars = cfPassword != null ? cfPassword.toCharArray() : new char[0];
					return new AuthenticationContext(credentialFactory.getUsername(), passwordChars, getAuthenticationDomain());
				case SPNEGO:
					if(!StringUtils.isEmpty(getKdc()) && !StringUtils.isEmpty(getRealm())) {
						System.setProperty("java.security.krb5.kdc", getKdc());
						System.setProperty("java.security.krb5.realm", getRealm());
					}

					HashMap<String, String> loginParams = new HashMap<>();
					loginParams.put("principal", credentialFactory.getUsername());
					LoginContext lc;
					try {
						lc = new LoginContext(credentialFactory.getUsername(), null,
								new UsernameAndPasswordCallbackHandler(credentialFactory.getUsername(), credentialFactory.getPassword()),
								new KerberosLoginConfiguration(loginParams));
						lc.login();

						Subject subject = lc.getSubject();
						KerberosPrincipal krbPrincipal = subject.getPrincipals(KerberosPrincipal.class).iterator().next();

						Oid spnego = new Oid(SPNEGO_OID);
						Oid kerberos5 = new Oid(KERBEROS5_OID);

						final GSSManager manager = GSSManager.getInstance();

						final GSSName name = manager.createName(krbPrincipal.toString(), GSSName.NT_USER_NAME);
						Set<Oid> mechs = new HashSet<>(Arrays.asList(manager.getMechsForName(name.getStringNameType())));
						final Oid mech;

						if (mechs.contains(kerberos5)) {
							mech = kerberos5;
						} else if (mechs.contains(spnego)) {
							mech = spnego;
						} else {
							throw new IllegalArgumentException("No mechanism found");
						}

						GSSCredential creds = Subject.doAs(subject, (PrivilegedExceptionAction<GSSCredential>) () -> manager.createCredential(name, GSSCredential.DEFAULT_LIFETIME, mech, GSSCredential.INITIATE_ONLY));

						return new GSSAuthenticationContext(krbPrincipal.getName(), krbPrincipal.getRealm(), subject, creds);
					} catch (Exception e) {
						if(e.getMessage().contains("Cannot locate default realm")) {
							throw new FileSystemException("Please fill the kdc and realm field or provide krb5.conf file including realm",e);
						}
						throw new FileSystemException(e);
					}
			}
		}
		return null;
	}

	@Override
	public String toFile(String filename) throws FileSystemException {
		return filename;
	}

	@Override
	public String toFile(String folder, String filename) throws FileSystemException {
		return toFile(folder+"/"+filename);
	}

	@Override
	public DirectoryStream<String> listFiles(String folder) throws FileSystemException {
		return FileSystemUtils.getDirectoryStream(new FilesIterator(folder, diskShare.list(folder)));
	}

	@Override
	public boolean exists(String f) throws FileSystemException {
		return isFolder(f) ? diskShare.folderExists(f) : diskShare.fileExists(f);
	}

	@Override
	public OutputStream createFile(String f) throws FileSystemException, IOException {
		Set<AccessMask> accessMask = new HashSet<>(EnumSet.of(AccessMask.FILE_ADD_FILE));
		Set<SMB2CreateOptions> createOptions = new HashSet<>(
				EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_WRITE_THROUGH));

		final File file = diskShare.openFile(f, accessMask, null, SMB2ShareAccess.ALL,
				SMB2CreateDisposition.FILE_OVERWRITE_IF, createOptions);
		OutputStream out = file.getOutputStream();
		FilterOutputStream fos = new FilterOutputStream(out) {

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
		return fos;
	}

	@Override
	public OutputStream appendFile(String f) throws FileSystemException, IOException {
		final File file = getFile(f, AccessMask.FILE_APPEND_DATA, SMB2CreateDisposition.FILE_OPEN_IF);
		OutputStream out = file.getOutputStream();
		FilterOutputStream fos = new FilterOutputStream(out) {

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
		return fos;
	}

	@Override
	public Message readFile(String filename, String charset) throws FileSystemException, IOException {
		return new Samba2Message(getFile(filename, AccessMask.GENERIC_READ, SMB2CreateDisposition.FILE_OPEN), FileSystemUtils.getContext(this, filename, charset));
	}

	private class Samba2Message extends Message {

		public Samba2Message(File file, Map<String,Object> context) {
			super(() -> {
				InputStream is = file.getInputStream();
				FilterInputStream fis = new FilterInputStream(is) {

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
				return fis;

			}, context, file.getClass());
		}
	}

	@Override
	public void deleteFile(String f) throws FileSystemException {
		diskShare.rm(f);
	}

	@Override
	public String renameFile(String source, String destination) throws FileSystemException {
		try (File file = getFile(source, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_OPEN)) {
			file.rename(destination, true);
		}
		return destination;
	}

	@Override
	public String moveFile(String f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		try (File file = getFile(f, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_OPEN)) {
			String destination = toFile(destinationFolder, f);
			file.rename(destination, false);
			return destination;
		}
	}

	@Override
	public String copyFile(String f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		try (File file = getFile(f, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_OPEN)) {
			String destination = toFile(destinationFolder, f);
			try (File destinationFile = getFile(f, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_OVERWRITE)) {
				file.remoteCopyTo(destinationFile);
			} catch (TransportException | BufferException e) {
				throw new FileSystemException("cannot copy file ["+f+"] to ["+destinationFolder+"]",e);
			}
			return destination;
		}
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(String f) {
		return null;
	}

	public boolean isFolder(String f) throws FileSystemException {
		try {
			return diskShare.getFileInformation(f).getStandardInformation().isDirectory();
		}catch(SMBApiException e) {
			if(NtStatus.valueOf(e.getStatusCode()).equals(NtStatus.STATUS_OBJECT_NAME_NOT_FOUND)) {
				return false;
			}
			if(NtStatus.valueOf(e.getStatusCode()).equals(NtStatus.STATUS_DELETE_PENDING)) {
				return false;
			}
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		return isFolder(toFile(folder));
	}

	@Override
	public void createFolder(String folder) throws FileSystemException {
		if (folderExists(folder)) {
			throw new FileSystemException("Create directory for [" + folder + "] has failed. Directory already exists.");
		}
		diskShare.mkdir(folder);
	}

	@Override
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException {
		if (!folderExists(folder)) {
			throw new FileSystemException("Remove directory for [" + folder + "] has failed. Directory does not exist.");
		}
		try {
			diskShare.rmdir(folder, removeNonEmptyFolder);
		} catch(SMBApiException e) {
			throw new FileSystemException("Remove directory for [" + folder + "] has failed.", e);
		}
	}

	private File getFile(String filename, AccessMask accessMask, SMB2CreateDisposition createDisposition) {
		Set<SMB2ShareAccess> shareAccess = new HashSet<>();
		shareAccess.addAll(SMB2ShareAccess.ALL);

		Set<SMB2CreateOptions> createOptions = new HashSet<>();
		createOptions.add(SMB2CreateOptions.FILE_WRITE_THROUGH);

		Set<AccessMask> accessMaskSet = new HashSet<>();
		accessMaskSet.add(accessMask);
		File file;

		file = diskShare.openFile(filename, accessMaskSet, null, shareAccess, createDisposition, createOptions);
		return file;
	}

	private Directory getFolder(String filename, AccessMask accessMask, SMB2CreateDisposition createDisposition) {
		Set<SMB2ShareAccess> shareAccess = new HashSet<>();
		shareAccess.addAll(SMB2ShareAccess.ALL);

		Set<AccessMask> accessMaskSet = new HashSet<>();
		accessMaskSet.add(accessMask);

		Directory file;
		file = diskShare.openDirectory(filename, accessMaskSet, null, shareAccess, createDisposition, null);
		return file;
	}

	@Override
	public long getFileSize(String f) throws FileSystemException {
		long size;
		if (isFolder(f)) {
			try (Directory dir = getFolder(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN)) {
				size = dir.getFileInformation().getStandardInformation().getAllocationSize();
				return size;
			}
		}
		try (File file = getFile(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN)) {
			size = file.getFileInformation().getStandardInformation().getAllocationSize();
			return size;
		}
	}

	@Override
	public String getName(String f) {
		return f;
	}

	@Override
	public String getParentFolder(String f) {
		String filePath = f;
		if(filePath.endsWith("\\")) {
			filePath = filePath.substring(0, filePath.lastIndexOf("\\"));
		}
		return filePath.substring(0, f.lastIndexOf("\\"));
	}

	@Override
	public String getCanonicalName(String f) throws FileSystemException {
		return f;
	}

	@Override
	public Date getModificationTime(String f) throws FileSystemException {
		if (isFolder(f)) {
			try (Directory dir = getFolder(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN)) {
				return dir.getFileInformation().getBasicInformation().getLastWriteTime().toDate();
			}
		}
		try (File file = getFile(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN)) {
			return file.getFileInformation().getBasicInformation().getLastWriteTime().toDate();
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		return "domain ["+getAuthenticationDomain()+"] share ["+getShare()+"]";
	}

	/** the destination, aka smb://xxx/yyy share */
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

	/** domain, in case the user account is bound to a domain */
	public void setAuthenticationDomain(String domain) {
		this.authenticationDomain = domain;
	}
	@Deprecated
	@ConfigurationWarning("Please use attribute authenticationDomain instead")
	public void setDomain(String domain) {
		setAuthenticationDomain(domain);
	}

	/**
	 * Type of the authentication either 'NTLM' or 'SPNEGO'
	 * @ff.default SPNEGO
	 */
	public void setAuthType(Samba2AuthType authType) {
		this.authType = authType;
	}

	/** Kerberos Domain Controller, as set in java.security.krb5.kdc */
	public void setKdc(String kdc) {
		this.kdc = kdc;
	}

	/** Kerberos Realm, as set in java.security.krb5.realm */
	public void setRealm(String realm) {
		this.realm = realm;
	}

	/**
	 * controls whether hidden files are seen or not
	 * @ff.default false
	 */
	public void setListHiddenFiles(boolean listHiddenFiles) {
		this.listHiddenFiles = listHiddenFiles;
	}

	class FilesIterator implements Iterator<String> {

		private List<FileIdBothDirectoryInformation> files;
		private int i = 0;
		private String prefix;

		public FilesIterator(String parent, List<FileIdBothDirectoryInformation> list) {
			prefix = parent != null ? parent + "\\" : "";
			files = new ArrayList<>();
			for (FileIdBothDirectoryInformation info : list) {
				if (!StringUtils.equals(".", info.getFileName()) && !StringUtils.equals("..", info.getFileName())) {
					boolean isHidden = EnumWithValue.EnumUtils.isSet(info.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_HIDDEN);
					try {
						FileStandardInformation fai = diskShare.getFileInformation(prefix + info.getFileName()).getStandardInformation();
						boolean accessible = !fai.isDeletePending();
						boolean isDirectory = fai.isDirectory();
						if (accessible && !isDirectory) {
							if (isListHiddenFiles()) {
								files.add(info);
							} else {
								if (!isHidden) {
									files.add(info);
								}
							}
						}
					} catch (SMBApiException e) {
						if(NtStatus.valueOf(e.getStatusCode()).equals(NtStatus.STATUS_DELETE_PENDING)) {
							log.debug("delete pending for file ["+ info.getFileName()+"]");
						} else {
							throw e;
						}
					}

				}
			}
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.size();
		}

		@Override
		public String next() {
			return prefix + files.get(i++).getFileName();
		}

		@Override
		public void remove() {
			try {
				deleteFile(prefix + files.get(i++).getFileName());
			} catch (FileSystemException e) {
				log.error("Unable to close disk share after deleting the file",e);
			}
		}
	}

}
