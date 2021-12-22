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

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.encryption.HasKeystore;
import nl.nn.adapterframework.encryption.HasTruststore;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.FtpFileSystem;
import nl.nn.adapterframework.ftp.FtpSession.FtpType;
import nl.nn.adapterframework.ftp.FtpSession.Prot;

public class FtpFileSystemSender extends FileSystemSender<FTPFile, FtpFileSystem> implements HasKeystore, HasTruststore {

	private final String FTPFILESYSTEM = "nl.nn.adapterframework.filesystem.FtpFileSystem";

	public FtpFileSystemSender() {
		setFileSystem(new FtpFileSystem());
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setRemoteDirectory(String remoteDirectory) {
		getFileSystem().setRemoteDirectory(remoteDirectory);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setHost(String host) {
		getFileSystem().setHost(host);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setPort(int port) {
		getFileSystem().setPort(port);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setAuthAlias(String alias) {
		getFileSystem().setAuthAlias(alias);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setPassword(String passwd) {
		getFileSystem().setPassword(passwd);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setProxyUsername(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	@IbisDocRef({FTPFILESYSTEM})
	@Deprecated
	@ConfigurationWarning("use attribute ftpType instead")
	public void setFtpTypeDescription(FtpType ftpTypeDescription) {
		getFileSystem().setFtpTypeDescription(ftpTypeDescription);
	}
	@IbisDocRef({FTPFILESYSTEM})
	public void setFtpType(FtpType value) {
		getFileSystem().setFtpType(value);
	}
	public FtpType getFtpType() {
		return getFileSystem().getFtpType();
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setFileType(String fileType) {
		getFileSystem().setFileType(fileType);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setMessageIsContent(boolean messageIsContent) {
		getFileSystem().setMessageIsContent(messageIsContent);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setPassive(boolean b) {
		getFileSystem().setPassive(b);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setProxyTransportType(int proxyTransportType) {
		getFileSystem().setProxyTransportType(proxyTransportType);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setPrefCSEncryption(String prefCSEncryption) {
		getFileSystem().setPrefCSEncryption(prefCSEncryption);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setPrefSCEncryption(String prefSCEncryption) {
		getFileSystem().setPrefSCEncryption(prefSCEncryption);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setPrivateKeyFilePath(String privateKeyFilePath) {
		getFileSystem().setPrivateKeyFilePath(privateKeyFilePath);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setPrivateKeyAuthAlias(String privateKeyAuthAlias) {
		getFileSystem().setPrivateKeyAuthAlias(privateKeyAuthAlias);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setPrivateKeyPassword(String passPhrase) {
		getFileSystem().setPrivateKeyPassword(passPhrase);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setKnownHostsPath(String knownHostsPath) {
		getFileSystem().setKnownHostsPath(knownHostsPath);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setConsoleKnownHostsVerifier(boolean verifier) {
		getFileSystem().setConsoleKnownHostsVerifier(verifier);
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setKeystore(String keystore) {
		getFileSystem().setKeystore(keystore);
	}
	@Override
	public String getKeystore() {
		return getFileSystem().getKeystore();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setKeystoreType(KeystoreType keystoreType) {
		getFileSystem().setKeystoreType(keystoreType);
	}
	@Override
	public KeystoreType getKeystoreType() {
		return getFileSystem().getKeystoreType();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		getFileSystem().setKeystoreAuthAlias(keystoreAuthAlias);
	}
	@Override
	public String getKeystoreAuthAlias() {
		return getFileSystem().getKeystoreAuthAlias();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setKeystorePassword(String keystorePassword) {
		getFileSystem().setKeystorePassword(keystorePassword);
	}
	@Override
	public String getKeystorePassword() {
		return getFileSystem().getKeystorePassword();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setKeystoreAlias(String keystoreAlias) {
		getFileSystem().setKeystoreAlias(keystoreAlias);
	}
	@Override
	public String getKeystoreAlias() {
		return getFileSystem().getKeystoreAlias();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		getFileSystem().setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}
	@Override
	public String getKeystoreAliasAuthAlias() {
		return getFileSystem().getKeystoreAliasAuthAlias();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setKeystoreAliasPassword(String keystoreAliasPassword) {
		getFileSystem().setKeystoreAliasPassword(keystoreAliasPassword);
	}
	@Override
	public String getKeystoreAliasPassword() {
		return getFileSystem().getKeystoreAliasPassword();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		getFileSystem().setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	@Override
	public String getKeyManagerAlgorithm() {
		return getFileSystem().getKeyManagerAlgorithm();
	}

	
	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setTruststore(String truststore) {
		getFileSystem().setTruststore(truststore);
	}
	@Override
	public String getTruststore() {
		return getFileSystem().getTruststore();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setTruststoreType(KeystoreType truststoreType) {
		getFileSystem().setTruststoreType(truststoreType);
	}
	@Override
	public KeystoreType getTruststoreType() {
		return getFileSystem().getTruststoreType();
	}


	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		getFileSystem().setTruststoreAuthAlias(truststoreAuthAlias);
	}
	@Override
	public String getTruststoreAuthAlias() {
		return getFileSystem().getTruststoreAuthAlias();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setTruststorePassword(String truststorePassword) {
		getFileSystem().setTruststorePassword(truststorePassword);
	}
	@Override
	public String getTruststorePassword() {
		return getFileSystem().getTruststorePassword();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		getFileSystem().setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	@Override
	public String getTrustManagerAlgorithm() {
		return getFileSystem().getTrustManagerAlgorithm();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setVerifyHostname(boolean verifyHostname) {
		getFileSystem().setVerifyHostname(verifyHostname);
	}
	@Override
	public boolean isVerifyHostname() {
		return getFileSystem().isVerifyHostname();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		getFileSystem().setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}
	@Override
	public boolean isAllowSelfSignedCertificates() {
		return getFileSystem().isAllowSelfSignedCertificates();
	}

	@Override
	@IbisDocRef({FTPFILESYSTEM})
	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		getFileSystem().setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}
	@Override
	public boolean isIgnoreCertificateExpiredException() {
		return getFileSystem().isIgnoreCertificateExpiredException();
	}

	@IbisDocRef({FTPFILESYSTEM})
	@Deprecated
	@ConfigurationWarning("use attribute prot=\"P\" instead")
	public void setProtP(boolean protP) {
		getFileSystem().setProtP(protP);
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setProt(Prot prot) {
		getFileSystem().setProt(prot);
	}
	public Prot getProt() {
		return getFileSystem().getProt();
	}

	@IbisDocRef({FTPFILESYSTEM})
	public void setKeyboardInteractive(boolean keyboardInteractive) {
		getFileSystem().setKeyboardInteractive(keyboardInteractive);
	}
}