/*
 * $Log: FtpSession.java,v $
 * Revision 1.2  2005-10-17 12:21:23  europe\m00f531
 * *** empty log message ***
 *
 * Revision 1.1  2005/10/11 13:03:31  John Dekker <john.dekker@ibissource.org>
 * Supports retrieving files (FtpFileRetrieverPipe) and sending files (FtpSender)
 * via one of the FTP protocols (ftp, sftp, ftps both implicit as explicit).
 *
 */
package nl.nn.adapterframework.ftp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.sftp.SftpFile;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;

/**
 * Helper class for sftp and ftp
 * 
 * @author John Dekker
 */
public class FtpSession {
	public static final String version = "$RCSfile: FtpSession.java,v $  $Revision: 1.2 $ $Date: 2005-10-17 12:21:23 $";
	protected Logger logger = Logger.getLogger(this.getClass());
	
	// configuration parameters, global for all types
	private String host;
	private int port = 22;
	private String username;
	private String password;
	private String proxyHost;
	private int proxyPort = 1080;
	private String proxyUsername;
	private String proxyPassword;
	private String ftpTypeDescription = "FTP";
	private String transferMode = null;
	private boolean messageIsContent;
	
	// configuration property for sftp
	private int proxyTransportType = SshConnectionProperties.USE_SOCKS5_PROXY;
	
	// configuration parameters for ftps
	private boolean jdk13Compatibility = false;
	private boolean verifyHostname = true;
	private String certificate;
	private String certificatePassword;
	private String truststore = null;
	private String truststorePassword = null;
	private String keystoreType="pkcs12";
	private String truststoreType="jks";
	private boolean allowSelfSignedCertificates = false;
	private boolean protP = false;
	
	// private members
	private int ftpType;
	private SshClient sshClient;
	private SftpClient sftpClient;
	private FTPClient ftpClient;
	
	// types of ftp transports
	static final int FTP = 1;
	static final int SFTP = 2;
	static final int FTPS_IMPLICIT = 3;
	static final int FTPS_EXPLICIT_SSL = 4;
	static final int FTPS_EXPLICIT_TLS = 5;

	// configure
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(host)) {
			throw new ConfigurationException("Attribute [host] is not set");
		}
		if (StringUtils.isEmpty(username)) {
			throw new ConfigurationException("Attribute [username] is not set");
		}
		if (proxyTransportType < 1 && proxyTransportType > 4) {
			throw new ConfigurationException("Incorrect value for [proxyTransportType]");
		}
		if (StringUtils.isEmpty(ftpTypeDescription)) {
			throw new ConfigurationException("Attribute [ftpTypeDescription] is not set");
		}
		else {
			ftpTypeDescription = ftpTypeDescription.toUpperCase();
			if (ftpTypeDescription.equals("FTP")) {
				ftpType = FTP;
			}
			else if (ftpTypeDescription.equals("SFTP")) {
				ftpType = SFTP;
			}
			else if (ftpTypeDescription.equals("FTPSI")) {
				ftpType = FTPS_IMPLICIT;
			}
			else if (ftpTypeDescription.equals("FTPSX(SSL)")) {
				ftpType = FTPS_EXPLICIT_SSL;
			}
			else if (ftpTypeDescription.equals("FTPSX(TLS)")) {
				ftpType = FTPS_EXPLICIT_TLS;
			}
			else {
				throw new ConfigurationException("Attribute [ftpTypeDescription] has incorrect value [" + ftpTypeDescription + "]. Should be one of FTP, SFTP, FTPSI, FTPSX(SSL) or FTPSX(TLS)");
			}
		}
		
		try {
			getTransferModeIntValue();
		}
		catch(IOException e) {
			throw new ConfigurationException(e.getMessage());
		}
	}

	public void openClient(String remoteDirectory) throws FtpConnectException {
		if (ftpType == SFTP) {
			if (sftpClient == null || sftpClient.isClosed()) {
				openSftpClient(remoteDirectory);
			}
		}
		else {
			if (ftpClient == null || ! ftpClient.isConnected()) {
				openFtpClient(remoteDirectory);
			}
		}
	}

	private void openSftpClient(String remoteDirectory) throws FtpConnectException {
		try {
			// Set the connection properties and if necessary the proxy properties
			SshConnectionProperties sshProp = new SshConnectionProperties();
			sshProp.setHost(host);
			sshProp.setPort(port);
//			sshProp.setPrefCSEncryption("blowfish-cbc");
//			sshProp.setPrefPublicKey("ssh-rsa");
			
			if (! StringUtils.isEmpty(proxyHost)) {
				sshProp.setTransportProvider(proxyTransportType);
				sshProp.setProxyHost(proxyHost);
				sshProp.setProxyPort(proxyPort);
				if (! StringUtils.isEmpty(proxyUsername)) {
					sshProp.setProxyUsername(proxyUsername);
					sshProp.setProxyPassword(proxyPassword);
				}
			}

			// make a secure connection with the remote host 
			sshClient = new SshClient();
			sshClient.connect(sshProp, new IgnoreHostKeyVerification());
			
			// pass the authentication information
			PasswordAuthenticationClient pac = new PasswordAuthenticationClient();
			pac.setUsername(username);
			pac.setPassword(password);
			
			int result = sshClient.authenticate(pac);
			if (result != AuthenticationProtocolState.COMPLETE) {
				closeSftpClient();
				throw new IOException("Could not authenticate to sftp server " + result);
			}
			
			// use the connection for sftp
			sftpClient = sshClient.openSftpClient();
			
			if (! StringUtils.isEmpty(remoteDirectory)) {
				sftpClient.cd(remoteDirectory);
			}
		}
		catch(IOException e) {
			closeSftpClient();
			throw new FtpConnectException(e);
		}
	}

	private void openFtpClient(String remoteDirectory) throws FtpConnectException {
		try {
			// set proxy properties
			if (! StringUtils.isEmpty(proxyHost)) {
				System.getProperties().put("ftpProxySet", "true" );
				System.getProperties().put("ftpProxyHost", proxyHost);
				System.getProperties().put("ftpProxyPort", "" + proxyPort);
			}
			
			// connect and logic using normal, non-secure ftp 
			ftpClient = createFTPClient();
			ftpClient.connect(host, port);
			ftpClient.login(username, password);
	
			if (! StringUtils.isEmpty(remoteDirectory)) {
				if (! ftpClient.changeWorkingDirectory(remoteDirectory)) {
					throw new IOException(ftpClient.getReplyString());
				}
			}
			
			if (! StringUtils.isEmpty(transferMode)) {
				ftpClient.setFileTransferMode(getTransferModeIntValue());
				if (ftpClient.getReply() < 200 || ftpClient.getReply() >= 300) {
					logger.warn("SendCommand(mode, " + transferMode + ") returned " + ftpClient.getReplyString());
				}
			}
		}
		catch(Exception e) {
			closeFtpClient();
			throw new FtpConnectException(e);
		}
	}

	private int getTransferModeIntValue() throws IOException {
		if (StringUtils.isEmpty(transferMode))
			return FTPClient.ASCII_FILE_TYPE;
		else if ("ASCII".equals(transferMode))
			return FTPClient.ASCII_FILE_TYPE;
		else if ("BINARY".equals(transferMode))
			return FTPClient.BINARY_FILE_TYPE;
		else if ("EBCDIC".equals(transferMode))
			return FTPClient.EBCDIC_FILE_TYPE;
		else if ("LOCAL".equals(transferMode))
			return FTPClient.LOCAL_FILE_TYPE;
		else if ("STREAM".equals(transferMode))
			return FTPClient.STREAM_TRANSFER_MODE;
		else if ("COMPRESSED".equals(transferMode))
			return FTPClient.COMPRESSED_TRANSFER_MODE;
		else 
			throw new IOException("Unknown transfermode [" + transferMode + "] specified, use one of ASCII, BINARY, EBCDIC, LOCAL, STREAM, COMPRESSED");
	}

	private FTPClient createFTPClient() throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException {
		if (ftpType == FTP) {
			return new FTPClient();
		}
		return new FTPsClient(this);
	}

	public void closeClient() {
		if (ftpType == SFTP) {
			closeSftpClient();
		}
		else {
			closeFtpClient();
		}
	}
	
	private void closeSftpClient() {
		if (sshClient != null) {
			if (sshClient.isConnected()) {
				sshClient.disconnect();
			}
			sshClient = null;
			sftpClient = null;
		}
	}
	
	private void closeFtpClient() {
		if (ftpClient != null) {
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				}
				catch(Exception e) {
					logger.error("Error while closeing FtpClient", e);
				}
			}
			ftpClient = null;
		}
	}
	
	public String put(String message, String remoteDirectory, String remoteFilenamePattern, boolean closeAfterSend) throws Exception {
		if (messageIsContent) {
			return _put(message, remoteDirectory, remoteFilenamePattern, closeAfterSend);
		}
		else {
			List remoteFilenames = _put(FileUtils.getListFromNames(message, ';'), remoteDirectory, remoteFilenamePattern, closeAfterSend);
			return FileUtils.getNamesFromList(remoteFilenames, ';');	
		}
	}
	
	/**
	 * @param contents
	 * @param remoteDirectory
	 * @param remoteFilenamePattern
	 * @param closeAfterSend
	 * @return name of the create remote file
	 * @throws Exception
	 */
	private String _put(String contents, String remoteDirectory, String remoteFilenamePattern, boolean closeAfterSend) throws Exception {
		openClient(remoteDirectory);
		
		// get remote name
		String remoteFilename = FileUtils.getFilename("", remoteFilenamePattern, true);
		
		// open local file
		InputStream is = new ByteArrayInputStream(contents.getBytes());
		try {  
			if (ftpType == SFTP) {
				sftpClient.put(is, remoteFilename);
			}
			else {
				if (! ftpClient.storeFile(remoteFilename, is)) {
					throw new IOException(ftpClient.getReplyString());
				}
			}
		}
		finally {
			is.close();

			if (closeAfterSend) {
				closeClient();
			}
		}
		return remoteFilename;
	}
	
	/**
	 * @param filenames
	 * @param remoteDirectory
	 * @param remoteFilenamePattern
	 * @param closeAfterSend
	 * @return list of remotely created files
	 * @throws Exception
	 */
	private List _put(List filenames, String remoteDirectory, String remoteFilenamePattern, boolean closeAfterSend) throws Exception {
		openClient(remoteDirectory);
		
		try {
			LinkedList remoteFilenames = new LinkedList();
			for (Iterator filenameIt = filenames.iterator(); filenameIt.hasNext(); ) {
				String localFilename = (String)filenameIt.next();
				File localFile = new File(localFilename);
				
				// get remote name
				String remoteFilename = null;
				if (! StringUtils.isEmpty(remoteFilenamePattern)) {
					remoteFilename = FileUtils.getFilename(localFile, remoteFilenamePattern, true);
				}
				else {
					remoteFilename = localFile.getName();
				}
				
				// open local file
				FileInputStream fis = new FileInputStream(localFile);
				try {  
					if (ftpType == SFTP) {
						sftpClient.put(fis, remoteFilename);
					}
					else {
						if (! ftpClient.storeFile(remoteFilename, fis)) {
							throw new IOException(ftpClient.getReplyString());
						}
					}
				}
				finally {
					fis.close();
				}
				remoteFilenames.add(remoteFilename);
			}
			return remoteFilenames;
		}
		finally {
			if (closeAfterSend) {
				closeClient();
			}
		}
	}

	private List _ls(String remoteDirectory, boolean closeAfterSend) throws Exception {
		openClient(remoteDirectory);

		try {
			if (ftpType == SFTP) {
				List result = new LinkedList();
				List listOfSftpFiles = sftpClient.ls();
				for (Iterator sftpFileIt = listOfSftpFiles.iterator(); sftpFileIt.hasNext();) {
					SftpFile file = (SftpFile)sftpFileIt.next();
					result.add(file.getAbsolutePath());
				}
				return result;
			}
			else {
				return FileUtils.getListFromNames(ftpClient.listNames());
			}
		}
		finally {
			if (closeAfterSend) {
				closeClient();
			}
		}
	}
	
	public String lsAsString(String remoteDirectory, boolean closeAfterSend) throws Exception {
		List result = _ls(remoteDirectory, closeAfterSend);
		return FileUtils.getNamesFromList(result, ';');
	}
	
	public String get(String localDirectory, String remoteDirectory, String filenames, String localFilenamePattern, boolean closeAfterGet) throws Exception {
		if (messageIsContent) {
			return _get(remoteDirectory, FileUtils.getListFromNames(filenames, ';'), closeAfterGet);
		}
		else {
			List result = _get(localDirectory, remoteDirectory, FileUtils.getListFromNames(filenames, ';'), localFilenamePattern, closeAfterGet);
			return FileUtils.getNamesFromList(result, ';');	
		}
	}

	/**
	 * @param remoteDirectory
	 * @param filenames
	 * @param closeAfterGet
	 * @return concatenation of the contents of all received files
	 * @throws Exception
	 */
	private String _get(String remoteDirectory, List filenames, boolean closeAfterGet) throws Exception {
		openClient(remoteDirectory);
		
		try {
			StringBuffer result = new StringBuffer();
			for (Iterator filenameIt = filenames.iterator(); filenameIt.hasNext(); ) {
				String remoteFilename = (String)filenameIt.next();
				OutputStream os = null;

				os = new ByteArrayOutputStream();

				try {
					if (ftpType == SFTP) {
						sftpClient.get(remoteFilename, os);
					}
					else {
						if (! ftpClient.retrieveFile(remoteFilename, os)) {
							throw new IOException(ftpClient.getReplyString());
						}
					}
				}
				finally {
					os.close();
				}
				
				result.append(((ByteArrayOutputStream)os).toString());
			}
			return result.toString();
		}
		finally {
			if (closeAfterGet) {
				closeClient();
			}
		}
	}
	
	/**
	 * @param remoteDirectory
	 * @param filenames
	 * @param closeAfterGet
	 * @return ; seperated string with filenames of locally created files 
	 * @throws Exception
	 */
	private List _get(String localDirectory, String remoteDirectory, List filenames, String localFilenamePattern, boolean closeAfterGet) throws Exception {
		openClient(remoteDirectory);
		
		try {
			LinkedList remoteFilenames = new LinkedList();
			for (Iterator filenameIt = filenames.iterator(); filenameIt.hasNext(); ) {
				String remoteFilename = (String)filenameIt.next();

				String localFilename = remoteFilename;
				if (! StringUtils.isEmpty(localFilenamePattern)) {
					localFilename = FileUtils.getFilename(remoteFilename, localFilenamePattern, true);
				}
				
				File localFile = new File(localDirectory, localFilename);
				OutputStream os = new FileOutputStream(localFile);
				try {
					if (ftpType == SFTP) {
						sftpClient.get(remoteFilename, os);
					}
					else {
						if (! ftpClient.retrieveFile(remoteFilename, os)) {
							throw new IOException(ftpClient.getReplyString());
						}
					}
				}
				catch(IOException e) {
					os.close();
					os = null;
					localFile.delete();
					throw e;
				}
				finally {
					if (os != null)
						os.close();
				}
				
				remoteFilenames.add(localFile.getAbsolutePath());
			}
			return remoteFilenames;
		}
		finally {
			if (closeAfterGet) {
				closeClient();
			}
		}
	}
	
	String getHost() {
		return host;
	}

	String getPassword() {
		return password;
	}

	int getPort() {
		return port;
	}

	String getProxyHost() {
		return proxyHost;
	}

	String getProxyPassword() {
		return proxyPassword;
	}

	int getProxyPort() {
		return proxyPort;
	}

	int getProxyTransportType() {
		return proxyTransportType;
	}

	String getProxyUsername() {
		return proxyUsername;
	}

	String getUsername() {
		return username;
	}

	public void setHost(String string) {
		host = string;
	}

	public void setPassword(String string) {
		password = string;
	}

	public void setPort(int i) {
		port = i;
	}

	public void setProxyHost(String string) {
		proxyHost = string;
	}

	public void setProxyPassword(String string) {
		proxyPassword = string;
	}

	public void setProxyPort(int i) {
		proxyPort = i;
	}

	public void setProxyTransportType(int i) {
		proxyTransportType = i;
	}

	public void setProxyUsername(String string) {
		proxyUsername = string;
	}

	public void setUsername(String string) {
		username = string;
	}

	public String getFtpTypeDescription() {
		return ftpTypeDescription;
	}

	public void setFtpTypeDescription(String string) {
		ftpTypeDescription = string;
	}

	int getFtpType() {
		return ftpType;
	}

	public String getCertificate() {
		return certificate;
	}

	public String getCertificatePassword() {
		return certificatePassword;
	}

	public String getTruststore() {
		return truststore;
	}

	public String getTruststorePassword() {
		return truststorePassword;
	}

	public void setCertificate(String string) {
		certificate = string;
	}

	public void setCertificatePassword(String string) {
		certificatePassword = string;
	}

	public void setTruststore(String string) {
		truststore = string;
	}

	public void setTruststorePassword(String string) {
		truststorePassword = string;
	}

	public boolean isJdk13Compatibility() {
		return jdk13Compatibility;
	}

	public void setJdk13Compatibility(boolean b) {
		jdk13Compatibility = b;
	}

	public String getKeystoreType() {
		return keystoreType;
	}

	public void setKeystoreType(String string) {
		keystoreType = string;
	}

	public String getTruststoreType() {
		return truststoreType;
	}

	public void setTruststoreType(String string) {
		truststoreType = string;
	}

	public boolean isVerifyHostname() {
		return verifyHostname;
	}

	public void setVerifyHostname(boolean b) {
		verifyHostname = b;
	}
	
	public boolean isAllowSelfSignedCertificates() {
		return allowSelfSignedCertificates;
	}

	public void setAllowSelfSignedCertificates(boolean b) {
		allowSelfSignedCertificates = b;
	}

	public void setTransferMode(String string) {
		transferMode = string;
	}

	public boolean isProtp() {
		return protP;
	}

	public void setProtP(boolean b) {
		protP = b;
	}

	public boolean isMessageIsContent() {
		return messageIsContent;
	}

	public void setMessageIsContent(boolean b) {
		messageIsContent = b;
	}

}
