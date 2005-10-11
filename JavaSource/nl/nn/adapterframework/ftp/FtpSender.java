/*
 * $Log: FtpSender.java,v $
 * Revision 1.1  2005-10-11 13:03:29  europe\m00f531
 * Supports retrieving files (FtpFileRetrieverPipe) and sending files (FtpSender)
 * via one of the FTP protocols (ftp, sftp, ftps both implicit as explicit).
 *
 */
package nl.nn.adapterframework.ftp;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

import org.apache.commons.lang.StringUtils;

/**
 * FTP client voor het versturen van files via FTP
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
 * <tr><td>{@link #setTransferMode(string) mode}</td><td>transfermode, one of ASCII, BINARY, EBCDIC, STREAM, COMPRESSED</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAllowSelfSignedCertificates(boolean) allowSelfSignedCertificates}</td><td>if true, the server certificate can be self signed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProtP(boolean) protP}</td><td>if true, the server returns data via another socket</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 *  
 * @author: John Dekker
 */
public class FtpSender implements ISender {
	public static final String version = "$RCSfile: FtpSender.java,v $  $Revision: 1.1 $ $Date: 2005-10-11 13:03:29 $";
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
	 * @see nl.nn.adapterframework.core.ISender#open()
	 */
	public void open() throws SenderException {
	}
	
	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.ISender#close()
	 */
	public void close() throws SenderException {
		ftpSession.closeClient();
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
	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		try {
			ftpSession.put(message, remoteDirectory, remoteFilenamePattern, true);
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

	public void setTransferMode(String transferMode) {
		ftpSession.setTransferMode(transferMode);
	}

	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		ftpSession.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}

	public void setProtP(boolean protP) {
		ftpSession.setProtP(protP);
	}
}
