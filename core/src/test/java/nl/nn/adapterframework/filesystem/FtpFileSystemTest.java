package nl.nn.adapterframework.filesystem;


import java.io.File;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpSession;

public class FtpFileSystemTest extends LocalFileSystemTestBase<FTPFile, FtpFileSystem> {

	FtpFileSystem ffs;
	
	private String localFilePath = "C:/Users/Daniel/Desktop/";
	private String share = null;
	private String relativePath = "DummyFolder/";
	private String username = "test";
	private String password = "test";
	private String host = "10.0.0.179";
	private int port = 22;

	@Override
	protected File getFileHandle(String filename) {
		return new File(localFilePath + relativePath + filename);
	}
	
	@Override
	protected FtpFileSystem getFileSystem() throws ConfigurationException {
		ffs = new FtpFileSystem();
		FtpSession session = ffs.getFtpSession();
		
		session.setUsername(username);
		session.setPassword(password);
		session.setHost(host);
		session.setPort(port);
		ffs.configure();
		
		return ffs;
	}
}