/*
   Copyright 2020-2026 WeAreFrank!

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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Extension to {@link IBasicFileSystem} that can be implemented to allow creation of files and folders.
 * <p>
 * For writable filesystems, the name of a file can be freely chosen, and:
 * - moving or copying a file to a folder probably will not change its name
 * - moving or copying a file to a folder can 'overwrite' a file already present in the folder
 * To accommodate these situations, for writable filesystems we support overwrite protection and rollover.
 * This requires that writeableFileSystem.getName() returns the name of the file in the directory, not the full name including the folder name.
 * </p>
 * @author Gerrit van Brakel
 *
 * @param <F> File representation
 */
public interface IWritableFileSystem<F> extends IBasicFileSystem<F> {

	/**
	 * Create a file with the given content inputstream
	 * @param file FileSystem file reference
	 * @param content to write or NULL. When NULL existing files should be overwritten, and new files should be created.
	 */
	void createFile(@NonNull F file, @Nullable InputStream content) throws FileSystemException, IOException;

	void appendFile(@NonNull F file, @Nullable InputStream content) throws FileSystemException, IOException;

	/**
	 * Renames the file to a new name, possibly in another folder.
	 * Does not need to check for the existence of the source or non-existence of the destination.
	 */
	F renameFile(@NonNull F source, @NonNull F destination) throws FileSystemException;
}
