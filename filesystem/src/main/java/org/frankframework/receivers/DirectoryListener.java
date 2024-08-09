/*
   Copyright 2019, 2021 WeAreFrank!

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
package org.frankframework.receivers;

import java.nio.file.Path;

import org.frankframework.doc.Category;
import org.frankframework.filesystem.FileSystemListener;
import org.frankframework.filesystem.LocalFileSystem;

/**
 * Listener that looks in a FileSystem for files. <code>messageType</code> dictates what information of the file is passed in the message.
 *
 * <p>
 * Example usage:<br>
 * <pre><code>
 * &lt;DirectoryListener
 * 	name="directoryListener"
 * 	messageType="info"
 * 	root="${rootdirectory}"
 * 	inputFolder="in"
 * 	inProcessFolder="inProcess"
 * 	errorFolder="error"
 * 	createFolders="true"
 * /&gt;
 * </code></pre>
 *
 * </p>
 */
@Category("Basic")
public class DirectoryListener extends FileSystemListener<Path, LocalFileSystem>{

	@Override
	protected LocalFileSystem createFileSystem() {
		return new LocalFileSystem();
	}

	/** Optional base folder, that serves as root for all other folders */
	public void setRoot(String root) {
		getFileSystem().setRoot(root);
	}

	/**
	 * Determines the contents of the message that is sent to the pipeline. The value of the attribute matching the searchKey is returned when using <code>ATTRIBUTE</code>
	 * @ff.default PATH
	 */
	public void setMessageType(MessageType messageType) {
		super.setMessageType(messageType);
	}

}
