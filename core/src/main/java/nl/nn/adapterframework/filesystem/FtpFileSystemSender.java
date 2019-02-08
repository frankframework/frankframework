/*
   Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.filesystem;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpSession;

/**
 * 
 * @author Daniël Meyer
 *
 */
public class FtpFileSystemSender extends FileSystemSender<FTPFile, FtpFileSystem> {
	
	private FtpSession session;
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		setFileSystem(new FtpFileSystem());
		session = getFileSystem().getFtpSession();
	}
	
	
	
	public FtpSession getFtpSession() {
		return session;
	}
	
	public void setRemoteDirectory(String remoteDirectory) {
		getFileSystem().setRemoteDirectory(remoteDirectory);
	}
	public String getRemoteDirectory() {
		return getFileSystem().getRemoteDirectory();
	}

	public void setRemoteFilenamePattern(String string) {
		getFileSystem().setRemoteFilenamePattern(string);
	}
	public String getRemoteFilenamePattern() {
		return getFileSystem().getRemoteFilenamePattern();
	}


	

	public void setHost(String host) {
		session.setHost(host);
	}
	public void setPort(int port) {
		session.setPort(port);
	}

	public void setAuthAlias(String alias) {
		session.setAuthAlias(alias);
	}
	public void setUsername(String username) {
		session.setUsername(username);
	}
	public void setPassword(String passwd) {
		session.setPassword(passwd);
	}

	public void setProxyHost(String proxyHost) {
		session.setProxyHost(proxyHost);
	}
	public void setProxyPort(int proxyPort) {
		session.setProxyPort(proxyPort);
	}
	public void setProxyAuthAlias(String proxyAuthAlias) {
		session.setProxyAuthAlias(proxyAuthAlias);
	}
	public void setProxyUsername(String proxyUsername) {
		session.setProxyUsername(proxyUsername);
	}
	public void setProxyPassword(String proxyPassword) {
		session.setProxyPassword(proxyPassword);
	}

	public void setFtpTypeDescription(String ftpTypeDescription) {
		session.setFtpTypeDescription(ftpTypeDescription);
	}
	public void setFileType(String fileType) {
		session.setFileType(fileType);
	}
	public void setMessageIsContent(boolean messageIsContent) {
		session.setMessageIsContent(messageIsContent);
	}
	public void setPassive(boolean b) {
		session.setPassive(b);
	}


	public void setProxyTransportType(int proxyTransportType) {
		session.setProxyTransportType(proxyTransportType);
	}
	public void setPrefCSEncryption(String prefCSEncryption) {
		session.setPrefCSEncryption(prefCSEncryption);
	}
	public void setPrefSCEncryption(String prefSCEncryption) {
		session.setPrefSCEncryption(prefSCEncryption);
	}

	public void setPrivateKeyFilePath(String privateKeyFilePath) {
		session.setPrivateKeyFilePath(privateKeyFilePath);
	}
	public void setPrivateKeyAuthAlias(String privateKeyAuthAlias) {
		session.setPrivateKeyAuthAlias(privateKeyAuthAlias);
	}
	public void setPrivateKeyPassword(String passPhrase) {
		session.setPrivateKeyPassword(passPhrase);
	}
	public void setKnownHostsPath(String knownHostsPath) {
		session.setKnownHostsPath(knownHostsPath);
	}
	public void setConsoleKnownHostsVerifier(boolean verifier) {
		session.setConsoleKnownHostsVerifier(verifier);
	}


	public void setCertificate(String certificate) {
		session.setCertificate(certificate);
	}
	public String getCertificate() {
		return session.getCertificate();
	}
	public void setCertificateType(String keystoreType) {
		session.setCertificateType(keystoreType);
	}
	public String getCertificateType() {
		return session.getCertificateType();
	}
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		session.setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	public void setCertificateAuthAlias(String certificateAuthAlias) {
		session.setCertificateAuthAlias(certificateAuthAlias);
	}
	public String getCertificateAuthAlias() {
		return session.getCertificateAuthAlias();
	}
	public void setCertificatePassword(String certificatePassword) {
		session.setCertificatePassword(certificatePassword);
	}
	public String getCertificatePassword() {
		return session.getCertificatePassword();
	}


	public void setTruststore(String truststore) {
		session.setTruststore(truststore);
	}
	public void setTruststoreType(String truststoreType) {
		session.setTruststoreType(truststoreType);
	}
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		session.setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		session.setTruststoreAuthAlias(truststoreAuthAlias);
	}
	public void setTruststorePassword(String truststorePassword) {
		session.setTruststorePassword(truststorePassword);
	}

	public void setJdk13Compatibility(boolean jdk13Compatibility) {
		session.setJdk13Compatibility(jdk13Compatibility);
	}
	public void setVerifyHostname(boolean verifyHostname) {
		session.setVerifyHostname(verifyHostname);
	}
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		session.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}
	public void setProtP(boolean protP) {
		session.setProtP(protP);
	}
	public void setKeyboardInteractive(boolean keyboardInteractive) {
		session.setKeyboardInteractive(keyboardInteractive);
	}
}