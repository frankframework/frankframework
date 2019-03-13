/*
   Copyright 2019 Nationale-Nederlanden

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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;
import nl.nn.adapterframework.util.LogUtil;

/**
 * 
 * @author Daniël Meyer
 *
 */
public class FtpFileSystem extends FtpSession implements IFileSystem<FTPFile> {

	protected Logger log = LogUtil.getLogger(this);

	private String remoteDirectory = "";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
	}

	@Override
	public void open() throws FileSystemException {
		try {
			openClient("");
		} catch (FtpConnectException e) {
			throw new FileSystemException("Cannot connect to the FTP server with domain ["+getHost()+"]", e);
		}
	}

	@Override
	public void close() {
		closeClient();
	}

	@Override
	public FTPFile toFile(String filename) throws FileSystemException {
		FTPFile ftpFile = new FTPFile();
		ftpFile.setName(filename);

		return ftpFile;
	}

	@Override
	public Iterator<FTPFile> listFiles() throws FileSystemException {
		try {
			return new FTPFilePathIterator(ftpClient.listFiles(remoteDirectory));
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean exists(FTPFile f) throws FileSystemException {
		try {
			FTPFile[] files = ftpClient.listFiles();
			for (FTPFile o : files) {
				if (o.isDirectory()) {
					if ((f.getName().endsWith("/") ? o.getName() + "/" : o.getName()).equals(f.getName())) {
						return true;
					}
				} else if (o.getName().equals(f.getName())) {
					return true;
				}
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
		return false;
	}

	private FilterOutputStream completePendingCommand(OutputStream os) {
		FilterOutputStream fos = new FilterOutputStream(os) {
			@Override
			public void close() throws IOException {
				super.close();
				ftpClient.completePendingCommand();
			}
		};
		return fos;
	}

	@Override
	public OutputStream createFile(FTPFile f) throws FileSystemException, IOException {
		OutputStream outputStream = ftpClient.storeFileStream(f.getName());
		return completePendingCommand(outputStream);
	}

	@Override
	public OutputStream appendFile(FTPFile f) throws FileSystemException, IOException {
		OutputStream outputStream = ftpClient.appendFileStream(f.getName());
		return completePendingCommand(outputStream);
	}

	@Override
	public InputStream readFile(FTPFile f) throws FileSystemException, IOException {
		InputStream inputStream = ftpClient.retrieveFileStream(f.getName());
		ftpClient.completePendingCommand();
		return inputStream;
	}

	@Override
	public void deleteFile(FTPFile f) throws FileSystemException {
		try {
			ftpClient.deleteFile(f.getName());
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean isFolder(FTPFile f) throws FileSystemException {
		return f.isDirectory();
	}

	@Override
	public void createFolder(FTPFile f) throws FileSystemException {
		if(exists(f)) {
			throw new FileSystemException("Create directory for [" + f.getName() + "] has failed. Directory already exists.");
		}
		try {
			ftpClient.makeDirectory(f.getName());
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void removeFolder(FTPFile f) throws FileSystemException {
		if(!exists(f)) {
			throw new FileSystemException("Remove directory for [" + f.getName() + "] has failed. Directory does not exist.");
		}
		try {
			ftpClient.removeDirectory(f.getName());
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void renameTo(FTPFile f, String destination) throws FileSystemException {
		if(exists(toFile(destination))) {
			throw new FileSystemException("Cannot rename file. Destination file already exists.");
		}
		try {
			ftpClient.rename(f.getName(), destination);
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public long getFileSize(FTPFile f, boolean isFolder) throws FileSystemException {
		return f.getSize();
	}

	@Override
	public String getName(FTPFile f) throws FileSystemException {
		return f.getName();
	}

	@Override
	public String getCanonicalName(FTPFile f, boolean isFolder) throws FileSystemException {
		return f.getName();
	}

	@Override
	public Date getModificationTime(FTPFile f, boolean isFolder) throws FileSystemException {
		try {
			return ftpClient.listFiles(f.getName())[0].getTimestamp().getTime();
		} catch (IndexOutOfBoundsException oobe) {
			throw new FileSystemException("File could not be found", oobe);
		} catch (IOException e) {
			throw new FileSystemException("Could not retrieve file", e);
		}
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(FTPFile f) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("user", f.getUser());
		attributes.put("group", f.getGroup());
		attributes.put("type", f.getType());
		attributes.put("rawListing", f.getRawListing());
		attributes.put("link", f.getLink());
		attributes.put("hardLinkCount", f.getHardLinkCount());
		return attributes;
	}

	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	private class FTPFilePathIterator implements Iterator<FTPFile> {

		private FTPFile files[];
		private int i = 0;

		FTPFilePathIterator(FTPFile files[]) {
			this.files = files;
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.length;
		}

		@Override
		public FTPFile next() {
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
}