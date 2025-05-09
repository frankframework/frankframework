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
package org.frankframework.pipes;

import jcifs.smb.SmbFile;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.filesystem.AbstractFileSystemPipe;
import org.frankframework.filesystem.smb.Samba1FileSystem;
import org.frankframework.filesystem.smb.Samba1FileSystemDelegator;

/**
 * Uses the (old) SMB 1 protocol.
 * <br/>
 * Only supports NTLM authentication.
 */
@Deprecated(since = "9.2.0", forRemoval = true)
@ConfigurationWarning("NTLM authentication is unsecure, if possible upgrade to SMB3 asap.")
public class Samba1Pipe extends AbstractFileSystemPipe<SmbFile, Samba1FileSystem> implements Samba1FileSystemDelegator {

	public Samba1Pipe() {
		setFileSystem(new Samba1FileSystem());
	}
}
