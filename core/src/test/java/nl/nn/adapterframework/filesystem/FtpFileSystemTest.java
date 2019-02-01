package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

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
//				ffs.setRemoteDirectory("C:/Users/Daniel/Desktop/DummyFolder/");
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
	
	@After
	public void shutdown() {
//		if()
//		ffs.getFtpSession().closeClient();
	}

//	@Override
//	public void testListFile() throws IOException, FileSystemException {
//		String contents1 = "maakt niet uit";
//		String contents2 = "maakt ook niet uit";
//		createFile(FILE1, contents1);
//		createFile(FILE2, contents2);
//		System.out.println(readFile(FILE1));
//		System.out.println(readFile(FILE2));
//		
//		Iterator<FTPFile> it = fileSystem.listFiles();
//		assertTrue(it.hasNext());
//		FTPFile file = it.next();
//		System.out.println("File 1: " + file);
//		assertTrue(it.hasNext());
//		file = it.next();
//		System.out.println("File 2: " + file);
//		assertTrue(it.hasNext());
//		file = it.next();
//		System.out.println("File 3: " + file);
//		assertFalse(it.hasNext());
//
//		deleteFile(FILE1);
//
//		it = fileSystem.listFiles();
//		assertTrue(it.hasNext());
//		file = it.next();
//		System.out.println("File 1: " + file);
//		assertTrue(it.hasNext());
//		file = it.next();
//		System.out.println("File 2: " + file);
//		assertFalse(it.hasNext());
//	}
}