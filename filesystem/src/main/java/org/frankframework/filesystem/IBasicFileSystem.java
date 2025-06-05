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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Date;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.stream.Message;

/**
 * Interface to represent a basic filesystem, in which files can be
 * listed, read, deleted or moved to a folder.
 * <p>
 * For Basic filesystems, filenames could be more or less globally unique IDs. In such a case:
 * - moving or copying a file to a folder might change its name
 * - moving or copying a file to a folder will never 'overwrite' a file already present in the folder
 * and therefore for basic filesystems:
 * - toFile(folder, filename) may return always the same result as toFile(filename)
 * - rollover and overwrite protection is not supported
 *
 * @author Gerrit van Brakel
 *
 * @param <F> Representation of file and folder.
 */
public interface IBasicFileSystem<F> extends HasPhysicalDestination, AutoCloseable {

	void configure() throws ConfigurationException;
	void open() throws FileSystemException;
	@Override
	void close() throws FileSystemException;

	boolean isOpen();

	/**
	 * Lists files, directories or both, from a 'folder' or in the 'root' of the filesystem (when folder is null).
	 * Only lists the objects as defined by the type filter.
	 */
	DirectoryStream<F> list(String folder, TypeFilter filter) throws FileSystemException;
	int getNumberOfFilesInFolder(String folder) throws FileSystemException;
	/**
	 * Get a string representation of an identification of a file.
	 * Must pair up with the implementation of {@link #toFile(String)}.
	 * Can reflect name a file has in its folder, is not expected to be unique over folders.
	 */
	String getName(F f);
	String getParentFolder(F f) throws FileSystemException;
	/**
	 * Get a file 'F' representation of an identification of a file.
	 * Must pair up with the implementation of {@link #getName(Object)}.
	 */
	F toFile(@Nullable String filename) throws FileSystemException;
	/**
	 * Creates a reference to a file. If filename is not absolute, it will be created in 'defaultFolder'.
	 */
	F toFile(@Nullable String defaultFolder, @Nullable String filename) throws FileSystemException;
	boolean exists(F f) throws FileSystemException;
	boolean isFolder(F f) throws FileSystemException;

	boolean folderExists(String folder) throws FileSystemException;
	Message readFile(F f, String charset) throws FileSystemException, IOException;
	void deleteFile(F f) throws FileSystemException;

	/**
	 * Moves the file to another folder.
	 * Does not need to check for existence of the source or non-existence of the destination.
	 * Returns the moved file, or null if no file was moved or there is no reference to the moved file.<br/>
	 */
	F moveFile(F f, String destinationFolder, boolean createFolder) throws FileSystemException;

	/**
	 * Copies the file to another folder.
	 * Does not need to check for existence of the source or non-existence of the destination.
	 * Returns the copied file, or null if no file was copied or there is no reference to the copied file.
	 */
	F copyFile(F f, String destinationFolder, boolean createFolder) throws FileSystemException;

	void createFolder(String folder) throws FileSystemException;
	void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException;


	long getFileSize(F f) throws FileSystemException;
	String getCanonicalName(F f) throws FileSystemException;
	Date getModificationTime(F f) throws FileSystemException;
	@Nullable
	Map<String, Object> getAdditionalFileProperties(F f) throws FileSystemException;

	/**
	 * Safe method to get a string representing the canonical name of a file, or an error message
	 * if the canonical name could not be established.
	 *
	 * Because this method is not guaranteed to return the actual canonical name, it should be used
	 * only for error messages and logging.
	 *
	 * @param f File for which to try to get canonical name
	 * @return Either the canonical name of the file, or an error.
	 */
	default String getCanonicalNameOrErrorMessage(F f) {
		try {
			return getCanonicalName(f);
		} catch (FileSystemException e) {
			return "<Cannot get true canonical name, error: [" + e.getMessage() + "]>";
		}
	}
}
