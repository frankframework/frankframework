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
package nl.nn.adapterframework.senders;

import jcifs.smb.SmbFile;
import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.Samba1FileSystem;
import nl.nn.adapterframework.filesystem.smb.Samba1FileSystemDelegator;

/**
 * Uses the (old) SMB 1 protocol.
 * <br/>
 * Only supports NTLM authentication.
 */
public class Samba1Sender extends FileSystemSender<SmbFile, Samba1FileSystem> implements Samba1FileSystemDelegator {

	public Samba1Sender() {
		setFileSystem(new Samba1FileSystem());
	}
}
