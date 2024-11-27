/*
   Copyright 2023 WeAreFrank!

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

import org.frankframework.filesystem.AbstractFileSystemListener;
import org.frankframework.filesystem.sftp.SftpFileRef;
import org.frankframework.filesystem.sftp.SftpFileSystem;
import org.frankframework.filesystem.sftp.SftpFileSystemDelegator;

/**
 * File listener for an SFTP connection.
 *
 * {@inheritDoc}
 */
public class SftpFileSystemListener extends AbstractFileSystemListener<SftpFileRef, SftpFileSystem> implements SftpFileSystemDelegator {

	@Override
	public SftpFileSystem createFileSystem() {
		return SftpFileSystemDelegator.super.createFileSystem();
	}

	/**
	 * Determines the contents of the message that is sent to the pipeline. The value of the attribute matching the searchKey is returned when using <code>ATTRIBUTE</code>
	 * @ff.default PATH
	 */
	public void setMessageType(MessageType messageType) {
		super.setMessageType(messageType);
	}

}
