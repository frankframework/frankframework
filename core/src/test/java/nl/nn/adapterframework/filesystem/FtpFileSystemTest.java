package nl.nn.adapterframework.filesystem;

import java.io.File;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.After;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class FtpFileSystemTest extends LocalFileSystemTestBase<FTPFile, FtpFileSystem> {

	FtpFileSystem ffs;

	@Override
	protected File getFileHandle(String filename) {
		return new File("C:/Users/Daniel/Desktop/DummyFolder/"+filename);
	}
	
	@Override
	protected FtpFileSystem getFileSystem() throws ConfigurationException {
		ffs = new FtpFileSystem();
		ffs.setUsername("test");
		ffs.setPassword("test");
		ffs.setHost("10.0.0.179");
		ffs.setPort(22);
		
//		ffs.setRemoteDirectory("C:/Users/Daniel/Desktop/DummyFolder/");
		ffs.configure();
		return ffs;
	}
	
	@After
	public void shutdown() {
		ffs.getFtpSession().closeClient();
	}
}