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
package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Date;
import java.util.Map;

import javax.annotation.Nonnull;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.stream.Message;

/**
 * Interface to represent a basic filesystem, in which files can be
 * listed, read, deleted or moved to a folder.
 *
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
public interface IBasicFileSystem<F> extends HasPhysicalDestination{

	public void configure() throws ConfigurationException;
	public void open() throws FileSystemException;
	public void close() throws FileSystemException;

	public boolean isOpen();

	/**
	 * Lists all files in 'folder' or in the 'root' of the filesystem (when folder is null).
	 * Should list only 'files', no folders.
	 */
	public DirectoryStream<F> listFiles(String folder) throws FileSystemException;
	public int getNumberOfFilesInFolder(String folder) throws FileSystemException;
	/**
	 * Get a string representation of an identification of a file.
	 * Must pair up with the implementation of {@link #toFile(String)}.
	 * Can reflect name a file has in its folder, is not expected to be unique over folders.
	 */
	public String getName(F f);
	public String getParentFolder(F f) throws FileSystemException;
	/**
	 * Get a file 'F' representation of an identification of a file.
	 * Must pair up with the implementation of {@link #getName(Object)}.
	 */
	public F toFile(@Nonnull String filename) throws FileSystemException;
	/**
	 * Creates a reference to a file. If filename is not absolute, it will be created in 'defaultFolder'.
	 */
	public F toFile(String defaultFolder, @Nonnull String filename) throws FileSystemException;
	public boolean exists(F f) throws FileSystemException;

	public boolean folderExists(String folder) throws FileSystemException;
	public Message readFile(F f, String charset) throws FileSystemException, IOException;
	public void deleteFile(F f) throws FileSystemException;

	/**
	 * Moves the file to a another folder.
	 * Does not need to check for existence of the source or non-existence of the destination.
	 * Returns the moved file, or null if no file was moved or there is no reference to the moved file.<br/>
	 * If the reference to the moved file is unknown after the move, then:<br/>
	 *   if <code>resultantMustBeReturned</code> is set, then an Exception must be thrown, preferably before the file is moved;<br/>
	 *   if <code>resultantMustBeReturned</code> is not set, then a null result returned might also mean the file was moved successfully, but with unknown destination;<br/>
	 * @param resultantMustBeReturned TODO
	 */
	public F moveFile(F f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException;

	/**
	 * Copies the file to a another folder.
	 * Does not need to check for existence of the source or non-existence of the destination.
	 * Returns the copied file, or null if no file was copied or there is no reference to the copied file.
	 * If the reference to the copied file is unknown after the copy, then:<br/>
	 *   if <code>resultantMustBeReturned</code> is set, then an Exception must be thrown, preferably before the file is copied;<br/>
	 *   if <code>resultantMustBeReturned</code> is not set, then a null result returned might also mean the file was copied successfully, but with unknown destination;<br/>
	 * @param resultantMustBeReturned TODO
	 */
	public F copyFile(F f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException;

	public void createFolder(String folder) throws FileSystemException;
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException;


	public long getFileSize(F f) throws FileSystemException;
	public String getCanonicalName(F f) throws FileSystemException;
	public Date getModificationTime(F f) throws FileSystemException;

	public Map<String, Object> getAdditionalFileProperties(F f) throws FileSystemException;

}
