package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.ftp.FTPFileRef;

/**
 *  This test class is created to test both FtpFileSystem and FtpFileSystemSender classes.
 * @author alisihab
 *
 */
public class FtpFileSystemTest extends FileSystemTest<FTPFileRef, FtpFileSystem> {

	private String username = "wearefrank";
	private String password = "pass_123";
	private String host = "localhost";
	private String remoteDirectory = "/home/wearefrank/dir";
	private int port = 21;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new FtpFileSystemTestHelper(username, password, host, remoteDirectory, port);
	}

	@Override
	public FtpFileSystem createFileSystem() {
		FtpFileSystem fileSystem = new FtpFileSystem();
		fileSystem.setHost(host);
		fileSystem.setUsername(username);
		fileSystem.setPassword(password);
		fileSystem.setRemoteDirectory(remoteDirectory);
		fileSystem.setPort(port);

		return fileSystem;
	}

	@Override
	@Ignore
	public void basicFileSystemTestCopyFile() throws Exception {
		//Ignore this test as the copy action is not implemented/supported
	}

	@Test
	public void testFTPFileRefSetRelative() {
		assertEquals("test123", new FTPFileRef("test123").getName());
		assertEquals("folder/test123", new FTPFileRef("folder/test123").getName());
	}

	@Test
	public void testFTPFileRefSetFolder() {
		FTPFileRef ref1 = new FTPFileRef("test123");
		ref1.setFolder("folder");
		assertEquals("folder/test123", ref1.getName());
	}

	@Test
	public void testFTPFileRefRelativeWithSetFolder() {
		FTPFileRef ref2 = new FTPFileRef("folder1/test123");
		ref2.setFolder("folder2");
		assertEquals("folder2/folder1/test123", ref2.getName());
	}

	@Test
	public void testFTPFileRefWindowsSlash() {
		FTPFileRef ref2 = new FTPFileRef("folder1\\test123");
		ref2.setFolder("folder2");
		assertEquals("folder2/folder1/test123", ref2.getName());
	}
}
