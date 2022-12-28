/*
   Copyright 2019-2021 WeAreFrank!

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

import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.encryption.HasKeystore;
import nl.nn.adapterframework.encryption.HasTruststore;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.FtpFileSystem;
import nl.nn.adapterframework.ftp.FTPFileRef;
import nl.nn.adapterframework.ftp.FtpSession.FileType;
import nl.nn.adapterframework.ftp.FtpSession.FtpType;
import nl.nn.adapterframework.ftp.FtpSession.Prot;

public class FtpFileSystemSender extends FileSystemSender<FTPFileRef, FtpFileSystem> implements HasKeystore, HasTruststore {

	private final String FTPFILESYSTEM = "nl.nn.adapterframework.filesystem.FtpFileSystem";

	public FtpFileSystemSender() {
		setFileSystem(new FtpFileSystem());
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setRemoteDirectory(String remoteDirectory) {
		getFileSystem().setRemoteDirectory(remoteDirectory);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setHost(String host) {
		getFileSystem().setHost(host);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setPort(int port) {
		getFileSystem().setPort(port);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setAuthAlias(String alias) {
		getFileSystem().setAuthAlias(alias);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setPassword(String passwd) {
		getFileSystem().setPassword(passwd);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setProxyUsername(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	@Deprecated
	@ConfigurationWarning("use attribute ftpType instead")
	public void setFtpTypeDescription(FtpType ftpTypeDescription) {
		getFileSystem().setFtpTypeDescription(ftpTypeDescription);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setFtpType(FtpType value) {
		getFileSystem().setFtpType(value);
	}
	public FtpType getFtpType() {
		return getFileSystem().getFtpType();
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setFileType(FileType fileType) {
		getFileSystem().setFileType(fileType);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setMessageIsContent(boolean messageIsContent) {
		getFileSystem().setMessageIsContent(messageIsContent);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setPassive(boolean b) {
		getFileSystem().setPassive(b);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setProxyTransportType(int proxyTransportType) {
		getFileSystem().setProxyTransportType(proxyTransportType);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setPrefCSEncryption(String prefCSEncryption) {
		getFileSystem().setPrefCSEncryption(prefCSEncryption);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setPrefSCEncryption(String prefSCEncryption) {
		getFileSystem().setPrefSCEncryption(prefSCEncryption);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setPrivateKeyFilePath(String privateKeyFilePath) {
		getFileSystem().setPrivateKeyFilePath(privateKeyFilePath);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setPrivateKeyAuthAlias(String privateKeyAuthAlias) {
		getFileSystem().setPrivateKeyAuthAlias(privateKeyAuthAlias);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setPrivateKeyPassword(String passPhrase) {
		getFileSystem().setPrivateKeyPassword(passPhrase);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setKnownHostsPath(String knownHostsPath) {
		getFileSystem().setKnownHostsPath(knownHostsPath);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setConsoleKnownHostsVerifier(boolean verifier) {
		getFileSystem().setConsoleKnownHostsVerifier(verifier);
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setKeystore(String keystore) {
		getFileSystem().setKeystore(keystore);
	}
	@Override
	public String getKeystore() {
		return getFileSystem().getKeystore();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setKeystoreType(KeystoreType keystoreType) {
		getFileSystem().setKeystoreType(keystoreType);
	}
	@Override
	public KeystoreType getKeystoreType() {
		return getFileSystem().getKeystoreType();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		getFileSystem().setKeystoreAuthAlias(keystoreAuthAlias);
	}
	@Override
	public String getKeystoreAuthAlias() {
		return getFileSystem().getKeystoreAuthAlias();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setKeystorePassword(String keystorePassword) {
		getFileSystem().setKeystorePassword(keystorePassword);
	}
	@Override
	public String getKeystorePassword() {
		return getFileSystem().getKeystorePassword();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setKeystoreAlias(String keystoreAlias) {
		getFileSystem().setKeystoreAlias(keystoreAlias);
	}
	@Override
	public String getKeystoreAlias() {
		return getFileSystem().getKeystoreAlias();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		getFileSystem().setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}
	@Override
	public String getKeystoreAliasAuthAlias() {
		return getFileSystem().getKeystoreAliasAuthAlias();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setKeystoreAliasPassword(String keystoreAliasPassword) {
		getFileSystem().setKeystoreAliasPassword(keystoreAliasPassword);
	}
	@Override
	public String getKeystoreAliasPassword() {
		return getFileSystem().getKeystoreAliasPassword();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		getFileSystem().setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	@Override
	public String getKeyManagerAlgorithm() {
		return getFileSystem().getKeyManagerAlgorithm();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setTruststore(String truststore) {
		getFileSystem().setTruststore(truststore);
	}
	@Override
	public String getTruststore() {
		return getFileSystem().getTruststore();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setTruststoreType(KeystoreType truststoreType) {
		getFileSystem().setTruststoreType(truststoreType);
	}
	@Override
	public KeystoreType getTruststoreType() {
		return getFileSystem().getTruststoreType();
	}


	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		getFileSystem().setTruststoreAuthAlias(truststoreAuthAlias);
	}
	@Override
	public String getTruststoreAuthAlias() {
		return getFileSystem().getTruststoreAuthAlias();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setTruststorePassword(String truststorePassword) {
		getFileSystem().setTruststorePassword(truststorePassword);
	}
	@Override
	public String getTruststorePassword() {
		return getFileSystem().getTruststorePassword();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		getFileSystem().setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	@Override
	public String getTrustManagerAlgorithm() {
		return getFileSystem().getTrustManagerAlgorithm();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setVerifyHostname(boolean verifyHostname) {
		getFileSystem().setVerifyHostname(verifyHostname);
	}
	@Override
	public boolean isVerifyHostname() {
		return getFileSystem().isVerifyHostname();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		getFileSystem().setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}
	@Override
	public boolean isAllowSelfSignedCertificates() {
		return getFileSystem().isAllowSelfSignedCertificates();
	}

	@Override
	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		getFileSystem().setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}
	@Override
	public boolean isIgnoreCertificateExpiredException() {
		return getFileSystem().isIgnoreCertificateExpiredException();
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	@Deprecated
	@ConfigurationWarning("use attribute prot=\"P\" instead")
	public void setProtP(boolean protP) {
		getFileSystem().setProtP(protP);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setProt(Prot prot) {
		getFileSystem().setProt(prot);
	}
	public Prot getProt() {
		return getFileSystem().getProt();
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.FtpFileSystem */
	public void setKeyboardInteractive(boolean keyboardInteractive) {
		getFileSystem().setKeyboardInteractive(keyboardInteractive);
	}
}