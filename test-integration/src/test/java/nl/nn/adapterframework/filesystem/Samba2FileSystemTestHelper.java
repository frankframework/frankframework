/*
   Copyright 2019, 2020 WeAreFrank!

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

/**
 * This test class is created to test both Samba2FileSystem and Samba2FileSystemSender classes.
 * 
 * Instructions to create a share on a windows system:
 * - First create a directory you want to share (location doesn't matter)
 * - Right click to that directory -> properties -> Sharing Tab -> Advanced Sharing Options -> Check Share this Folder option -> 
 * Click Permissions -> Set users to be shared if necessary -> Set permissions(Full Control, read, write) -> Click Apply.
 * To verify share:
 * - open file explorer -> write \\localhost on address bar. You will see the share.
 * 
 * @author alisihab
 *
 */


import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.auth.GSSAuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.Directory;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

/**
 * 
 * @author alisihab
 *
 */
public class Samba2FileSystemTestHelper implements IFileSystemTestHelper {

	protected Logger log = LogUtil.getLogger(this);
	private final String SPNEGO_OID="1.3.6.1.5.5.2";
	private final String KERBEROS5_OID="1.2.840.113554.1.2.2";
	private String authType = "SPNEGO";
	//private List<String> authTypes = Arrays.asList("NTLM", "SPNEGO");
	private boolean listHiddenFiles = false;
	private SMBClient client = null;
	private Connection connection;
	private Session session;
	private DiskShare diskShare;
	private String kdc = null;
	private String realm = null;
	private String shareName = null;
	private String userName = null;
	private String password = null;
	private String host = null;
	private Integer port = null;
	private String domain = null;
	private String authAlias = null;

		
	public Samba2FileSystemTestHelper(String shareFolder, String userName, String password, String host, Integer port, String kdc, String realm) {
		this.shareName = shareFolder;
		this.userName = userName;
		this.password = password;
		this.host = host;
		this.port = port;
		this.kdc = kdc;
		this.realm = realm;
	}

	public void setUp() throws Exception {
		open();
	}

	@Override
	public void tearDown() throws Exception {
		System.out.println("");
	}
	
	private void open() throws FileSystemException {
		try {
			AuthenticationContext auth = authenticate();
			client = new SMBClient();
			connection = client.connect(host, port);
			if(connection.isConnected()) {
				log.debug("successfully created connection to ["+connection.getRemoteHostname()+"]");
			}
			session = connection.authenticate(auth);
			if(session == null) {
				throw new FileSystemException("Cannot create session for user ["+userName+"] on domain ["+domain+"]");
			}
			diskShare = (DiskShare) session.connectShare(shareName);
			if(diskShare == null) {
				throw new FileSystemException("Cannot connect to the share ["+ shareName +"]");
			}
		} catch (IOException e) {
			throw new FileSystemException("Cannot connect to samba server", e);
		}
	}

	private void close() throws FileSystemException {
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
		CredentialFactory credentialFactory = new CredentialFactory(getAuthAlias(), getUserName(), getPassword());
		if (StringUtils.isNotEmpty(credentialFactory.getUsername())) {
			if(StringUtils.equalsIgnoreCase(authType, "NTLM")) {
				return new AuthenticationContext(getUserName(), password.toCharArray(), getDomain());
			}else if(StringUtils.equalsIgnoreCase(authType, "SPNEGO")) {

				if(!StringUtils.isEmpty(getKdc()) && !StringUtils.isEmpty(getRealm())) {
					System.setProperty("java.security.krb5.kdc", getKdc());
					System.setProperty("java.security.krb5.realm", getRealm());
				}

				HashMap<String, String> loginParams = new HashMap<String, String>();
				loginParams.put("principal", getUserName());
				LoginContext lc;
				try {
					lc = new LoginContext(getUserName(), null, 
							new UsernameAndPasswordCallbackHandler(getUserName(), getPassword()),
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
	public boolean _fileExists(String folder, String filename) throws Exception {
		boolean exists = diskShare.fileExists(filename);
		return exists;
	}

	@Override
	public boolean _folderExists(String folderName) throws Exception {
		boolean exists = diskShare.folderExists(folderName);
		return exists;
	}

	@Override
	public void _deleteFile(String folder, String filename) throws Exception {
		diskShare.rm(filename);
	}

	@Override
	public OutputStream _createFile(String folder, String filename) throws Exception {
		Set<AccessMask> accessMask = new HashSet<AccessMask>(EnumSet.of(AccessMask.FILE_ADD_FILE));
		Set<SMB2CreateOptions> createOptions = new HashSet<SMB2CreateOptions>(
				EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_WRITE_THROUGH));
		
		final File file = diskShare.openFile(filename, accessMask, null, SMB2ShareAccess.ALL,
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
	public InputStream _readFile(String folder, String filename) throws Exception {
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

	private boolean isFolder(String f) throws FileSystemException {
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

	private String toFile(String filename) throws FileSystemException {
		return filename;
	}

	private boolean folderExists(String folder) throws FileSystemException {
		return isFolder(toFile(folder));
	}

	@Override
	public void _createFolder(String folderName) throws Exception {
		if (folderExists(folderName)) {
			throw new FileSystemException("Create directory for [" + folderName + "] has failed. Directory already exists.");
		} else {
			diskShare.mkdir(folderName);
		}
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		if (!folderExists(folderName)) {
			throw new FileSystemException("Remove directory for [" + folderName + "] has failed. Directory does not exist.");
		} else {
			diskShare.rmdir(folderName, true);
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

	public String getShare() {
		return shareName;
	}
	/** the destination, aka smb://xxx/yyy share */
	public void setShare(String share) {
		this.shareName = share;
	}

	public String getPassword() {
		return password;
	}
	/** the smb share password */
	public void setPassword(String password) {
		this.password = password;
	}

	public String getAuthAlias() {
		return authAlias;
	}
	/** alias used to obtain credentials for the smb share */
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	public String getDomain() {
		return domain;
	}
	/** domain, in case the user account is bound to a domain */
	public void setDomain(String domain) {
		this.domain = domain;
	}


	public String getAuthType() {
		return authType;
	}
	/**
	 * Type of the authentication either 'NTLM' or 'SPNEGO' 
	 * @ff.default SPNEGO
	 */
	public void setAuthType(String authType) {
		this.authType = authType;
	}
	
	public String getKdc() {
		return kdc;
	}
	/** Kerberos Domain Controller, as set in java.security.krb5.kdc */
	public void setKdc(String kdc) {
		this.kdc = kdc;
	}
	
	public boolean isListHiddenFiles() {
		return listHiddenFiles;
	}
	/**
	 * controls whether hidden files are seen or not
	 * @ff.default false
	 */
	public void setListHiddenFiles(boolean listHiddenFiles) {
		this.listHiddenFiles = listHiddenFiles;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}
	
	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

}
