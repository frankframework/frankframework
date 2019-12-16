/*
   Copyright 2019 Integration Partners

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
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;

/**
 * Interface to represent a basic filesystem, in which files can be 
 * listed, read, deleted or moved to a folder.
 * 
 * 
 * @author Gerrit van Brakel
 *
 * @param <F> File representation
 */
public interface IBasicFileSystem<F> extends HasPhysicalDestination{

	public void configure() throws ConfigurationException;
	public void open() throws FileSystemException;
	public void close() throws FileSystemException;

	
	/**
	 * Lists all files in 'folder' or in the 'root' of the filesystem (when folder is null). 
	 * Should list only 'files', no folders.
	 */
	public Iterator<F> listFiles(String folder) throws FileSystemException;
	
	/**
	 * Get a string representation of an identification of a file, expected to be in the 'root' folder. 
	 * Must pair up with the implementation of {@link #toFile(String)}.
	 */
	public String getName(F f);
	/**
	 * Get a file 'F' representation of an identification of a file. 
	 * Must pair up with the implementation of {@link #getName(Object)}.
	 */
	public F toFile(String filename) throws FileSystemException;
	public F toFile(String folder, String filename) throws FileSystemException;
	public boolean exists(F f) throws FileSystemException;

	public boolean folderExists(String folder) throws FileSystemException;
	public InputStream readFile(F f) throws FileSystemException, IOException;
	public void deleteFile(F f) throws FileSystemException;
	public F moveFile(F f, String destinationFolder, boolean createFolder) throws FileSystemException;

	public void createFolder(String folder) throws FileSystemException;
	public void removeFolder(String folder) throws FileSystemException;


	public long getFileSize(F f) throws FileSystemException;
	public String getCanonicalName(F f) throws FileSystemException;
	public Date getModificationTime(F f) throws FileSystemException;

	public Map<String, Object> getAdditionalFileProperties(F f) throws FileSystemException;

}
