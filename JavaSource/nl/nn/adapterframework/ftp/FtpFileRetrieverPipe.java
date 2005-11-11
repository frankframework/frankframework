/*
 * $Log: FtpFileRetrieverPipe.java,v $
 * Revision 1.5  2005-11-11 12:30:38  europe\l166817
 * Aanpassingen door John Dekker
 *
 * Revision 1.4  2005/11/07 08:21:35  John Dekker <john.dekker@ibissource.org>
 * Enable sftp public/private key authentication
 *
 * Revision 1.3  2005/10/24 09:59:18  John Dekker <john.dekker@ibissource.org>
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

import java.io.IOException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe for retreiving files via (s)ftp. The path of the created local file is returned.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.FtpFileRetrieverPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the pip</td><td>&nbsp;</td></tr>
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
 * <tr><td>{@link #setLocalFilenamePattern(string) filenamePattern}</td><td>pattern (in MessageFormat) of the local filename</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLocalDirectory(string) directory}</td><td>local directory in which files have to be downloaded</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoteDirectory(string) directory}</td><td>remote directory</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileType(string) fileType}</td><td>File type, one of ASCII, BINARY</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAllowSelfSignedCertificates(boolean) allowSelfSignedCertificates}</td><td>if true, the server certificate can be self signed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProtP(boolean) protP}</td><td>if true, the server returns data via another socket</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrivateKeyFile(String) privateKeyFile}</td><td>Path to private key file for SFTP authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassphrase(String) passphrase}</td><td>Passphrase of private key file</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKnownHostsPath(String) knownHostsPath}</td><td>path to file with knownhosts</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefCSEncryption(String) prefCSEncryption}</td><td>Optional preferred encryption from client to server for sftp protocol</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefSCEncryption(String) prefSCEncryption}</td><td>Optional preferred encryption from server to client for sftp protocol</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageIsContent(boolean) messageIsContent}</td><td>if true, the received ftp contents is put in the message, otherwise it is put in locally created files</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #deleteAfterGet(boolean) deleteAfterGet}</td><td>if true, the remote file is deleted after it is retrieved</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 */
public class FtpFileRetrieverPipe extends FixedForwardPipe {
	public static final String version = "$RCSfile: FtpFileRetrieverPipe.java,v $  $Revision: 1.5 $ $Date: 2005-11-11 12:30:38 $";

	private String name;
	private String failureForward;
	private String localFilenamePattern;
	private String localDirectory;
	private String remoteDirectory;
	private boolean deleteAfterGet;
	private FtpSession ftpSession;
	
	public FtpFileRetrieverPipe() {
		ftpSession = new FtpSession();
	}
	
	public void configure() throws ConfigurationException {
		super.configure();
		
		ftpSession.configure();
		
		if (StringUtils.isEmpty(name)) {
			throw new ConfigurationException("Attribute [name] is not set");
		}
		if (! StringUtils.isEmpty(failureForward) && findForward(failureForward) == null) {
			throw new ConfigurationException("Attribute [failureForward] refers to a non-existing path " + failureForward);
		}
	}
	
	public void stop() {
		super.stop();

		try {		
			ftpSession.closeClient();
		}
		catch(Exception e) {
		}
	}
 
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String orgFilename = (String)input;
		try {
			boolean close = ! deleteAfterGet;
			String localFilename = ftpSession.get(getParameterList(), session, localDirectory, remoteDirectory, orgFilename, localFilenamePattern, close);
			ftpSession.deleteRemote(remoteDirectory, orgFilename, true);
			return new PipeRunResult(getForward(), localFilename);
		}
		catch(IOException e) {
			if (! StringUtils.isEmpty(failureForward)) {
				log.warn("Error while getting file " + remoteDirectory + "/" + input, e);
				return new PipeRunResult(findForward(failureForward), input);
			}
			throw new PipeRunException(this, "Error while getting file [" + orgFilename + "]", e); 
		}
		catch(Exception e) {
			throw new PipeRunException(this, "Error while getting file [" + orgFilename + "]", e); 
		}
	}

	public FtpSession getFtpSession() {
		return ftpSession;
	}

	public String getLocalFilenamePattern() {
		return localFilenamePattern;
	}

	public String getName() {
		return name;
	}

	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setFtpSession(FtpSession session) {
		ftpSession = session;
	}

	public void setLocalFilenamePattern(String string) {
		localFilenamePattern = string;
	}

	public void setName(String string) {
		name = string;
	}

	public void setRemoteDirectory(String string) {
		remoteDirectory = string;
	}

	public String getLocalDirectory() {
		return localDirectory;
	}

	public void setLocalDirectory(String string) {
		localDirectory = string;
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

	public boolean isDeleteAfterGet() {
		return deleteAfterGet;
	}

	public void setDeleteAfterGet(boolean b) {
		deleteAfterGet = b;
	}

	public void setPassphrase(String passPhrase) {
		ftpSession.setPassphrase(passPhrase);
	}

	public void setKnownHostsPath(String knownHostsPath) {
		ftpSession.setKnownHostsPath(knownHostsPath);
	}
}
