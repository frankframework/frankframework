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

import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;

public class Samba2Sender extends FileSystemSender<String, Samba2FileSystem> {

	public Samba2Sender() {
		setFileSystem(new Samba2FileSystem());
	}

	@IbisDoc({ "1", "the destination, aka smb://xxx/yyy share", "" })
	public void setShare(String share) {
		getFileSystem().setShare(share);
	}

	@IbisDoc({ "2", "the smb share username", "" })
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@IbisDoc({ "3", "the smb share password", "" })
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	@IbisDoc({ "4", "alias used to obtain credentials for the smb share", "" })
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@IbisDoc({ "5", "domain, in case the user account is bound to a domain", "" })
	public void setDomain(String domain) {
		getFileSystem().setDomain(domain);
	}

	@IbisDoc({ "6", "Type of the authentication either 'NTLM' or 'SPNEGO' ", "SPNEGO" })
	public void setAuthType(String authType) {
		getFileSystem().setAuthType(authType);
	}

	@IbisDoc({ "7", "Kerberos Domain Controller, as set in java.security.krb5.kdc. If authentication type specified as SPNEGO and realm is specified then this field must be filled.", "" })
	public void setKdc(String kdc) {
		getFileSystem().setKdc(kdc);
	}

	@IbisDoc({ "8", "Kerberos Realm, as set in java.security.krb5.realm. If authentication type specified as SPNEGO this field must be filled. If not filled then default realm is used", "" })
	public void setRealm(String realm) {
		getFileSystem().setRealm(realm);
	}

}
