package nl.nn.adapterframework.filesystem;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpSession;

public class FtpFileSystemSender extends FileSystemSender<FTPFile, FtpFileSystem> 
implements IFileSystemSender {
	
	private FtpFileSystem ffs;
	private FtpSession session;
	
//	
//	@Override
//	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc)
//			throws SenderException, TimeOutException {
//		try {
//			IPipeLineSession session=null;
//			if (prc!=null) {
//				session=prc.getSession();
//			}
//			ffs.getFtpSession().put(paramList, session, message, ffs.getRemoteDirectory(), ffs.getRemoteFilenamePattern(), true);
//		} catch(SenderException e) {
//			throw e;
//		} catch(Exception e) {
//			throw new SenderException("Error during ftp-ing " + message, e);
//		}
//		return message;
//	}
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		setFileSystem(ffs = new FtpFileSystem());
		session = ffs.getFtpSession();
	}
	
	public FtpSession getFtpSession() {
		return session;
	}
	
	public void setRemoteDirectory(String remoteDirectory) {
		ffs.setRemoteDirectory(remoteDirectory);
	}
	public String getRemoteDirectory() {
		return ffs.getRemoteDirectory();
	}

	public void setRemoteFilenamePattern(String string) {
		ffs.setRemoteFilenamePattern(string);
	}
	public String getRemoteFilenamePattern() {
		return ffs.getRemoteFilenamePattern();
	}


	

	public void setHost(String host) {
		session.setHost(host);
	}
	public void setPort(int port) {
		session.setPort(port);
	}

	public void setAuthAlias(String alias) {
		session.setAuthAlias(alias);
	}
	public void setUsername(String username) {
		session.setUsername(username);
	}
	public void setPassword(String passwd) {
		session.setPassword(passwd);
	}

	public void setProxyHost(String proxyHost) {
		session.setProxyHost(proxyHost);
	}
	public void setProxyPort(int proxyPort) {
		session.setProxyPort(proxyPort);
	}
	public void setProxyAuthAlias(String proxyAuthAlias) {
		session.setProxyAuthAlias(proxyAuthAlias);
	}
	public void setProxyUsername(String proxyUsername) {
		session.setProxyUsername(proxyUsername);
	}
	public void setProxyPassword(String proxyPassword) {
		session.setProxyPassword(proxyPassword);
	}

	public void setFtpTypeDescription(String ftpTypeDescription) {
		session.setFtpTypeDescription(ftpTypeDescription);
	}
	public void setFileType(String fileType) {
		session.setFileType(fileType);
	}
	public void setMessageIsContent(boolean messageIsContent) {
		session.setMessageIsContent(messageIsContent);
	}
	public void setPassive(boolean b) {
		session.setPassive(b);
	}


	public void setProxyTransportType(int proxyTransportType) {
		session.setProxyTransportType(proxyTransportType);
	}
	public void setPrefCSEncryption(String prefCSEncryption) {
		session.setPrefCSEncryption(prefCSEncryption);
	}
	public void setPrefSCEncryption(String prefSCEncryption) {
		session.setPrefSCEncryption(prefSCEncryption);
	}

	public void setPrivateKeyFilePath(String privateKeyFilePath) {
		session.setPrivateKeyFilePath(privateKeyFilePath);
	}
	public void setPrivateKeyAuthAlias(String privateKeyAuthAlias) {
		session.setPrivateKeyAuthAlias(privateKeyAuthAlias);
	}
	public void setPrivateKeyPassword(String passPhrase) {
		session.setPrivateKeyPassword(passPhrase);
	}
	public void setKnownHostsPath(String knownHostsPath) {
		session.setKnownHostsPath(knownHostsPath);
	}
	public void setConsoleKnownHostsVerifier(boolean verifier) {
		session.setConsoleKnownHostsVerifier(verifier);
	}


	public void setCertificate(String certificate) {
		session.setCertificate(certificate);
	}
	public String getCertificate() {
		return session.getCertificate();
	}
	public void setCertificateType(String keystoreType) {
		session.setCertificateType(keystoreType);
	}
	public String getCertificateType() {
		return session.getCertificateType();
	}
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		session.setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	public void setCertificateAuthAlias(String certificateAuthAlias) {
		session.setCertificateAuthAlias(certificateAuthAlias);
	}
	public String getCertificateAuthAlias() {
		return session.getCertificateAuthAlias();
	}
	public void setCertificatePassword(String certificatePassword) {
		session.setCertificatePassword(certificatePassword);
	}
	public String getCertificatePassword() {
		return session.getCertificatePassword();
	}


	public void setTruststore(String truststore) {
		session.setTruststore(truststore);
	}
	public void setTruststoreType(String truststoreType) {
		session.setTruststoreType(truststoreType);
	}
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		session.setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		session.setTruststoreAuthAlias(truststoreAuthAlias);
	}
	public void setTruststorePassword(String truststorePassword) {
		session.setTruststorePassword(truststorePassword);
	}

	public void setJdk13Compatibility(boolean jdk13Compatibility) {
		session.setJdk13Compatibility(jdk13Compatibility);
	}
	public void setVerifyHostname(boolean verifyHostname) {
		session.setVerifyHostname(verifyHostname);
	}
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		session.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}
	public void setProtP(boolean protP) {
		session.setProtP(protP);
	}
	public void setKeyboardInteractive(boolean keyboardInteractive) {
		session.setKeyboardInteractive(keyboardInteractive);
	}
}