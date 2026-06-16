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
package org.frankframework.filesystem.ftp;

import org.frankframework.doc.ReferTo;
import org.frankframework.encryption.HasKeystore;
import org.frankframework.encryption.HasTruststore;
import org.frankframework.encryption.KeystoreConfiguration;
import org.frankframework.encryption.TruststoreConfiguration;
import org.frankframework.filesystem.ftp.FtpSession.FileType;
import org.frankframework.filesystem.ftp.FtpSession.FtpType;
import org.frankframework.filesystem.ftp.FtpSession.Prot;
import org.frankframework.filesystem.ftp.FtpSession.TransportType;

public interface FtpFileSystemDelegator extends HasKeystore, HasTruststore {

	default FtpFileSystem createFileSystem() {
		return new FtpFileSystem();
	}

	FtpFileSystem getFileSystem();

	@ReferTo(FtpFileSystem.class)
	default void setRemoteDirectory(String remoteDirectory) {
		getFileSystem().setRemoteDirectory(remoteDirectory);
	}

	@ReferTo(FtpFileSystem.class)
	default void setHost(String host) {
		getFileSystem().setHost(host);
	}

	@ReferTo(FtpFileSystem.class)
	default void setPort(int port) {
		getFileSystem().setPort(port);
	}

	@ReferTo(FtpFileSystem.class)
	default void setAuthAlias(String alias) {
		getFileSystem().setAuthAlias(alias);
	}

	@ReferTo(FtpFileSystem.class)
	default void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@ReferTo(FtpFileSystem.class)
	default void setPassword(String passwd) {
		getFileSystem().setPassword(passwd);
	}

	@ReferTo(FtpFileSystem.class)
	default void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@ReferTo(FtpFileSystem.class)
	default void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@ReferTo(FtpFileSystem.class)
	default void setFtpType(FtpType value) {
		getFileSystem().setFtpType(value);
	}

	@ReferTo(FtpFileSystem.class)
	default void setFileType(FileType fileType) {
		getFileSystem().setFileType(fileType);
	}

	@ReferTo(FtpFileSystem.class)
	default void setPassive(boolean b) {
		getFileSystem().setPassive(b);
	}

	@ReferTo(FtpFileSystem.class)
	default void setProxyTransportType(TransportType proxyTransportType) {
		getFileSystem().setProxyTransportType(proxyTransportType);
	}

	@ReferTo(FtpFileSystem.class)
	default void setProt(Prot prot) {
		getFileSystem().setProt(prot);
	}

	@Override
	default void setKeystoreConfiguration(KeystoreConfiguration keystoreConfiguration) {
		getFileSystem().setKeystoreConfiguration(keystoreConfiguration);
	}

	/**
	 * Override default method to use the keystoreConfiguration in the fileSystem
	 */
	@Override
	default KeystoreConfiguration getKeystoreConfiguration() {
		return getFileSystem().getKeystoreConfiguration();
	}

	@Override
	default void setTruststoreConfiguration(TruststoreConfiguration truststoreConfiguration) {
		getFileSystem().setTruststoreConfiguration(truststoreConfiguration);
	}

	/**
	 * Override default method to use the truststoreConfiguration in the fileSystem
	 */
	@Override
	default TruststoreConfiguration  getTruststoreConfiguration() {
		return getFileSystem().getTruststoreConfiguration();
	}
}
