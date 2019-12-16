/*
   Copyright 2018 Nationale-Nederlanden

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
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

/**
 * 
 * @author alisihab
 *
 */
public class Samba1FileSystem implements IWritableFileSystem<SmbFile> {

	protected Logger log = LogUtil.getLogger(this);

	private String share = null;
	private String username = null;
	private String password = null;
	private String authAlias = null;
	private String domain = null;
	private boolean isForce;
	private boolean listHiddenFiles = false;

	private NtlmPasswordAuthentication auth = null;
	private SmbFile smbContext;

	@Override
	public void configure() throws ConfigurationException {
		if (getShare() == null)
			throw new ConfigurationException("server share endpoint is required");
		if (!getShare().startsWith("smb://"))
			throw new ConfigurationException("attribute share must begin with [smb://]");

		//Setup credentials if applied, may be null.
		//NOTE: When using NtmlPasswordAuthentication without username it returns GUEST
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if (StringUtils.isNotEmpty(cf.getUsername())) {
			auth = new NtlmPasswordAuthentication(getDomain(), cf.getUsername(), cf.getPassword());
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

	}

	@Override
	public void close() {
		// Automatically closes
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
	public Iterator<SmbFile> listFiles(String folder) throws FileSystemException {
		try {
			if (!isListHiddenFiles()) {
				SmbFileFilter filter = new SmbFileFilter() {

					@Override
					public boolean accept(SmbFile file) throws SmbException {
						return !file.isHidden();
					}
				};
				return new SmbFileIterator(smbContext.listFiles(filter));
			}
			return new SmbFileIterator(smbContext.listFiles());
		} catch (SmbException e) {
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
	public InputStream readFile(SmbFile f) throws IOException {
		SmbFileInputStream is = new SmbFileInputStream(f);
		return is;
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
	public void removeFolder(String folder) throws FileSystemException {
		try {
			if (folderExists(folder)) {
				toFile(folder).delete();
			} else {
				throw new FileSystemException("Remove directory for [" + folder + "] has failed. Directory does not exist.");
			}
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public SmbFile renameFile(SmbFile f, String newName, boolean force) throws FileSystemException {
		SmbFile dest;
		try {
			dest = new SmbFile(smbContext, newName);
			if (exists(dest)) {
				if (force)
					dest.delete();
				else {
					throw new FileSystemException("Cannot rename file. Destination file already exists.");
				}
			}
			f.renameTo(dest);
			return dest;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public SmbFile moveFile(SmbFile f, String destinationFolder, boolean createFolder) throws FileSystemException {
		SmbFile dest;
		try {
			dest = new SmbFile(smbContext, destinationFolder+"/"+f.getName());
			if (!exists(dest)) {
				throw new FileSystemException("Cannot move file. Destination folder ["+destinationFolder+"] does not exists.");
			}
			if (!isFolder(dest)) {
				throw new FileSystemException("Cannot move file. Destination folder ["+destinationFolder+"] is not a folder.");
			}
			f.renameTo(dest);
			return dest;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		return "domain ["+getDomain()+"] share ["+getShare()+"]";
	}


	private class SmbFileIterator implements Iterator<SmbFile> {

		private SmbFile files[];
		private int i = 0;

		public SmbFileIterator(SmbFile files[]) {
			this.files = files;
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
			try {
				deleteFile(files[i++]);
			} catch (FileSystemException e) {
				log.warn(e);
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
	public String getCanonicalName(SmbFile f) throws FileSystemException {
		return f.getCanonicalPath();
	}

	@Override
	public Date getModificationTime(SmbFile f) throws FileSystemException {
		return new Date(f.getLastModified());
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(SmbFile file) {
		return null;
	}

	public String getShare() {
		return share;
	}
	@IbisDoc({ "1", "the destination, aka smb://xxx/yyy share", "" })
	public void setShare(String share) {
		this.share = share;
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

	@IbisDoc({ "6", "when <code>true</code>, intermediate directories are created also", "false" })
	public void setForce(boolean force) {
		isForce = force;
	}

	public boolean isListHiddenFiles() {
		return listHiddenFiles;
	}
	@IbisDoc({ "7", "controls whether hidden files are seen or not", "false" })
	public void setListHiddenFiles(boolean listHiddenFiles) {
		this.listHiddenFiles = listHiddenFiles;
	}


}
