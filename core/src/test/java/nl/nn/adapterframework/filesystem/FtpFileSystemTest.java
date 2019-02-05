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
	
//	@Test
//	public void testGetInfo() throws IOException, FileSystemException {
//		String filename = FILE1;
//		createFile(filename, "Eerste versie van de file");
//		FTPFile file = fileSystem.toFile(filename);
//		file.setGroup("dummy");
//		
//		Calendar c = Calendar.getInstance();
//		c.setTimeInMillis(1549275764000L);
//		file.setTimestamp(c);
//		
//		String result = ffs.getInfo(file);
//		
//		assertEquals("<file name=\"file1.txt\" group=\"dummy\" type=\"3\" size=\"0\" isDirectory=\"false\" hardLinkCount=\"0\" lastModified=\"04/02/2019 11:22:44\" />",
//				result.trim());
//	}
}