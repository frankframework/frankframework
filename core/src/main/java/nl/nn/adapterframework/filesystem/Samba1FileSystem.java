/*
   Copyright 2018 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;

/**
 *
 * Uses the SMB 1 protocol
 *
 */
public class Samba1FileSystem extends FileSystemBase<SmbFile> implements IWritableFileSystem<SmbFile> {
	private final @Getter(onMethod = @__(@Override)) String domain = "SMB";

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

		//Setup credentials if applied, may be null.
		//NOTE: When using NtmlPasswordAuthentication without username it returns GUEST
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if (StringUtils.isNotEmpty(cf.getUsername())) {
			auth = new NtlmPasswordAuthentication(getAuthenticationDomain(), cf.getUsername(), cf.getPassword());
			log.debug("setting authentication to [" + auth.toString() + "]");
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
	public SmbFile toFile(String filename) throws FileSystemException {
		try {
			return new SmbFile(smbContext, filename);
		} catch (IOException e) {
			throw new FileSystemException("unable to get SMB file [" + filename + "]", e);
		}
	}

	@Override
	public SmbFile toFile(String folder, String filename) throws FileSystemException {
		return toFile(folder+"/"+filename);
	}

	@Override
	public DirectoryStream<SmbFile> listFiles(String folder) throws FileSystemException {
		try {
			return FileSystemUtils.getDirectoryStream(new SmbFileIterator(folder));
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
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
	public OutputStream createFile(SmbFile f) throws FileSystemException {
		try {
			return new SmbFileOutputStream(f);
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public OutputStream appendFile(SmbFile f) throws FileSystemException {
		try {
			return new SmbFileOutputStream(f, true);
		} catch (Exception e) {
			throw new FileSystemException(e);
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
				throw new FileSystemException("File ["+f.getName()+"] not found");
			}
			if (f.isFile()) {
				f.delete();
			} else {
				throw new FileSystemException(
						"Trying to remove [" + f.getName() + "] which is a directory instead of a file");
			}
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

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
				throw new FileSystemException("Create directory for [" + folder + "] has failed. Directory already exists.");
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
			if (folderExists(normalized)) {
				toFile(normalized).delete();
			} else {
				throw new FileSystemException("Remove directory for [" + normalized + "] has failed. Directory does not exist.");
			}
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public SmbFile renameFile(SmbFile source, SmbFile destination) throws FileSystemException {
		try {
			source.renameTo(destination);
			return destination;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public SmbFile moveFile(SmbFile f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		SmbFile dest = toFile(destinationFolder, f.getName());
		try {
			f.renameTo(dest);
			return dest;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public SmbFile copyFile(SmbFile f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		if (!folderExists(destinationFolder)) {
			if(createFolder) {
				createFolder(destinationFolder);
			} else {
				throw new FileSystemException("destination does not exist");
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

		private SmbFile[] files;
		private int i = 0;

		public SmbFileIterator(String folder) throws IOException {
			SmbFileFilter filter = new SmbFileFilter() {

				@Override
				public boolean accept(SmbFile file) throws SmbException {
					return (!isListHiddenFiles() && !file.isHidden()) && file.isFile();
				}
			};
			SmbFile f = smbContext;
			if(StringUtils.isNotBlank(folder)) {
				String normalizedFolder = FilenameUtils.normalizeNoEndSeparator(folder, true)+"/";
				f = new SmbFile(smbContext, normalizedFolder);
			}
			this.files = f.listFiles(filter);
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.length;
		}

		@Override
		public SmbFile next() {
			return files[i++];
		}

		@Override
		public void remove() {
			SmbFile file = files[i++];
			try {
				deleteFile(file);
			} catch (FileSystemException e) {
				log.warn("unable to delete file ["+getCanonicalName(file)+"]", e);
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
	public Map<String, Object> getAdditionalFileProperties(SmbFile file) {
		return null;
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
