package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;
import nl.nn.adapterframework.util.XmlBuilder;

public class FtpFileSystem implements IFileSystem<FTPFile> {

	private FtpSession ftpSession;
	
	private String remoteDirectory;
	private String remoteFilenamePattern=null;

	private class FTPFilePathIterator implements Iterator<FTPFile> {

		private FTPFile files[];
		int i=0;
		
		FTPFilePathIterator(FTPFile files[]) {
			Vector<FTPFile> fList = new Vector<FTPFile>();
			for(int i = 0; i < files.length; i++) {
				if(files[i].isFile()) {
					fList.addElement(files[i]);
				}
			}
			
			this.files = new FTPFile[fList.size()];
			fList.copyInto(this.files);
		}
		
		@Override
		public boolean hasNext() {
			return files!=null && i<files.length;
		}

		@Override
		public FTPFile next() {			
			return files[i++];
		}

		@Override
		public void remove() {
			try {
				deleteFile(files[i++]);
			} catch (FileSystemException e) {
				e.printStackTrace();
			}
		}
	}
	
	public FtpFileSystem() {
		ftpSession = new FtpSession();
	}
	
	@Override
	public void configure() throws ConfigurationException {
		ftpSession.configure();

		try {
			ftpSession.openClient("");
		} catch (FtpConnectException e) {
			e.printStackTrace();
		}
	}

	@Override
	public FTPFile toFile(String filename) throws FileSystemException {
		FTPFile ftpFile;
		ftpFile = new FTPFile();
		ftpFile.setName(filename);
		
		return ftpFile;
	}

	@Override
	public Iterator<FTPFile> listFiles() throws FileSystemException {
		try {
			return new FTPFilePathIterator(ftpSession.ftpClient.listFiles(remoteDirectory));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean exists(FTPFile f) throws FileSystemException {
		try {
			FTPFile[] files = ftpSession.ftpClient.listFiles();
			for(FTPFile o : files) {
				if(o.getName().equals(f.getName())) {
					return true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public OutputStream createFile(FTPFile f) throws FileSystemException, IOException {
		return ftpSession.ftpClient.storeFileStream(f.getName());
	}

	@Override
	public OutputStream appendFile(FTPFile f) throws FileSystemException, IOException {
		return ftpSession.ftpClient.appendFileStream(f.getName());
	}

	@Override
	public InputStream readFile(FTPFile f) throws FileSystemException, IOException {
		return ftpSession.ftpClient.retrieveFileStream(f.getName());
	}
	
	@Override
	public void deleteFile(FTPFile f) throws FileSystemException {
		try {
			ftpSession.ftpClient.deleteFile(f.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getInfo(FTPFile f) throws FileSystemException {
		return getFileInfoAsXML(f);
	}

	private String getFileInfoAsXML(FTPFile f) throws FileSystemException {
		XmlBuilder fileXml = new XmlBuilder("file");
		fileXml.addAttribute("name", f.getName());
		fileXml.addAttribute("user", f.getUser());
		fileXml.addAttribute("group", f.getGroup());
		fileXml.addAttribute("type", f.getType());
		fileXml.addAttribute("size", "" + f.getSize());
		fileXml.addAttribute("rawListing", f.getRawListing());
		fileXml.addAttribute("isDirectory", "" + isFolder(f));
		fileXml.addAttribute("link", f.getLink());
		fileXml.addAttribute("hardLinkCount", f.getHardLinkCount());
		
		if(f.getTimestamp() != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
			Date date = new Date(f.getTimestamp().getTimeInMillis());
			fileXml.addAttribute("lastModified", sdf.format(date));
		}
		
		return fileXml.toXML();
	}

	@Override
	public boolean isFolder(FTPFile f) throws FileSystemException {
		return f.isDirectory();
	}

	@Override
	public void createFolder(FTPFile f) throws FileSystemException {
		try {
			ftpSession.ftpClient.makeDirectory(f.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void removeFolder(FTPFile f) throws FileSystemException {
		try {
			ftpSession.ftpClient.removeDirectory(f.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public FtpSession getFtpSession() {
		return ftpSession;
	}
	
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}
	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setRemoteFilenamePattern(String string) {
		remoteFilenamePattern = string;
	}
	public String getRemoteFilenamePattern() {
		return remoteFilenamePattern;
	}


	

	public void setHost(String host) {
		ftpSession.setHost(host);
	}
	public void setPort(int port) {
		ftpSession.setPort(port);
	}

	public void setAuthAlias(String alias) {
		ftpSession.setAuthAlias(alias);
	}
	public void setUsername(String username) {
		ftpSession.setUsername(username);
	}
	public void setPassword(String passwd) {
		ftpSession.setPassword(passwd);
	}

	public void setProxyHost(String proxyHost) {
		ftpSession.setProxyHost(proxyHost);
	}
	public void setProxyPort(int proxyPort) {
		ftpSession.setProxyPort(proxyPort);
	}
	public void setProxyAuthAlias(String proxyAuthAlias) {
		ftpSession.setProxyAuthAlias(proxyAuthAlias);
	}
	public void setProxyUsername(String proxyUsername) {
		ftpSession.setProxyUsername(proxyUsername);
	}
	public void setProxyPassword(String proxyPassword) {
		ftpSession.setProxyPassword(proxyPassword);
	}

	public void setFtpTypeDescription(String ftpTypeDescription) {
		ftpSession.setFtpTypeDescription(ftpTypeDescription);
	}
	public void setFileType(String fileType) {
		ftpSession.setFileType(fileType);
	}
	public void setMessageIsContent(boolean messageIsContent) {
		ftpSession.setMessageIsContent(messageIsContent);
	}
	public void setPassive(boolean b) {
		ftpSession.setPassive(b);
	}


	public void setProxyTransportType(int proxyTransportType) {
		ftpSession.setProxyTransportType(proxyTransportType);
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
	public void setPrivateKeyAuthAlias(String privateKeyAuthAlias) {
		ftpSession.setPrivateKeyAuthAlias(privateKeyAuthAlias);
	}
	public void setPrivateKeyPassword(String passPhrase) {
		ftpSession.setPrivateKeyPassword(passPhrase);
	}
	public void setKnownHostsPath(String knownHostsPath) {
		ftpSession.setKnownHostsPath(knownHostsPath);
	}
	public void setConsoleKnownHostsVerifier(boolean verifier) {
		ftpSession.setConsoleKnownHostsVerifier(verifier);
	}


	public void setCertificate(String certificate) {
		ftpSession.setCertificate(certificate);
	}
	public String getCertificate() {
		return ftpSession.getCertificate();
	}
	public void setCertificateType(String keystoreType) {
		ftpSession.setCertificateType(keystoreType);
	}
	public String getCertificateType() {
		return ftpSession.getCertificateType();
	}
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		ftpSession.setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	public void setCertificateAuthAlias(String certificateAuthAlias) {
		ftpSession.setCertificateAuthAlias(certificateAuthAlias);
	}
	public String getCertificateAuthAlias() {
		return ftpSession.getCertificateAuthAlias();
	}
	public void setCertificatePassword(String certificatePassword) {
		ftpSession.setCertificatePassword(certificatePassword);
	}
	public String getCertificatePassword() {
		return ftpSession.getCertificatePassword();
	}


	public void setTruststore(String truststore) {
		ftpSession.setTruststore(truststore);
	}
	public void setTruststoreType(String truststoreType) {
		ftpSession.setTruststoreType(truststoreType);
	}
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		ftpSession.setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		ftpSession.setTruststoreAuthAlias(truststoreAuthAlias);
	}
	public void setTruststorePassword(String truststorePassword) {
		ftpSession.setTruststorePassword(truststorePassword);
	}

	public void setJdk13Compatibility(boolean jdk13Compatibility) {
		ftpSession.setJdk13Compatibility(jdk13Compatibility);
	}
	public void setVerifyHostname(boolean verifyHostname) {
		ftpSession.setVerifyHostname(verifyHostname);
	}
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		ftpSession.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}
	public void setProtP(boolean protP) {
		ftpSession.setProtP(protP);
	}
	public void setKeyboardInteractive(boolean keyboardInteractive) {
		ftpSession.setKeyboardInteractive(keyboardInteractive);
	}
}