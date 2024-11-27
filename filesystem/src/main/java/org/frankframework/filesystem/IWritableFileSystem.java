/*
   Copyright 2020-2024 WeAreFrank!

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
import java.io.InputStream;
import java.io.OutputStream;

import org.frankframework.util.StreamUtil;

/**
 * Extension to {@link IBasicFileSystem} that can be implemented to allow creation of files and folders.
 *
 * For writable filesystems, the name of a file can be freely chosen, and:
 * - moving or copying a file to a folder probably will not change its name
 * - moving or copying a file to a folder can 'overwrite' a file already present in the folder
 * To accommodate these situations, for writable filesystems we support overwrite protection and rollover.
 * This requires that writeableFileSystem.getName() returns the name of the file in the directory, not the full name including the folder name.
 *
 * @author Gerrit van Brakel
 *
 * @param <F> File representation
 */
public interface IWritableFileSystem<F> extends IBasicFileSystem<F> {

	OutputStream createFile(F f) throws FileSystemException, IOException;

	/**
	 * @param file FileSystem file reference
	 * @param content to write or NULL. When NULL existing files should be overwritten, and new files should be created.
	 */
	default void createFile(F file, InputStream content) throws FileSystemException, IOException {
		try (OutputStream out = createFile(file)) {
			StreamUtil.streamToStream(content, out);
		}
	}

	OutputStream appendFile(F f) throws FileSystemException, IOException;

	default void appendFile(F f, InputStream content) throws FileSystemException, IOException {
		try (OutputStream out = appendFile(f)) {
			StreamUtil.streamToStream(content, out);
		}
	}

	/**
	 * Renames the file to a new name, possibly in a another folder.
	 * Does not need to check for existence of the source or non-existence of the destination.
	 */
	F renameFile(F source, F destination) throws FileSystemException;
}
