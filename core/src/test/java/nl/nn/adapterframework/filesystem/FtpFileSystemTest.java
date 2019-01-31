package nl.nn.adapterframework.filesystem;

import java.io.File;
import java.io.IOException;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.Before;
import org.junit.After;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class FtpFileSystemTest extends LocalFileSystemTestBase<FTPFile, FtpFileSystem> {

	FtpFileSystem ffs;
	private boolean setupDone;

	@Override
	protected synchronized File getFileHandle(String filename) {
		return new File("C:/Users/Daniel/Desktop/DummyFolder/"+filename);
	}
	
	@Before
	public void setup() {
		if(!setupDone) {	
			try {
				ffs = new FtpFileSystem();
				ffs.setUsername("test");
				ffs.setPassword("test");
				ffs.setHost("10.0.0.179");
				ffs.setPort(22);
				ffs.configure();
				
				setupDone = true;
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		}
		
		try {
			super.setup();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected FtpFileSystem getFileSystem() throws ConfigurationException {
		return ffs;
	}
	
	@After
	public void shutdown() {
//		if()
//		ffs.getFtpSession().closeClient();
	}
}