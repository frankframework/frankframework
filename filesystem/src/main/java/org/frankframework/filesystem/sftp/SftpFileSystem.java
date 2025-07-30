/*
   Copyright 2023-2025 WeAreFrank!

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
package org.frankframework.filesystem.sftp;

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

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

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
import org.frankframework.util.LogUtil;

/**
 * Implementation of SFTP FileSystem
 *
 * @author Niels Meijer
 */
@DestinationType(Type.FILE_SYSTEM)
public class SftpFileSystem extends SftpSession implements IWritableFileSystem<SftpFileRef> {
	private final Logger log = LogUtil.getLogger(this);

	private String remoteDirectory = "";

	private ChannelSftp ftpClient;

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
		log.debug("checking if SFTP connection is open");
		try {
			if (ftpClient == null || ftpClient.getSession() == null) {
				return false;
			}
			ftpClient.getSession().sendKeepAliveMsg(); // Send a keep-alive packet to validate a working connection
		} catch (Exception ignored) {
			return false;
		}
		return ftpClient.isConnected() && isSessionStillWorking();
	}

	@Override
	public SftpFileRef toFile(@Nullable String filename) throws FileSystemException {
		return toFile(null, filename);
	}

	@Override
	public SftpFileRef toFile(@Nullable String folder, @Nullable String filename) throws FileSystemException {
		return new SftpFileRef(filename, folder);
	}

	@Override
	public int getNumberOfFilesInFolder(String folder) throws FileSystemException {
		try {
			List<LsEntry> files = listFolder(folder);
			return (int) files.stream().filter(f -> !f.getAttrs().isDir()).count();
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public DirectoryStream<SftpFileRef> list(String folder, TypeFilter filter) throws FileSystemException {
		try {
			return FileSystemUtils.getDirectoryStream(new SftpFilePathIterator(folder, listFolder(folder), filter));
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

	@Override
	public boolean isFolder(SftpFileRef sftpFileRef) {
		return sftpFileRef.isDirectory();
	}

	private SftpFileRef findFile(SftpFileRef file) throws SftpException {
		try {
			List<LsEntry> files = listFolder(file.getName());
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

	private List<LsEntry> listFolder(String folder) throws SftpException {
		String path = folder == null ? "*" : folder;
		return new ArrayList<>(ftpClient.ls(path));
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
			getFileAttributes(f);
			InputStream inputStream = ftpClient.get(f.getName());
			return new Message(inputStream, FileSystemUtils.getContext(this, f, charset));
		} catch (SftpException | IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void deleteFile(SftpFileRef f) throws FileSystemException {
		final String canonicalName = getCanonicalName(f);
		if (!exists(f)) {
			throw new FileNotFoundException("Cannot delete file [" + canonicalName + "] from SFTP filesystem because it does not exist");
		}
		try {
			ftpClient.rm(canonicalName);
		} catch (SftpException e) {
			throw new FileSystemException("Could not delete file [" + canonicalName + "]: " + e.getMessage());
		}
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		String pwd;
		try {
			pwd = ftpClient.pwd();
			try {
				if (folder.startsWith("/")) {
					ftpClient.cd(folder);
				} else {
					ftpClient.cd(pwd + "/" + folder); // Faster and more fail-safe method to ensure the target is a folder and not secretly a file
				}
				return true;
			} finally {
				ftpClient.cd(pwd);
			}
		} catch (SftpException e) {
			if (e.id == 2 || e.id == 4) { // 2 == File not found, 4 == Can't change directory
				return false;
			}
			throw new FileSystemException(e);
		}
	}

	public void changeDirectory(final String folder) throws FileSystemException {
		if (StringUtils.isBlank(folder)) {
			return;
		}
		try {
			ftpClient.cd(folder);
		} catch (SftpException e) {
			throw new FileSystemException("unable to change remote directory to [" + folder + "]", e);
		}
	}

	@Override
	public void createFolder(String folder) throws FileSystemException {
		if(folderExists(folder)) {
			throw new FolderAlreadyExistsException("Create directory for [" + folder + "] has failed. Directory already exists.");
		}

		try {
			String[] folders = folder.split("/");
			for(int i = 1; i < folders.length; i++) {
				folders[i] = folders[i - 1] + "/" + folders[i];
			}
			for(String f : folders) {
				if(!f.isEmpty() && !folderExists(f)) {
					log.debug("creating folder [{}]", f);
					ftpClient.mkdir(f);
				}
			}
		} catch (SftpException e) {
			throw new FileSystemException("Cannot create directory", e);
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
				ftpClient.rmdir(folder);
			}
		} catch (SftpException e) {
			if(e.id == 18) { // Directory not empty
				throw new FileSystemException("Cannot remove folder [" + folder + "]. Directory not empty.");
			}
			throw new FileSystemException(e);
		}
	}

	/**
	 * Recursively remove directory
	 */
	private void removeDirectoryContent(String folder) throws SftpException, FileSystemException {
		String pwd = ftpClient.pwd();
		List<LsEntry> files;
		if (StringUtils.isNotEmpty(folder) && folder.startsWith("/")) {
			files = listFolder(folder);
		} else {
			files = listFolder(pwd + "/" + folder);
		}
		for (LsEntry ftpFile : files) {
			String fileName = ftpFile.getFilename();
			if (".".equals(fileName) || "..".equals(fileName)) {
				continue;
			}
			if (ftpFile.getAttrs().isDir()) {
				String recursiveName = folder != null ? folder + "/" + ftpFile.getFilename() : ftpFile.getFilename();
				removeDirectoryContent(recursiveName);
			} else {
				SftpFileRef ftpFileRef = SftpFileRef.fromLsEntry(ftpFile, folder);
				log.debug("created SftpFileRef [{}]", ftpFileRef);
				deleteFile(ftpFileRef);
			}
		}
		ftpClient.cd(pwd);
		if (StringUtils.isNotEmpty(folder) && folder.startsWith("/")) {
			log.debug("removing folder [{}]", folder);
			ftpClient.rmdir(folder);
		} else {
			String folderToDelete;
			if ("/".equals(pwd)) { // Prevent double slashes
				folderToDelete = pwd + folder;
			} else {
				folderToDelete = pwd + "/" + folder;
			}
			log.debug("removing folder [{}]", folderToDelete);
			ftpClient.rmdir(folderToDelete);
		}
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
	public SftpFileRef moveFile(SftpFileRef f, String destinationFolder, boolean createFolder) throws FileSystemException {
		SftpFileRef destination = new SftpFileRef(getName(f), destinationFolder);
		if(exists(destination)) {
			throw new FileAlreadyExistsException("target already exists");
		} else if(createFolder && !folderExists(destinationFolder)) {
			createFolder(destinationFolder);
		}

		try {
			ftpClient.rename(getCanonicalName(f), destination.getName());
			return destination;
		} catch (SftpException e) {
			throw new FileSystemException("unable to move file", e);
		}
	}

	@Override
	public SftpFileRef copyFile(SftpFileRef f, String destinationFolder, boolean createFolder) throws FileSystemException {
		if(createFolder && !folderExists(destinationFolder)) {
			createFolder(destinationFolder);
		}

		SftpFileRef destination = new SftpFileRef(getName(f), destinationFolder);

		try (InputStream inputStream = ftpClient.get(f.getName()); SerializableFileReference ref = SerializableFileReference.of(inputStream) ) {
			ftpClient.put(ref.getInputStream(), destination.getName());
		} catch (Exception e) {
			throw new FileSystemException("unable to copy file", e);
		}
		return destination;
	}

	@Override
	public long getFileSize(SftpFileRef f) {
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
		String name = file.getFilename();
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
	public String getParentFolder(SftpFileRef file) {
		return file.getFolder();
	}

	@Override
	public String getCanonicalName(SftpFileRef f) {
		return f.getName(); // Should include folder structure if known
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
	@Nullable
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
	 * Path of the file or directory to start working.
	 * @ff.default Home folder of the sftp user
	 */
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	private class SftpFilePathIterator implements Iterator<SftpFileRef> {
		private final List<SftpFileRef> items;
		private int i = 0;

		SftpFilePathIterator(String folder, List<LsEntry> fileEntities, TypeFilter filter) {
			items = new ArrayList<>();
			for (LsEntry ftpFile : fileEntities) {
				if (ftpFile.getAttrs().isDir() && filter.includeFolders()) {
					SftpFileRef fileRef = SftpFileRef.fromLsEntry(ftpFile, folder);
					// Skip folders without name, like '.' and '..'
					if (StringUtils.isNotBlank(fileRef.getFilename())) {
						log.debug("adding directory SftpFileRef [{}] to the collection", fileRef);
						items.add(fileRef);
					}
				} else if (!ftpFile.getAttrs().isDir() && filter.includeFiles()) {
					SftpFileRef fileRef = SftpFileRef.fromLsEntry(ftpFile, folder);
					log.debug("adding file SftpFileRef [{}] to the collection", fileRef);
					items.add(fileRef);
				}
			}
		}

		@Override
		public boolean hasNext() {
			return items != null && i < items.size();
		}

		@Override
		public SftpFileRef next() {
			if(!hasNext()) {
				throw new NoSuchElementException();
			}

			return items.get(i++);
		}

		@Override
		public void remove() {
			SftpFileRef file = items.get(i++);
			try {
				deleteFile(file);
			} catch (FileSystemException e) {
				log.warn("unable to remove file [{}]", getCanonicalNameOrErrorMessage(file), e);
			}
		}
	}

}
