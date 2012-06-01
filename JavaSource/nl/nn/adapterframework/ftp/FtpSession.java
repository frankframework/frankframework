/*
 * $Log: FtpSession.java,v $
 * Revision 1.21  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.20  2011/12/20 12:11:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved error handling
 *
 * Revision 1.19  2011/11/30 13:52:04  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.17  2011/06/27 15:39:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * enabled KeyboardInteractive login (experimental)
 * allow to set keyManagerAlgorithm and trustManagerAlgorithm
 *
 * Revision 1.16  2010/03/19 07:22:32  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * default port for FTP is 21 instead of 22 (which is for SFTP)
 *
 * Revision 1.15  2007/05/07 08:35:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic change
 *
 * Revision 1.14  2007/02/12 13:50:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.13  2006/01/05 14:17:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.12  2005/12/20 09:34:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected attribute checking for SFTP
 *
 * Revision 1.11  2005/12/19 16:46:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework, lots of changes
 *
 * Revision 1.10  2005/12/07 15:52:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.KBIAuthenticationClient;
import com.sshtools.j2ssh.authentication.KBIPrompt;
import com.sshtools.j2ssh.authentication.KBIRequestHandler;
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
 * Helper class for sftp and ftp.
 * 
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.ftp.FtpSession</td><td>&nbsp;</td></tr>
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
 * 
 * @author John Dekker
 */
public class FtpSession {
	protected Logger log = LogUtil.getLogger(this);

	// types of ftp transports
	static final int FTP = 1;
	static final int SFTP = 2;
	static final int FTPS_IMPLICIT = 3;
	static final int FTPS_EXPLICIT_SSL = 4;
	static final int FTPS_EXPLICIT_TLS = 5;

	// indication of ftp-subtype, defines which client
	private int ftpType;
	
	// configuration parameters, global for all types
	private String host;
	private int port = 21;
	private String authAlias;
	private String username;
	private String password;
	private String proxyHost;
	private int proxyPort = 1080;
	private String proxyAuthAlias;
	private String proxyUsername;
	private String proxyPassword;
	private String ftpTypeDescription = "FTP";
	private String fileType = null;
	private boolean messageIsContent=false;
	private boolean passive=true;
	private boolean keyboardInteractive=false;
	
	// configuration property for sftp
	private int proxyTransportType = SshConnectionProperties.USE_SOCKS5_PROXY;
	private String prefCSEncryption = null;
	private String prefSCEncryption = null;
	private String privateKeyFilePath = null;
	private String privateKeyAuthAlias;
	private String privateKeyPassword = null;
	private String knownHostsPath = null;
	private boolean consoleKnownHostsVerifier = false;
	
	// configuration parameters for ftps
	private String certificate;
	private String certificateType="pkcs12";
	private String certificateAuthAlias;
	private String certificatePassword;
	private String keyManagerAlgorithm=null;
	private String truststore = null;
	private String truststoreType="jks";
	private String truststoreAuthAlias;
	private String truststorePassword = null;
	private String trustManagerAlgorithm=null;
	private boolean jdk13Compatibility = false;
	private boolean verifyHostname = true;
	private boolean allowSelfSignedCertificates = false;
	private boolean protP = false;
	
	
	
	// private members
	private SshClient sshClient;
	private SftpClient sftpClient;
	public FTPClient ftpClient;
	

	// configure
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(ftpTypeDescription)) {
			throw new ConfigurationException("Attribute [ftpTypeDescription] is not set");
		}
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

		if (StringUtils.isEmpty(host)) {
			throw new ConfigurationException("Attribute [host] is not set");
		}
		if (StringUtils.isEmpty(username) && StringUtils.isEmpty(getAuthAlias())) {
			if (ftpType!=SFTP) {
				throw new ConfigurationException("Neither attribute 'username' nor 'authAlias' is set");
			}
			else if (StringUtils.isEmpty(privateKeyAuthAlias)) {
				throw new ConfigurationException("Neither attribute 'username' nor 'authAlias' nor 'privateKeyAuthAlias' is set");
			}
		}
		if (proxyTransportType < 1 && proxyTransportType > 4) {
			throw new ConfigurationException("Incorrect value for [proxyTransportType]");
		}
		
		try {
			getFileTypeIntValue();
		}
		catch(IOException e) {
			throw new ConfigurationException(e);
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
				CredentialFactory pcf = new CredentialFactory(getProxyAuthAlias(), proxyUsername, proxyPassword);
	
				if (! StringUtils.isEmpty(pcf.getUsername())) {
					sshProp.setProxyUsername(pcf.getUsername());
					sshProp.setProxyPassword(pcf.getPassword());
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
			
			SshAuthenticationClient sac;
			if (!isKeyboardInteractive()) {
				// pass the authentication information
				sac = getSshAuthentication();
			} else {
				
				// TODO: detecteren dat sshClient.getAvailableAuthMethods("ftpmsg")
				// wel keyboard-interactive terug geeft, maar geen password en dan deze methode
				// gebruiken
				final CredentialFactory credentialFactory = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
				KBIAuthenticationClient kbiAuthenticationClient = new KBIAuthenticationClient();
				kbiAuthenticationClient.setUsername(credentialFactory.getUsername());
				kbiAuthenticationClient.setKBIRequestHandler(
					new KBIRequestHandler() {
						public void showPrompts(String name, String instruction, KBIPrompt[] prompts) {
							//deze 3 regels in x.zip naar Zenz gemaild, hielp ook niet
							if(prompts==null) {
								return;
							}
							for(int i=0; i<prompts.length; i++) {
								prompts[i].setResponse(credentialFactory.getPassword());
							}
						}
					}
				);
				sac=kbiAuthenticationClient;
			}
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
			CredentialFactory pkcf = new CredentialFactory(getPrivateKeyAuthAlias(), getUsername(), getPrivateKeyPassword());
			pk.setUsername(pkcf.getUsername());
			SshPrivateKeyFile pkFile = SshPrivateKeyFile.parse(new File(privateKeyFilePath));
			pk.setKey(pkFile.toPrivateKey(pkcf.getPassword()));
			return pk; 
		}
			CredentialFactory usercf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
			if (StringUtils.isNotEmpty(usercf.getPassword())) {
			PasswordAuthenticationClient pac = new PasswordAuthenticationClient();
			pac.setUsername(usercf.getUsername());
			pac.setPassword(usercf.getPassword());
			return pac;
		}
		throw new Exception("Unknown authentication type, either the password or the privateKeyFile must be filled");
	}


	
	protected void checkReply(String cmd) throws IOException  {
		if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
			throw new IOException("Command [" + cmd + "] returned error [" + ftpClient.getReplyCode() + "]: " + ftpClient.getReplyString());
		} 
		if (log.isDebugEnabled()) log.debug("Command [" + cmd + "] returned " + ftpClient.getReplyString());
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
			if (isPassive()) {
				ftpClient.enterLocalPassiveMode();
			}
			CredentialFactory usercf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
			ftpClient.login(usercf.getUsername(), usercf.getPassword());
	
			if (! StringUtils.isEmpty(remoteDirectory)) {
				ftpClient.changeWorkingDirectory(remoteDirectory);
				checkReply("changeWorkingDirectory "+remoteDirectory);
			}
			
			if (StringUtils.isNotEmpty(fileType)) {
				ftpClient.setFileType(getFileTypeIntValue());
				checkReply("setFileType "+remoteDirectory);
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
					ftpClient.quit();
					log.debug(ftpClient.getReplyString());
					ftpClient.disconnect();
				}
				catch(Exception e) {
					log.error("Error while closeing FtpClient", e);
				}
			}
			ftpClient = null;
		}
	}
	
	public String put(ParameterList params, IPipeLineSession session, String message, String remoteDirectory, String remoteFilenamePattern, boolean closeAfterSend) throws Exception {
		if (messageIsContent) {
			return _put(params, session, message, remoteDirectory, remoteFilenamePattern, closeAfterSend);
		}
		List remoteFilenames = _put(params, session, FileUtils.getListFromNames(message, ';'), remoteDirectory, remoteFilenamePattern, closeAfterSend);
		return FileUtils.getNamesFromList(remoteFilenames, ';');	
	}
	
	/**
	 * Transfers the contents of a stream to a file on the server.
	 * 
	 * @param contents
	 * @param remoteDirectory
	 * @param remoteFilenamePattern
	 * @param closeAfterSend
	 * @return name of the create remote file
	 * @throws Exception
	 */
	private String _put(ParameterList params, IPipeLineSession session, String contents, String remoteDirectory, String remoteFilenamePattern, boolean closeAfterSend) throws Exception {
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
				checkReply("storeFile "+remoteFilename);
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
	private List _put(ParameterList params, IPipeLineSession session, List filenames, String remoteDirectory, String remoteFilenamePattern, boolean closeAfterSend) throws Exception {
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
						checkReply("storeFile "+remoteFilename);
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
			return FileUtils.getListFromNames(ftpClient.listNames());
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
	
	public String get(ParameterList params, IPipeLineSession session, String localDirectory, String remoteDirectory, String filenames, String localFilenamePattern, boolean closeAfterGet) throws Exception {
		if (messageIsContent) {
			return _get(remoteDirectory, FileUtils.getListFromNames(filenames, ';'), closeAfterGet);
		}
		List result = _get(params, session, localDirectory, remoteDirectory, FileUtils.getListFromNames(filenames, ';'), localFilenamePattern, closeAfterGet);
		return FileUtils.getNamesFromList(result, ';');	
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
						checkReply("retrieve "+remoteFilename);
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
	private List _get(ParameterList params, IPipeLineSession session, String localDirectory, String remoteDirectory, List filenames, String localFilenamePattern, boolean closeAfterGet) throws Exception {
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
						checkReply("retrieve "+remoteFilename);
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


	public void setHost(String string) {
		host = string;
	}
	String getHost() {
		return host;
	}
	
	public void setPort(int i) {
		port = i;
	}
	int getPort() {
		return port;
	}



	public void setAuthAlias(String string) {
		authAlias = string;
	}
	public String getAuthAlias() {
		return authAlias;
	}

	public void setUsername(String string) {
		username = string;
	}
	String getUsername() {
		return username;
	}

	public void setPassword(String string) {
		password = string;
	}
	String getPassword() {
		return password;
	}
	

	
	public void setProxyHost(String string) {
		proxyHost = string;
	}
	String getProxyHost() {
		return proxyHost;
	}

	public void setProxyPort(int i) {
		proxyPort = i;
	}
	int getProxyPort() {
		return proxyPort;
	}

	public void setProxyAuthAlias(String string) {
		proxyAuthAlias = string;
	}
	public String getProxyAuthAlias() {
		return proxyAuthAlias;
	}

	public void setProxyUsername(String string) {
		proxyUsername = string;
	}
	String getProxyUsername() {
		return proxyUsername;
	}

	public void setProxyPassword(String string) {
		proxyPassword = string;
	}
	String getProxyPassword() {
		return proxyPassword;
	}


	int getFtpType() {
		return ftpType;
	}
	public void setFtpTypeDescription(String string) {
		ftpTypeDescription = string;
	}
	String getFtpTypeDescription() {
		return ftpTypeDescription;
	}


	public void setFileType(String string) {
		fileType = string;
	}

	public void setMessageIsContent(boolean b) {
		messageIsContent = b;
	}
	boolean isMessageIsContent() {
		return messageIsContent;
	}

	public void setPassive(boolean b) {
		passive = b;
	}
	public boolean isPassive() {
		return passive;
	}



	public void setProxyTransportType(int i) {
		proxyTransportType = i;
	}
	int getProxyTransportType() {
		return proxyTransportType;
	}


	public void setPrefCSEncryption(String string) {
		prefCSEncryption = string;
	}
	String getPrefCSEncryption() {
		return prefCSEncryption;
	}

	public void setPrefSCEncryption(String string) {
		prefSCEncryption = string;
	}
	String getPrefSCEncryption() {
		return prefSCEncryption;
	}


	public void setPrivateKeyFilePath(String string) {
		privateKeyFilePath = string;
	}
	String getPrivateKeyFilePath() {
		return privateKeyFilePath;
	}
	
	public void setPrivateKeyAuthAlias(String string) {
		privateKeyAuthAlias = string;
	}
	public String getPrivateKeyAuthAlias() {
		return privateKeyAuthAlias;
	}

	public void setPrivateKeyPassword(String password) {
		privateKeyPassword = password;
	}
	String getPrivateKeyPassword() {
		return privateKeyPassword;
	}


	public void setKnownHostsPath(String string) {
		knownHostsPath = string;
	}
	String getKnownHostsPath() {
		return knownHostsPath;
	}

	public void setConsoleKnownHostsVerifier(boolean b) {
		consoleKnownHostsVerifier = b;
	}
	public boolean isConsoleKnownHostsVerifier() {
		return consoleKnownHostsVerifier;
	}





	public void setCertificate(String string) {
		certificate = string;
	}
	String getCertificate() {
		return certificate;
	}

	public void setCertificateType(String string) {
		certificateType = string;
	}
	String getCertificateType() {
		return certificateType;
	}

	public void setCertificateAuthAlias(String string) {
		certificateAuthAlias = string;
	}
	public String getCertificateAuthAlias() {
		return certificateAuthAlias;
	}

	public void setCertificatePassword(String string) {
		certificatePassword = string;
	}
	String getCertificatePassword() {
		return certificatePassword;
	}

	public String getKeyManagerAlgorithm() {
		return keyManagerAlgorithm;
	}

	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
	}


	public void setTruststore(String string) {
		truststore = string;
	}
	String getTruststore() {
		return truststore;
	}

	public void setTruststoreType(String string) {
		truststoreType = string;
	}
	String getTruststoreType() {
		return truststoreType;
	}

	public void setTruststoreAuthAlias(String string) {
		truststoreAuthAlias = string;
	}
	public String getTruststoreAuthAlias() {
		return truststoreAuthAlias;
	}

	public void setTruststorePassword(String string) {
		truststorePassword = string;
	}
	String getTruststorePassword() {
		return truststorePassword;
	}

	public String getTrustManagerAlgorithm() {
		return trustManagerAlgorithm;
	}

	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		this.trustManagerAlgorithm = trustManagerAlgorithm;
	}



	public void setJdk13Compatibility(boolean b) {
		jdk13Compatibility = b;
	}
	boolean isJdk13Compatibility() {
		return jdk13Compatibility;
	}

	public void setVerifyHostname(boolean b) {
		verifyHostname = b;
	}
	boolean isVerifyHostname() {
		return verifyHostname;
	}

	public void setAllowSelfSignedCertificates(boolean b) {
		allowSelfSignedCertificates = b;
	}
	boolean isAllowSelfSignedCertificates() {
		return allowSelfSignedCertificates;
	}


	public void setProtP(boolean b) {
		protP = b;
	}
	boolean isProtp() {
		return protP;
	}

	public boolean isKeyboardInteractive() {
		return keyboardInteractive;
	}

	public void setKeyboardInteractive(boolean keyboardInteractive) {
		this.keyboardInteractive = keyboardInteractive;
	}

}
