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
import java.util.Iterator;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * 
 * @author Daniël Meyer
 *
 */
public class FtpFileSystem implements IFileSystem<FTPFile> {

	protected Logger log = LogUtil.getLogger(this);
	
	private String username;
	private String password;
	private String host;
	private int port = 21;

	private String remoteDirectory = "";

	private FtpSession ftpSession = new FtpSession();

	@Override
	public void configure() throws ConfigurationException {
		ftpSession.setUsername(username);
		ftpSession.setPassword(password);
		ftpSession.setHost(host);
		ftpSession.setPort(port);
		ftpSession.configure();
	}

	@Override
	public void open() throws FileSystemException {
		try {
			ftpSession.openClient("");
		} catch (FtpConnectException e) {
			throw new FileSystemException("Cannot connect to the FTP server with domain ["+host+"]", e);
		}
	}

	@Override
	public void close() {
		ftpSession.closeClient();
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
			return new FTPFilePathIterator(ftpSession.ftpClient.listFiles(remoteDirectory));
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean exists(FTPFile f) throws FileSystemException {
		try {
			FTPFile[] files = ftpSession.ftpClient.listFiles();
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
				ftpSession.ftpClient.completePendingCommand();
			}
		};
		return fos;
	}

	@Override
	public OutputStream createFile(FTPFile f) throws FileSystemException, IOException {
		OutputStream outputStream = ftpSession.ftpClient.storeFileStream(f.getName());
		return completePendingCommand(outputStream);
	}

	@Override
	public OutputStream appendFile(FTPFile f) throws FileSystemException, IOException {
		OutputStream outputStream = ftpSession.ftpClient.appendFileStream(f.getName());
		return completePendingCommand(outputStream);
	}

	@Override
	public InputStream readFile(FTPFile f) throws FileSystemException, IOException {
		InputStream inputStream = ftpSession.ftpClient.retrieveFileStream(f.getName());
		ftpSession.ftpClient.completePendingCommand();
		return inputStream;
	}

	@Override
	public void deleteFile(FTPFile f) throws FileSystemException {
		try {
			ftpSession.ftpClient.deleteFile(f.getName());
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
			ftpSession.ftpClient.makeDirectory(f.getName());
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
			ftpSession.ftpClient.removeDirectory(f.getName());
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
			ftpSession.ftpClient.rename(f.getName(), destination);
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
			return ftpSession.ftpClient.listFiles(f.getName())[0].getTimestamp().getTime();
		} catch (IndexOutOfBoundsException oobe) {
			throw new FileSystemException("File could not be found", oobe);
		} catch (IOException e) {
			throw new FileSystemException("Could not retrieve file", e);
		}
	}

	@Override
	public void augmentFileInfo(XmlBuilder fileInfo, FTPFile f) {
		fileInfo.addAttribute("user", f.getUser());
		fileInfo.addAttribute("group", f.getGroup());
		fileInfo.addAttribute("type", f.getType());
		fileInfo.addAttribute("rawListing", f.getRawListing());
		fileInfo.addAttribute("link", f.getLink());
		fileInfo.addAttribute("hardLinkCount", f.getHardLinkCount());
	}

	public FtpSession getFtpSession() {
		return ftpSession;
	}
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
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