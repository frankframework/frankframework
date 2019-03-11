package nl.nn.adapterframework.senders;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.filesystem.FtpFileSystem;

public class FtpFileSystemSender extends FileSystemSender<FTPFile, FtpFileSystem> {

	public FtpFileSystemSender() {
		setFileSystem(new FtpFileSystem());
	}

	public void setRemoteDirectory(String remoteDirectory) {
		getFileSystem().setRemoteDirectory(remoteDirectory);
	}

	public void setHost(String host) {
		getFileSystem().getFtpSession().setHost(host);
	}

	public void setPort(int port) {
		getFileSystem().getFtpSession().setPort(port);
	}

	public void setAuthAlias(String alias) {
		getFileSystem().getFtpSession().setAuthAlias(alias);
	}

	public void setUsername(String username) {
		getFileSystem().getFtpSession().setUsername(username);
	}

	public void setPassword(String passwd) {
		getFileSystem().getFtpSession().setPassword(passwd);
	}

	public void setProxyHost(String proxyHost) {
		getFileSystem().getFtpSession().setProxyHost(proxyHost);
	}

	public void setProxyPort(int proxyPort) {
		getFileSystem().getFtpSession().setProxyPort(proxyPort);
	}

	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().getFtpSession().setProxyAuthAlias(proxyAuthAlias);
	}

	public void setProxyUsername(String proxyUsername) {
		getFileSystem().getFtpSession().setProxyUsername(proxyUsername);
	}

	public void setProxyPassword(String proxyPassword) {
		getFileSystem().getFtpSession().setProxyPassword(proxyPassword);
	}

	public void setFtpTypeDescription(String ftpTypeDescription) {
		getFileSystem().getFtpSession().setFtpTypeDescription(ftpTypeDescription);
	}

	public void setFileType(String fileType) {
		getFileSystem().getFtpSession().setFileType(fileType);
	}

	public void setMessageIsContent(boolean messageIsContent) {
		getFileSystem().getFtpSession().setMessageIsContent(messageIsContent);
	}

	public void setPassive(boolean b) {
		getFileSystem().getFtpSession().setPassive(b);
	}

	public void setProxyTransportType(int proxyTransportType) {
		getFileSystem().getFtpSession().setProxyTransportType(proxyTransportType);
	}

	public void setPrefCSEncryption(String prefCSEncryption) {
		getFileSystem().getFtpSession().setPrefCSEncryption(prefCSEncryption);
	}

	public void setPrefSCEncryption(String prefSCEncryption) {
		getFileSystem().getFtpSession().setPrefSCEncryption(prefSCEncryption);
	}

	public void setPrivateKeyFilePath(String privateKeyFilePath) {
		getFileSystem().getFtpSession().setPrivateKeyFilePath(privateKeyFilePath);
	}

	public void setPrivateKeyAuthAlias(String privateKeyAuthAlias) {
		getFileSystem().getFtpSession().setPrivateKeyAuthAlias(privateKeyAuthAlias);
	}

	public void setPrivateKeyPassword(String passPhrase) {
		getFileSystem().getFtpSession().setPrivateKeyPassword(passPhrase);
	}

	public void setKnownHostsPath(String knownHostsPath) {
		getFileSystem().getFtpSession().setKnownHostsPath(knownHostsPath);
	}

	public void setConsoleKnownHostsVerifier(boolean verifier) {
		getFileSystem().getFtpSession().setConsoleKnownHostsVerifier(verifier);
	}

	public void setCertificate(String certificate) {
		getFileSystem().getFtpSession().setCertificate(certificate);
	}

	public void setCertificateType(String keystoreType) {
		getFileSystem().getFtpSession().setCertificateType(keystoreType);
	}

	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		getFileSystem().getFtpSession().setKeyManagerAlgorithm(keyManagerAlgorithm);
	}

	public void setCertificateAuthAlias(String certificateAuthAlias) {
		getFileSystem().getFtpSession().setCertificateAuthAlias(certificateAuthAlias);
	}

	public void setCertificatePassword(String certificatePassword) {
		getFileSystem().getFtpSession().setCertificatePassword(certificatePassword);
	}

	public void setTruststore(String truststore) {
		getFileSystem().getFtpSession().setTruststore(truststore);
	}

	public void setTruststoreType(String truststoreType) {
		getFileSystem().getFtpSession().setTruststoreType(truststoreType);
	}

	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		getFileSystem().getFtpSession().setTrustManagerAlgorithm(trustManagerAlgorithm);
	}

	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		getFileSystem().getFtpSession().setTruststoreAuthAlias(truststoreAuthAlias);
	}

	public void setTruststorePassword(String truststorePassword) {
		getFileSystem().getFtpSession().setTruststorePassword(truststorePassword);
	}

	public void setJdk13Compatibility(boolean jdk13Compatibility) {
		getFileSystem().getFtpSession().setJdk13Compatibility(jdk13Compatibility);
	}

	public void setVerifyHostname(boolean verifyHostname) {
		getFileSystem().getFtpSession().setVerifyHostname(verifyHostname);
	}

	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		getFileSystem().getFtpSession().setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}

	public void setProtP(boolean protP) {
		getFileSystem().getFtpSession().setProtP(protP);
	}

	public void setKeyboardInteractive(boolean keyboardInteractive) {
		getFileSystem().getFtpSession().setKeyboardInteractive(keyboardInteractive);
	}
}