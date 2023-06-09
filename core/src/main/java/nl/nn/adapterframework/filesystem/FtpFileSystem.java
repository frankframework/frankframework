/*
   Copyright 2019-2023 WeAreFrank!

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
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.ftp.FTPFileRef;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.SerializableFileReference;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Implementation of FTP and FTPs FileSystem
 * 
 * @author Daniël Meyer
 * @author Niels Meijer
 */
public class FtpFileSystem extends FtpSession implements IWritableFileSystem<FTPFileRef> {
	private final Logger log = LogUtil.getLogger(this);

	private final @Getter(onMethod = @__(@Override)) String domain = "FTP";
	private String remoteDirectory = "";

	private FTPClient ftpClient;

	@Override
	public void open() throws FileSystemException {
		try {
			ftpClient = openClient(remoteDirectory);
		} catch (FtpConnectException e) {
			throw new FileSystemException("Cannot connect to the FTP server with domain ["+getHost()+"]", e);
		}
	}

	@Override
	public void close() {
		close(ftpClient);
		ftpClient = null;
	}


	@Override
	public boolean isOpen() {
		return ftpClient != null && ftpClient.isConnected();
	}


	@Override
	public FTPFileRef toFile(String filename) throws FileSystemException {
		return toFile(null, filename);
	}

	@Override
	public FTPFileRef toFile(String folder, String filename) throws FileSystemException {
		FTPFileRef ftpFile = new FTPFileRef();
		ftpFile.setName(filename);
		ftpFile.setFolder(folder);
		return ftpFile;
	}

	@Override
	public int getNumberOfFilesInFolder(String folder) throws FileSystemException {
		try {
			FTPFile[] files = ftpClient.listFiles(folder, FTPFile::isFile);
			return files == null? 0 : files.length;
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public DirectoryStream<FTPFileRef> listFiles(String folder) throws FileSystemException {
		try {
			return FileSystemUtils.getDirectoryStream(new FTPFilePathIterator(folder, ftpClient.listFiles(folder)));
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean exists(FTPFileRef file) throws FileSystemException {
		return findFile(file) != null;
	}

	private @Nullable FTPFileRef findFile(FTPFileRef file) throws FileSystemException {
		try {
			FTPFile[] files = ftpClient.listFiles(file.getFolder(), f -> f.getName().equals(file.getFileName()));
			if(files != null && files.length > 0) {
				return FTPFileRef.fromFTPFile(files[0]);
			}
		} catch(IOException e) {
			throw new FileSystemException("unable to browse remote directory", e);
		}
		return null;
	}

	private FilterOutputStream completePendingCommand(OutputStream os) {
		return new FilterOutputStream(os) {
			@Override
			public void close() throws IOException {
				super.close();
				if(ftpClient.getReplyCode() == FTPReply.FILE_STATUS_OK) {
					ftpClient.completePendingCommand();
				}
			}
		};
	}

	@Override
	public OutputStream createFile(FTPFileRef f) throws FileSystemException, IOException {
		OutputStream outputStream = ftpClient.storeFileStream(f.getName());
		return completePendingCommand(outputStream);
	}

	@Override
	public OutputStream appendFile(FTPFileRef f) throws FileSystemException, IOException {
		OutputStream outputStream = ftpClient.appendFileStream(f.getName());
		return completePendingCommand(outputStream);
	}

	@Override
	public Message readFile(FTPFileRef f, String charset) throws FileSystemException, IOException {
		updateFileAttributes(f);
		InputStream inputStream = ftpClient.retrieveFileStream(f.getName());
		ftpClient.completePendingCommand();
		return new Message(inputStream, FileSystemUtils.getContext(this, f, charset));
	}

	@Override
	public void deleteFile(FTPFileRef f) throws FileSystemException {
		try {
			ftpClient.deleteFile(getCanonicalName(f));
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		String pwd = null;
		try {
			pwd = ftpClient.printWorkingDirectory();
			try {
				return ftpClient.changeWorkingDirectory(pwd + "/" + folder);
			} finally {
				ftpClient.changeWorkingDirectory(pwd);
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
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
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException {
		if(!folderExists(folder)) {
			throw new FileSystemException("Remove directory for [" + folder + "] has failed. Directory does not exist.");
		}
		try {
			if(removeNonEmptyFolder) {
				removeDirectoryContent(folder);
			} else {
				ftpClient.removeDirectory(folder);
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	/**
	 * Recursively remove directory
	 * @param folder
	 * @throws IOException
	 * @throws FileSystemException
	 */
	private void removeDirectoryContent(String folder) throws IOException, FileSystemException {
		String pwd = ftpClient.printWorkingDirectory();
		if(ftpClient.changeWorkingDirectory(pwd+"/"+folder)) {
			FTPFile[] files = ftpClient.listFiles();
			for (FTPFile ftpFile : files) {
				String fileName=ftpFile.getName();
				if (fileName.equals(".") || fileName.equals("..")) {
					continue;
				}
				if(ftpFile.isDirectory()) {
					removeDirectoryContent(fileName);
				} else {
					FTPFileRef ftpFileRef = FTPFileRef.fromFTPFile(ftpFile);
					deleteFile(ftpFileRef);
				}
			}
			ftpClient.changeWorkingDirectory(pwd);
			ftpClient.removeDirectory(pwd+"/"+folder);
		}
	}

	@Override
	public FTPFileRef renameFile(FTPFileRef source, FTPFileRef destination) throws FileSystemException {
		try {
			ftpClient.rename(getCanonicalName(source), getCanonicalName(destination));
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
		return destination;
	}

	@Override
	public FTPFileRef moveFile(FTPFileRef f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		FTPFileRef destination = new FTPFileRef(getName(f));
		destination.setFolder(destinationFolder);
		try {
			if(exists(destination)) {
				throw new FileSystemException("target already exists");
			}
			if(ftpClient.rename(getCanonicalName(f), destination.getName())) {
				return destination;
			}
			throw new FileSystemException("unable to move file");
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public FTPFileRef copyFile(FTPFileRef f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		if(createFolder && !folderExists(destinationFolder)) {
			createFolder(destinationFolder);
		}

		FTPFileRef destination = new FTPFileRef(getName(f));
		destination.setFolder(destinationFolder);

		try (InputStream inputStream = ftpClient.retrieveFileStream(f.getName()); SerializableFileReference ref = SerializableFileReference.of(inputStream) ) {
			ftpClient.completePendingCommand();
			if(ftpClient.storeFile(destination.getName(), ref.getInputStream())) {
				return destination;
			}

			throw new FileSystemException("unable to copy file");
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public long getFileSize(FTPFileRef f) throws FileSystemException {
		updateFileAttributes(f);
		return f.getSize();
	}

	@Override
	public String getName(FTPFileRef file) {
		if(file == null) {
			return null;
		}
		return file.getFileName();
	}

	@Override
	public String getParentFolder(FTPFileRef file) throws FileSystemException {
		return file.getFolder();
	}

	@Override
	public String getCanonicalName(FTPFileRef f) {
		return f.getName(); //Should include folder structure if known
	}

	@Override
	public Date getModificationTime(FTPFileRef file) throws FileSystemException {
		updateFileAttributes(file);
		return file.getTimestamp().getTime();
	}

	private FTPFile updateFileAttributes(FTPFileRef file) throws FileSystemException {
		if(file.getTimestamp() == null) {
			FTPFile fetch = findFile(file);
			if(fetch == null) {
				throw new FileSystemException("file does not exist on remote server");
			}
			file.updateFTPFile(fetch);
		}
		return file;
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(FTPFileRef f) {
		Map<String, Object> attributes = new HashMap<>();
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

	/**
	 * pathname of the file or directory to list.
	 * @ff.default Home folder of the ftp user
	 */
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	private class FTPFilePathIterator implements Iterator<FTPFileRef> {

		private List<FTPFileRef> files;
		private int i = 0;

		FTPFilePathIterator(String folder, FTPFile[] filesArr) {
			files = new ArrayList<>();
			for (FTPFile ftpFile : filesArr) {
				if(ftpFile.isFile()) {
					FTPFileRef fileRef = FTPFileRef.fromFTPFile(ftpFile);
					fileRef.setFolder(folder);
					files.add(fileRef);
				}
			}
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.size();
		}

		@Override
		public FTPFileRef next() {
			if(!hasNext()) {
				throw new NoSuchElementException();
			}

			return files.get(i++);
		}

		@Override
		public void remove() {
			FTPFileRef file = files.get(i++);
			try {
				deleteFile(file);
			} catch (FileSystemException e) {
				log.warn("unable to remove file ["+getCanonicalName(file)+"]", e);
			}
		}
	}

}