/*
   Copyright 2018 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import jakarta.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.filesystem.AbstractFileSystem;
import org.frankframework.filesystem.FileAlreadyExistsException;
import org.frankframework.filesystem.FileNotFoundException;
import org.frankframework.filesystem.FileSystemException;
import org.frankframework.filesystem.FileSystemUtils;
import org.frankframework.filesystem.FolderAlreadyExistsException;
import org.frankframework.filesystem.FolderNotFoundException;
import org.frankframework.filesystem.IWritableFileSystem;
import org.frankframework.filesystem.TypeFilter;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;

/**
 * Uses the (old) SMB 1 protocol.
 * <br/>
 * Only supports NTLM authentication.
 */
@Log4j2
public class Samba1FileSystem extends AbstractFileSystem<SmbFile> implements IWritableFileSystem<SmbFile> {

	private @Getter String share = null;
	private @Getter String username = null;
	private @Getter String password = null;
	private @Getter String authAlias = null;
	private @Getter String authenticationDomain = null;
	private @Getter boolean isForce;
	private @Getter boolean listHiddenFiles = false;

	private NtlmPasswordAuthentication auth = null;
	private SmbFile smbContext;

	@Override
	public void configure() throws ConfigurationException {
		if (getShare() == null)
			throw new ConfigurationException("server share endpoint is required");
		if (!getShare().startsWith("smb://"))
			throw new ConfigurationException("attribute share must begin with [smb://]");
		if(!getShare().endsWith("/"))
			setShare(getShare()+"/");

		// Setup credentials if applied, may be null.
		// NOTE: When using NtmlPasswordAuthentication without username it returns GUEST
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if (StringUtils.isNotEmpty(cf.getUsername())) {
			auth = new NtlmPasswordAuthentication(getAuthenticationDomain(), cf.getUsername(), cf.getPassword());
			log.debug("setting authentication to [{}]", auth);
		}
	}

	@Override
	public void open() throws FileSystemException {
		try {
			smbContext = new SmbFile(getShare(), auth);
			smbContext.connect();
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
		super.open();
	}

	@Override
	public SmbFile toFile(@Nullable String filename) throws FileSystemException {
		if (filename == null) {
			return null;
		}
		try {
			return new SmbFile(smbContext, filename);
		} catch (IOException e) {
			throw new FileSystemException("unable to get SMB file [" + filename + "]", e);
		}
	}

	@Override
	public SmbFile toFile(@Nullable String folder, @Nullable String filename) throws FileSystemException {
		return toFile(folder+"/"+filename);
	}

	@Override
	public DirectoryStream<SmbFile> list(SmbFile folder, TypeFilter filter) throws FileSystemException {
		try {
			return FileSystemUtils.getDirectoryStream(new SmbFileIterator(folder != null ? folder.getURL().getFile() : null, filter));
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public DirectoryStream<SmbFile> list(String folder, TypeFilter filter) throws FileSystemException {
		SmbFile actualFolder = toFile(folder);
		return list(actualFolder, filter);
	}

	@Override
	public boolean exists(SmbFile f) throws FileSystemException {
		try {
			return f.exists();
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void createFile(SmbFile file, InputStream content) throws IOException {
		try (OutputStream out = new SmbFileOutputStream(file)) {
			if (content != null) {
				content.transferTo(out);
			}
		}
	}

	@Override
	public void appendFile(SmbFile file, InputStream content) throws IOException {
		try (OutputStream out = new SmbFileOutputStream(file, true)) {
			if (content != null) {
				content.transferTo(out);
			}
		}
	}

	@Override
	public Message readFile(SmbFile f, String charset) throws IOException, FileSystemException {
		return new Message(new SmbFileInputStream(f), FileSystemUtils.getContext(this, f, charset));
	}

	@Override
	public void deleteFile(SmbFile f) throws FileSystemException {
		try {
			if (!f.exists()) {
				throw new FileNotFoundException("File ["+f.getName()+"] not found");
			}
			if (f.isFile()) {
				f.delete();
			} else {
				throw new FileSystemException(
						"Trying to remove [" + f.getName() + "] which is a directory instead of a file");
			}
		} catch (SmbException e) {
			throw new FileSystemException("Could not delete file [" + getCanonicalNameOrErrorMessage(f) + "]: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean isFolder(SmbFile f) throws FileSystemException {
		try {
			return f.isDirectory();
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		return isFolder(toFile(folder));
	}

	@Override
	public void createFolder(String folder) throws FileSystemException {
		try {
			if(folderExists(folder)) {
				throw new FolderAlreadyExistsException("Create directory for [" + folder + "] has failed. Directory already exists.");
			}
			if (isForce) {
				toFile(folder).mkdirs();
			} else {
				toFile(folder).mkdir();
			}
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException {
		String normalized = FilenameUtils.normalizeNoEndSeparator(folder, true) + "/";
		try {
			if (!folderExists(normalized)) {
				throw new FolderNotFoundException("Cannot remove folder [" + normalized + "]. Directory does not exist.");
			}
			if(!removeNonEmptyFolder && list(folder, TypeFilter.FILES_ONLY).iterator().hasNext()) {
				throw new FileSystemException("Cannot remove folder [" + folder + "]. Directory not empty.");
			}

			toFile(normalized).delete();
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public SmbFile renameFile(SmbFile source, SmbFile destination) throws FileSystemException {
		if (!exists(source)) {
			throw new FileNotFoundException("Cannot find file [" + getName(source) + "], cannot rename.");
		}
		if (exists(destination)) {
			throw new FileAlreadyExistsException("Cannot rename [" + getName(source) + "], destination [" + getName(destination) + "] already exists");
		}
		try {
			source.renameTo(destination);
			return destination;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public SmbFile moveFile(SmbFile f, String destinationFolder, boolean createFolder) throws FileSystemException {
		if (!exists(f)) {
			throw new FileNotFoundException("Cannot find file [" + getName(f) + "], cannot move.");
		}
		if (!folderExists(destinationFolder)) {
			if(createFolder) {
				createFolder(destinationFolder);
			} else {
				throw new FolderNotFoundException("destination does not exist");
			}
		}

		SmbFile dest = toFile(destinationFolder, f.getName());

		try {
			if (f.equals(dest)) {
				throw new FileSystemException("Cannot move file [" + getName(f) + "] to itself");
			}

			f.renameTo(dest);
			return dest;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public SmbFile copyFile(SmbFile f, String destinationFolder, boolean createFolder) throws FileSystemException {
		if (!exists(f)) {
			throw new FileNotFoundException("Cannot find file [" + getName(f) + "], cannot copy.");
		}
		if (!folderExists(destinationFolder)) {
			if(createFolder) {
				createFolder(destinationFolder);
			} else {
				throw new FolderNotFoundException("destination does not exist");
			}
		}

		SmbFile dest = toFile(destinationFolder, f.getName());
		try {
			f.copyTo(dest);
			return dest;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		return "domain ["+getAuthenticationDomain()+"] share ["+getShare()+"]";
	}


	private class SmbFileIterator implements Iterator<SmbFile> {

		private final SmbFile[] files;
		private int i = 0;

		public SmbFileIterator(String folder, TypeFilter typeFilter) throws IOException {
			SmbFileFilter filter = switch (typeFilter) {
				case FILES_ONLY -> file -> (!isListHiddenFiles() && !file.isHidden()) && file.isFile();
				case FOLDERS_ONLY -> file -> (!isListHiddenFiles() && !file.isHidden()) && file.isDirectory();
				case FILES_AND_FOLDERS -> file -> (!isListHiddenFiles() && !file.isHidden());
			};
			SmbFile f = smbContext;
			if (StringUtils.isNotBlank(folder)) {
				String normalizedFolder = FilenameUtils.normalizeNoEndSeparator(folder, true) + "/";
				f = new SmbFile(smbContext, normalizedFolder);
			}
			files = f.listFiles(filter);
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.length;
		}

		@Override
		public SmbFile next() {
			if (i >= files.length) {
				throw new NoSuchElementException();
			}
			return files[i++];
		}

		@Override
		public void remove() {
			SmbFile file = files[i++];
			try {
				deleteFile(file);
			} catch (FileSystemException e) {
				log.warn("unable to delete file [{}]", getCanonicalNameOrErrorMessage(file), e);
			}
		}
	}

	@Override
	public long getFileSize(SmbFile f) throws FileSystemException {
		try {
			return f.length();
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String getName(SmbFile f) {
		if(f.getName().endsWith("/")) {
			return StringUtils.chop(f.getName());
		}
		return f.getName();
	}

	@Override
	public String getParentFolder(SmbFile f) {
		return f.getParent();
	}

	@Override
	public String getCanonicalName(SmbFile f) {
		return f.getCanonicalPath();
	}

	@Override
	public Date getModificationTime(SmbFile f) {
		return new Date(f.getLastModified());
	}

	@Override
	@Nullable
	public Map<String, Object> getAdditionalFileProperties(SmbFile file) {
		return null;
	}

	/** The destination, aka smb://xxx/yyy share */
	public void setShare(String share) {
		this.share = share;
	}

	/** The SMB share username */
	public void setUsername(String username) {
		this.username = username;
	}

	/** The SMB share password */
	public void setPassword(String password) {
		this.password = password;
	}

	/** Alias used to obtain credentials for the SMB share */
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	/** logon/authentication domain, in case the user account is bound to a domain such as Active Directory. */
	public void setDomainName(String domain) {
		this.authenticationDomain = domain;
	}

	/**
	 * when <code>true</code>, intermediate directories are created also
	 * @ff.default false
	 */
	public void setForce(boolean force) {
		isForce = force;
	}

	/**
	 * controls whether hidden files are seen or not
	 * @ff.default false
	 */
	public void setListHiddenFiles(boolean listHiddenFiles) {
		this.listHiddenFiles = listHiddenFiles;
	}
}
