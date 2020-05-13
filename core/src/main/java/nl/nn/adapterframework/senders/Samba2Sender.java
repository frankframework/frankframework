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

import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;

public class Samba2Sender extends FileSystemSender<String, Samba2FileSystem> {

	private final static String SAMBA_2_FILE_SYSTEM = "nl.nn.adapterframework.filesystem.Samba2FileSystem";

	public Samba2Sender() {
		setFileSystem(new Samba2FileSystem());
	}

	@IbisDocRef({"1", SAMBA_2_FILE_SYSTEM})
	public void setShare(String share) {
		getFileSystem().setShare(share);
	}

	@IbisDocRef({"2", SAMBA_2_FILE_SYSTEM})
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@IbisDocRef({"3", SAMBA_2_FILE_SYSTEM})
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	@IbisDocRef({"4", SAMBA_2_FILE_SYSTEM})
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@IbisDocRef({"5", SAMBA_2_FILE_SYSTEM})
	public void setDomain(String domain) {
		getFileSystem().setDomain(domain);
	}

	@IbisDocRef({"6", SAMBA_2_FILE_SYSTEM})
	public void setAuthType(String authType) {
		getFileSystem().setAuthType(authType);
	}

	@IbisDocRef({"7", SAMBA_2_FILE_SYSTEM})
	public void setKdc(String kdc) {
		getFileSystem().setKdc(kdc);
	}

	@IbisDocRef({"8", SAMBA_2_FILE_SYSTEM})
	public void setRealm(String realm) {
		getFileSystem().setRealm(realm);
	}

	@IbisDocRef({"10", SAMBA_2_FILE_SYSTEM})
	public void setPort(Integer port) {
		getFileSystem().setPort(port);
	}

}
