/*
   Copyright 2023 WeAreFrank!

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
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import lombok.Getter;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.SftpFileRef;
import nl.nn.adapterframework.ftp.SftpSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.SerializableFileReference;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Implementation of SFTP FileSystem
 * 
 * @author Niels Meijer
 */
public class SftpFileSystem extends SftpSession implements IWritableFileSystem<SftpFileRef> {
	private final Logger log = LogUtil.getLogger(this);

	private final @Getter(onMethod = @__(@Override)) String domain = "FTP";
	private String remoteDirectory = "";

	private ChannelSftp ftpClient;

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
	public SftpFileRef toFile(String filename) throws FileSystemException {
		return toFile(null, filename);
	}

	@Override
	public SftpFileRef toFile(String folder, String filename) throws FileSystemException {
		SftpFileRef ftpFile = new SftpFileRef();
		ftpFile.setName(filename);
		ftpFile.setFolder(folder);
		return ftpFile;
	}

	@Override
	public int getNumberOfFilesInFolder(String folder) throws FileSystemException {
		try {
			Vector<LsEntry> files = ftpClient.ls(folder);
			return (int) files.stream().filter(f -> !f.getAttrs().isDir()).count();
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public DirectoryStream<SftpFileRef> listFiles(String folder) throws FileSystemException {
		try {
			return FileSystemUtils.getDirectoryStream(new SftpFilePathIterator(folder, ftpClient.ls( (folder == null) ? "*" : folder )));
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean exists(SftpFileRef file) throws FileSystemException {
		try {
			return findFile(file) != null;
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
	}

	private SftpFileRef findFile(SftpFileRef file) throws SftpException {
		try {
			Vector<LsEntry> files = ftpClient.ls(file.getName());
			if(!files.isEmpty()) {
				return SftpFileRef.fromLsEntry(files.get(0));
			}
		} catch (SftpException e) {
			if(e.id != 2) { // ID 2 == File Not Found
				throw e;
			}
		}
		return null;
	}

	@Override
	public OutputStream createFile(SftpFileRef f) throws FileSystemException, IOException {
		try {
			return ftpClient.put(f.getName());
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public OutputStream appendFile(SftpFileRef f) throws FileSystemException {
		try {
			return ftpClient.put(f.getName(), ChannelSftp.APPEND);
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public Message readFile(SftpFileRef f, String charset) throws FileSystemException {
		try {
			InputStream inputStream = ftpClient.get(f.getName());
			return new Message(inputStream, FileSystemUtils.getContext(this, f, charset));
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void deleteFile(SftpFileRef f) throws FileSystemException {
		try {
			ftpClient.rm(getCanonicalName(f));
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		String pwd = null;
		try {
			pwd = ftpClient.pwd();
			try {
				ftpClient.cd(pwd + "/" + folder); //Faster and more fail-safe method to ensure the target is a folder and not secretly a file
				return true;
			} finally {
				ftpClient.cd(pwd);
			}
		} catch (SftpException e) {
			if(e.id == 2 || e.id == 4) { // 2 == File not found, 4 == Can't change directory
				return false;
			}
			throw new FileSystemException(e);
		}
	}

	@Override
	public void createFolder(String folder) throws FileSystemException {
		if(folderExists(folder)) {
			throw new FileSystemException("Create directory for [" + folder + "] has failed. Directory already exists.");
		}
		try {
			ftpClient.mkdir(folder);
		} catch (SftpException e) {
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
				ftpClient.rmdir(folder);
			}
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
	}

	/**
	 * Recursively remove directory
	 * @param folder
	 * @throws FileSystemException
	 */
	private void removeDirectoryContent(String folder) throws SftpException, FileSystemException {
		String pwd = ftpClient.pwd();
		Vector<LsEntry> files = ftpClient.ls(pwd+"/"+folder);
		for (LsEntry ftpFile : files) {
			String fileName = ftpFile.getFilename();
			if (fileName.equals(".") || fileName.equals("..")) {
				continue;
			}
			if(ftpFile.getAttrs().isDir()) {
				String recursiveName = (folder != null) ? folder + "/" + ftpFile.getFilename() : ftpFile.getFilename();
				removeDirectoryContent(recursiveName);
			} else {
				SftpFileRef ftpFileRef = SftpFileRef.fromLsEntry(ftpFile);
				ftpFileRef.setFolder(folder);
				deleteFile(ftpFileRef);
			}
		}
		ftpClient.cd(pwd);
		ftpClient.rmdir(pwd+"/"+folder);
	}

	@Override
	public SftpFileRef renameFile(SftpFileRef source, SftpFileRef destination) throws FileSystemException {
		try {
			ftpClient.rename(getCanonicalName(source), getCanonicalName(destination));
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
		return destination;
	}

	@Override
	public SftpFileRef moveFile(SftpFileRef f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		SftpFileRef destination = new SftpFileRef(getName(f));
		destination.setFolder(destinationFolder);
		if(exists(destination)) {
			throw new FileSystemException("target already exists");
		}

		try {
			ftpClient.rename(getCanonicalName(f), destination.getName());
			return destination;
		} catch (SftpException e) {
			throw new FileSystemException("unable to move file", e);
		}
	}

	@Override
	public SftpFileRef copyFile(SftpFileRef f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		if(createFolder && !folderExists(destinationFolder)) {
			createFolder(destinationFolder);
		}

		SftpFileRef destination = new SftpFileRef(getName(f));
		destination.setFolder(destinationFolder);

		try (InputStream inputStream = ftpClient.get(f.getName()); SerializableFileReference ref = SerializableFileReference.of(inputStream) ) {
			ftpClient.put(ref.getInputStream(), destination.getName());
		} catch (Exception e) {
			throw new FileSystemException("unable to copy file", e);
		}
		return destination;
	}

	@Override
	public long getFileSize(SftpFileRef f) throws FileSystemException {
		getFileAttributes(f);
		return f.getSize();
	}

	private SftpATTRS getFileAttributes(SftpFileRef f) {
		if(f.getAttrs() == null) {
			try {
				f.setAttrs(ftpClient.stat(f.getName()));
			} catch (SftpException e) {
				log.warn("unable to fetch file attributes for [{}]", f.getName(), e);
			}
		}
		return f.getAttrs();
	}

	@Override
	public String getName(SftpFileRef file) {
		if(file == null) {
			return null;
		}
		return file.getFilename();
	}

	@Override
	public String getParentFolder(SftpFileRef file) throws FileSystemException {
		return file.getFolder();
	}

	@Override
	public String getCanonicalName(SftpFileRef f) {
		return f.getName(); //Should include folder structure if known
	}

	@Override
	public Date getModificationTime(SftpFileRef f) throws FileSystemException {
		SftpATTRS attrs = getFileAttributes(f);
		if(attrs != null) {
			return new Date(attrs.getMTime());
		}
		throw new FileSystemException("unable to get modification time");
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(SftpFileRef f) {
		Map<String, Object> attributes = new HashMap<>();
		SftpATTRS attrs = getFileAttributes(f);
		if(attrs != null) {
			attributes.put("user", attrs.getUId());
			attributes.put("group", attrs.getGId());
			attributes.put("rawListing", attrs.toString());
			attributes.put("link", attrs.isLink());
		}
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

	private class SftpFilePathIterator implements Iterator<SftpFileRef> {

		private List<SftpFileRef> files;
		private int i = 0;

		SftpFilePathIterator(String folder, Vector<LsEntry> fileEnties) {
			files = new ArrayList<>();
			for (LsEntry ftpFile : fileEnties) {
				if(!ftpFile.getAttrs().isDir()) {
					SftpFileRef fileRef = SftpFileRef.fromLsEntry(ftpFile);
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
		public SftpFileRef next() {
			if(!hasNext()) {
				throw new NoSuchElementException();
			}

			return files.get(i++);
		}

		@Override
		public void remove() {
			SftpFileRef file = files.get(i++);
			try {
				deleteFile(file);
			} catch (FileSystemException e) {
				log.warn("unable to remove file ["+getCanonicalName(file)+"]", e);
			}
		}
	}

}