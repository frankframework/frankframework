/*
   Copyright 2013 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.encryption.HasKeystore;
import nl.nn.adapterframework.encryption.HasTruststore;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.ftp.FtpSession.FileType;
import nl.nn.adapterframework.ftp.FtpSession.FtpType;
import nl.nn.adapterframework.ftp.FtpSession.Prot;
import nl.nn.adapterframework.ftp.FtpSession.TransportType;
import nl.nn.adapterframework.senders.SenderWithParametersBase;
import nl.nn.adapterframework.stream.Message;

/**
 * FTP client voor het versturen van files via FTP.
 *
 *
 * @author John Dekker
 */
@Deprecated
@ConfigurationWarning("Please replace with FtpFileSystemSender")
public class FtpSender extends SenderWithParametersBase implements HasKeystore, HasTruststore {

	private FtpSession ftpSession;

	private String remoteDirectory;
	private String remoteFilenamePattern = null;

	public FtpSender() {
		this.ftpSession = new FtpSession();
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ftpSession.configure();
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		try {
			ftpSession.put(paramList, session, message.asString(), remoteDirectory, remoteFilenamePattern, true);
		} catch(SenderException e) {
			throw e;
		} catch(Exception e) {
			throw new SenderException("Error during ftp-ing " + message, e);
		}
		return new SenderResult(message);
	}

	/** remote directory in which files have to be uploaded */
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}
	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	/** filename pattern for uploaded files */
	public void setRemoteFilenamePattern(String string) {
		remoteFilenamePattern = string;
	}
	public String getRemoteFilenamePattern() {
		return remoteFilenamePattern;
	}

	/** name or ip adres of remote host */
	public void setHost(String host) {
		ftpSession.setHost(host);
	}

	/**
	 * portnumber of remote host
	 * @ff.default 21
	 */
	public void setPort(int port) {
		ftpSession.setPort(port);
	}

	/** name of the alias to obtain credentials to authenticatie on remote server */
	public void setAuthAlias(String alias) {
		ftpSession.setAuthAlias(alias);
	}

	/** name of the user to authenticatie on remote server */
	public void setUsername(String username) {
		ftpSession.setUsername(username);
	}

	/** name of the password to authenticatie on remote server */
	public void setPassword(String passwd) {
		ftpSession.setPassword(passwd);
	}

	/** proxy host name */
	public void setProxyHost(String proxyHost) {
		ftpSession.setProxyHost(proxyHost);
	}

	/**
	 * proxy port
	 * @ff.default 1080
	 */
	public void setProxyPort(int proxyPort) {
		ftpSession.setProxyPort(proxyPort);
	}

	/** name of the alias to obtain credentials to authenticate on proxy */
	public void setProxyAuthAlias(String proxyAuthAlias) {
		ftpSession.setProxyAuthAlias(proxyAuthAlias);
	}

	/** default user name in case proxy requires authentication */
	public void setProxyUsername(String proxyUsername) {
		ftpSession.setProxyUsername(proxyUsername);
	}

	/** default password in case proxy requires authentication */
	public void setProxyPassword(String proxyPassword) {
		ftpSession.setProxyPassword(proxyPassword);
	}

	@Deprecated
	@ConfigurationWarning("use attribute ftpType instead")
	public void setFtpTypeDescription(FtpType value) {
		setFtpType(value);
	}
	public void setFtpType(FtpType string) {
		ftpSession.setFtpType(string);
	}
	public FtpType getFtpType() {
		return ftpSession.getFtpType();
	}

	public void setFileType(FileType fileType) {
		ftpSession.setFileType(fileType);
	}

	/**
	 * if true, the contents of the message is send, otherwise it message contains the local filenames of the files to be send
	 * @ff.default false
	 */
	public void setMessageIsContent(boolean messageIsContent) {
		ftpSession.setMessageIsContent(messageIsContent);
	}

	/**
	 * if true, passive ftp is used: before data is sent, a pasv command is issued, and the connection is set up by the server
	 * @ff.default true
	 */
	public void setPassive(boolean b) {
		ftpSession.setPassive(b);
	}


	/**
	 * (sftp) transport type in case of sftp
	 */
	public void setProxyTransportType(TransportType proxyTransportType) {
		ftpSession.setProxyTransportType(proxyTransportType);
	}

	/** (sftp) optional preferred encryption from client to server for sftp protocol */
	public void setPrefCSEncryption(String prefCSEncryption) {
		ftpSession.setPrefCSEncryption(prefCSEncryption);
	}

	/** (sftp) optional preferred encryption from server to client for sftp protocol */
	public void setPrefSCEncryption(String prefSCEncryption) {
		ftpSession.setPrefSCEncryption(prefSCEncryption);
	}

	/** (sftp) path to private key file for sftp authentication */
	public void setPrivateKeyFilePath(String privateKeyFilePath) {
		ftpSession.setPrivateKeyFilePath(privateKeyFilePath);
	}

	/** (sftp) name of the alias to obtain credentials for passphrase of private key file */
	public void setPrivateKeyAuthAlias(String privateKeyAuthAlias) {
		ftpSession.setPrivateKeyAuthAlias(privateKeyAuthAlias);
	}

	/** (sftp) passphrase of private key file */
	public void setPrivateKeyPassword(String passPhrase) {
		ftpSession.setPrivateKeyPassword(passPhrase);
	}

	/** (sftp) path to file with knownhosts */
	public void setKnownHostsPath(String knownHostsPath) {
		ftpSession.setKnownHostsPath(knownHostsPath);
	}

	/**
	 * (sftp) Verify the hosts againt the knownhosts file.
	 * @ff.default true
	 */
	public void setStrictHostKeyChecking(boolean verifier) {
		ftpSession.setStrictHostKeyChecking(verifier);
	}


	@Override
	/** (ftps) resource url to certificate to be used for authentication */
	public void setKeystore(String certificate) {
		ftpSession.setKeystore(certificate);
	}
	@Override
	public String getKeystore() {
		return ftpSession.getKeystore();
	}

	@Override
	/**
	 * (ftps) 
	 * @ff.default pkcs12
	 */
	public void setKeystoreType(KeystoreType keystoreType) {
		ftpSession.setKeystoreType(keystoreType);
	}
	@Override
	public KeystoreType getKeystoreType() {
		return ftpSession.getKeystoreType();
	}

	/**
	 * selects the algorithm to generate keymanagers. can be left empty to use the servers default algorithm
	 * @ff.default websphere: ibmx509
	 */
	@Override
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		ftpSession.setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	@Override
	public String getKeyManagerAlgorithm() {
		return ftpSession.getKeyManagerAlgorithm();
	}

	@Override
	/** (ftps) alias used to obtain certificate password */
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		ftpSession.setKeystoreAuthAlias(keystoreAuthAlias);
	}
	@Override
	public String getKeystoreAuthAlias() {
		return ftpSession.getKeystoreAuthAlias();
	}

	@Override
	/**
	 * (ftps) 
	 * @ff.default  
	 */
	public void setKeystorePassword(String keystorePassword) {
		ftpSession.setKeystorePassword(keystorePassword);
	}
	@Override
	public String getKeystorePassword() {
		return ftpSession.getKeystorePassword();
	}

	@Override
	/** (ftps) resource url to certificate to be used for authentication */
	public void setKeystoreAlias(String alias) {
		ftpSession.setKeystore(alias);
	}
	@Override
	public String getKeystoreAlias() {
		return ftpSession.getKeystoreAlias();
	}
	@Override
	/** (ftps) alias used to obtain certificate password */
	public void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		ftpSession.setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}
	@Override
	public String getKeystoreAliasAuthAlias() {
		return ftpSession.getKeystoreAliasAuthAlias();
	}

	@Override
	/**
	 * (ftps) 
	 * @ff.default  
	 */
	public void setKeystoreAliasPassword(String keystoreAliasPassword) {
		ftpSession.setKeystoreAliasPassword(keystoreAliasPassword);
	}
	@Override
	public String getKeystoreAliasPassword() {
		return ftpSession.getKeystoreAliasPassword();
	}



	/** (ftps) resource url to truststore to be used for authentication */
	@Override
	public void setTruststore(String truststore) {
		ftpSession.setTruststore(truststore);
	}
	@Override
	public String getTruststore() {
		return ftpSession.getTruststore();
	}

	/**
	 * (ftps) 
	 * @ff.default jks
	 */
	@Override
	public void setTruststoreType(KeystoreType truststoreType) {
		ftpSession.setTruststoreType(truststoreType);
	}
	@Override
	public KeystoreType getTruststoreType() {
		return ftpSession.getTruststoreType();
	}


	/**
	 * selects the algorithm to generate trustmanagers. can be left empty to use the servers default algorithm
	 * @ff.default websphere: ibmx509
	 */
	@Override
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		ftpSession.setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	@Override
	public String getTrustManagerAlgorithm() {
		return ftpSession.getTrustManagerAlgorithm();
	}


	/** (ftps) alias used to obtain truststore password */
	@Override
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		ftpSession.setTruststoreAuthAlias(truststoreAuthAlias);
	}
	@Override
	public String getTruststoreAuthAlias() {
		return ftpSession.getTruststoreAuthAlias();
	}

	/**
	 * (ftps) 
	 * @ff.default  
	 */
	@Override
	public void setTruststorePassword(String truststorePassword) {
		ftpSession.setTruststorePassword(truststorePassword);
	}
	@Override
	public String getTruststorePassword() {
		return ftpSession.getTruststorePassword();
	}

	/**
	 * (ftps) when true, the hostname in the certificate will be checked against the actual hostname
	 * @ff.default true
	 */
	@Override
	public void setVerifyHostname(boolean verifyHostname) {
		ftpSession.setVerifyHostname(verifyHostname);
	}
	@Override
	public boolean isVerifyHostname() {
		return ftpSession.isVerifyHostname();
	}

	/**
	 * (ftps) if true, the server certificate can be self signed
	 * @ff.default false
	 */
	@Override
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		ftpSession.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}
	@Override
	public boolean isAllowSelfSignedCertificates() {
		return ftpSession.isAllowSelfSignedCertificates();
	}

	@Override
	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		ftpSession.setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}
	@Override
	public boolean isIgnoreCertificateExpiredException() {
		return ftpSession.isIgnoreCertificateExpiredException();
	}

	/**
	 * (ftps) if true, the server returns data via a SSL socket
	 * @ff.default false
	 */
	@Deprecated
	@ConfigurationWarning("use attribute prot=\"P\" instead")
	public void setProtP(boolean b) {
		ftpSession.setProt(Prot.P);
	}

	/**
	 * <ul>
	 * <li>C - Clear</li>
	 * <li>S - Safe(SSL protocol only)</li>
	 * <li>E - Confidential(SSL protocol only)</li>
	 * <li>P - Private</li>
	 * </ul>
	 *
	 */
	/**
	 * Sets the <code>Data Channel Protection Level</code>.
	 * @ff.default C
	 */
	public void setProt(Prot prot) {
		ftpSession.setProt(prot);
	}
	public Prot getProt() {
		return ftpSession.getProt();
	}

	/**
	 * when true, keyboardinteractive is used to login
	 * @ff.default false
	 */
	public void setKeyboardInteractive(boolean keyboardInteractive) {
		ftpSession.setKeyboardInteractive(keyboardInteractive);
	}
}
