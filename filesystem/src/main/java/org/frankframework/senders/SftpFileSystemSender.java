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
package org.frankframework.senders;

import org.frankframework.filesystem.AbstractFileSystemSender;
import org.frankframework.filesystem.sftp.SftpFileRef;
import org.frankframework.filesystem.sftp.SftpFileSystem;
import org.frankframework.filesystem.sftp.SftpFileSystemDelegator;

public class SftpFileSystemSender extends AbstractFileSystemSender<SftpFileRef, SftpFileSystem> implements SftpFileSystemDelegator {

	public SftpFileSystemSender() {
		setFileSystem(new SftpFileSystem());
	}
}
