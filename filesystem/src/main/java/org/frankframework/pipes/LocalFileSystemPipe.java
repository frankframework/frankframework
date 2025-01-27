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
package org.frankframework.pipes;

import java.nio.file.Path;

import org.frankframework.doc.ReferTo;
import org.frankframework.filesystem.AbstractFileSystemPipe;
import org.frankframework.filesystem.LocalFileSystem;

/**
 * Pipe to work with the server local filesystem.
 * <p>
 *     In addition to regular parameters for filesystem senders, it is possible
 *     to set custom extended attributes on files by prefixing parameter names with
 *     {@value org.frankframework.filesystem.ISupportsCustomFileAttributes#FILE_ATTRIBUTE_PARAM_PREFIX}.
 *     This prefix will be not be part of the actual metadata property name.
 * </p>
 * <p>
 *     The string value of these parameters will be used as value of the custom metadata attribute.
 * </p>
 * <p>
 *     If extended attributes actually can be written depends on the underlying OS / filesystem.
 * </p>
 */
public class LocalFileSystemPipe extends AbstractFileSystemPipe<Path, LocalFileSystem> {

	public LocalFileSystemPipe() {
		setFileSystem(new LocalFileSystem());
	}

	@ReferTo(LocalFileSystem.class)
	public void setRoot(String root) {
		getFileSystem().setRoot(root);
	}

	@ReferTo(LocalFileSystem.class)
	public void setCreateRootFolder(boolean createRootFolder) {
		getFileSystem().setCreateRootFolder(createRootFolder);
	}
}
