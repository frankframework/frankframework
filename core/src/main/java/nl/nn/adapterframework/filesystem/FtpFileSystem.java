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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;

/**
 * 
 * @author Daniël Meyer
 *
 */
public class FtpFileSystem extends FtpSession implements IWritableFileSystem<FTPFile> {

	private String remoteDirectory = "";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
	}

	@Override
	public void open() throws FileSystemException {
		try {
			openClient(remoteDirectory);
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
	public FTPFile toFile(String folder, String filename) throws FileSystemException {
		return toFile(folder+"/"+filename);
	}

	@Override
	public Iterator<FTPFile> listFiles(String folder) throws FileSystemException {
		try {
			return new FTPFilePathIterator(folder, ftpClient.listFiles(folder));
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean exists(FTPFile f) throws FileSystemException {
		try {
			FTPFile[] files = ftpClient.listFiles(f.getName());
			if(files.length > 0) {
				return true;
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
				if(ftpClient.getReplyCode() == FTPReply.CODE_150) {
					ftpClient.completePendingCommand();
				}
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
	public boolean folderExists(String folder) throws FileSystemException {
		boolean isDirectory = false;
		try {
			FTPFile[] files = ftpClient.listFiles(folder);
			if(files.length > 1) 
				isDirectory = true;
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
		
		return isDirectory;
	}

	@Override
	public void createFolder(String folder) throws FileSystemException {
		if(folderExists(folder)) {
			throw new FileSystemException("Create directory for [" + folder + "] has failed. Directory already exists.");
		}
		try {
			ftpClient.makeDirectory(folder);
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void removeFolder(String folder) throws FileSystemException {
		if(!folderExists(folder)) {
			throw new FileSystemException("Remove directory for [" + folder + "] has failed. Directory does not exist.");
		}
		try {
			ftpClient.removeDirectory(folder);
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public FTPFile renameFile(FTPFile f, String newName, boolean force) throws FileSystemException {
		if(exists(toFile(newName))) {
			throw new FileSystemException("Cannot rename file. Destination file already exists.");
		}
		try {
			ftpClient.rename(f.getName(), newName);
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
		return toFile(newName);
	}

	protected String getDestinationFilename(FTPFile f, String destinationFolder, boolean createFolder, String action) throws FileSystemException {
		if(!folderExists(destinationFolder)) {
			if(createFolder) {
				createFolder(destinationFolder);
			}
			throw new FileSystemException("Cannot "+action+" file. Destination folder ["+destinationFolder+"] does not exist.");
		}
		return destinationFolder+"/"+f.getName();
	}

	@Override
	public FTPFile moveFile(FTPFile f, String destinationFolder, boolean createFolder) throws FileSystemException {
		String destinationFilename = getDestinationFilename(f, destinationFolder, createFolder, "move");
		try {
			ftpClient.rename(f.getName(), destinationFilename);
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
		return toFile(destinationFilename);
	}
	
	@Override
	public FTPFile copyFile(FTPFile f, String destinationFolder, boolean createFolder) throws FileSystemException {
		throw new NotImplementedException("CopyFile not implemented for FtpFileSystem");
	}
	
	@Override
	public long getFileSize(FTPFile f) throws FileSystemException {
		return f.getSize();
	}

	@Override
	public String getName(FTPFile f) {
		return f.getName();
	}

	@Override
	public String getCanonicalName(FTPFile f) throws FileSystemException {
		return f.getName();
	}

	@Override
	public Date getModificationTime(FTPFile f) throws FileSystemException {
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


	@Override
	public String getPhysicalDestinationName() {
		return "remote directory ["+remoteDirectory+"]";
	}

	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	private class FTPFilePathIterator implements Iterator<FTPFile> {

		private List<FTPFile> files;
		private String prefix;
		private int i = 0;

		FTPFilePathIterator(String folder, FTPFile filesArr[]) {
			prefix = folder != null ? folder + "/" : "";
			files = new ArrayList<FTPFile>();
			for (FTPFile ftpFile : filesArr) {
				if(!ftpFile.isDirectory()) {
					ftpFile.setName(prefix + ftpFile.getName());
					files.add(ftpFile);
				}
			}
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.size();
		}

		@Override
		public FTPFile next() {
			return files.get(i++);
		}

		@Override
		public void remove() {
			try {
				deleteFile(files.get(i++));
			} catch (FileSystemException e) {
				log.warn(e);
			}
		}
	}
}