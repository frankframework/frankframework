/*
 * $Log: FtpSession.java,v $
 * Revision 1.10  2005-12-07 15:52:15  europe\L190409
 * improved logging & checking for reply-code
 *
 * Revision 1.8  2005/11/08 09:31:09  John Dekker <john.dekker@ibissource.org>
 * Bug concerning filenames resolved
 *
 * Revision 1.7  2005/11/07 09:41:25  John Dekker <john.dekker@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.6  2005/11/07 08:21:36  John Dekker <john.dekker@ibissource.org>
 * Enable sftp public/private key authentication
 *
 * Revision 1.5  2005/10/24 12:12:33  John Dekker <john.dekker@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.4  2005/10/24 11:41:27  John Dekker <john.dekker@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.3  2005/10/24 09:59:19  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.2  2005/10/17 12:21:23  John Dekker <john.dekker@ibissource.org>
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
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.authentication.PublicKeyAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationClient;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.sftp.SftpFile;
import com.sshtools.j2ssh.transport.AbstractKnownHostsKeyVerification;
import com.sshtools.j2ssh.transport.ConsoleKnownHostsKeyVerification;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKeyFile;

/**
 * Helper class for sftp and ftp
 * 
 * @author John Dekker
 */
public class FtpSession {
	public static final String version = "$RCSfile: FtpSession.java,v $  $Revision: 1.10 $ $Date: 2005-12-07 15:52:15 $";
	protected Logger log = Logger.getLogger(this.getClass());
	
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
	private String fileType = null;
	private boolean messageIsContent;
	
	// configuration property for sftp
	private int proxyTransportType = SshConnectionProperties.USE_SOCKS5_PROXY;
	private String prefCSEncryption = null;
	private String prefSCEncryption = null;
	private String privateKeyFilePath = null;
	private String passphrase = null;
	private String knownHostsPath = null;
	private boolean consoleKnownHostsVerifier = false;
	
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
			getFileTypeIntValue();
		}
		catch(IOException e) {
			throw new ConfigurationException(e.getMessage());
		}
	}

	public void openClient(String remoteDirectory) throws FtpConnectException {
		log.debug("Open ftp client");
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
			if (StringUtils.isNotEmpty(prefCSEncryption))
				sshProp.setPrefCSEncryption(prefCSEncryption);
			if (StringUtils.isNotEmpty(prefSCEncryption))
				sshProp.setPrefCSEncryption(prefSCEncryption);
			
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
			if (StringUtils.isNotEmpty(knownHostsPath)) {
				AbstractKnownHostsKeyVerification hv = null;
				if (consoleKnownHostsVerifier) { 
					hv = new ConsoleKnownHostsKeyVerification(knownHostsPath); 
				}
				else {
					hv = new SftpHostVerification(knownHostsPath);
				}
				sshClient.connect(sshProp, hv);
			}
			else {
				sshClient.connect(sshProp, new IgnoreHostKeyVerification());
			}
			
			// pass the authentication information
			SshAuthenticationClient sac = getSshAuthentication();
			
			int result = sshClient.authenticate(sac);
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
		catch(Exception e) {
			closeSftpClient();
			throw new FtpConnectException(e);
		}
	}

	private SshAuthenticationClient getSshAuthentication() throws Exception {
		if (StringUtils.isNotEmpty(privateKeyFilePath)) {
			PublicKeyAuthenticationClient pk = new PublicKeyAuthenticationClient();
			pk.setUsername(username);
			SshPrivateKeyFile pkFile = SshPrivateKeyFile.parse(new File(privateKeyFilePath));
			pk.setKey(pkFile.toPrivateKey(passphrase));
			return pk; 
		}
		if (StringUtils.isNotEmpty(password)) {
			PasswordAuthenticationClient pac = new PasswordAuthenticationClient();
			pac.setUsername(username);
			pac.setPassword(password);
			return pac;
		}
		throw new Exception("Unknown authentication type, either the password or the privateKeyFile must be filled");
	}


	protected boolean replyCodeIsOK(int replyCode) {
		log.debug("FTP replyCode ["+replyCode+"]");
		return replyCode>= 200 && replyCode < 300 || replyCode==125;
	}
	
	protected void checkReply(int replyCode, String cmd) throws IOException  {
		if (!replyCodeIsOK(replyCode)) {
			throw new IOException("Command [" + cmd + "] returned error [" + replyCode + "]: " + ftpClient.getReplyString());
		} else {
			log.debug("Command [" + cmd + "] returned " + ftpClient.getReplyString());
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
				ftpClient.changeWorkingDirectory(remoteDirectory);
				checkReply(ftpClient.getReplyCode(),"changeWorkingDirectory "+remoteDirectory);
			}
			
			if (StringUtils.isNotEmpty(fileType)) {
				ftpClient.setFileType(getFileTypeIntValue());
				checkReply(ftpClient.getReplyCode(),"setFileType "+remoteDirectory);
			}
		}
		catch(Exception e) {
			closeFtpClient();
			throw new FtpConnectException(e);
		}
	}

	private int getFileTypeIntValue() throws IOException {
		if (StringUtils.isEmpty(fileType))
			return org.apache.commons.net.ftp.FTP.ASCII_FILE_TYPE;
		else if ("ASCII".equals(fileType))
			return org.apache.commons.net.ftp.FTP.ASCII_FILE_TYPE;
		else if ("BINARY".equals(fileType))
			return org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE;
		else {
			throw new IOException("Unknown Type [" + fileType + "] specified, use one of ASCII, BINARY");
		}
	}

	private FTPClient createFTPClient() throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException {
		if (ftpType == FTP) {
			return new FTPClient();
		}
		return new FTPsClient(this);
	}

	public void closeClient() {
		log.debug("Close ftp client");
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
					log.error("Error while closeing FtpClient", e);
				}
			}
			ftpClient = null;
		}
	}
	
	public String put(ParameterList params, PipeLineSession session, String message, String remoteDirectory, String remoteFilenamePattern, boolean closeAfterSend) throws Exception {
		if (messageIsContent) {
			return _put(params, session, message, remoteDirectory, remoteFilenamePattern, closeAfterSend);
		}
		else {
			List remoteFilenames = _put(params, session, FileUtils.getListFromNames(message, ';'), remoteDirectory, remoteFilenamePattern, closeAfterSend);
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
	private String _put(ParameterList params, PipeLineSession session, String contents, String remoteDirectory, String remoteFilenamePattern, boolean closeAfterSend) throws Exception {
		openClient(remoteDirectory);
		
		// get remote name
		String remoteFilename = FileUtils.getFilename(params, session, (File)null, remoteFilenamePattern);
		
		// open local file
		InputStream is = new ByteArrayInputStream(contents.getBytes());
		try {  
			if (ftpType == SFTP) {
				sftpClient.put(is, remoteFilename);
			}
			else {
				ftpClient.storeFile(remoteFilename, is);
				checkReply(ftpClient.getReplyCode(),"storeFile "+remoteFilename);
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
	private List _put(ParameterList params, PipeLineSession session, List filenames, String remoteDirectory, String remoteFilenamePattern, boolean closeAfterSend) throws Exception {
		openClient(remoteDirectory);
		
		try {
			LinkedList remoteFilenames = new LinkedList();
			for (Iterator filenameIt = filenames.iterator(); filenameIt.hasNext(); ) {
				String localFilename = (String)filenameIt.next();
				File localFile = new File(localFilename);
				
				// get remote name
				String remoteFilename = null;
				if (! StringUtils.isEmpty(remoteFilenamePattern)) {
					remoteFilename = FileUtils.getFilename(params, session, localFile, remoteFilenamePattern);
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
						ftpClient.storeFile(remoteFilename, fis);
						checkReply(ftpClient.getReplyCode(),"storeFile "+remoteFilename);
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

	public List ls(String remoteDirectory, boolean filesOnly, boolean closeAfterSend) throws Exception {
		openClient(remoteDirectory);

		try {
			if (ftpType == SFTP) {
				List result = new LinkedList();
				List listOfSftpFiles = sftpClient.ls();
				for (Iterator sftpFileIt = listOfSftpFiles.iterator(); sftpFileIt.hasNext();) {
					SftpFile file = (SftpFile)sftpFileIt.next();
					String filename = file.getFilename();
					if (filesOnly || (! file.isDirectory())) {
						if (! filename.startsWith(".")) {
							result.add(filename);
						}
					}
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
	
	public String lsAsString(String remoteDirectory, boolean filesOnly, boolean closeAfterSend) throws Exception {
		List result = ls(remoteDirectory, filesOnly, closeAfterSend);
		return FileUtils.getNamesFromList(result, ';');
	}
	
	public String get(ParameterList params, PipeLineSession session, String localDirectory, String remoteDirectory, String filenames, String localFilenamePattern, boolean closeAfterGet) throws Exception {
		if (messageIsContent) {
			return _get(remoteDirectory, FileUtils.getListFromNames(filenames, ';'), closeAfterGet);
		}
		else {
			List result = _get(params, session, localDirectory, remoteDirectory, FileUtils.getListFromNames(filenames, ';'), localFilenamePattern, closeAfterGet);
			return FileUtils.getNamesFromList(result, ';');	
		}
	}
	
	public void deleteRemote(String remoteDirectory, String filename, boolean closeAfterDelete) throws Exception {
		openClient(remoteDirectory);

		try {
			if (ftpType == SFTP) {
				sftpClient.rm(filename);
			}
			else {
				ftpClient.deleteFile(filename);
			}
		}
		finally {
			if (closeAfterDelete) {
				closeClient();
			}
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
						ftpClient.retrieveFile(remoteFilename, os);
						int replyCode = ftpClient.getReplyCode();
						checkReply(replyCode,"retrieve "+remoteFilename);
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
	private List _get(ParameterList params, PipeLineSession session, String localDirectory, String remoteDirectory, List filenames, String localFilenamePattern, boolean closeAfterGet) throws Exception {
		openClient(remoteDirectory);
		
		try {
			LinkedList remoteFilenames = new LinkedList();
			for (Iterator filenameIt = filenames.iterator(); filenameIt.hasNext(); ) {
				String remoteFilename = (String)filenameIt.next();

				String localFilename = remoteFilename;
				if (! StringUtils.isEmpty(localFilenamePattern)) {
					localFilename = FileUtils.getFilename(params, session, remoteFilename, localFilenamePattern);
				}
				
				File localFile = new File(localDirectory, localFilename);
				OutputStream os = new FileOutputStream(localFile,false);
				try {
					if (ftpType == SFTP) {
						sftpClient.get(remoteFilename, os);
					}
					else {
						ftpClient.retrieveFile(remoteFilename, os);
						int replyCode = ftpClient.getReplyCode();
						checkReply(replyCode,"retrieve "+remoteFilename);
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

	String getFtpTypeDescription() {
		return ftpTypeDescription;
	}

	public void setFtpTypeDescription(String string) {
		ftpTypeDescription = string;
	}

	int getFtpType() {
		return ftpType;
	}

	String getCertificate() {
		return certificate;
	}

	String getCertificatePassword() {
		return certificatePassword;
	}

	String getTruststore() {
		return truststore;
	}

	String getTruststorePassword() {
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

	boolean isJdk13Compatibility() {
		return jdk13Compatibility;
	}

	public void setJdk13Compatibility(boolean b) {
		jdk13Compatibility = b;
	}

	String getKeystoreType() {
		return keystoreType;
	}

	public void setKeystoreType(String string) {
		keystoreType = string;
	}

	String getTruststoreType() {
		return truststoreType;
	}

	public void setTruststoreType(String string) {
		truststoreType = string;
	}

	boolean isVerifyHostname() {
		return verifyHostname;
	}

	public void setVerifyHostname(boolean b) {
		verifyHostname = b;
	}
	
	boolean isAllowSelfSignedCertificates() {
		return allowSelfSignedCertificates;
	}

	public void setAllowSelfSignedCertificates(boolean b) {
		allowSelfSignedCertificates = b;
	}

	public void setFileType(String string) {
		fileType = string;
	}

	boolean isProtp() {
		return protP;
	}

	public void setProtP(boolean b) {
		protP = b;
	}

	boolean isMessageIsContent() {
		return messageIsContent;
	}

	public void setMessageIsContent(boolean b) {
		messageIsContent = b;
	}

	String getPrefCSEncryption() {
		return prefCSEncryption;
	}

	String getPrefSCEncryption() {
		return prefSCEncryption;
	}

	String getPrivateKeyFilePath() {
		return privateKeyFilePath;
	}

	public void setPrefCSEncryption(String string) {
		prefCSEncryption = string;
	}

	public void setPrefSCEncryption(String string) {
		prefSCEncryption = string;
	}

	public void setPrivateKeyFilePath(String string) {
		privateKeyFilePath = string;
	}

	String getPassphrase() {
		return passphrase;
	}

	public void setPassphrase(String string) {
		passphrase = string;
	}

	String getKnownHostsPath() {
		return knownHostsPath;
	}

	public void setKnownHostsPath(String string) {
		knownHostsPath = string;
	}

	public boolean isConsoleKnownHostsVerifier() {
		return consoleKnownHostsVerifier;
	}

	public void setConsoleKnownHostsVerifier(boolean b) {
		consoleKnownHostsVerifier = b;
	}

}
