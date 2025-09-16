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
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.DestinationType;
import org.frankframework.core.DestinationType.Type;
import org.frankframework.doc.Default;
import org.frankframework.stream.Message;
import org.frankframework.stream.PathMessage;
import org.frankframework.util.StreamUtil;

/**
 * {@link IWritableFileSystem FileSystem} representation of the local filesystem.
 *
 * @author Gerrit van Brakel
 *
 */
@Log4j2
@DestinationType(Type.FILE_SYSTEM)
public class LocalFileSystem extends AbstractFileSystem<Path> implements IWritableFileSystem<Path>, ISupportsCustomFileAttributes<Path> {
	public static final String ORIGINAL_LAST_MODIFIED_TIME_ATTRIBUTE = "originalLastModifiedTime";

	private @Getter boolean createRootFolder = false;

	private static final String FILE_DELIMITER = "/";

	/**
	 * Path to the folder that serves as the root of this virtual filesystem. All specifications of folders or files are relative to this root.
	 * When the root is left unspecified, absolute paths to files and folders can be used
	 */
	@Getter @Setter private String root;

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
	public Path toFile(@Nullable String filename) throws FileSystemException {
		return toFile(null, filename);
	}

	@Override
	public Path toFile(@Nullable String folder, @Nullable String filename) throws FileSystemException {
		if (filename==null) {
			filename="";
		}
		if (StringUtils.isNotEmpty(folder) && !(filename.contains(FILE_DELIMITER) || filename.contains("\\"))) {
			filename = folder + FILE_DELIMITER + filename;
		}
		if (StringUtils.isNotEmpty(getRoot())) {
			Path result = Paths.get(filename);
			if (result.isAbsolute()) {
				return result;
			}
			filename = getRoot()+ FILE_DELIMITER + filename;
		}
		if (StringUtils.isEmpty(filename)) {
			throw new FileSystemException("no filesystem-root, file or folder specified");
		}
		return Paths.get(filename);
	}

	@Override
	public DirectoryStream<Path> list(Path folder, TypeFilter filter) throws FileSystemException {
		if (folder == null) {
			folder = toFile(null);
		}
		if (!folderExists(folder)) {
			throw new FolderNotFoundException("Cannot list files in ["+folder+"], no such folder found");
		}
		DirectoryStream.Filter<Path> directoryStreamFilter = switch (filter) {
			case FILES_ONLY -> file -> !Files.isDirectory(file);
			case FOLDERS_ONLY -> Files::isDirectory;
			case FILES_AND_FOLDERS -> file -> true;
		};
		try {
			return Files.newDirectoryStream(folder, directoryStreamFilter);
		} catch (IOException e) {
			throw new FileSystemException("Cannot list files in ["+folder+"]", e);
		}
	}

	@Override
	public boolean exists(Path f) {
		return Files.exists(f);
	}

	@Override
	public void createFile(Path file, InputStream content) throws IOException {
		try (OutputStream out = Files.newOutputStream(file)) {
			StreamUtil.streamToStream(content, out);
		}
	}

	@Override
	public void createFile(Path file, InputStream contents, Map<String, String> customFileAttributes) throws FileSystemException, IOException {
		try {
			// Create the file first
			createFile(file, contents);

			// Then add the custom attributes
			addCustomFileAttributes(file, customFileAttributes);

		} catch (Exception e) {
			throw ExceptionUtils.asRuntimeException(e);
		}
	}

	private static void addCustomFileAttributes(@Nonnull Path file, @Nonnull Map<String, String> customFileAttributes) throws IOException {
		if (customFileAttributes.isEmpty()) {
			return;
		}
		if (!Files.getFileStore(file).supportsFileAttributeView(UserDefinedFileAttributeView.class)) {
			log.warn("Custom file attributes not supported by FileStore [{}]", Files.getFileStore(file));
			return;
		}

		// We need to restore the original file modified time, because we use it to append to the filename in some configurations of the DirectoryListener.
		FileTime lastModifiedTime = Files.getLastModifiedTime(file);
		UserDefinedFileAttributeView userDefinedAttributes = getUserDefinedAttributes(file);

		// Cannot do this with Java Stream operation because of the IOException thrown by write operation
		for (Map.Entry<String, String> entry : customFileAttributes.entrySet()) {
			userDefinedAttributes.write(entry.getKey(), Charset.defaultCharset().encode(entry.getValue()));
		}

		// Writing the attributes changes the lastModifiedTime, but FS listener functionality depends on that value.
		// So we store it in another attribute and try to restore the original value.
		if (!hasAttribute(userDefinedAttributes, ORIGINAL_LAST_MODIFIED_TIME_ATTRIBUTE)) {
			userDefinedAttributes.write(ORIGINAL_LAST_MODIFIED_TIME_ATTRIBUTE, Charset.defaultCharset().encode(String.valueOf(lastModifiedTime.toMillis())));
		}
		try {
			Files.setLastModifiedTime(file, lastModifiedTime);
		} catch (IOException e) {
			// If this fails, ignore. Can be a Docker related permission-issue that shouldn't affect functionality.
			log.trace(() -> "Cannot set last modified time for [%s]".formatted(file), e);
		}
	}

	private static UserDefinedFileAttributeView getUserDefinedAttributes(@Nonnull Path file) {
		return Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
	}

	private static boolean hasAttribute(@Nonnull UserDefinedFileAttributeView userDefinedAttributes, String attributeName) throws IOException {
		return userDefinedAttributes.list().stream().anyMatch(attributeName::equals);
	}

	private static @Nonnull String readAttribute(@Nonnull UserDefinedFileAttributeView userDefinedAttributes, @Nonnull String attributeName) throws IOException {
		ByteBuffer bfr = ByteBuffer.allocate(userDefinedAttributes.size(attributeName));
		userDefinedAttributes.read(attributeName, bfr);
		return new String(bfr.array());
	}

	@Override
	public void appendFile(Path f, InputStream content) throws FileSystemException, IOException {
		try (OutputStream out = Files.newOutputStream(f, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
			StreamUtil.streamToStream(content, out);
		}
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

	public boolean folderExists(Path folder) {
		return isFolder(folder);
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
		return renameFile(source, destination, Map.of());
	}

	@Override
	public Path renameFile(Path source, Path destination, Map<String, String> customFileAttributes) throws FileSystemException {
		try {
			Path result = Files.move(source, destination);
			addCustomFileAttributes(result, customFileAttributes);
			return result;
		} catch (FileNotFoundException e) {
			throw new org.frankframework.filesystem.FileNotFoundException(e);
		} catch (IOException e) {
			throw new FileSystemException("Cannot rename file ["+ source +"] to ["+ destination +"]", e);
		}
	}

	@Override
	public Path moveFile(Path f, String destinationFolder, boolean createFolder) throws FileSystemException {
		return moveFile(f, destinationFolder, createFolder, Map.of());
	}

	@Override
	public Path moveFile(Path f, String destinationFolder, boolean createFolder, Map<String, String> customFileAttributes) throws FileSystemException {
		if(createFolder && !folderExists(destinationFolder)) {
			try {
				Files.createDirectories(toFile(destinationFolder));
			} catch (IOException e) {
				throw new FileSystemException("Cannot create folder ["+ destinationFolder +"]", e);
			}
		}

		try {
			Path target = toFile(destinationFolder, getName(f));
			if (exists(target)) {
				throw new FileSystemException("Cannot move file ["+ f +"] to ["+ destinationFolder+"], source and destination are the same");
			}

			Path result = Files.move(f, target);
			addCustomFileAttributes(result, customFileAttributes);
			return result;
		} catch (FileNotFoundException e) {
			throw new org.frankframework.filesystem.FileNotFoundException(e);
		} catch (IOException e) {
			throw new FileSystemException("Cannot move file ["+ f +"] to ["+ destinationFolder+"]", e);
		}
	}

	@Override
	public Path copyFile(Path f, String destinationFolder, boolean createFolder) throws FileSystemException {
		return copyFile(f, destinationFolder, createFolder, Map.of());
	}

	@Override
	public Path copyFile(Path f, String destinationFolder, boolean createFolder, Map<String, String> customFileAttributes) throws FileSystemException {
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
			addCustomFileAttributes(target, customFileAttributes);
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
			// If the original LastModifiedTime is stored in a user-defined attribute, return that value
			if (Files.getFileStore(f).supportsFileAttributeView(UserDefinedFileAttributeView.class)) {
				UserDefinedFileAttributeView userDefinedAttributes = getUserDefinedAttributes(f);
				if (hasAttribute(userDefinedAttributes, ORIGINAL_LAST_MODIFIED_TIME_ATTRIBUTE)) {
					String lastModifiedTime = readAttribute(userDefinedAttributes, ORIGINAL_LAST_MODIFIED_TIME_ATTRIBUTE);
					return new Date(Long.parseLong(lastModifiedTime));
				}
			}

			return new Date(Files.getLastModifiedTime(f).toMillis());
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	@Nullable
	public Map<String, Object> getAdditionalFileProperties(Path file) throws FileSystemException {
		try {
			if (!Files.exists(file)) return null;
			UserDefinedFileAttributeView userDefinedAttributes = getUserDefinedAttributes(file);
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
			return new String(buffer.array());
		} else {
			return attributeValue.toString();
		}
	}

	@Nullable
	@Override
	public String getCustomFileAttribute(@Nonnull Path file, @Nonnull String name) throws FileSystemException {
		Map<String, Object> additionalFileProperties = getAdditionalFileProperties(file);
		if (additionalFileProperties == null) return null;
		return Objects.toString(additionalFileProperties.get(name), null);
	}

	@Override
	public String getPhysicalDestinationName() {
		return "root ["+(getRoot()==null?"":getRoot())+"]";
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
