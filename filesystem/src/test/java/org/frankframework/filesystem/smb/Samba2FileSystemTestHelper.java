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
package org.frankframework.filesystem.smb;

import static com.hierynomus.msfscc.FileAttributes.FILE_ATTRIBUTE_DIRECTORY;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.filesystem.FileSystemException;
import org.frankframework.filesystem.FolderNotFoundException;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.LogUtil;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.Factory;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.auth.Authenticator;
import com.hierynomus.smbj.auth.NtlmAuthenticator;
import com.hierynomus.smbj.auth.SpnegoAuthenticator;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

/**
 *
 * @author alisihab
 *
 */
public class Samba2FileSystemTestHelper implements IFileSystemTestHelper {

	protected Logger log = LogUtil.getLogger(this);
	private SMBClient client = null;
	private Connection connection;
	private Session session;
	private DiskShare diskShare;
	private String shareName = null;
	private String userName = null;
	private String password = null;
	private String host = null;
	private Integer port = null;
	private String domain = null;

	public Samba2FileSystemTestHelper(String host, Integer port, String shareFolder, String userName, String password, String domain) {
		this.shareName = shareFolder;
		this.userName = userName;
		this.password = password;
		this.host = host;
		this.port = port;
		this.domain = domain;
	}

	@Override
	public void setUp() throws Exception {
		open();
		cleanFolder();
	}

	@Override
	public void tearDown() throws Exception {
		close();
	}

	private void open() throws Exception {
		AuthenticationContext auth = new AuthenticationContext(userName, password.toCharArray(), domain);
		List<Factory.Named<Authenticator>> authenticators = new ArrayList<>();
		authenticators.add(new NtlmAuthenticator.Factory());
		authenticators.add(new SpnegoAuthenticator.Factory());

		SmbConfig config = SmbConfig.builder()
				.withAuthenticators(authenticators)
				.build();
		client = new SMBClient(config);
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
	}

	@Override
	public void close() {
		CloseUtils.closeSilently(diskShare, session, connection, client);
		diskShare = null;
		session = null;
		connection = null;
		client = null;
	}

	private void cleanFolder() throws Exception {
		_deleteFolder(null);
	}

	@Override
	public boolean _fileExists(String folder, String filename) throws Exception {
		String path = folder != null ? folder + "/" + filename : filename;
		boolean exists = diskShare.fileExists(path);
		return exists;
	}

	@Override
	public boolean _folderExists(String folderName) throws Exception {
		boolean exists = diskShare.folderExists(folderName);
		return exists;
	}

	@Override
	public void _deleteFile(String folder, String filename) throws Exception {
		String path = folder != null ? folder + "/" + filename : filename;
		diskShare.rm(path);
	}

	@Override
	public String createFile(String folder, String filename, String contents) throws Exception {
		Set<AccessMask> accessMask = new HashSet<>(EnumSet.of(AccessMask.FILE_ADD_FILE));
		Set<SMB2CreateOptions> createOptions = new HashSet<>(EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_WRITE_THROUGH));

		String path = folder != null ? folder + "/" + filename : filename;
		final File file = diskShare.openFile(path, accessMask, null, SMB2ShareAccess.ALL,
				SMB2CreateDisposition.FILE_OVERWRITE_IF, createOptions);

		try (OutputStream out = file.getOutputStream()) {
			if(StringUtils.isNotEmpty(contents)) {
				out.write(contents.getBytes());
			}
		}

		file.close();
		return filename;
	}

	@Override
	public InputStream _readFile(String folder, String filename) throws Exception {
		String path = folder != null ? folder + "/" + filename : filename;
		final File file = getFile(path, AccessMask.GENERIC_READ, SMB2CreateDisposition.FILE_OPEN);
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
			if(NtStatus.valueOf(e.getStatusCode()) == NtStatus.STATUS_OBJECT_NAME_NOT_FOUND) {
				return false;
			}
			if(NtStatus.valueOf(e.getStatusCode()) == NtStatus.STATUS_DELETE_PENDING) {
				return false;
			}

			throw new FileSystemException(e);
		}
	}

	private String toFile(String filename) {
		return filename;
	}

	private boolean folderExists(String folder) throws FileSystemException {
		return isFolder(toFile(folder));
	}

	@Override
	public void _createFolder(String folderName) throws Exception {
		if (!folderExists(folderName)) {
			diskShare.mkdir(folderName);
		}
//		throw new FileSystemException("Create directory for [" + folderName + "] has failed. Directory already exists.");
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		if (folderName != null) {
			if (!folderExists(folderName)) {
				throw new FolderNotFoundException("Remove directory for [" + folderName + "] has failed. Directory does not exist.");
			} else {
				diskShare.rmdir(folderName, true);
			}
			return;
		}

		List<FileIdBothDirectoryInformation> list = diskShare.list("/");
		for(FileIdBothDirectoryInformation fi : list) {
			if(".".equals(fi.getFileName()) || "..".equals(fi.getFileName())) {
				continue;
			}
			if(!EnumWithValue.EnumUtils.isSet(fi.getFileAttributes(), FILE_ATTRIBUTE_DIRECTORY)) {
				diskShare.rm(fi.getFileName());
			} else {
				_deleteFolder(fi.getFileName());
			}
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
		log.debug("resolved name [{}] to [{}]", filename, file);
		return file;
	}
}
