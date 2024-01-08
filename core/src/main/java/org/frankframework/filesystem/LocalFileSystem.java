/*
   Copyright 2019-2022 WeAreFrank!

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
package org.frankframework.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.stream.Message;
import org.frankframework.stream.PathMessage;

/**
 * {@link IWritableFileSystem FileSystem} representation of the local filesystem.
 *
 * @author Gerrit van Brakel
 *
 */
public class LocalFileSystem extends FileSystemBase<Path> implements IWritableFileSystem<Path> {
	private final @Getter(onMethod = @__(@Override)) String domain = "LocalFilesystem";

	private String root;

	@Override
	public void configure() throws ConfigurationException {
		// No Action is required
	}

	@Override
	public Path toFile(String filename) {
		return toFile(null, filename);
	}

	@Override
	public Path toFile(String folder, String filename) {
		if (filename==null) {
			filename="";
		}
		if (StringUtils.isNotEmpty(folder) && !(filename.contains("/") || filename.contains("\\"))) {
			filename = folder +"/" + filename;
		}
		if (StringUtils.isNotEmpty(getRoot())) {
			Path result = Paths.get(filename);
			if (result.isAbsolute()) {
				return result;
			}
			filename = getRoot()+"/"+ filename;
		}
		return Paths.get(filename);
	}

	@Override
	public DirectoryStream<Path> listFiles(String folder) throws FileSystemException {
		final Path dir = toFile(folder);

		DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<>() {
			@Override
			public boolean accept(Path file) throws IOException {
				return !Files.isDirectory(file);
			}
		};
		try {
			return Files.newDirectoryStream(dir, filter);
		} catch (IOException e) {
			throw new FileSystemException("Cannot list files in ["+folder+"]", e);
		}
	}

	@Override
	public boolean exists(Path f) {
		return Files.exists(f);
	}

	@Override
	public OutputStream createFile(Path f) throws IOException {
		return Files.newOutputStream(f);
	}

	@Override
	public OutputStream appendFile(Path f) throws IOException {
		return Files.newOutputStream(f, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}

	@Override
	public Message readFile(Path f, String charset) throws FileSystemException {
		return new PathMessage(f, FileSystemUtils.getContext(this, f, charset));
	}

	@Override
	public void deleteFile(Path f) throws FileSystemException {
		try {
			Files.delete(f);
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	public boolean isFolder(Path f) {
		return Files.isDirectory(f);
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		return isFolder(toFile(folder));
	}

	@Override
	public void createFolder(String folder) throws FileSystemException {
		if (!folderExists(folder)) {
			try {
				Files.createDirectories(toFile(folder));
			} catch (IOException e) {
				throw new FileSystemException("Cannot create folder ["+ folder +"]", e);
			}
		} else {
			throw new FileSystemException("Create directory for [" + folder + "] has failed. Directory already exists.");
		}
	}

	@Override
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException {
		if (folderExists(folder)) {
			try {
				if(removeNonEmptyFolder) {
					try (Stream<Path> directoryStream = Files.walk(toFile(folder))) {
						directoryStream.sorted(Comparator.reverseOrder())
						.map(Path::toFile)
						.forEach(File::delete);
					}
				} else {
					Files.delete(toFile(folder));
				}
			} catch (IOException e) {
				throw new FileSystemException("Cannot remove folder ["+ folder +"]",e);
			}
		}else {
			throw new FileSystemException("Remove directory for [" + folder + "] has failed. Directory does not exist.");
		}
	}

	@Override
	public Path renameFile(Path source, Path destination) throws FileSystemException {
		try {
			return Files.move(source, destination);
		} catch (IOException e) {
			throw new FileSystemException("Cannot rename file ["+ source.toString() +"] to ["+ destination.toString() +"]", e);
		}
	}

	@Override
	public Path moveFile(Path f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		if(createFolder && !folderExists(destinationFolder)) {
			try {
				Files.createDirectories(toFile(destinationFolder));
			} catch (IOException e) {
				throw new FileSystemException("Cannot create folder ["+ destinationFolder +"]", e);
			}
		}
		try {
			return Files.move(f, toFile(destinationFolder, getName(f)));
		} catch (IOException e) {
			throw new FileSystemException("Cannot move file ["+ f.toString() +"] to ["+ destinationFolder+"]", e);
		}
	}
	@Override
	public Path copyFile(Path f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		if(createFolder && !folderExists(destinationFolder)) {
			try {
				Files.createDirectories(toFile(destinationFolder));
			} catch (IOException e) {
				throw new FileSystemException("Cannot create folder ["+ destinationFolder +"]", e);
			}
		}
		Path target = toFile(destinationFolder, getName(f));
		try {
			Files.copy(f, target);
		} catch (IOException e) {
			throw new FileSystemException("Cannot copy file ["+ f.toString()+"] to ["+ destinationFolder+"]", e);
		}
		return target;
	}

	@Override
	public long getFileSize(Path f) throws FileSystemException {
		try {
			return Files.size(f);
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String getName(Path f) {
		if(f.getFileName() != null) {
			return f.getFileName().toString();
		}
		return null;
	}

	@Override
	public String getParentFolder(Path f) throws FileSystemException {
		return getCanonicalName(f.getParent());
	}

	@Override
	public String getCanonicalName(Path f) throws FileSystemException {
		try {
			return f.toFile().getCanonicalPath();
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public Date getModificationTime(Path f) throws FileSystemException {
		try {
			return new Date(Files.readAttributes(f, BasicFileAttributes.class).lastModifiedTime().toMillis());
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(Path f) {
		return null;
	}

	@Override
	public String getPhysicalDestinationName() {
		return "root ["+(getRoot()==null?"":getRoot())+"]";
	}

	/**
	 * Path to the folder that serves as the root of this virtual filesystem. All specifications of folders or files are relative to this root.
	 * When the root is left unspecified, absolute paths to files and folders can be used
	 */
	public void setRoot(String root) {
		this.root = root;
	}
	public String getRoot() {
		return root;
	}

}
