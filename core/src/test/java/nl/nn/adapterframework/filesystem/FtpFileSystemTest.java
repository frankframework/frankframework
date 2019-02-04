package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.Before;
import org.junit.Test;
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
	
	@Override
	protected void finalizeCommand() throws IOException {
		ffs.getFtpSession().ftpClient.completePendingCommand();
	}
	
	@Test
	public void testGetInfo() throws IOException, FileSystemException {
		String filename = FILE1;
		createFile(filename, "Eerste versie van de file");
		FTPFile file = fileSystem.toFile(filename);
		file.setGroup("dummy");
		
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(1549275764000L);
		file.setTimestamp(c);
		
		String result = ffs.getInfo(file);
		
		assertEquals("<file name=\"file1.txt\" group=\"dummy\" type=\"3\" size=\"0\" isDirectory=\"false\" hardLinkCount=\"0\" lastModified=\"04/02/2019 11:22:44\" />",
				result.trim());
	}
}