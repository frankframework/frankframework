/*
   Copyright 2013 Nationale-Nederlanden

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
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;

/**
 * Pipe for retreiving files via (s)ftp. The path of the created local file is returned.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.ftp.FtpFileRetrieverPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLocalFilenamePattern(String) localFilenamePattern}</td><td>pattern (in MessageFormat) of the local filename</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLocalDirectory(String) localDirectory}</td><td>local directory in which files have to be downloaded</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteDirectory(String) remoteDirectory}</td><td>remote directory</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDeleteAfterGet(boolean) deleteAfterGet}</td><td>if true, the remote file is deleted after it is retrieved</td><td>false</td></tr>
 * 
 * <tr><td>{@link #setHost(String) host}</td><td>name or ip adres of remote host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPort(int) port}</td><td>portnumber of remote host</td><td>21</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>name of the alias to obtain credentials to authenticatie on remote server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>name of the user to authenticatie on remote server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>name of the password to authenticatie on remote server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyHost(String) proxyHost}</td><td>proxy host name</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPort(int) proxyPort}</td><td>proxy port</td><td>1080</td></tr>
 * <tr><td>{@link #setProxyAuthAlias(String) proxyAuthAlias}</td><td>name of the alias to obtain credentials to authenticate on proxy</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyUsername(String) proxyUsername}</td><td>default user name in case proxy requires authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPassword(String) proxyPassword}</td><td>default password in case proxy requires authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFtpTypeDescription(String) ftpTypeDescription}</td><td>One of FTP, SFTP, FTPS(I) or FTPSI, FTPSX(SSL), FTPSX(TLS)</td><td>FTP</td></tr>
 * <tr><td>{@link #setFileType(String) fileType}</td><td>File type, one of ASCII, BINARY</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageIsContent(boolean) messageIsContent}</td><td>if true, the contents of the message is send, otherwise it message contains the local filenames of the files to be send</td><td>false</td></tr>
 * <tr><td>{@link #setPassive(boolean) passive}</td><td>if true, passive FTP is used: before data is sent, a PASV command is issued, and the connection is set up by the server</td><td>true</td></tr>
 * <tr><td>{@link #setProxyTransportType(int) ProxyTransportType}</td><td>(SFTP) transport type in case of sftp (1=standard, 2=http, 3=socks4, 4=socks5)</td><td>4</td></tr>
 * <tr><td>{@link #setPrefCSEncryption(String) prefCSEncryption}</td><td>(SFTP) Optional preferred encryption from client to server for sftp protocol</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefSCEncryption(String) prefSCEncryption}</td><td>(SFTP) Optional preferred encryption from server to client for sftp protocol</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrivateKeyFilePath(String) privateKeyFilePath}</td><td>(SFTP) Path to private key file for SFTP authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrivateKeyAuthAlias(String) privateKeyAuthAlias}</td><td>(SFTP) name of the alias to obtain credentials for Passphrase of private key file</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrivateKeyPassword(String) privateKeyPassword}</td><td>(SFTP) Passphrase of private key file</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKnownHostsPath(String) knownHostsPath}</td><td>(SFTP) path to file with knownhosts</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setConsoleKnownHostsVerifier(boolean) consoleKnownHostsVerifier}</td><td>(SFTP) &nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setCertificate(String) certificate}</td><td>(FTPS) resource URL to certificate to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificateType(String) certificateType}</td><td>(FTPS) &nbsp;</td><td>pkcs12</td></tr>
 * <tr><td>{@link #setKeyManagerAlgorithm(String) keyManagerAlgorithm}</td><td>selects the algorithm to generate keymanagers. Can be left empty to use the servers default algorithm</td><td>WebSphere: IbmX509</td></tr>
 * <tr><td>{@link #setCertificateAuthAlias(String) certificateAuthAlias}</td><td>(FTPS) alias used to obtain certificate password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificatePassword(String) certificatePassword}</td><td>(FTPS) &nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststore(String) truststore}</td><td>(FTPS) resource URL to truststore to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreType(String) truststoreType}</td><td>(FTPS) &nbsp;</td><td>jks</td></tr>
 * <tr><td>{@link #setTrustManagerAlgorithm(String) trustManagerAlgorithm}</td><td>selects the algorithm to generate trustmanagers. Can be left empty to use the servers default algorithm</td><td>WebSphere: IbmX509</td></tr>
 * <tr><td>{@link #setTruststoreAuthAlias(String) truststoreAuthAlias}</td><td>(FTPS) alias used to obtain truststore password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststorePassword(String) truststorePassword}</td><td>(FTPS) &nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJdk13Compatibility(boolean) jdk13Compatibility}</td><td>(FTPS) enables the use of certificates on JDK 1.3.x. The SUN reference implementation JSSE 1.0.3 is included for convenience</td><td>false</td></tr>
 * <tr><td>{@link #setVerifyHostname(boolean) verifyHostname}</td><td>(FTPS) when true, the hostname in the certificate will be checked against the actual hostname</td><td>true</td></tr>
 * <tr><td>{@link #setAllowSelfSignedCertificates(boolean) allowSelfSignedCertificates}</td><td>(FTPS) if true, the server certificate can be self signed</td><td>false</td></tr>
 * <tr><td>{@link #setProtP(boolean) protP}</td><td>(FTPS) if true, the server returns data via another socket</td><td>false</td></tr>
 * <tr><td>{@link #setKeyboardInteractive(boolean) keyboardInteractive}</td><td>when true, KeyboardInteractive is used to login</td><td>false</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when a file has been retrieved</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, and otherwise under same condition as "success"</td></tr>
 * <tr><td>"exception"</td><td>an exception was thrown retrieving the file. The result passed to the next pipe is the input of the pipe</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 * @since   4.4
 */
public class FtpFileRetrieverPipe extends FixedForwardPipe {

	private FtpSession ftpSession;

	private final static String EXCEPTIONFORWARD = "exception";
	
	private String localFilenamePattern=null;
	private String localDirectory=null;;
	private String remoteDirectory=null;
	private boolean deleteAfterGet=false;
	

	public FtpFileRetrieverPipe() {
		ftpSession = new FtpSession();
	}
	
	public void configure() throws ConfigurationException {
		super.configure();
//		PipeForward exceptionForward = findForward(EXCEPTIONFORWARD);
//		if (exceptionForward==null) {
//			throw new ConfigurationException(getLogPrefix(null)+"must specify forward ["+EXCEPTIONFORWARD+"]"); 
//		}
		ftpSession.configure();
	}
	
	public void stop() {
		super.stop();
		try {		
			ftpSession.closeClient();
		} catch(Exception e) {
			log.warn(getLogPrefix(null)+"exception closing ftpSession",e);
		}
	}
 
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		String orgFilename = (String)input;
		try {
			boolean close = ! deleteAfterGet;
			String localFilename = ftpSession.get(getParameterList(), session, localDirectory, remoteDirectory, orgFilename, localFilenamePattern, close);
			if (deleteAfterGet) {
				ftpSession.deleteRemote(remoteDirectory, orgFilename, true);
			} 
			return new PipeRunResult(getForward(), localFilename);
		}
		catch(Exception e) {
			String msg="Error while getting file [" + remoteDirectory + "/" + input+"]";
			PipeForward exceptionForward = findForward(EXCEPTIONFORWARD);
			if (exceptionForward!=null) {
				log.warn(msg, e);
				return new PipeRunResult(exceptionForward, input);
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


	public void setLocalFilenamePattern(String string) {
		localFilenamePattern = string;
	}
	public String getLocalFilenamePattern() {
		return localFilenamePattern;
	}

	public void setLocalDirectory(String string) {
		localDirectory = string;
	}
	public String getLocalDirectory() {
		return localDirectory;
	}

	public void setRemoteDirectory(String string) {
		remoteDirectory = string;
	}
	public String getRemoteDirectory() {
		return remoteDirectory;
	}


	public void setDeleteAfterGet(boolean b) {
		deleteAfterGet = b;
	}
	public boolean isDeleteAfterGet() {
		return deleteAfterGet;
	}




	public void setHost(String host) {
		ftpSession.setHost(host);
	}
	public void setPort(int port) {
		ftpSession.setPort(port);
	}

	public void setAuthAlias(String alias) {
		ftpSession.setAuthAlias(alias);
	}
	public void setUsername(String username) {
		ftpSession.setUsername(username);
	}
	public void setPassword(String passwd) {
		ftpSession.setPassword(passwd);
	}

	public void setProxyHost(String proxyHost) {
		ftpSession.setProxyHost(proxyHost);
	}
	public void setProxyPort(int proxyPort) {
		ftpSession.setProxyPort(proxyPort);
	}
	public void setProxyAuthAlias(String proxyAuthAlias) {
		ftpSession.setProxyAuthAlias(proxyAuthAlias);
	}
	public void setProxyUsername(String proxyUsername) {
		ftpSession.setProxyUsername(proxyUsername);
	}
	public void setProxyPassword(String proxyPassword) {
		ftpSession.setProxyPassword(proxyPassword);
	}

	public void setFtpTypeDescription(String ftpTypeDescription) {
		ftpSession.setFtpTypeDescription(ftpTypeDescription);
	}
	public void setFileType(String fileType) {
		ftpSession.setFileType(fileType);
	}
	public void setMessageIsContent(boolean messageIsContent) {
		ftpSession.setMessageIsContent(messageIsContent);
	}
	public void setPassive(boolean b) {
		ftpSession.setPassive(b);
	}


	public void setProxyTransportType(int proxyTransportType) {
		ftpSession.setProxyTransportType(proxyTransportType);
	}
	public void setPrefCSEncryption(String prefCSEncryption) {
		ftpSession.setPrefCSEncryption(prefCSEncryption);
	}
	public void setPrefSCEncryption(String prefSCEncryption) {
		ftpSession.setPrefSCEncryption(prefSCEncryption);
	}

	public void setPrivateKeyFilePath(String privateKeyFilePath) {
		ftpSession.setPrivateKeyFilePath(privateKeyFilePath);
	}
	public void setPrivateKeyAuthAlias(String privateKeyAuthAlias) {
		ftpSession.setPrivateKeyAuthAlias(privateKeyAuthAlias);
	}
	public void setPrivateKeyPassword(String passPhrase) {
		ftpSession.setPrivateKeyPassword(passPhrase);
	}
	public void setKnownHostsPath(String knownHostsPath) {
		ftpSession.setKnownHostsPath(knownHostsPath);
	}
	public void setConsoleKnownHostsVerifier(boolean verifier) {
		ftpSession.setConsoleKnownHostsVerifier(verifier);
	}


	public void setCertificate(String certificate) {
		ftpSession.setCertificate(certificate);
	}
	public void setCertificateType(String keystoreType) {
		ftpSession.setCertificateType(keystoreType);
	}
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		ftpSession.setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	public void setCertificateAuthAlias(String certificateAuthAlias) {
		ftpSession.setCertificateAuthAlias(certificateAuthAlias);
	}
	public void setCertificatePassword(String certificatePassword) {
		ftpSession.setCertificatePassword(certificatePassword);
	}


	public void setTruststore(String truststore) {
		ftpSession.setTruststore(truststore);
	}
	public void setTruststoreType(String truststoreType) {
		ftpSession.setTruststoreType(truststoreType);
	}
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		ftpSession.setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		ftpSession.setTruststoreAuthAlias(truststoreAuthAlias);
	}
	public void setTruststorePassword(String truststorePassword) {
		ftpSession.setTruststorePassword(truststorePassword);
	}

	public void setJdk13Compatibility(boolean jdk13Compatibility) {
		ftpSession.setJdk13Compatibility(jdk13Compatibility);
	}
	public void setVerifyHostname(boolean verifyHostname) {
		ftpSession.setVerifyHostname(verifyHostname);
	}
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		ftpSession.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}
	public void setProtP(boolean protP) {
		ftpSession.setProtP(protP);
	}
	public void setKeyboardInteractive(boolean keyboardInteractive) {
		ftpSession.setKeyboardInteractive(keyboardInteractive);
	}

}
