/*
   Copyright 2013 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS4;
import com.jcraft.jsch.ProxySOCKS5;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.encryption.AuthSSLContextFactory;
import nl.nn.adapterframework.encryption.HasKeystore;
import nl.nn.adapterframework.encryption.HasTruststore;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Helper class for sftp and ftp.
 * 
 *
 * 
 * @author John Dekker
 */
public class SftpSession implements IConfigurable, HasKeystore, HasTruststore {
	private static final Logger LOG = LogUtil.getLogger(SftpSession.class);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	public enum TransportType {
		HTTP, SOCKS4, SOCKS5
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

	// configuration property for sftp
	private TransportType proxyTransportType = TransportType.SOCKS5;
	private String prefCSEncryption = null;
	private String prefSCEncryption = null;
	private String privateKeyFilePath = null;
	private @Getter String privateKeyAuthAlias;
	private @Getter String privateKeyPassword = null;
	private String knownHostsPath = null;
	private boolean strictHostKeyChecking = true;

	// configuration parameters for SSL Context and SocketFactory
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

	private JSch jsch;
	private ChannelSftp sftpClient;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(host)) {
			throw new ConfigurationException("Attribute [host] is not set");
		}
		if (StringUtils.isEmpty(username) && StringUtils.isEmpty(getAuthAlias()) && StringUtils.isEmpty(privateKeyAuthAlias)) {
			throw new ConfigurationException("Neither attribute 'username' nor 'authAlias' nor 'privateKeyAuthAlias' is set");
		}

		AuthSSLContextFactory.verifyKeystoreConfiguration(this, this);

		try {
			jsch = new JSch();
			if (StringUtils.isNotEmpty(privateKeyFilePath)) {
				CredentialFactory pkcf = new CredentialFactory(getPrivateKeyAuthAlias(), getUsername(), getPrivateKeyPassword());
				jsch.addIdentity(privateKeyFilePath, pkcf.getPassword());
			}

			if (StringUtils.isNotEmpty(knownHostsPath)) {
				jsch.setKnownHosts(knownHostsPath);
			}
		} catch (JSchException e) {
			throw new ConfigurationException("unable to configure Java Secure Channel", e);
		}
	}

	public synchronized ChannelSftp openClient(String remoteDirectory) throws FtpConnectException {
		LOG.debug("open sftp client");
		if (sftpClient == null || sftpClient.isClosed()) {
			openSftpClient(remoteDirectory);
		}
		return sftpClient;
	}

	private void openSftpClient(String remoteDirectory) throws FtpConnectException {
		try {
			Session sftpSession = createSftpSession(jsch);
			ChannelSftp channel = (ChannelSftp) sftpSession.openChannel("sftp");
			channel.connect();

			if (StringUtils.isNotEmpty(remoteDirectory)) {
				channel.cd(remoteDirectory);
			}

			sftpClient = channel;
		} catch (JSchException e) {
			throw new FtpConnectException("unable to open SFTP channel");
		} catch (SftpException e) {
			throw new FtpConnectException("unable to enter remote directory ["+remoteDirectory+"]");
		}
	}

	private Session createSftpSession(JSch jsch) throws FtpConnectException {
		try {
			final CredentialFactory credentialFactory = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());

			Session sftpSession = jsch.getSession(credentialFactory.getUsername(), host, port);

			if (StringUtils.isNotEmpty(getPassword())) {
				sftpSession.setConfig("PreferredAuthentications", "password");
				sftpSession.setPassword(getPassword());
			} else {
				sftpSession.setConfig("PreferredAuthentications", "publickey");
			}

			if(!strictHostKeyChecking) {
				sftpSession.setConfig("StrictHostKeyChecking", "no");
			}

			// Set the connection properties and if necessary the proxy properties
			if (StringUtils.isNotEmpty(prefCSEncryption)) {
				sftpSession.setConfig("cipher.s2c", prefCSEncryption);
			}
			if (StringUtils.isNotEmpty(prefSCEncryption)) {
				sftpSession.setConfig("cipher.s2c", prefSCEncryption);
			}

			if (! StringUtils.isEmpty(proxyHost)) {
				sftpSession.setProxy(createProxy());
			}

			// make a secure connection with the remote host
			SSLContext sslContext = AuthSSLContextFactory.createSSLContext(this, this, "TLS");
			sftpSession.setSocketFactory(new SftpSocketFactory(sslContext));

			sftpSession.connect();

			if (!sftpSession.isConnected()) {
				throw new FtpConnectException("Could not authenticate to sftp server");
			}

			return sftpSession;
		}
		catch(JSchException  e) {
			throw new FtpConnectException(e);
		}
	}

	private Proxy createProxy() {
		CredentialFactory pcf = new CredentialFactory(getProxyAuthAlias(), proxyUsername, proxyPassword);
		switch (proxyTransportType) {
			case HTTP: {
				ProxyHTTP proxy = new ProxyHTTP(proxyHost, proxyPort);
				if (StringUtils.isNotEmpty(pcf.getUsername())) {
					proxy.setUserPasswd(pcf.getUsername(), pcf.getPassword());
				}
				return proxy;
			}
			case SOCKS4: {
				ProxySOCKS4 proxy = new ProxySOCKS4(proxyHost, proxyPort);
				if (StringUtils.isNotEmpty(pcf.getUsername())) {
					proxy.setUserPasswd(pcf.getUsername(), pcf.getPassword());
				}
				return proxy;
			}
			case SOCKS5: {
				ProxySOCKS5 proxy = new ProxySOCKS5(proxyHost, proxyPort);
				if (StringUtils.isNotEmpty(pcf.getUsername())) {
					proxy.setUserPasswd(pcf.getUsername(), pcf.getPassword());
				}
				return proxy;
			}
			default:
				throw new IllegalStateException("proxy type does not exist");
		}
	}

	public static void close(ChannelSftp sftpClient) {
		LOG.debug("closing ftp client");
		if (sftpClient != null && sftpClient.isConnected()) {
			sftpClient.disconnect();
		}
	}

	public void close() {
		close(sftpClient);
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

	/**
	 * Transport type in case of sftp
	 * @ff.default SOCKS5
	 */
	public void setProxyTransportType(TransportType type) {
		proxyTransportType = type;
	}

	/** Optional preferred encryption from client to server for sftp protocol */
	public void setPrefCSEncryption(String string) {
		prefCSEncryption = string;
	}

	/** Optional preferred encryption from server to client for sftp protocol */
	public void setPrefSCEncryption(String string) {
		prefSCEncryption = string;
	}

	/** Path to private key file for sftp authentication */
	public void setPrivateKeyFilePath(String string) {
		privateKeyFilePath = string;
	}

	/** Name of the alias to obtain credentials for passphrase of private key file */
	public void setPrivateKeyAuthAlias(String string) {
		privateKeyAuthAlias = string;
	}

	/** Passphrase of private key file */
	public void setPrivateKeyPassword(String password) {
		privateKeyPassword = password;
	}

	/** Path to file with knownhosts */
	public void setKnownHostsPath(String string) {
		knownHostsPath = string;
	}

	/**
	 * Verify the hosts againt the knownhosts file.
	 * @ff.default true
	 */
	public void setStrictHostKeyChecking(boolean b) {
		strictHostKeyChecking = b;
	}

	/** Resource url to keystore or certificate to be used for authentication. If none specified, the JVMs default keystore will be used. */
	@Override
	public void setKeystore(String string) {
		keystore = string;
	}

	/** Type of keystore 
	 * @ff.default pkcs12
	 */
	@Override
	public void setKeystoreType(KeystoreType value) {
		keystoreType = value;
	}

	/** Authentication alias used to obtain keystore password */
	@Override
	public void setKeystoreAuthAlias(String string) {
		keystoreAuthAlias = string;
	}

	/** Default password to access keystore */
	@Override
	public void setKeystorePassword(String string) {
		keystorePassword = string;
	}

	/** Key manager algorithm. Can be left empty to use the servers default algorithm */
	@Override
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
	}

	/** Alias to obtain specific certificate or key in keystore */
	@Override
	public void setKeystoreAlias(String string) {
		keystoreAlias = string;
	}

	/** Authentication alias to authenticate access to certificate or key indicated by <code>keystoreAlias</code> */
	@Override
	public void setKeystoreAliasAuthAlias(String string) {
		keystoreAliasAuthAlias = string;
	}

	/** Default password to authenticate access to certificate or key indicated by <code>keystoreAlias</code> */
	@Override
	public void setKeystoreAliasPassword(String string) {
		keystoreAliasPassword = string;
	}

	/** Resource url to truststore to be used for authenticating peer. If none specified, the JVMs default truststore will be used. */
	@Override
	public void setTruststore(String string) {
		truststore = string;
	}

	/** Type of truststore 
	 * @ff.default jks
	 */
	@Override
	public void setTruststoreType(KeystoreType value) {
		truststoreType = value;
	}

	/** Authentication alias used to obtain truststore password */
	@Override
	public void setTruststoreAuthAlias(String string) {
		truststoreAuthAlias = string;
	}

	/** Default password to access truststore */
	@Override
	public void setTruststorePassword(String string) {
		truststorePassword = string;
	}

	/** Trust manager algorithm. Can be left empty to use the servers default algorithm */
	@Override
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		this.trustManagerAlgorithm = trustManagerAlgorithm;
	}

	/** If <code>true</code>, the hostname in the certificate will be checked against the actual hostname of the peer */
	@Override
	public void setVerifyHostname(boolean b) {
		verifyHostname = b;
	}

	/** If <code>true</code>, self signed certificates are accepted
	 * @ff.default false
	 */
	@Override
	public void setAllowSelfSignedCertificates(boolean b) {
		allowSelfSignedCertificates = b;
	}

	/**
	 * If <code>true</code>, CertificateExpiredExceptions are ignored
	 * @ff.default false
	 */
	@Override
	public void setIgnoreCertificateExpiredException(boolean b) {
		ignoreCertificateExpiredException = b;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
}
