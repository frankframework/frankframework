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
package nl.nn.adapterframework.ftp;

import nl.nn.adapterframework.doc.ReferTo;
import nl.nn.adapterframework.filesystem.SftpFileSystem;
import nl.nn.adapterframework.ftp.SftpSession.TransportType;

public interface SftpFileSystemDelegator {

	default SftpFileSystem createFileSystem() {
		return new SftpFileSystem();
	}

	SftpFileSystem getFileSystem();

	@ReferTo(SftpFileSystem.class)
	default void setRemoteDirectory(String remoteDirectory) {
		getFileSystem().setRemoteDirectory(remoteDirectory);
	}

	@ReferTo(SftpFileSystem.class)
	default void setHost(String host) {
		getFileSystem().setHost(host);
	}

	@ReferTo(SftpFileSystem.class)
	default void setPort(int port) {
		getFileSystem().setPort(port);
	}

	@ReferTo(SftpFileSystem.class)
	default void setAuthAlias(String alias) {
		getFileSystem().setAuthAlias(alias);
	}

	@ReferTo(SftpFileSystem.class)
	default void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@ReferTo(SftpFileSystem.class)
	default void setPassword(String passwd) {
		getFileSystem().setPassword(passwd);
	}

	@ReferTo(SftpFileSystem.class)
	default void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@ReferTo(SftpFileSystem.class)
	default void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@ReferTo(SftpFileSystem.class)
	default void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	@ReferTo(SftpFileSystem.class)
	default void setProxyUsername(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}

	@ReferTo(SftpFileSystem.class)
	default void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	@ReferTo(SftpFileSystem.class)
	default void setProxyTransportType(TransportType proxyTransportType) {
		getFileSystem().setProxyTransportType(proxyTransportType);
	}

	@ReferTo(SftpFileSystem.class)
	default void setPrefCSEncryption(String prefCSEncryption) {
		getFileSystem().setPrefCSEncryption(prefCSEncryption);
	}

	@ReferTo(SftpFileSystem.class)
	default void setPrefSCEncryption(String prefSCEncryption) {
		getFileSystem().setPrefSCEncryption(prefSCEncryption);
	}

	@ReferTo(SftpFileSystem.class)
	default void setPrivateKeyFilePath(String privateKeyFilePath) {
		getFileSystem().setPrivateKeyFilePath(privateKeyFilePath);
	}

	@ReferTo(SftpFileSystem.class)
	default void setPrivateKeyAuthAlias(String privateKeyAuthAlias) {
		getFileSystem().setPrivateKeyAuthAlias(privateKeyAuthAlias);
	}

	@ReferTo(SftpFileSystem.class)
	default void setPrivateKeyPassword(String privateKeyPassword) {
		getFileSystem().setPrivateKeyPassword(privateKeyPassword);
	}

	@ReferTo(SftpFileSystem.class)
	default void setKnownHostsPath(String knownHostsPath) {
		getFileSystem().setKnownHostsPath(knownHostsPath);
	}

	@ReferTo(SftpFileSystem.class)
	default void setStrictHostKeyChecking(boolean strictHostKeyChecking) {
		getFileSystem().setStrictHostKeyChecking(strictHostKeyChecking);
	}
}
