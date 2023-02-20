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
package nl.nn.adapterframework.pipes;

import jcifs.smb.SmbFile;
import nl.nn.adapterframework.filesystem.FileSystemPipe;
import nl.nn.adapterframework.filesystem.Samba1FileSystem;

public class Samba1Pipe extends FileSystemPipe<SmbFile, Samba1FileSystem> {

	public Samba1Pipe() {
		setFileSystem(new Samba1FileSystem());
	}

	/** Shared folder name in the samba server */
	public void setShare(String share) {
		getFileSystem().setShare(share);
	}

	/** the smb share username */
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	/** the smb share password */
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	/** alias used to obtain credentials for the smb share */
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	/** in case the user account is bound to a domain */
	public void setDomain(String domain) {
		getFileSystem().setDomain(domain);
	}

	/**
	 * controls whether hidden files are seen or not
	 * @ff.default false
	 */
	public void setForce(boolean force) {
		getFileSystem().setForce(force);
	}

}
