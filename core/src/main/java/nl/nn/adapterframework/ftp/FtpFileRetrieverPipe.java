/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.IOException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.ftp.FtpSession.FtpType;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;

/**
 * Pipe for retreiving files via (s)ftp. The path of the created local file is returned.
 * 
 * @author John Dekker
 * @since   4.4
 */
@Deprecated
@ConfigurationWarning("Please replace with FtpFileSystemListener")
public class FtpFileRetrieverPipe extends FixedForwardPipe {

	private FtpSession ftpSession;

	private String localFilenamePattern=null;
	private String localDirectory=null;;
	private String remoteDirectory=null;
	private boolean deleteAfterGet=false;
	

	public FtpFileRetrieverPipe() {
		ftpSession = new FtpSession();
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
//		PipeForward exceptionForward = findForward(EXCEPTIONFORWARD);
//		if (exceptionForward==null) {
//			throw new ConfigurationException(getLogPrefix(null)+"must specify forward ["+EXCEPTIONFORWARD+"]"); 
//		}
		ftpSession.configure();
	}
	
	@Override
	public void stop() {
		super.stop();
		try {		
			ftpSession.closeClient();
		} catch(Exception e) {
			log.warn(getLogPrefix(null)+"exception closing ftpSession",e);
		}
	}
 
	/** 
* @see nl.nn.adapterframework.core.IPipe#doPipe(Message, PipeLineSession)
	 */
	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String orgFilename;
		try {
			orgFilename = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
		}
		try {
			boolean close = ! deleteAfterGet;
			String localFilename = ftpSession.get(getParameterList(), session, localDirectory, remoteDirectory, orgFilename, localFilenamePattern, close);
			if (deleteAfterGet) {
				ftpSession.deleteRemote(remoteDirectory, orgFilename, true);
			} 
			return new PipeRunResult(getSuccessForward(), localFilename);
		}
		catch(Exception e) {
			String msg="Error while getting file [" + remoteDirectory + "/" + orgFilename+"]";
			PipeForward exceptionForward = findForward(PipeForward.EXCEPTION_FORWARD_NAME);
			if (exceptionForward!=null) {
				log.warn(msg, e);
				return new PipeRunResult(exceptionForward, message);
			}
			throw new PipeRunException(this, msg, e);
		}
	}



	public void setFtpSession(FtpSession session) {
		ftpSession = session;
	}
	public FtpSession getFtpSession() {
		return ftpSession;
	}


	@IbisDoc({"pattern (in messageformat) of the local filename", ""})
	public void setLocalFilenamePattern(String string) {
		localFilenamePattern = string;
	}
	public String getLocalFilenamePattern() {
		return localFilenamePattern;
	}

	@IbisDoc({"local directory in which files have to be downloaded", ""})
	public void setLocalDirectory(String string) {
		localDirectory = string;
	}
	public String getLocalDirectory() {
		return localDirectory;
	}

	@IbisDoc({"remote directory", ""})
	public void setRemoteDirectory(String string) {
		remoteDirectory = string;
	}
	public String getRemoteDirectory() {
		return remoteDirectory;
	}


	@IbisDoc({"if true, the remote file is deleted after it is retrieved", "false"})
	public void setDeleteAfterGet(boolean b) {
		deleteAfterGet = b;
	}
	public boolean isDeleteAfterGet() {
		return deleteAfterGet;
	}




	@IbisDoc({"name or ip adres of remote host", ""})
	public void setHost(String host) {
		ftpSession.setHost(host);
	}

	@IbisDoc({"portnumber of remote host", "21"})
	public void setPort(int port) {
		ftpSession.setPort(port);
	}

	@IbisDoc({"name of the alias to obtain credentials to authenticatie on remote server", ""})
	public void setAuthAlias(String alias) {
		ftpSession.setAuthAlias(alias);
	}

	@IbisDoc({"name of the user to authenticatie on remote server", ""})
	public void setUsername(String username) {
		ftpSession.setUsername(username);
	}

	@IbisDoc({"name of the password to authenticatie on remote server", ""})
	public void setPassword(String passwd) {
		ftpSession.setPassword(passwd);
	}

	@IbisDoc({"proxy host name", ""})
	public void setProxyHost(String proxyHost) {
		ftpSession.setProxyHost(proxyHost);
	}

	@IbisDoc({"proxy port", "1080"})
	public void setProxyPort(int proxyPort) {
		ftpSession.setProxyPort(proxyPort);
	}

	@IbisDoc({"name of the alias to obtain credentials to authenticate on proxy", ""})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		ftpSession.setProxyAuthAlias(proxyAuthAlias);
	}

	@IbisDoc({"default user name in case proxy requires authentication", ""})
	public void setProxyUsername(String proxyUsername) {
		ftpSession.setProxyUsername(proxyUsername);
	}

	@IbisDoc({"default password in case proxy requires authentication", ""})
	public void setProxyPassword(String proxyPassword) {
		ftpSession.setProxyPassword(proxyPassword);
	}

	public void setFtpTypeDescription(FtpType ftpTypeDescription) {
		ftpSession.setFtpTypeDescription(ftpTypeDescription);
	}

	@IbisDoc({"file type, one of ascii, binary", ""})
	public void setFileType(String fileType) {
		ftpSession.setFileType(fileType);
	}

	@IbisDoc({"if true, the contents of the message is send, otherwise it message contains the local filenames of the files to be send", "false"})
	public void setMessageIsContent(boolean messageIsContent) {
		ftpSession.setMessageIsContent(messageIsContent);
	}

	@IbisDoc({"if true, passive ftp is used: before data is sent, a pasv command is issued, and the connection is set up by the server", "true"})
	public void setPassive(boolean b) {
		ftpSession.setPassive(b);
	}


	@IbisDoc({"(sftp) transport type in case of sftp (1=standard, 2=http, 3=socks4, 4=socks5)", "4"})
	public void setProxyTransportType(int proxyTransportType) {
		ftpSession.setProxyTransportType(proxyTransportType);
	}

	@IbisDoc({"(sftp) optional preferred encryption from client to server for sftp protocol", ""})
	public void setPrefCSEncryption(String prefCSEncryption) {
		ftpSession.setPrefCSEncryption(prefCSEncryption);
	}

	@IbisDoc({"(sftp) optional preferred encryption from server to client for sftp protocol", ""})
	public void setPrefSCEncryption(String prefSCEncryption) {
		ftpSession.setPrefSCEncryption(prefSCEncryption);
	}

	@IbisDoc({"(sftp) path to private key file for sftp authentication", ""})
	public void setPrivateKeyFilePath(String privateKeyFilePath) {
		ftpSession.setPrivateKeyFilePath(privateKeyFilePath);
	}

	@IbisDoc({"(sftp) name of the alias to obtain credentials for passphrase of private key file", ""})
	public void setPrivateKeyAuthAlias(String privateKeyAuthAlias) {
		ftpSession.setPrivateKeyAuthAlias(privateKeyAuthAlias);
	}

	@IbisDoc({"(sftp) passphrase of private key file", ""})
	public void setPrivateKeyPassword(String passPhrase) {
		ftpSession.setPrivateKeyPassword(passPhrase);
	}

	@IbisDoc({"(sftp) path to file with knownhosts", ""})
	public void setKnownHostsPath(String knownHostsPath) {
		ftpSession.setKnownHostsPath(knownHostsPath);
	}

	@IbisDoc({"(sftp) ", "false"})
	public void setConsoleKnownHostsVerifier(boolean verifier) {
		ftpSession.setConsoleKnownHostsVerifier(verifier);
	}


	@IbisDoc({"(ftps) resource url to certificate to be used for authentication", ""})
	public void setKeystore(String certificate) {
		ftpSession.setKeystore(certificate);
	}

	@IbisDoc({"(ftps) ", "pkcs12"})
	public void setKeystoreType(KeystoreType keystoreType) {
		ftpSession.setKeystoreType(keystoreType);
	}

	@IbisDoc({"selects the algorithm to generate keymanagers. can be left empty to use the servers default algorithm", "websphere: ibmx509"})
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		ftpSession.setKeyManagerAlgorithm(keyManagerAlgorithm);
	}

	@IbisDoc({"(ftps) alias used to obtain certificate password", ""})
	public void setCertificateAuthAlias(String certificateAuthAlias) {
		ftpSession.setKeystoreAuthAlias(certificateAuthAlias);
	}

	@IbisDoc({"(ftps) ", " "})
	public void setCertificatePassword(String certificatePassword) {
		ftpSession.setKeystorePassword(certificatePassword);
	}


	@IbisDoc({"(ftps) resource url to truststore to be used for authentication", ""})
	public void setTruststore(String truststore) {
		ftpSession.setTruststore(truststore);
	}

	@IbisDoc({"(ftps) ", "jks"})
	public void setTruststoreType(KeystoreType truststoreType) {
		ftpSession.setTruststoreType(truststoreType);
	}

	@IbisDoc({"selects the algorithm to generate trustmanagers. can be left empty to use the servers default algorithm", "websphere: ibmx509"})
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		ftpSession.setTrustManagerAlgorithm(trustManagerAlgorithm);
	}

	@IbisDoc({"(ftps) alias used to obtain truststore password", ""})
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		ftpSession.setTruststoreAuthAlias(truststoreAuthAlias);
	}

	@IbisDoc({"(ftps) ", " "})
	public void setTruststorePassword(String truststorePassword) {
		ftpSession.setTruststorePassword(truststorePassword);
	}

	@IbisDoc({"(ftps) when true, the hostname in the certificate will be checked against the actual hostname", "true"})
	public void setVerifyHostname(boolean verifyHostname) {
		ftpSession.setVerifyHostname(verifyHostname);
	}

	@IbisDoc({"(ftps) if true, the server certificate can be self signed", "false"})
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		ftpSession.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}

	@IbisDoc({"(ftps) if true, the server returns data via another socket", "false"})
	public void setProtP(boolean protP) {
		ftpSession.setProtP(protP);
	}

	@IbisDoc({"when true, keyboardinteractive is used to login", "false"})
	public void setKeyboardInteractive(boolean keyboardInteractive) {
		ftpSession.setKeyboardInteractive(keyboardInteractive);
	}

}
