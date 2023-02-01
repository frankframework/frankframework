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
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.doc.ReferTo;
import nl.nn.adapterframework.encryption.HasKeystore;
import nl.nn.adapterframework.encryption.HasTruststore;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.filesystem.FileSystemListener;
import nl.nn.adapterframework.filesystem.FtpFileSystem;
import nl.nn.adapterframework.ftp.FTPFileRef;
import nl.nn.adapterframework.ftp.FtpSession.FileType;
import nl.nn.adapterframework.ftp.FtpSession.FtpType;
import nl.nn.adapterframework.ftp.FtpSession.Prot;

public class FtpFileSystemListener extends FileSystemListener<FTPFileRef, FtpFileSystem> implements HasKeystore, HasTruststore {

	@Override
	protected FtpFileSystem createFileSystem() {
		return new FtpFileSystem();
	}

	@ReferTo(FtpFileSystem.class)
	public void setRemoteDirectory(String remoteDirectory) {
		getFileSystem().setRemoteDirectory(remoteDirectory);
	}

	@ReferTo(FtpFileSystem.class)
	public void setHost(String host) {
		getFileSystem().setHost(host);
	}

	@ReferTo(FtpFileSystem.class)
	public void setPort(int port) {
		getFileSystem().setPort(port);
	}

	@ReferTo(FtpFileSystem.class)
	public void setAuthAlias(String alias) {
		getFileSystem().setAuthAlias(alias);
	}

	@ReferTo(FtpFileSystem.class)
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@ReferTo(FtpFileSystem.class)
	public void setPassword(String passwd) {
		getFileSystem().setPassword(passwd);
	}

	@ReferTo(FtpFileSystem.class)
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@ReferTo(FtpFileSystem.class)
	public void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@ReferTo(FtpFileSystem.class)
	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	@ReferTo(FtpFileSystem.class)
	public void setProxyUsername(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}

	@ReferTo(FtpFileSystem.class)
	public void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	@ReferTo(FtpFileSystem.class)
	public void setFtpType(FtpType value) {
		getFileSystem().setFtpType(value);
	}
	public FtpType getFtpType() {
		return getFileSystem().getFtpType();
	}

	@ReferTo(FtpFileSystem.class)
	public void setFileType(FileType fileType) {
		getFileSystem().setFileType(fileType);
	}

	@ReferTo(FtpFileSystem.class)
	public void setMessageIsContent(boolean messageIsContent) {
		getFileSystem().setMessageIsContent(messageIsContent);
	}

	@ReferTo(FtpFileSystem.class)
	public void setPassive(boolean b) {
		getFileSystem().setPassive(b);
	}

	@ReferTo(FtpFileSystem.class)
	public void setProxyTransportType(int proxyTransportType) {
		getFileSystem().setProxyTransportType(proxyTransportType);
	}

	@ReferTo(FtpFileSystem.class)
	public void setPrefCSEncryption(String prefCSEncryption) {
		getFileSystem().setPrefCSEncryption(prefCSEncryption);
	}

	@ReferTo(FtpFileSystem.class)
	public void setPrefSCEncryption(String prefSCEncryption) {
		getFileSystem().setPrefSCEncryption(prefSCEncryption);
	}

	@ReferTo(FtpFileSystem.class)
	public void setPrivateKeyFilePath(String privateKeyFilePath) {
		getFileSystem().setPrivateKeyFilePath(privateKeyFilePath);
	}

	@ReferTo(FtpFileSystem.class)
	public void setPrivateKeyAuthAlias(String privateKeyAuthAlias) {
		getFileSystem().setPrivateKeyAuthAlias(privateKeyAuthAlias);
	}

	@ReferTo(FtpFileSystem.class)
	public void setPrivateKeyPassword(String passPhrase) {
		getFileSystem().setPrivateKeyPassword(passPhrase);
	}

	@ReferTo(FtpFileSystem.class)
	public void setKnownHostsPath(String knownHostsPath) {
		getFileSystem().setKnownHostsPath(knownHostsPath);
	}

	@ReferTo(FtpFileSystem.class)
	public void setConsoleKnownHostsVerifier(boolean verifier) {
		getFileSystem().setConsoleKnownHostsVerifier(verifier);
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setKeystore(String keystore) {
		getFileSystem().setKeystore(keystore);
	}
	@Override
	public String getKeystore() {
		return getFileSystem().getKeystore();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setKeystoreType(KeystoreType keystoreType) {
		getFileSystem().setKeystoreType(keystoreType);
	}
	@Override
	public KeystoreType getKeystoreType() {
		return getFileSystem().getKeystoreType();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		getFileSystem().setKeystoreAuthAlias(keystoreAuthAlias);
	}
	@Override
	public String getKeystoreAuthAlias() {
		return getFileSystem().getKeystoreAuthAlias();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setKeystorePassword(String keystorePassword) {
		getFileSystem().setKeystorePassword(keystorePassword);
	}
	@Override
	public String getKeystorePassword() {
		return getFileSystem().getKeystorePassword();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setKeystoreAlias(String keystoreAlias) {
		getFileSystem().setKeystoreAlias(keystoreAlias);
	}
	@Override
	public String getKeystoreAlias() {
		return getFileSystem().getKeystoreAlias();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		getFileSystem().setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}
	@Override
	public String getKeystoreAliasAuthAlias() {
		return getFileSystem().getKeystoreAliasAuthAlias();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setKeystoreAliasPassword(String keystoreAliasPassword) {
		getFileSystem().setKeystoreAliasPassword(keystoreAliasPassword);
	}
	@Override
	public String getKeystoreAliasPassword() {
		return getFileSystem().getKeystoreAliasPassword();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		getFileSystem().setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	@Override
	public String getKeyManagerAlgorithm() {
		return getFileSystem().getKeyManagerAlgorithm();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setTruststore(String truststore) {
		getFileSystem().setTruststore(truststore);
	}
	@Override
	public String getTruststore() {
		return getFileSystem().getTruststore();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setTruststoreType(KeystoreType truststoreType) {
		getFileSystem().setTruststoreType(truststoreType);
	}
	@Override
	public KeystoreType getTruststoreType() {
		return getFileSystem().getTruststoreType();
	}


	@Override
	@ReferTo(FtpFileSystem.class)
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		getFileSystem().setTruststoreAuthAlias(truststoreAuthAlias);
	}
	@Override
	public String getTruststoreAuthAlias() {
		return getFileSystem().getTruststoreAuthAlias();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setTruststorePassword(String truststorePassword) {
		getFileSystem().setTruststorePassword(truststorePassword);
	}
	@Override
	public String getTruststorePassword() {
		return getFileSystem().getTruststorePassword();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		getFileSystem().setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	@Override
	public String getTrustManagerAlgorithm() {
		return getFileSystem().getTrustManagerAlgorithm();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setVerifyHostname(boolean verifyHostname) {
		getFileSystem().setVerifyHostname(verifyHostname);
	}
	@Override
	public boolean isVerifyHostname() {
		return getFileSystem().isVerifyHostname();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		getFileSystem().setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}
	@Override
	public boolean isAllowSelfSignedCertificates() {
		return getFileSystem().isAllowSelfSignedCertificates();
	}

	@Override
	@ReferTo(FtpFileSystem.class)
	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		getFileSystem().setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}
	@Override
	public boolean isIgnoreCertificateExpiredException() {
		return getFileSystem().isIgnoreCertificateExpiredException();
	}


	@ReferTo(FtpFileSystem.class)
	public void setProt(Prot prot) {
		getFileSystem().setProt(prot);
	}
	public Prot getProt() {
		return getFileSystem().getProt();
	}

	@ReferTo(FtpFileSystem.class)
	public void setKeyboardInteractive(boolean keyboardInteractive) {
		getFileSystem().setKeyboardInteractive(keyboardInteractive);
	}
}
