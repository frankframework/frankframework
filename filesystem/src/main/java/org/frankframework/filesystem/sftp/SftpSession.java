/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.filesystem.sftp;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import com.jcraft.jsch.ChannelExec;
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
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IConfigurable;
import org.frankframework.filesystem.FileSystemException;
import org.frankframework.util.CredentialFactory;

/**
 * Helper class for sftp.
 *
 * @author Niels Meijer
 */
@Log4j2
public class SftpSession implements IConfigurable {
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
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

	public synchronized ChannelSftp openClient(String remoteDirectory) throws FileSystemException {
		log.debug("open sftp client");
		if (sftpClient == null || sftpClient.isClosed()) {
			openSftpClient(remoteDirectory);
		}
		return sftpClient;
	}

	private void openSftpClient(String remoteDirectory) throws FileSystemException {
		try {
			Session sftpSession = createSftpSession(jsch);
			ChannelSftp channel = (ChannelSftp) sftpSession.openChannel("sftp");
			channel.connect();

			if (StringUtils.isNotEmpty(remoteDirectory)) {
				channel.cd(remoteDirectory);
			}

			sftpClient = channel;
		} catch (JSchException e) {
			throw new FileSystemException("unable to open SFTP channel");
		} catch (SftpException e) {
			throw new FileSystemException("unable to enter remote directory ["+remoteDirectory+"]");
		}
	}

	private Session createSftpSession(JSch jsch) throws FileSystemException {
		try {
			final CredentialFactory credentialFactory = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());

			Session sftpSession = jsch.getSession(credentialFactory.getUsername(), host, port);

			if (StringUtils.isNotEmpty(credentialFactory.getPassword())) {
				sftpSession.setConfig("PreferredAuthentications", "password");
				sftpSession.setPassword(credentialFactory.getPassword());
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
			sftpSession.connect();

			if (!sftpSession.isConnected()) {
				throw new FileSystemException("could not authenticate to sftp server");
			}
			log.debug("created new sftp session to host {}", sftpSession.getHost());
			return sftpSession;
		}
		catch(JSchException e) {
			throw new FileSystemException("cannot connect to the FTP server with domain [" + getHost() + "] at port [" + getPort() + "]", e);
		}
	}

	protected boolean isSessionStillWorking() {
		try {
			Session session = sftpClient.getSession();
			ChannelExec testChannel = (ChannelExec) session.openChannel("exec");
			testChannel.setCommand("true");
			testChannel.connect();
			testChannel.disconnect();
			return true;
		} catch (JSchException e) {
			log.info("SFTP session is not working anymore.");
		}
		return false;
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
		if (sftpClient != null && sftpClient.isConnected()) {
			log.debug("closing sftp client");
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

	@Override
	public void setName(String name) {
		this.name = name;
	}
}
