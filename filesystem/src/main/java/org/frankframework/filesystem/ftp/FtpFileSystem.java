/*
   Copyright 2019-2025 WeAreFrank!

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
package org.frankframework.filesystem.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.DestinationType;
import org.frankframework.core.DestinationType.Type;
import org.frankframework.filesystem.FileAlreadyExistsException;
import org.frankframework.filesystem.FileNotFoundException;
import org.frankframework.filesystem.FileSystemException;
import org.frankframework.filesystem.FileSystemUtils;
import org.frankframework.filesystem.FolderAlreadyExistsException;
import org.frankframework.filesystem.FolderNotFoundException;
import org.frankframework.filesystem.IWritableFileSystem;
import org.frankframework.filesystem.TypeFilter;
import org.frankframework.stream.Message;
import org.frankframework.stream.SerializableFileReference;

/**
 * Implementation of FTP and FTPs FileSystem
 *
 * @author Daniël Meyer
 * @author Niels Meijer
 */
@Log4j2
@DestinationType(Type.FILE_SYSTEM)
public class FtpFileSystem extends FtpSession implements IWritableFileSystem<FTPFileRef> {

	private String remoteDirectory = "";
	private FTPClient ftpClient;

	@Override
	public void open() throws FileSystemException {
		ftpClient = openClient(remoteDirectory);
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
	public FTPFileRef toFile(@Nullable String filename) throws FileSystemException {
		return toFile(null, filename);
	}

	@Override
	public FTPFileRef toFile(@Nullable String folder, @Nullable String filename) throws FileSystemException {
		return new FTPFileRef(filename, folder);
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
	public DirectoryStream<FTPFileRef> list(FTPFileRef folder, TypeFilter filter) throws FileSystemException {
		try {
			String folderName = getCanonicalName(folder);
			return FileSystemUtils.getDirectoryStream(new FTPFilePathIterator(folderName, ftpClient.listFiles(folderName), filter));
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean exists(FTPFileRef file) throws FileSystemException {
		return findFile(file) != null;
	}

	@Override
	public boolean isFolder(FTPFileRef ftpFileRef) {
		return ftpFileRef.isDirectory();
	}

	private @Nullable FTPFileRef findFile(FTPFileRef file) throws FileSystemException {
		try {
			FTPFile[] files = ftpClient.listFiles(file.getFolder(), f -> f.getName().equals(file.getFileName()));
			if(files != null && files.length > 0) {
				return FTPFileRef.fromFTPFile(files[0], file.getFolder());
			}
		} catch(IOException e) {
			throw new FileSystemException("unable to browse remote directory", e);
		}
		return null;
	}

	@Override
	public void createFile(FTPFileRef file, InputStream content) throws IOException {
		try (InputStream isToUse = content != null ? content : InputStream.nullInputStream()) {
			ftpClient.storeFile(file.getName(), isToUse);
		}
	}

	@Override
	public void appendFile(FTPFileRef file, InputStream content) throws IOException {
		try (InputStream isToUse = content != null ? content : InputStream.nullInputStream()) {
			ftpClient.appendFile(file.getName(), isToUse);
		}
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
		final String canonicalName = getCanonicalName(f);
		try {
			ftpClient.deleteFile(canonicalName);
		} catch (IOException e) {
			throw new FileSystemException("Could not delete file [" + canonicalName + "]: " + e.getMessage());
		}
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		String pwd;
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
		if (folderExists(folder)) {
			throw new FolderAlreadyExistsException("Create directory for [" + folder + "] has failed. Directory already exists.");
		}
		try {
			String[] folders = folder.split("/");
			for (int i = 1; i < folders.length; i++) {
				folders[i] = folders[i - 1] + "/" + folders[i];
			}
			for (String f : folders) {
				if (!f.isEmpty() && !folderExists(f)) {
					log.debug("creating folder [{}]", f);
					boolean created = ftpClient.makeDirectory(f);
					if (!created) {
						throw new FileSystemException("Cannot create folder: " + f);
					}
				}
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException {
		if(!folderExists(folder)) {
			throw new FolderNotFoundException("Remove directory for [" + folder + "] has failed. Directory does not exist.");
		}
		try {
			if(removeNonEmptyFolder) {
				removeDirectoryContent(folder);
			} else {
				log.debug("removing folder [{}]", folder);
				boolean removed = ftpClient.removeDirectory(folder);
				if (!removed) {
					throw new FileSystemException("Cannot remove folder: " + folder);
				}
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
				if (".".equals(fileName) || "..".equals(fileName)) {
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
			log.debug("removing folder [{}/{}]", pwd, folder);
			boolean removed = ftpClient.removeDirectory(pwd + "/" + folder);
			if (!removed) {
				throw new FileSystemException("Cannot remove folder");
			}
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
	public FTPFileRef moveFile(FTPFileRef f, String destinationFolder, boolean createFolder) throws FileSystemException {
		FTPFileRef destination = new FTPFileRef(getName(f), destinationFolder);
		try {
			if(exists(destination)) {
				throw new FileAlreadyExistsException("target already exists");
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
	public FTPFileRef copyFile(FTPFileRef f, String destinationFolder, boolean createFolder) throws FileSystemException {
		if(createFolder && !folderExists(destinationFolder)) {
			createFolder(destinationFolder);
		}

		FTPFileRef destination = new FTPFileRef(getName(f), destinationFolder);

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
		String name = file.getFileName();
		if(StringUtils.isNotEmpty(name)) {
			return name;
		}
		String folder = file.getFolder();
		if (folder != null) { // Folder: only take part before last slash
			int lastSlashPos = folder.lastIndexOf('/', folder.length() - 2);
			return folder.substring(lastSlashPos + 1);
		}

		return null;
	}

	@Override
	public String getParentFolder(FTPFileRef file) {
		return file.getFolder();
	}

	@Override
	public String getCanonicalName(FTPFileRef f) {
		return f.getName();  // Should include folder structure if known
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
				throw new FileNotFoundException("file does not exist on remote server");
			}
			file.updateFTPFile(fetch);
		}
		return file;
	}

	@Override
	@Nullable
	public Map<String, Object> getAdditionalFileProperties(FTPFileRef f) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("user", f.getUser());
		attributes.put("group", f.getGroup());
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
		private final List<FTPFileRef> files = new ArrayList<>();
		private int i = 0;

		FTPFilePathIterator(String folder, FTPFile[] filesArr, TypeFilter filter) {
			for (FTPFile ftpFile : filesArr) {
				FTPFileRef fileRef = FTPFileRef.fromFTPFile(ftpFile, folder);
				if (ftpFile.isDirectory() && filter.includeFolders()) {
					log.debug("adding directory FTPFileRef [{}] to the collection", fileRef);
					files.add(fileRef);
				} else if (ftpFile.isFile() && filter.includeFiles()) {
					log.debug("adding file FTPFileRef [{}] to the collection", fileRef);
					files.add(fileRef);
				}
			}
		}

		@Override
		public boolean hasNext() {
			return i < files.size();
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
				log.warn("unable to remove file [{}]: {}", ()-> getCanonicalNameOrErrorMessage(file), e::getMessage);
			}
		}
	}

}
