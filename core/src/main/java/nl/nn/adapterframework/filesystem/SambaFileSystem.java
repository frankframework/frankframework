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
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * 
 * 
 * @author alisihab
 *
 */
public class SambaFileSystem implements IFileSystem<SmbFile> {

	protected Logger log = LogUtil.getLogger(this);

	private String domain = null;
	private String username = null;
	private String password = null;
	private String authAlias = null;
	private String share = null;
	private boolean isForce;
	private boolean listHiddenFiles = true;

	private NtlmPasswordAuthentication auth = null;
	private SmbFile smbContext;

	@Override
	public void configure() throws ConfigurationException {
		if (getShare() == null)
			throw new ConfigurationException("server share endpoint is required");
		if (!getShare().startsWith("smb://"))
			throw new ConfigurationException("url must begin with [smb://]");

		//Setup credentials if applied, may be null.
		//NOTE: When using NtmlPasswordAuthentication without username it returns GUEST
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if (StringUtils.isNotEmpty(cf.getUsername())) {
			auth = new NtlmPasswordAuthentication(getDomain(), cf.getUsername(), cf.getPassword());
			log.debug("setting authentication to [" + auth.toString() + "]");
		}
		open();
	}

	@Override
	public void open() throws ConfigurationException {
		try {
			smbContext = new SmbFile(getShare(), auth);
			smbContext.connect();
		} catch (IOException e) {
			throw new ConfigurationException(e);
		}

	}

	@Override
	public void close() {
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
	public Iterator<SmbFile> listFiles() {
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
			e.printStackTrace();
		}
		return null;
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
				throw new FileSystemException("file not found");
			}
			if (f.isFile()) {
				f.delete();
			} else {
				throw new FileSystemException("trying to remove [" + f.getName()
						+ "] which is a directory instead of a file");
			}
		} catch (SmbException e) {
			throw new FileSystemException(e);
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
	public void createFolder(SmbFile f) throws FileSystemException {
		try {
			if (isForce) {
				f.mkdirs();
			} else {
				f.mkdir();
			}
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void removeFolder(SmbFile f) throws FileSystemException {
		try {
			if (exists(f)) {
				if (f.isDirectory()) {
					f.delete();
				} else {
					throw new FileSystemException("trying to remove file [" + f.getName()
							+ "] which is a file instead of a directory");
				}
			} else {
				throw new FileSystemException(
						"trying to remove file [" + f.getName() + "] which does not exist");
			}
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void renameTo(SmbFile f, String destination) throws FileSystemException {
		SmbFile dest;
		try {
			dest = new SmbFile(smbContext, destination);
			if (dest.exists()) {
				if (isForce)
					dest.delete();
				else {
					return;
				}
			}
			f.renameTo(dest);
		} catch (SmbException e) {
			throw new FileSystemException(e);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

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
	}

	@Override
	public long getFileSize(SmbFile f, boolean isFolder) throws FileSystemException {
		try {
			return f.length();
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String getName(SmbFile f) throws FileSystemException {
		return f.getName();
	}

	@Override
	public String getCanonicalName(SmbFile f, boolean isFolder) throws FileSystemException {
		return f.getCanonicalPath();
	}

	@Override
	public Date getModificationTime(SmbFile f, boolean isFolder) throws FileSystemException {
		return new Date(f.getLastModified());
	}

	@Override
	public void augmentDirectoryInfo(XmlBuilder dirInfo, SmbFile file) {
		dirInfo.addAttribute("name", file.getName());
	}

	public boolean isListHiddenFiles() {
		return listHiddenFiles;
	}

	public void setListHiddenFiles(boolean listHiddenFiles) {
		this.listHiddenFiles = listHiddenFiles;
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

	public NtlmPasswordAuthentication getAuth() {
		return auth;
	}

	public void setAuth(NtlmPasswordAuthentication auth) {
		this.auth = auth;
	}

	public String getShare() {
		return share;
	}

	public void setShare(String share) {
		this.share = share;
	}

	public void setForce(boolean force) {
		isForce = force;
	}

	public SmbFile getSmbContext() {
		return smbContext;
	}

	public void setSmbContext(SmbFile smbContext) {
		this.smbContext = smbContext;
	}

}
