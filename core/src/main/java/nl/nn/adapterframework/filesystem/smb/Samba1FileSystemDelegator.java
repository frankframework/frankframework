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
import nl.nn.adapterframework.filesystem.Samba1FileSystem;

public interface Samba1FileSystemDelegator {

	Samba1FileSystem getFileSystem();

	@ReferTo(Samba1FileSystem.class)
	default void setShare(String share) {
		getFileSystem().setShare(share);
	}

	@ReferTo(Samba1FileSystem.class)
	default void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@ReferTo(Samba1FileSystem.class)
	default void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	@ReferTo(Samba1FileSystem.class)
	default void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@ReferTo(Samba1FileSystem.class)
	@Deprecated
	@ConfigurationWarning("Please use attribute domainName instead")
	default void setDomain(String domain) {
		getFileSystem().setDomainName(domain);
	}
	@Deprecated
	@ReferTo(Samba1FileSystem.class)
	@ConfigurationWarning("Please use attribute domainName instead")
	default void setAuthenticationDomain(String domain) {
		getFileSystem().setDomainName(domain);
	}
	@ReferTo(Samba1FileSystem.class)
	default void setDomainName(String domain) {
		getFileSystem().setDomainName(domain);
	}

	@ReferTo(Samba1FileSystem.class)
	default void setForce(boolean force) {
		getFileSystem().setForce(force);
	}

	@ReferTo(Samba1FileSystem.class)
	default void setListHiddenFiles(boolean listHiddenFiles) {
		getFileSystem().setListHiddenFiles(listHiddenFiles);
	}
}
