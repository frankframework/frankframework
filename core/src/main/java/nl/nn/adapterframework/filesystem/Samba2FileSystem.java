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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.Directory;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

/**
 * @author alisihab
 *
 */
public class Samba2FileSystem implements IWritableFileSystem<String> {

	protected Logger log = LogUtil.getLogger(this);
	
	private String domain = null;
	private String username = null;
	private String password = null;
	private String authAlias = null;
	/** Share name is required*/
	private String shareName = null;
	private boolean isForce;
	private boolean listHiddenFiles = true;

	private AuthenticationContext auth;
	private SMBClient client = null;
	private Connection connection;
	private Session session;
	private DiskShare diskShare;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getShare())) {
			throw new ConfigurationException("server share endpoint is required");
		}

		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if (StringUtils.isNotEmpty(cf.getUsername())) {
			auth = new AuthenticationContext(username, password.toCharArray(), domain);
		}
	}

	@Override
	public void open() throws FileSystemException {
		try {
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
			diskShare.close();
			diskShare = null;
			session.close();
			connection.close();
			client.close();
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String toFile(String filename) throws FileSystemException {
		return filename;
	}

	@Override
	public Iterator<String> listFiles(String folder) throws FileSystemException {
		return new FilesIterator(diskShare.list(""));
	}

	@Override
	public boolean exists(String f) throws FileSystemException {
		boolean exists = diskShare.fileExists(f);
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

		return out;
	}

	@Override
	public OutputStream appendFile(String f) throws FileSystemException, IOException {
		final File file = getFile(f, AccessMask.FILE_APPEND_DATA, SMB2CreateDisposition.FILE_OPEN_IF);
		OutputStream out = file.getOutputStream();

		return out;
	}

	@Override
	public InputStream readFile(String filename) throws FileSystemException, IOException {
		final File file = getFile(filename, AccessMask.GENERIC_READ, SMB2CreateDisposition.FILE_OPEN);
		InputStream is = file.getInputStream();

		return is;
	}

	@Override
	public void deleteFile(String f) {
		diskShare.rm(f);
	}

	@Override
	public String renameFile(String f, String newName, boolean force) throws FileSystemException {
		File file = getFile(f, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_OPEN);
		if (exists(newName) && !isForce) {
			throw new FileSystemException("Cannot rename file. Destination file already exists.");
		}
		file.rename(newName, isForce);
		file.close();
		return newName;
	}

	@Override
	public String moveFile(String f, String to, boolean createFolder) throws FileSystemException {
		File file = getFile(f, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_OPEN);
		if (exists(to)) {
			if (!isFolder(to)) {
				throw new FileSystemException("Cannot move file. Destination file ["+to+"] is not a folder.");
			}
		} else {
			if (isForce) {
				createFolder(to);
			} else {
				throw new FileSystemException("Cannot move file. Destination folder ["+to+"] does not exist.");
			}
		}
		file.rename(to, isForce);
		file.close();
		return to+"/"+file.getFileName();
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(String f) {
		return null;
	}

	@Override
	public boolean isFolder(String f) throws FileSystemException {
		boolean isFolder = diskShare.getFileInformation(f).getStandardInformation().isDirectory();
		return isFolder;
	}

	@Override
	public void createFolder(String f) throws FileSystemException {
		if (diskShare.folderExists(f)) {
			throw new FileSystemException("Create directory for [" + f + "] has failed. Directory already exists.");
		} else {
			diskShare.mkdir(f);
		}
	}

	@Override
	public void removeFolder(String f) throws FileSystemException {
		if (!diskShare.folderExists(f)) {
			throw new FileSystemException("Remove directory for [" + f + "] has failed. Directory does not exist.");
		} else {
			diskShare.rmdir(f, true);
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
//		if (isFolder) {
//			Directory dir = getFolder(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN);
//			size = dir.getFileInformation().getStandardInformation().getAllocationSize();
//			dir.close();
//			return size;
//		} else {
			File file = getFile(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN);
			size = file.getFileInformation().getStandardInformation().getAllocationSize();
			file.close();
			return size;
//		}
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
//		if (isFolder) {
//			Directory dir = getFolder(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN);
//			Date date = dir.getFileInformation().getBasicInformation().getLastWriteTime().toDate();
//			dir.close();
//			return date;
//		} else {
			File file = getFile(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN);
			Date date = file.getFileInformation().getBasicInformation().getLastWriteTime().toDate();
			file.close();
			return date;
//		}
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	public String getShare() {
		return shareName;
	}

	public void setShare(String share) {
		this.shareName = share;
	}

	public boolean isForce() {
		return isForce;
	}

	public void setForce(boolean isForce) {
		this.isForce = isForce;
	}

	public boolean isListHiddenFiles() {
		return listHiddenFiles;
	}

	public void setListHiddenFiles(boolean listHiddenFiles) {
		this.listHiddenFiles = listHiddenFiles;
	}

	class FilesIterator implements Iterator<String> {

		private List<FileIdBothDirectoryInformation> files;
		private int i = 0;

		public FilesIterator(List<FileIdBothDirectoryInformation> list) {
			files = list;
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.size();
		}

		@Override
		public String next() {
			return files.get(i++).getFileName();
		}

		@Override
		public void remove() {
			deleteFile(files.get(i++).getFileName());
		}
	}

}
