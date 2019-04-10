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
import java.io.OutputStream;

/**
 * Extension to {@link IBasicFileSystem} that can be implemented to allow creation of files and folders.
 * 
 * @author Gerrit van Brakel
 *
 * @param <F> File representation
 */
public interface IWritableFileSystem<F> extends IBasicFileSystem<F> {

	public OutputStream createFile(F f) throws FileSystemException, IOException;
	public OutputStream appendFile(F f) throws FileSystemException, IOException;
	/**
	 * Renames the file to a new name in the same folder, returns the 'new' file.
	 * @param force when true, overwrites existing file, eventually
	 */
	public F renameFile(F f, String newName, boolean force) throws FileSystemException;

	public void createFolder(F f) throws FileSystemException;
	public void removeFolder(F f) throws FileSystemException;

}
