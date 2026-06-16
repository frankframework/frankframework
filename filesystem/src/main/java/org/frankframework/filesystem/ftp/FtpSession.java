/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.filesystem.ftp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IConfigurable;
import org.frankframework.doc.DocumentedEnum;
import org.frankframework.doc.EnumLabel;
import org.frankframework.encryption.AuthSSLContextFactory;
import org.frankframework.encryption.HasKeystore;
import org.frankframework.encryption.HasTruststore;
import org.frankframework.encryption.KeystoreConfiguration;
import org.frankframework.encryption.TruststoreConfiguration;
import org.frankframework.filesystem.FileSystemException;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.LogUtil;

/**
 * Base class for FTP(s) connections
 *
 * @author John Dekker
 */
public class FtpSession implements IConfigurable, HasKeystore, HasTruststore {
	private static final Logger LOG = LogUtil.getLogger(FtpSession.class);
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private @Getter KeystoreConfiguration keystoreConfiguration = createKeystoreConfiguration();
	private @Getter TruststoreConfiguration truststoreConfiguration = createTruststoreConfiguration();

	private @Getter FtpType ftpType = FtpType.FTP;

	public enum FtpType implements DocumentedEnum {
		@EnumLabel("FTP") FTP(null, true),
		@EnumLabel("FTPSI") FTPS_IMPLICIT("TLS", true),
		@EnumLabel("FTPSX(TLS)") FTPS_EXPLICIT_TLS("TLS", false),
		@EnumLabel("FTPSX(SSL)") FTPS_EXPLICIT_SSL("SSL", false);

		private final @Getter boolean implicit;
		private final @Getter String protocol;
		FtpType(String protocol, boolean implicit) {
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

		final int ftpFileType;

		FileType(int ftpFileType) {
			this.ftpFileType=ftpFileType;
		}
	}

	public enum TransportType {
		DIRECT, HTTP, SOCKS
	}

	// configuration parameters, global for all types
	private @Getter String host;
	private @Getter int port = 21;
	private @Getter String authAlias;
	private @Getter String username;
	private @Getter String password;

	private @Getter String proxyHost;
	private @Getter int proxyPort = 1080;
	private TransportType proxyTransportType = TransportType.SOCKS;

	private @Getter FileType fileType = null;
	private @Getter boolean passive=true;

	private FTPClient ftpClient;

	@Override
	public void configure() throws ConfigurationException {
		if (getFtpType() == null) {
			throw new ConfigurationException("Attribute [ftpType] is not set");
		}

		if (StringUtils.isEmpty(host)) {
			throw new ConfigurationException("Attribute [host] is not set");
		}

		AuthSSLContextFactory.verifyKeystoreConfiguration(this, this);
	}

	public synchronized FTPClient openClient(String remoteDirectory) throws FileSystemException {
		LOG.debug("open ftp client");
		if (ftpClient == null || ! ftpClient.isConnected()) {
			openFtpClient(remoteDirectory);
		}
		return ftpClient;
	}

	private void openFtpClient(String remoteDirectory) throws FileSystemException {
		try {
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

			if (fileType != null) {
				ftpClient.setFileType(fileType.ftpFileType);
				checkReply("setFileType "+remoteDirectory);
			}
		} catch(Exception e) {
			close(ftpClient);
			throw new FileSystemException("Cannot connect to the FTP server with domain ["+getHost()+"]", e);
		}
	}

	private void checkReply(String cmd) throws IOException  {
		if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
			throw new IOException("Command [" + cmd + "] returned error [" + ftpClient.getReplyCode() + "]: " + ftpClient.getReplyString());
		}
		LOG.debug("Command [{}] returned {}", () -> cmd, ftpClient::getReplyString);
	}

	private FTPClient createFTPClient() throws IOException, GeneralSecurityException {
		FtpType transport = getFtpType();
		Proxy proxy = getProxy();
		if (transport == FtpType.FTP) {
			FTPClient client = new FTPClient();
			if(proxy != null) {
				client.setProxy(proxy); // May not set NULL
			}
			return client;
		}

		SSLContext sslContext = AuthSSLContextFactory.createSSLContext(this, this, transport.getProtocol());
		FTPSClient client = new FTPSClient(transport.isImplicit(), sslContext);

		if(proxy != null) {
			client.setProxy(proxy);
			client.setSocketFactory(sslContext.getSocketFactory()); // Have to set the SocketFactory again
		}

		if(isVerifyHostname()) {
			client.setTrustManager(null); // When NULL it overrides the default 'ValidateServerCertificateTrustManager' and uses the JVM Default
		}

		if(prot != Prot.C) { // Have to check if not C because that removes the SSLSocketFactory
			client.execPROT(prot.name());
		}

		return client;
	}

	private Proxy getProxy() {
		if (StringUtils.isNotEmpty(proxyHost)) {
			Proxy.Type type;
			switch (proxyTransportType) {
			case DIRECT:
				type = Proxy.Type.DIRECT;
				break;
			case SOCKS:
				type = Proxy.Type.SOCKS;
				break;
			case HTTP:
				type = Proxy.Type.HTTP;
				break;
			default:
				throw new IllegalStateException("invalid proxy type");
			}
			return new Proxy(type, new InetSocketAddress(host, port));
		}
		return null;
	}

	// Used in tests
	protected static void close(FTPClient ftpClient) {
		if (ftpClient != null && ftpClient.isConnected()) {
			LOG.debug("closing ftp client");
			try {
				ftpClient.disconnect();
			}
			catch(Exception e) {
				LOG.error("error while closeing FtpClient", e);
			}
		}
	}

	public void close() {
		close(ftpClient);
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
	 * If <code>true</code>, passive ftp is used: before data is sent, a pasv command is issued, and the connection is set up by the server
	 * @ff.default true
	 */
	public void setPassive(boolean b) {
		passive = b;
	}

	/**
	 * (ftps) Transport type in case of sftp
	 * @ff.default SOCKS5
	 */
	public void setProxyTransportType(TransportType type) {
		proxyTransportType = type;
	}

	/**
	 * Sets the <code>Data Channel Protection Level</code>.
	 * @ff.default C
	 */
	public void setProt(Prot prot) {
		this.prot = prot;
	}

	@Override
	public void setKeystoreConfiguration(KeystoreConfiguration keystoreConfiguration) {
		this.keystoreConfiguration = keystoreConfiguration;
	}

	@Override
	public void setTruststoreConfiguration(TruststoreConfiguration truststoreConfiguration) {
		this.truststoreConfiguration = truststoreConfiguration;
	}
}
