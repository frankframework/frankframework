/*
   Copyright 2019-2024 WeAreFrank!

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.doc.Default;
import org.frankframework.stream.Message;
import org.frankframework.stream.PathMessage;

/**
 * {@link IWritableFileSystem FileSystem} representation of the local filesystem.
 *
 * @author Gerrit van Brakel
 *
 */
public class LocalFileSystem extends FileSystemBase<Path> implements IWritableFileSystem<Path>, ISupportsCustomFileAttributes<Path> {
	private final @Getter String domain = "LocalFilesystem";

	private @Getter boolean createRootFolder = false;

	private String root;

	@Override
	public void configure() throws ConfigurationException {
		// No Action is required
	}

	@Override
	public void open() throws FileSystemException {
		if (createRootFolder && root != null && !Files.exists(Paths.get(root))) {
			createFolder(root);
		}

		super.open();
	}

	@Override
	public Path toFile(@Nullable String filename) {
		return toFile(null, filename);
	}

	@Override
	public Path toFile(@Nullable String folder, @Nullable String filename) {
		if (filename==null) {
			filename="";
		}
		if (StringUtils.isNotEmpty(folder) && !(filename.contains("/") || filename.contains("\\"))) {
			filename = folder + "/" + filename;
		}
		if (StringUtils.isNotEmpty(getRoot())) {
			Path result = Paths.get(filename);
			if (result.isAbsolute()) {
				return result;
			}
			filename = getRoot()+ "/" + filename;
		}
		return Paths.get(filename);
	}

	@Override
	public DirectoryStream<Path> list(String folder, TypeFilter filter) throws FileSystemException {
		if (!folderExists(folder)) {
			throw new FolderNotFoundException("Cannot list files in ["+folder+"], no such folder found");
		}
		final Path dir = toFile(folder);

		DirectoryStream.Filter<Path> directoryStreamFilter = switch (filter) {
			case FILES_ONLY -> file -> !Files.isDirectory(file);
			case FOLDERS_ONLY -> Files::isDirectory;
			case FILES_AND_FOLDERS -> file -> true;
		};
		try {
			return Files.newDirectoryStream(dir, directoryStreamFilter);
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
	public void createFile(Path file, InputStream contents, Map<String, String> customFileAttributes) throws FileSystemException, IOException {
		try {
			// Create the file first
			createFile(file, contents);

			// Then add the custom attributes
			UserDefinedFileAttributeView userDefinedAttributes = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);

			// Stream can't handle the possible IOException
			for (Map.Entry<String, String> entry : customFileAttributes.entrySet()) {
				userDefinedAttributes.write(entry.getKey(), Charset.defaultCharset().encode(entry.getValue()));
			}

		} catch (Exception e) {
			throw ExceptionUtils.asRuntimeException(e);
		}
	}

	@Override
	public OutputStream appendFile(Path f) throws IOException {
		return Files.newOutputStream(f, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}

	@Override
	public Message readFile(Path f, String charset) throws FileSystemException {
		if (!Files.exists(f)) {
			throw new org.frankframework.filesystem.FileNotFoundException("Cannot find file ["+f+"].");
		}
		return new PathMessage(f, FileSystemUtils.getContext(this, f, charset));
	}

	@Override
	public void deleteFile(Path f) throws FileSystemException {
		try {
			Files.delete(f);
		} catch (FileNotFoundException e) {
			throw new org.frankframework.filesystem.FileNotFoundException("Cannot find file [" + f + "] to delete", e);
		} catch (IOException e) {
			throw new FileSystemException("Could not delete file [" + getCanonicalNameOrErrorMessage(f) + "]: " + e.getMessage());
		}
	}

	@Override
	public boolean isFolder(Path f) {
		return Files.isDirectory(f);
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		return isFolder(toFile(folder));
	}

	@Override
	public void createFolder(String folder) throws FileSystemException {
		if (folderExists(folder)) {
			throw new FolderAlreadyExistsException("Create folder for [" + folder + "] has failed. Directory already exists.");
		}
		try {
			Files.createDirectories(toFile(folder));
		} catch (IOException e) {
			throw new FileSystemException("Cannot create folder ["+ folder +"]", e);
		}
	}

	@Override
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException {
		if (!folderExists(folder)) {
			throw new FolderNotFoundException("Remove directory for [" + folder + "] has failed. Directory does not exist.");
		}
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
	}

	@Override
	public Path renameFile(Path source, Path destination) throws FileSystemException {
		try {
			return Files.move(source, destination);
		} catch (FileNotFoundException e) {
			throw new org.frankframework.filesystem.FileNotFoundException(e);
		} catch (IOException e) {
			throw new FileSystemException("Cannot rename file ["+ source +"] to ["+ destination +"]", e);
		}
	}

	@Override
	public Path moveFile(Path f, String destinationFolder, boolean createFolder) throws FileSystemException {
		if(createFolder && !folderExists(destinationFolder)) {
			try {
				Files.createDirectories(toFile(destinationFolder));
			} catch (IOException e) {
				throw new FileSystemException("Cannot create folder ["+ destinationFolder +"]", e);
			}
		}
		try {
			return Files.move(f, toFile(destinationFolder, getName(f)));
		} catch (FileNotFoundException e) {
			throw new org.frankframework.filesystem.FileNotFoundException(e);
		} catch (IOException e) {
			throw new FileSystemException("Cannot move file ["+ f +"] to ["+ destinationFolder+"]", e);
		}
	}
	@Override
	public Path copyFile(Path f, String destinationFolder, boolean createFolder) throws FileSystemException {
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
		} catch (FileNotFoundException e) {
			throw new org.frankframework.filesystem.FileNotFoundException(e);
		} catch (IOException e) {
			throw new FileSystemException("Cannot copy file ["+ f +"] to ["+ destinationFolder+"]", e);
		}
		return target;
	}

	@Override
	public long getFileSize(Path f) throws FileSystemException {
		try {
			return Files.size(f);
		} catch (FileNotFoundException e) {
			throw new org.frankframework.filesystem.FileNotFoundException(e);
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
	@Nullable
	public Map<String, Object> getAdditionalFileProperties(Path file) throws FileSystemException {
		try {
			if (!Files.exists(file)) return null;
			UserDefinedFileAttributeView userDefinedAttributes = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
			List<String> attributeNames = userDefinedAttributes.list();
			if (attributeNames == null || attributeNames.isEmpty()) return null;
			String attrSpec = "user:" + String.join(",", attributeNames);
			Map<String, Object> result = new LinkedHashMap<>();
			Files.readAttributes(file, attrSpec)
					.forEach((name, value) -> result.put(name, readAttributeValue(value)));
			return result;
		} catch (UnsupportedOperationException e) {
			return null;
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	private String readAttributeValue(Object attributeValue) {
		if (attributeValue instanceof byte[] bytes) {
			return new String(bytes);
		} else if (attributeValue instanceof ByteBuffer buffer) {
			return new String((buffer).array());
		} else {
			return attributeValue.toString();
		}
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

	/**
	 * Whether the LocalFileSystem tries to create the root folder if it doesn't exist yet.
	 * @param createRootFolder
	 */
	@Default("false")
	public void setCreateRootFolder(boolean createRootFolder) {
		this.createRootFolder = createRootFolder;
	}
}
