/*
 * $Log: FtpSender.java,v $
 * Revision 1.16  2010-03-19 07:22:32  m168309
 * default port for FTP is 21 instead of 22 (which is for SFTP)
 *
 * Revision 1.15  2010/03/10 14:30:06  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * rolled back testtool adjustments (IbisDebuggerDummy)
 *
 * Revision 1.13  2007/12/28 12:15:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getPassword
 *
 * Revision 1.11  2006/01/05 14:17:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.10  2005/12/19 17:22:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected typos in javadoc
 *
 * Revision 1.9  2005/12/19 16:46:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework, lots of changes
 *
 * Revision 1.8  2005/12/07 15:53:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * increased usage of superclass
 *
 * Revision 1.6  2005/11/07 08:21:35  John Dekker <john.dekker@ibissource.org>
 * Enable sftp public/private key authentication
 *
 * Revision 1.5  2005/10/31 14:42:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.4  2005/10/27 07:57:41  John Dekker <john.dekker@ibissource.org>
 * add setRemoteDirectory method
 *
 * Revision 1.3  2005/10/24 09:59:22  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.2  2005/10/17 12:21:21  John Dekker <john.dekker@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.1  2005/10/11 13:03:29  John Dekker <john.dekker@ibissource.org>
 * Supports retrieving files (FtpFileRetrieverPipe) and sending files (FtpSender)
 * via one of the FTP protocols (ftp, sftp, ftps both implicit as explicit).
 *
 */
package nl.nn.adapterframework.ftp;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * FTP client voor het versturen van files via FTP.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.ftp.FtpSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteDirectory(String) directory}</td><td>remote directory in which files have to be uploaded</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteFilenamePattern(String) filenamePattern}</td><td>filename pattern for uploaded files</td><td>&nbsp;</td></tr>
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
 * <tr><td>{@link #setCertificateAuthAlias(String) certificateAuthAlias}</td><td>(FTPS) alias used to obtain certificate password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificatePassword(String) certificatePassword}</td><td>(FTPS) &nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststore(String) truststore}</td><td>(FTPS) resource URL to truststore to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreType(String) truststoreType}</td><td>(FTPS) &nbsp;</td><td>jks</td></tr>
 * <tr><td>{@link #setTruststoreAuthAlias(String) truststoreAuthAlias}</td><td>(FTPS) alias used to obtain truststore password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststorePassword(String) truststorePassword}</td><td>(FTPS) &nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJdk13Compatibility(boolean) jdk13Compatibility}</td><td>(FTPS) enables the use of certificates on JDK 1.3.x. The SUN reference implementation JSSE 1.0.3 is included for convenience</td><td>false</td></tr>
 * <tr><td>{@link #setVerifyHostname(boolean) verifyHostname}</td><td>(FTPS) when true, the hostname in the certificate will be checked against the actual hostname</td><td>true</td></tr>
 * <tr><td>{@link #setAllowSelfSignedCertificates(boolean) allowSelfSignedCertificates}</td><td>(FTPS) if true, the server certificate can be self signed</td><td>false</td></tr>
 * <tr><td>{@link #setProtP(boolean) protP}</td><td>(FTPS) if true, the server returns data via another socket</td><td>false</td></tr>
 * </table>
 * </p>
 *  
 * @author: John Dekker
 */
public class FtpSender extends SenderWithParametersBase {
	public static final String version = "$RCSfile: FtpSender.java,v $  $Revision: 1.16 $ $Date: 2010-03-19 07:22:32 $";

	private FtpSession ftpSession;
	
	private String remoteDirectory;
	private String remoteFilenamePattern=null;
	
	public FtpSender() {
		this.ftpSession = new FtpSession();
	}
	
	public void configure() throws ConfigurationException {
		super.configure();
		ftpSession.configure();
	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		try {
			PipeLineSession session=null;
			if (prc!=null) {
				session=prc.getSession();
			}
			ftpSession.put(paramList, session, message, remoteDirectory, remoteFilenamePattern, true);
		} catch(SenderException e) {
			throw e;
		} catch(Exception e) {
			throw new SenderException("Error during ftp-ing " + message, e);
		}
		return message;
	}
	


	
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}
	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setRemoteFilenamePattern(String string) {
		remoteFilenamePattern = string;
	}
	public String getRemoteFilenamePattern() {
		return remoteFilenamePattern;
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
	public String getCertificate() {
		return ftpSession.getCertificate();
	}
	public void setCertificateType(String keystoreType) {
		ftpSession.setCertificateType(keystoreType);
	}
	public String getCertificateType() {
		return ftpSession.getCertificateType();
	}
	public void setCertificateAuthAlias(String certificateAuthAlias) {
		ftpSession.setCertificateAuthAlias(certificateAuthAlias);
	}
	public String getCertificateAuthAlias() {
		return ftpSession.getCertificateAuthAlias();
	}
	public void setCertificatePassword(String certificatePassword) {
		ftpSession.setCertificatePassword(certificatePassword);
	}
	public String getCertificatePassword() {
		return ftpSession.getCertificatePassword();
	}


	public void setTruststore(String truststore) {
		ftpSession.setTruststore(truststore);
	}
	public void setTruststoreType(String truststoreType) {
		ftpSession.setTruststoreType(truststoreType);
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
	

}
