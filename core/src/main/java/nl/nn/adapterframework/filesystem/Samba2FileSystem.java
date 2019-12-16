/*
   Copyright 2019 Integration Partners

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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
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
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.auth.GSSAuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.Directory;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

/**
 * 
 * @author alisihab
 *
 */
public class Samba2FileSystem implements IWritableFileSystem<String> {

	protected Logger log = LogUtil.getLogger(this);

	private final String SPNEGO_OID="1.3.6.1.5.5.2";
	private final String KERBEROS5_OID="1.2.840.113554.1.2.2";
	
	private String authType = "SPNEGO";
	private List<String> authTypes = Arrays.asList("NTLM", "SPNEGO");
	
	private String shareName = null;
	private String domain = null;
	private String kdc = null;
	private String realm = null;
	private String username = null;
	private String password = null;
	private String authAlias = null;

	private boolean listHiddenFiles = false;

	private SMBClient client = null;
	private Connection connection;
	private Session session;
	private DiskShare diskShare;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getShare())) {
			throw new ConfigurationException("server share endpoint is required");
		}
		if(!authTypes.contains(authType)) {
			throw new ConfigurationException("Invalid authType please provide one of the values "+authTypes);
		}
	}

	@Override
	public void open() throws FileSystemException {
		try {
			AuthenticationContext auth = authenticate();
			client = new SMBClient();
			connection = client.connect(domain);
			if(connection.isConnected()) {
				log.debug("successfully created connection to ["+connection.getRemoteHostname()+"]");
			}
			session = connection.authenticate(auth);
			if(session == null) {
				throw new FileSystemException("Cannot create session for user ["+username+"] on domain ["+domain+"]");
			}
			diskShare = (DiskShare) session.connectShare(shareName);
			if(diskShare == null) {
				throw new FileSystemException("Cannot connect to the share ["+ shareName +"]");
			}
		} catch (IOException e) {
			throw new FileSystemException("Cannot connect to samba server", e);
		}
	}

	@Override
	public void close() throws FileSystemException {
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
			if(StringUtils.equalsIgnoreCase(authType, "NTLM")) {
				return new AuthenticationContext(getUsername(), password.toCharArray(), getDomain());
			}else if(StringUtils.equalsIgnoreCase(authType, "SPNEGO")) {

				if(!StringUtils.isEmpty(getKdc()) && !StringUtils.isEmpty(getRealm())) {
					System.setProperty("java.security.krb5.kdc", getKdc());
					System.setProperty("java.security.krb5.realm", getRealm());
				}

				HashMap<String, String> loginParams = new HashMap<String, String>();
				loginParams.put("principal", getUsername());
				LoginContext lc;
				try {
					lc = new LoginContext(getUsername(), null, 
							new UsernameAndPasswordCallbackHandler(getUsername(), getPassword()),
							new KerberosLoginConfiguration(loginParams));
					lc.login();

					Subject subject = lc.getSubject();
					KerberosPrincipal krbPrincipal = subject.getPrincipals(KerberosPrincipal.class).iterator().next();

					Oid spnego = new Oid(SPNEGO_OID);
					Oid kerberos5 = new Oid(KERBEROS5_OID);

					final GSSManager manager = GSSManager.getInstance();

					final GSSName name = manager.createName(krbPrincipal.toString(), GSSName.NT_USER_NAME);
					Set<Oid> mechs = new HashSet<Oid>(Arrays.asList(manager.getMechsForName(name.getStringNameType())));
					final Oid mech;

					if (mechs.contains(kerberos5)) {
						mech = kerberos5;
					} else if (mechs.contains(spnego)) {
						mech = spnego;
					} else {
						throw new IllegalArgumentException("No mechanism found");
					}

					GSSCredential creds = Subject.doAs(subject, new PrivilegedExceptionAction<GSSCredential>() {
						@Override
						public GSSCredential run() throws GSSException {
							return manager.createCredential(name, GSSCredential.DEFAULT_LIFETIME, mech, GSSCredential.INITIATE_ONLY);
						}
					});

					GSSAuthenticationContext auth = new GSSAuthenticationContext(krbPrincipal.getName(), krbPrincipal.getRealm(), subject, creds);
					return auth;

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
	public Iterator<String> listFiles(String folder) throws FileSystemException {
		return new FilesIterator(folder, diskShare.list(folder));
	}

	@Override
	public boolean exists(String f) throws FileSystemException {
		boolean exists = isFolder(f) ? diskShare.folderExists(f) : diskShare.fileExists(f);
		return exists;
	}

	@Override
	public OutputStream createFile(String f) throws FileSystemException, IOException {
		Set<AccessMask> accessMask = new HashSet<AccessMask>(EnumSet.of(AccessMask.FILE_ADD_FILE));
		Set<SMB2CreateOptions> createOptions = new HashSet<SMB2CreateOptions>(
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
	public InputStream readFile(String filename) throws FileSystemException, IOException {
		final File file = getFile(filename, AccessMask.GENERIC_READ, SMB2CreateDisposition.FILE_OPEN);
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
	}

	@Override
	public void deleteFile(String f) throws FileSystemException {
		diskShare.rm(f);
	}

	@Override
	public String renameFile(String f, String newName, boolean force) throws FileSystemException {
		try (File file = getFile(f, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_OPEN)) {
			if (exists(newName) && !force) {
				throw new FileSystemException("Cannot rename file. Destination file already exists.");
			}
			file.rename(newName, force);
		}
		return newName;
	}

	@Override
	public String moveFile(String f, String to, boolean createFolder) throws FileSystemException {
		try (File file = getFile(f, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_OPEN)) {
			if (exists(to)) {
				if (!isFolder(to)) {
					throw new FileSystemException("Cannot move file. Destination file ["+to+"] is not a folder.");
				}
			} else {
				if (createFolder) {
					createFolder(to);
				} else {
					throw new FileSystemException("Cannot move file. Destination folder ["+to+"] does not exist.");
				}
			}
			String destination = to+"\\"+f;
			file.rename(destination, createFolder);
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
		} else {
			diskShare.mkdir(folder);
		}
	}

	@Override
	public void removeFolder(String folder) throws FileSystemException {
		if (!folderExists(folder)) {
			throw new FileSystemException("Remove directory for [" + folder + "] has failed. Directory does not exist.");
		} else {
			diskShare.rmdir(folder, true);
		}
	}

	private File getFile(String filename, AccessMask accessMask, SMB2CreateDisposition createDisposition) {
		Set<SMB2ShareAccess> shareAccess = new HashSet<SMB2ShareAccess>();
		shareAccess.addAll(SMB2ShareAccess.ALL);

		Set<SMB2CreateOptions> createOptions = new HashSet<SMB2CreateOptions>();
		createOptions.add(SMB2CreateOptions.FILE_WRITE_THROUGH);
		
		Set<AccessMask> accessMaskSet = new HashSet<AccessMask>();
		accessMaskSet.add(accessMask);
		File file;

		file = diskShare.openFile(filename, accessMaskSet, null, shareAccess, createDisposition, createOptions);
		return file;
	}

	private Directory getFolder(String filename, AccessMask accessMask, SMB2CreateDisposition createDisposition) {
		Set<SMB2ShareAccess> shareAccess = new HashSet<SMB2ShareAccess>();
		shareAccess.addAll(SMB2ShareAccess.ALL);

		Set<AccessMask> accessMaskSet = new HashSet<AccessMask>();
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
		} else {
			try (File file = getFile(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN)) {
				size = file.getFileInformation().getStandardInformation().getAllocationSize();
				return size;
			}
		}
	}

	@Override
	public String getName(String f) {
		return f;
	}

	@Override
	public String getCanonicalName(String f) throws FileSystemException {
		return f;
	}

	@Override
	public Date getModificationTime(String f) throws FileSystemException {
		if (isFolder(f)) {
			try (Directory dir = getFolder(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN)) {
				Date date = dir.getFileInformation().getBasicInformation().getLastWriteTime().toDate();
				return date;
			}
		} else {
			try (File file = getFile(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN)) {
				Date date = file.getFileInformation().getBasicInformation().getLastWriteTime().toDate();
				return date;
			}
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		return "domain ["+getDomain()+"] share ["+getShare()+"]";
	}


	
	public String getShare() {
		return shareName;
	}
	@IbisDoc({ "1", "the destination, aka smb://xxx/yyy share", "" })
	public void setShare(String share) {
		this.shareName = share;
	}

	public String getUsername() {
		return username;
	}
	@IbisDoc({ "2", "the smb share username", "" })
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}
	@IbisDoc({ "3", "the smb share password", "" })
	public void setPassword(String password) {
		this.password = password;
	}

	public String getAuthAlias() {
		return authAlias;
	}
	@IbisDoc({ "4", "alias used to obtain credentials for the smb share", "" })
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	public String getDomain() {
		return domain;
	}
	@IbisDoc({ "5", "domain, in case the user account is bound to a domain", "" })
	public void setDomain(String domain) {
		this.domain = domain;
	}


	public String getAuthType() {
		return authType;
	}
	@IbisDoc({ "6", "Type of the authentication either 'NTLM' or 'SPNEGO' ", "SPNEGO" })
	public void setAuthType(String authType) {
		this.authType = authType;
	}
	
	public String getKdc() {
		return kdc;
	}
	@IbisDoc({ "7", "Kerberos Domain Controller, as set in java.security.krb5.kdc", "" })
	public void setKdc(String kdc) {
		this.kdc = kdc;
	}
	
	public String getRealm() {
		return realm;
	}
	@IbisDoc({ "8", "Kerberos Realm, as set in java.security.krb5.realm", "" })
	public void setRealm(String realm) {
		this.realm = realm;
	}

	public boolean isListHiddenFiles() {
		return listHiddenFiles;
	}
	@IbisDoc({ "9", "controls whether hidden files are seen or not", "false" })
	public void setListHiddenFiles(boolean listHiddenFiles) {
		this.listHiddenFiles = listHiddenFiles;
	}

	class FilesIterator implements Iterator<String> {

		private List<FileIdBothDirectoryInformation> files;
		private int i = 0;
		private String prefix;

		public FilesIterator(String parent, List<FileIdBothDirectoryInformation> list) {
			prefix = parent != null ? parent + "\\" : "";
			files = new ArrayList<FileIdBothDirectoryInformation>();
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
