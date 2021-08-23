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
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.FtpFileSystem;
import nl.nn.adapterframework.ftp.FtpSession.FtpType;
import nl.nn.adapterframework.ftp.FtpSession.Prot;

public class FtpFileSystemSender extends FileSystemSender<FTPFile, FtpFileSystem> {

	public FtpFileSystemSender() {
		setFileSystem(new FtpFileSystem());
	}

	@IbisDoc({"pathname of The file or directory to list.", "Home folder of the ftp user."})
	public void setRemoteDirectory(String remoteDirectory) {
		getFileSystem().setRemoteDirectory(remoteDirectory);
	}

	@IbisDoc({"Name or ip address of remote host", ""})
	public void setHost(String host) {
		getFileSystem().setHost(host);
	}

	@IbisDoc({"Port number of remote host", "21"})
	public void setPort(int port) {
		getFileSystem().setPort(port);
	}

	@IbisDoc({"Name of the alias to obtain credentials to authenticate on remote server", ""})
	public void setAuthAlias(String alias) {
		getFileSystem().setAuthAlias(alias);
	}

	@IbisDoc({"Name of the user to authenticate on remote server", ""})
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@IbisDoc({"Password of the user to authenticate on remote server", ""})
	public void setPassword(String passwd) {
		getFileSystem().setPassword(passwd);
	}

	@IbisDoc({"Proxy host name", ""})
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@IbisDoc({"Proxy port", "1080"})
	public void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@IbisDoc({"Name of the alias to obtain credentials to authenticate on proxy", ""})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	@IbisDoc({"Default user name in case proxy requires authentication", ""})
	public void setProxyUsername(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}

	@IbisDoc({"Default password in case proxy requires authentication", ""})
	public void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	@IbisDoc({"One of ftp, sftp, ftps(i) or ftpsi, ftpsx(ssl), ftpsx(tls)", "ftp"})
	@Deprecated
	@ConfigurationWarning("use attribute ftpType instead")
	public void setFtpTypeDescription(String ftpTypeDescription) {
		getFileSystem().setFtpTypeDescription(ftpTypeDescription);
	}
	@IbisDoc({"FTP protocol to use", "ftp"})
	public void setFtpType(String value) {
		getFileSystem().setFtpType(value);
	}
	public FtpType getFtpTypeEnum() {
		return getFileSystem().getFtpTypeEnum();
	}

	@IbisDoc({"File type, one of ascii, binary", ""})
	public void setFileType(String fileType) {
		getFileSystem().setFileType(fileType);
	}

	@IbisDoc({"If true, the contents of the message is send, otherwise it message contains the local filenames of the files to be send", "false"})
	public void setMessageIsContent(boolean messageIsContent) {
		getFileSystem().setMessageIsContent(messageIsContent);
	}

	@IbisDoc({"If true, passive ftp is used: before data is sent, a pasv command is issued, and the connection is set up by the server", "true"})
	public void setPassive(boolean b) {
		getFileSystem().setPassive(b);
	}

	@IbisDoc({"(sftp) transport type in case of sftp (1=standard, 2=http, 3=socks4, 4=socks5)", "4"})
	public void setProxyTransportType(int proxyTransportType) {
		getFileSystem().setProxyTransportType(proxyTransportType);
	}

	@IbisDoc({"(sftp) optional preferred encryption from client to server for sftp protocol", ""})
	public void setPrefCSEncryption(String prefCSEncryption) {
		getFileSystem().setPrefCSEncryption(prefCSEncryption);
	}

	@IbisDoc({"(sftp) optional preferred encryption from server to client for sftp protocol", ""})
	public void setPrefSCEncryption(String prefSCEncryption) {
		getFileSystem().setPrefSCEncryption(prefSCEncryption);
	}

	@IbisDoc({"(sftp) path to private key file for sftp authentication", ""})
	public void setPrivateKeyFilePath(String privateKeyFilePath) {
		getFileSystem().setPrivateKeyFilePath(privateKeyFilePath);
	}

	@IbisDoc({"(sftp) name of the alias to obtain credentials for passphrase of private key file", ""})
	public void setPrivateKeyAuthAlias(String privateKeyAuthAlias) {
		getFileSystem().setPrivateKeyAuthAlias(privateKeyAuthAlias);
	}

	@IbisDoc({"(sftp) passphrase of private key file", ""})
	public void setPrivateKeyPassword(String passPhrase) {
		getFileSystem().setPrivateKeyPassword(passPhrase);
	}

	@IbisDoc({"(sftp) path to file with knownhosts", ""})
	public void setKnownHostsPath(String knownHostsPath) {
		getFileSystem().setKnownHostsPath(knownHostsPath);
	}

	@IbisDoc({"(sftp) ", "false"})
	public void setConsoleKnownHostsVerifier(boolean verifier) {
		getFileSystem().setConsoleKnownHostsVerifier(verifier);
	}

	@IbisDoc({"(ftps) resource url to certificate to be used for authentication", ""})
	public void setCertificate(String certificate) {
		getFileSystem().setCertificate(certificate);
	}

	@IbisDoc({"(ftps) ", "pkcs12"})
	public void setCertificateType(String keystoreType) {
		getFileSystem().setCertificateType(keystoreType);
	}

	@IbisDoc({"(ftps) alias used to obtain certificate password", ""})
	public void setCertificateAuthAlias(String certificateAuthAlias) {
		getFileSystem().setCertificateAuthAlias(certificateAuthAlias);
	}

	@IbisDoc({"(ftps) ", " "})
	public void setCertificatePassword(String certificatePassword) {
		getFileSystem().setCertificatePassword(certificatePassword);
	}

	@IbisDoc({"Selects the algorithm to generate keymanagers. can be left empty to use the servers default algorithm", "websphere: ibmx509"})
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		getFileSystem().setKeyManagerAlgorithm(keyManagerAlgorithm);
	}

	@IbisDoc({"(ftps) resource url to truststore to be used for authentication", ""})
	public void setTruststore(String truststore) {
		getFileSystem().setTruststore(truststore);
	}

	@IbisDoc({"(ftps) ", "jks"})
	public void setTruststoreType(String truststoreType) {
		getFileSystem().setTruststoreType(truststoreType);
	}

	@IbisDoc({"(ftps) alias used to obtain truststore password", ""})
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		getFileSystem().setTruststoreAuthAlias(truststoreAuthAlias);
	}

	@IbisDoc({"(ftps) ", " "})
	public void setTruststorePassword(String truststorePassword) {
		getFileSystem().setTruststorePassword(truststorePassword);
	}

	@IbisDoc({"Selects the algorithm to generate trustmanagers. can be left empty to use the servers default algorithm", "websphere: ibmx509"})
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		getFileSystem().setTrustManagerAlgorithm(trustManagerAlgorithm);
	}

	@IbisDoc({"(ftps) when true, the hostname in the certificate will be checked against the actual hostname", "true"})
	public void setVerifyHostname(boolean verifyHostname) {
		getFileSystem().setVerifyHostname(verifyHostname);
	}

	@IbisDoc({"(ftps) if true, the server certificate can be self signed", "false"})
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		getFileSystem().setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}

	@IbisDoc({"(ftps) if true, the server returns data via another socket", "false"})
	@Deprecated
	@ConfigurationWarning("use attribute prot=\"P\" instead")
	public void setProtP(boolean protP) {
		getFileSystem().setProtP(protP);
	}

	@IbisDoc({"Sets the <code>Data Channel Protection Level</code>", "C"})
	public void setProt(String prot) {
		getFileSystem().setProt(prot);
	}
	public Prot getProtEnum() {
		return getFileSystem().getProtEnum();
	}

	@IbisDoc({"When true, keyboardinteractive is used to login", "false"})
	public void setKeyboardInteractive(boolean keyboardInteractive) {
		getFileSystem().setKeyboardInteractive(keyboardInteractive);
	}
}