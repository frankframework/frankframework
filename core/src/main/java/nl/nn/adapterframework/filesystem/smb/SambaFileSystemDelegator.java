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
package nl.nn.adapterframework.filesystem.smb;

import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.doc.ReferTo;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;
import nl.nn.adapterframework.filesystem.Samba2FileSystem.Samba2AuthType;

public interface SambaFileSystemDelegator {

	Samba2FileSystem getFileSystem();

	@ReferTo(Samba2FileSystem.class)
	default void setHostname(String host) {
		getFileSystem().setHostname(host);
	}

	@ReferTo(Samba2FileSystem.class)
	default void setPort(int port) {
		getFileSystem().setPort(port);
	}

	@ReferTo(Samba2FileSystem.class)
	default void setAuthType(Samba2AuthType authType) {
		getFileSystem().setAuthType(authType);
	}

	@ReferTo(Samba2FileSystem.class)
	default void setAuthAlias(String alias) {
		getFileSystem().setAuthAlias(alias);
	}

	@ReferTo(Samba2FileSystem.class)
	default void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@ReferTo(Samba2FileSystem.class)
	default void setPassword(String passwd) {
		getFileSystem().setPassword(passwd);
	}

	@Deprecated
	@ConfigurationWarning("please use domainName instead")
	default void setDomain(String domain) {
		getFileSystem().setDomainName(domain);
	}
	/** domain, in case the user account is bound to a domain */
	default void setDomainName(String domain) {
		getFileSystem().setDomainName(domain);
	}

	@ReferTo(Samba2FileSystem.class)
	default void setShare(String share) {
		getFileSystem().setShare(share);
	}

	@ReferTo(Samba2FileSystem.class)
	default void setKdc(String kdc) {
		getFileSystem().setKdc(kdc);
	}

	@ReferTo(Samba2FileSystem.class)
	default void setRealm(String realm) {
		getFileSystem().setRealm(realm);
	}
}
