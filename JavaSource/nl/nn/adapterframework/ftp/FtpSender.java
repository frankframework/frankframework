/*
 * $Log: FtpSender.java,v $
 * Revision 1.7  2005-11-11 12:30:39  europe\l166817
 * Aanpassingen door John Dekker
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
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

import org.apache.commons.lang.StringUtils;

/**
 * FTP client voor het versturen van files via FTP.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.FtpSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setHost(String) host}</td><td>name or ip adres of remote host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPort(int) port}</td><td>portnumber of remote host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(string) username}</td><td>name of the user to authenticatie on remote server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(string) password}</td><td>name of the password to authenticatie on remote server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyTransportType(int) type}</td><td>transport type in case of sftp (1=standard, 2=http, 3=socks4, 4=socks5)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyHost(string) host}</td><td>proxy host name</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPort(int) port}</td><td>proxy port</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyUsername(string) username}</td><td>user name in case proxy requires authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPassword(string) password}</td><td>password in case proxy requires authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFtpTypeDescription(String) ftpTypeDescription}</td><td>One of FTP, SFTP, FTPS(I) or FTPSI, FTPSX(SSL), FTPSX(TLS)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteFilenamePattern(string) filenamePattern}</td><td>remote directory in which files have to be uploaded</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteDirectory(string) directory}</td><td>remote directory in which files have to be uploaded</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileType(string) fileType}</td><td>File type, one of ASCII, BINARY</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAllowSelfSignedCertificates(boolean) allowSelfSignedCertificates}</td><td>if true, the server certificate can be self signed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProtP(boolean) protP}</td><td>if true, the server returns data via another socket</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageIsContent(boolean) messageIsContent}</td><td>if true, the contents of the message is send, otherwise it message contains the local filenames of the files to be send</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrivateKeyFile(String) privateKeyFile}</td><td>Path to private key file for SFTP authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassphrase(String) passphrase}</td><td>Passphrase of private key file</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKnownHostsPath(String) knownHostsPath}</td><td>path to file with knownhosts</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefCSEncryption(String) prefCSEncryption}</td><td>Optional preferred encryption from client to server for sftp protocol</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefSCEncryption(String) prefSCEncryption}</td><td>Optional preferred encryption from server to client for sftp protocol</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 *  
 * @author: John Dekker
 */
public class FtpSender extends SenderWithParametersBase {
	public static final String version = "$RCSfile: FtpSender.java,v $  $Revision: 1.7 $ $Date: 2005-11-11 12:30:39 $";
	private String name;
	private String remoteFilenamePattern;
	private String remoteDirectory;
	private FtpSession ftpSession;
	
	public FtpSender() {
		this.ftpSession = new FtpSession();
	}
	
	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.ISender#configure()
	 */
	public void configure() throws ConfigurationException {
		ftpSession.configure();
		
		if (StringUtils.isEmpty(name)) {
			throw new ConfigurationException("Attribute [name] is not set");
		}
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.ISender#isSynchronous()
	 */
	public boolean isSynchronous() {
		return true;
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.ISender#sendMessage(java.lang.String, java.lang.String)
	 */
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		try {
			ftpSession.put(paramList, prc.getSession(), message, remoteDirectory, remoteFilenamePattern, true);
		}
		catch(SenderException e) {
			throw e;
		}
		catch(Exception e) {
			throw new SenderException("Error during ftp-ing " + message, e);
		}
		return message;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setHost(String host) {
		ftpSession.setHost(host);
	}

	public void setPassword(String passwd) {
		ftpSession.setPassword(passwd);
	}

	public void setPort(int port) {
		ftpSession.setPort(port);
	}

	public void setFtpTypeDescription(String ftpTypeDescription) {
		ftpSession.setFtpTypeDescription(ftpTypeDescription);
	}

	public void setUsername(String username) {
		ftpSession.setUsername(username);
	}

	public void setProxyHost(String proxyHost) {
		ftpSession.setProxyHost(proxyHost);
	}

	public void setProxyPassword(String proxyPassword) {
		ftpSession.setProxyPassword(proxyPassword);
	}

	public void setProxyPort(int proxyPort) {
		ftpSession.setProxyPort(proxyPort);
	}

	public void setProxyTransportType(int proxyTransportType) {
		ftpSession.setProxyTransportType(proxyTransportType);
	}

	public void setProxyUsername(String proxyUsername) {
		ftpSession.setProxyUsername(proxyUsername);
	}

	public String getRemoteFilenamePattern() {
		return remoteFilenamePattern;
	}

	public void setRemoteFilenamePattern(String string) {
		remoteFilenamePattern = string;
	}

	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setCertificate(String certificate) {
		ftpSession.setCertificate(certificate);
	}

	public void setCertificatePassword(String certificatePassword) {
		ftpSession.setCertificatePassword(certificatePassword);
	}

	public void setJdk13Compatibility(boolean jdk13Compatibility) {
		ftpSession.setJdk13Compatibility(jdk13Compatibility);
	}

	public void setKeystoreType(String keystoreType) {
		ftpSession.setKeystoreType(keystoreType);
	}

	public void setTruststore(String truststore) {
		ftpSession.setTruststore(truststore);
	}

	public void setTruststorePassword(String truststorePassword) {
		ftpSession.setTruststorePassword(truststorePassword);
	}

	public void setTruststoreType(String truststoreType) {
		ftpSession.setTruststoreType(truststoreType);
	}

	public void setVerifyHostname(boolean verifyHostname) {
		ftpSession.setVerifyHostname(verifyHostname);
	}

	public void setFileType(String fileType) {
		ftpSession.setFileType(fileType);
	}

	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		ftpSession.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
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

	public void setProtP(boolean protP) {
		ftpSession.setProtP(protP);
	}

	public void setMessageIsContent(boolean messageIsContent) {
		ftpSession.setMessageIsContent(messageIsContent);
	}

	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	public void setPassphrase(String passPhrase) {
		ftpSession.setPassphrase(passPhrase);
	}

	public void setKnownHostsPath(String knownHostsPath) {
		ftpSession.setKnownHostsPath(knownHostsPath);
	}
}
