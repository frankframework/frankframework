package nl.nn.adapterframework.filesystem;


import java.io.File;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.Ignore;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpSession;

@Ignore
public class FtpFileSystemTest extends LocalFileSystemTestBase<FTPFile, FtpFileSystem> {

	FtpFileSystem ffs;
	
	// TODO: Add local connection parameters. 
	
	private String localFilePath = "";
	private String share = null;
	private String relativePath = "DummyFolder/";
	private String username = "";
	private String password = "";
	private String host = "";
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