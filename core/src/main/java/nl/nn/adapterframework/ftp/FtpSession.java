/*
   Copyright 2013 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

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

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.doc.DocumentedEnum;
import nl.nn.adapterframework.doc.EnumLabel;
import nl.nn.adapterframework.encryption.AuthSSLContextFactory;
import nl.nn.adapterframework.encryption.HasKeystore;
import nl.nn.adapterframework.encryption.HasTruststore;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Helper class for sftp and ftp.
 *
 *
 *
 * @author John Dekker
 */
public class FtpSession implements IConfigurable, HasKeystore, HasTruststore {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private @Getter FtpType ftpType = FtpType.FTP;
	public enum FtpType implements DocumentedEnum {
		@EnumLabel("FTP") FTP(null, true),
		@EnumLabel("SFTP") SFTP(null, true),
		@EnumLabel("FTPSI") FTPS_IMPLICIT("TLS", true),
		@EnumLabel("FTPSX(TLS)") FTPS_EXPLICIT_TLS("TLS", false),
		@EnumLabel("FTPSX(SSL)") FTPS_EXPLICIT_SSL("SSL", false);

		private @Getter boolean implicit;
		private @Getter String protocol;
		private FtpType(String protocol, boolean implicit) {
			this.protocol = protocol;
			this.implicit = implicit;
		}
	}

	private @Getter Prot prot = Prot.C;
	public enum Prot {
		/** Clear */
		C,
		/** Safe(SSL protocol only) */
		S,
		/** Confidential(SSL protocol only) */
		E,
		/** Private */
		P
	}

	public enum FileType {

		ASCII(org.apache.commons.net.ftp.FTP.ASCII_FILE_TYPE),
		BINARY(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);

		int ftpFileType;

		private FileType(int ftpFileType) {
			this.ftpFileType=ftpFileType;
		}
	}

	private @Getter String name;

	// configuration parameters, global for all types
	private @Getter String host;
	private @Getter int port = 21;
	private @Getter String authAlias;
	private @Getter String username;
	private @Getter String password;
	private @Getter String proxyHost;
	private @Getter int proxyPort = 1080;
	private @Getter String proxyAuthAlias;
	private @Getter String proxyUsername;
	private @Getter String proxyPassword;
	private @Getter FileType fileType = null;
	private @Getter boolean messageIsContent=false;
	private @Getter boolean passive=true;
	private @Getter boolean keyboardInteractive=false;

	// configuration property for sftp
	private int proxyTransportType = SshConnectionProperties.USE_SOCKS5_PROXY;
	private String prefCSEncryption = null;
	private String prefSCEncryption = null;
	private String privateKeyFilePath = null;
	private @Getter String privateKeyAuthAlias;
	private @Getter String privateKeyPassword = null;
	private String knownHostsPath = null;
	private boolean consoleKnownHostsVerifier = false;

	// configuration parameters for ftps
	private @Getter String keystore;
	private @Getter String keystoreAuthAlias;
	private @Getter String keystorePassword;
	private @Getter KeystoreType keystoreType = KeystoreType.PKCS12;
	private @Getter String keystoreAlias;
	private @Getter String keystoreAliasAuthAlias;
	private @Getter String keystoreAliasPassword;
	private @Getter String keyManagerAlgorithm=null;
	private @Getter String truststore = null;
	private @Getter KeystoreType truststoreType=KeystoreType.JKS;
	private @Getter String truststoreAuthAlias;
	private @Getter String truststorePassword = null;
	private @Getter String trustManagerAlgorithm=null;
	private @Getter boolean verifyHostname = true;
	private @Getter boolean allowSelfSignedCertificates = false;
	private @Getter boolean ignoreCertificateExpiredException = false;

	private SshClient sshClient;
	private SftpClient sftpClient;
	public FTPClient ftpClient;

	@Override
	public void configure() throws ConfigurationException {
		if (getFtpType() == null) {
			throw new ConfigurationException("Attribute [ftpType] is not set");
		}

		if (StringUtils.isEmpty(host)) {
			throw new ConfigurationException("Attribute [host] is not set");
		}
		if (StringUtils.isEmpty(username) && StringUtils.isEmpty(getAuthAlias())) {
			if (ftpType != FtpType.SFTP) {
				throw new ConfigurationException("Neither attribute 'username' nor 'authAlias' is set");
			}
			else if (StringUtils.isEmpty(privateKeyAuthAlias)) {
				throw new ConfigurationException("Neither attribute 'username' nor 'authAlias' nor 'privateKeyAuthAlias' is set");
			}
		}
		if (proxyTransportType < 1 && proxyTransportType > 4) {
			throw new ConfigurationException("Incorrect value for [proxyTransportType]");
		}

		AuthSSLContextFactory.verifyKeystoreConfiguration(this, this);
	}

	public void openClient(String remoteDirectory) throws FtpConnectException {
		log.debug("Open ftp client");
		if (ftpType == FtpType.SFTP) {
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
						@Override
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

			if (fileType!=null) {
				ftpClient.setFileType(fileType.ftpFileType);
				checkReply("setFileType "+remoteDirectory);
			}
		}
		catch(Exception e) {
			closeFtpClient();
			throw new FtpConnectException(e);
		}
	}

	private FTPClient createFTPClient() throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException {
		FtpType transport = getFtpType();
		if (transport == FtpType.FTP) {
			return new FTPClient();
		}

		SSLContext sslContext = AuthSSLContextFactory.createSSLContext(this, this, getFtpType().getProtocol());
		FTPSClient client = new FTPSClient(sslContext);
		if(isVerifyHostname()) {
			client.setTrustManager(null);//When NULL it overrides the default 'ValidateServerCertificateTrustManager'
		}
		client = new FTPSClient(transport.getProtocol(), transport.isImplicit());

		if(prot != Prot.C) {
			client.execPROT(prot.name());
		}

		return client;
	}


	public void closeClient() {
		log.debug("Close ftp client");
		if (ftpType == FtpType.SFTP) {
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

	public String put(ParameterList params, PipeLineSession session, String message, String remoteDirectory, String remoteFilenamePattern, boolean closeAfterSend) throws Exception {
		if (messageIsContent) {
			return _put(params, session, message, remoteDirectory, remoteFilenamePattern, closeAfterSend);
		}
		List<String> remoteFilenames = _put(params, session, FileUtils.getListFromNames(message, ';'), remoteDirectory, remoteFilenamePattern, closeAfterSend);
		return FileUtils.getNamesFromList(remoteFilenames, ';');
	}

	/**
	 * Transfers the contents of a stream to a file on the server.
	 * @return name of the create remote file
	 */
	private String _put(ParameterList params, PipeLineSession session, String contents, String remoteDirectory, String remoteFilenamePattern, boolean closeAfterSend) throws Exception {
		openClient(remoteDirectory);

		// get remote name
		String remoteFilename = FileUtils.getFilename(params, session, (File)null, remoteFilenamePattern);

		// open local file
		InputStream is = new ByteArrayInputStream(contents.getBytes());
		try {
			if (ftpType == FtpType.SFTP) {
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
	 * @return list of remotely created files
	 */
	private List<String> _put(ParameterList params, PipeLineSession session, List<String> filenames, String remoteDirectory, String remoteFilenamePattern, boolean closeAfterSend) throws Exception {
		openClient(remoteDirectory);

		try {
			LinkedList<String> remoteFilenames = new LinkedList<String>();
			for (Iterator<String> filenameIt = filenames.iterator(); filenameIt.hasNext(); ) {
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
					if (ftpType == FtpType.SFTP) {
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

	public List<String> ls(String remoteDirectory, boolean filesOnly, boolean closeAfterSend) throws Exception {
		openClient(remoteDirectory);

		try {
			if (ftpType == FtpType.SFTP) {
				List<String> result = new LinkedList<>();
				List<?> listOfSftpFiles = sftpClient.ls();
				for (Object listOfSftpFile : listOfSftpFiles) {
					SftpFile file = (SftpFile) listOfSftpFile;
					String filename = file.getFilename();
					if (filesOnly || (!file.isDirectory())) {
						if (!filename.startsWith(".")) {
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
		List<String> result = ls(remoteDirectory, filesOnly, closeAfterSend);
		return FileUtils.getNamesFromList(result, ';');
	}

	public String get(ParameterList params, PipeLineSession session, String localDirectory, String remoteDirectory, String filenames, String localFilenamePattern, boolean closeAfterGet) throws Exception {
		if (messageIsContent) {
			return _get(remoteDirectory, FileUtils.getListFromNames(filenames, ';'), closeAfterGet);
		}
		List<String> result = _get(params, session, localDirectory, remoteDirectory, FileUtils.getListFromNames(filenames, ';'), localFilenamePattern, closeAfterGet);
		return FileUtils.getNamesFromList(result, ';');
	}

	public void deleteRemote(String remoteDirectory, String filename, boolean closeAfterDelete) throws Exception {
		openClient(remoteDirectory);

		try {
			if (ftpType == FtpType.SFTP) {
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
	 * @return concatenation of the contents of all received files
	 */
	private String _get(String remoteDirectory, List<String> filenames, boolean closeAfterGet) throws Exception {
		openClient(remoteDirectory);

		try {
			StringBuffer result = new StringBuffer();
			for (Iterator<String> filenameIt = filenames.iterator(); filenameIt.hasNext(); ) {
				String remoteFilename = filenameIt.next();
				OutputStream os = null;

				os = new ByteArrayOutputStream();

				try {
					if (ftpType == FtpType.SFTP) {
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
	 * Returns a list as separated string of filenames of locally created files
	 */
	private List<String> _get(ParameterList params, PipeLineSession session, String localDirectory, String remoteDirectory, List<String> filenames, String localFilenamePattern, boolean closeAfterGet) throws Exception {
		openClient(remoteDirectory);

		try {
			LinkedList<String> remoteFilenames = new LinkedList<String>();
			for (Iterator<String> filenameIt = filenames.iterator(); filenameIt.hasNext(); ) {
				String remoteFilename = (String)filenameIt.next();

				String localFilename = remoteFilename;
				if (! StringUtils.isEmpty(localFilenamePattern)) {
					localFilename = FileUtils.getFilename(params, session, remoteFilename, localFilenamePattern);
				}

				File localFile = new File(localDirectory, localFilename);
				OutputStream os = new FileOutputStream(localFile,false);
				try {
					if (ftpType == FtpType.SFTP) {
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

	/** Name or ip address of remote host */
	public void setHost(String string) {
		host = string;
	}

	/**
	 * Port number of remote host
	 * @ff.default 21
	 */
	public void setPort(int i) {
		port = i;
	}

	/** Name of the alias to obtain credentials to authenticatie on remote server */
	public void setAuthAlias(String string) {
		authAlias = string;
	}

	/** Name of the user to authenticatie on remote server */
	public void setUsername(String string) {
		username = string;
	}

	/** Password to authenticatie on remote server */
	public void setPassword(String string) {
		password = string;
	}

	/** Proxy hostname */
	public void setProxyHost(String string) {
		proxyHost = string;
	}

	/**
	 * Proxy port
	 * @ff.default 1080
	 */
	public void setProxyPort(int i) {
		proxyPort = i;
	}

	/** alias to obtain credentials to authenticate on proxy */
	public void setProxyAuthAlias(String string) {
		proxyAuthAlias = string;
	}

	/** Default user name in case proxy requires authentication */
	public void setProxyUsername(String string) {
		proxyUsername = string;
	}

	/** Default password in case proxy requires authentication */
	public void setProxyPassword(String string) {
		proxyPassword = string;
	}

	@Deprecated
	@ConfigurationWarning("use attribute ftpType instead")
	public void setFtpTypeDescription(FtpType value) {
		setFtpType(value);
	}
	/**
	 * FTP protocol to use
	 * @ff.default FTP
	 */
	public void setFtpType(FtpType value) {
		ftpType = value;
	}

	public void setFileType(FileType value) {
		fileType = value;
	}

	/**
	 * If <code>true</code>, the contents of the message is send, otherwise it message contains the local filenames of the files to be send
	 * @ff.default false
	 */
	public void setMessageIsContent(boolean b) {
		messageIsContent = b;
	}

	/**
	 * If <code>true</code>, passive ftp is used: before data is sent, a pasv command is issued, and the connection is set up by the server
	 * @ff.default true
	 */
	public void setPassive(boolean b) {
		passive = b;
	}

	/**
	 * (sftp) Transport type in case of sftp (1=standard, 2=http, 3=socks4, 4=socks5)
	 * @ff.default 4
	 */
	public void setProxyTransportType(int i) {
		proxyTransportType = i;
	}

	/** (sftp) Optional preferred encryption from client to server for sftp protocol */
	public void setPrefCSEncryption(String string) {
		prefCSEncryption = string;
	}

	/** (sftp) Optional preferred encryption from server to client for sftp protocol */
	public void setPrefSCEncryption(String string) {
		prefSCEncryption = string;
	}

	/** (sftp) Path to private key file for sftp authentication */
	public void setPrivateKeyFilePath(String string) {
		privateKeyFilePath = string;
	}

	/** (sftp) Name of the alias to obtain credentials for passphrase of private key file */
	public void setPrivateKeyAuthAlias(String string) {
		privateKeyAuthAlias = string;
	}

	/** (sftp) Passphrase of private key file */
	public void setPrivateKeyPassword(String password) {
		privateKeyPassword = password;
	}

	/** (sftp) Path to file with knownhosts */
	public void setKnownHostsPath(String string) {
		knownHostsPath = string;
	}

	/**
	 * (sftp)
	 * @ff.default false
	 */
	public void setConsoleKnownHostsVerifier(boolean b) {
		consoleKnownHostsVerifier = b;
	}
	public boolean isConsoleKnownHostsVerifier() {
		return consoleKnownHostsVerifier;
	}


	@Deprecated
	@ConfigurationWarning("Please use attribute keystore instead")
	public void setCertificate(String string) {
		setKeystore(string);
	}
	@Deprecated
	@ConfigurationWarning("has been replaced with keystoreType")
	public void setCertificateType(KeystoreType value) {
		setKeystoreType(value);
	}
	@Deprecated
	@ConfigurationWarning("Please use attribute keystoreAuthAlias instead")
	public void setCertificateAuthAlias(String string) {
		setKeystoreAuthAlias(string);
	}
	@Deprecated
	@ConfigurationWarning("Please use attribute keystorePassword instead")
	public void setCertificatePassword(String string) {
		setKeystorePassword(string);
	}

	/** (ftps) Resource url to keystore or certificate to be used for authentication. If none specified, the JVMs default keystore will be used. */
	@Override
	public void setKeystore(String string) {
		keystore = string;
	}

	/** (ftps) Type of keystore
	 * @ff.default pkcs12
	 */
	@Override
	public void setKeystoreType(KeystoreType value) {
		keystoreType = value;
	}

	/** (ftps) Authentication alias used to obtain keystore password */
	@Override
	public void setKeystoreAuthAlias(String string) {
		keystoreAuthAlias = string;
	}

	/** (ftps) Default password to access keystore */
	@Override
	public void setKeystorePassword(String string) {
		keystorePassword = string;
	}

	/** (ftps) Key manager algorithm. Can be left empty to use the servers default algorithm */
	@Override
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
	}

	/** (ftps) Alias to obtain specific certificate or key in keystore */
	@Override
	public void setKeystoreAlias(String string) {
		keystoreAlias = string;
	}

	/** (ftps) Authentication alias to authenticate access to certificate or key indicated by <code>keystoreAlias</code> */
	@Override
	public void setKeystoreAliasAuthAlias(String string) {
		keystoreAliasAuthAlias = string;
	}

	/** (ftps) Default password to authenticate access to certificate or key indicated by <code>keystoreAlias</code> */
	@Override
	public void setKeystoreAliasPassword(String string) {
		keystoreAliasPassword = string;
	}



	/** (ftps) Resource url to truststore to be used for authenticating peer. If none specified, the JVMs default truststore will be used. */
	@Override
	public void setTruststore(String string) {
		truststore = string;
	}

	/** (ftps) Type of truststore
	 * @ff.default jks
	 */
	@Override
	public void setTruststoreType(KeystoreType value) {
		truststoreType = value;
	}

	/** (ftps) Authentication alias used to obtain truststore password */
	@Override
	public void setTruststoreAuthAlias(String string) {
		truststoreAuthAlias = string;
	}

	/** (ftps) Default password to access truststore */
	@Override
	public void setTruststorePassword(String string) {
		truststorePassword = string;
	}

	/** (ftps) Trust manager algorithm. Can be left empty to use the servers default algorithm */
	@Override
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		this.trustManagerAlgorithm = trustManagerAlgorithm;
	}

	/** (ftps) If <code>true</code>, the hostname in the certificate will be checked against the actual hostname of the peer */
	@Override
	public void setVerifyHostname(boolean b) {
		verifyHostname = b;
	}

	/** (ftps) If <code>true</code>, self signed certificates are accepted
	 * @ff.default false
	 */
	@Override
	public void setAllowSelfSignedCertificates(boolean b) {
		allowSelfSignedCertificates = b;
	}

	/**
	 * (ftps) If <code>true</code>, CertificateExpiredExceptions are ignored
	 * @ff.default false
	 */
	@Override
	public void setIgnoreCertificateExpiredException(boolean b) {
		ignoreCertificateExpiredException = b;
	}

	/**
	 * (ftps) if true, the server returns data via a SSL socket
	 * @ff.default false
	 */
	@Deprecated
	@ConfigurationWarning("use attribute prot=\"P\" instead")
	public void setProtP(boolean b) {
		prot = Prot.P;
	}

	/**
	 * Sets the <code>Data Channel Protection Level</code>.
	 * @ff.default C
	 */
	public void setProt(Prot prot) {
		this.prot = prot;
	}

	/**
	 * When <code>true</code>, keyboardinteractive is used to login
	 * @ff.default false
	 */
	public void setKeyboardInteractive(boolean keyboardInteractive) {
		this.keyboardInteractive = keyboardInteractive;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
}
