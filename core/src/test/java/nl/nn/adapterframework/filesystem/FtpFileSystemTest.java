package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import nl.nn.adapterframework.ftp.FTPFileRef;

/**
 * This test class is created to test both FtpFileSystem and FtpFileSystemSender classes.
 * 
 * @author Ali Sihab
 * @author Niels Meijer
 *
 */
public class FtpFileSystemTest extends FileSystemTest<FTPFileRef, FtpFileSystem> {

	private String username = "wearefrank";
	private String password = "pass_123";
	private String host = "localhost";
	private int port = 21;
	private String remoteDirectory = "/home/wearefrank/dir";

	private FakeFtpServer ftpServer;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if("localhost".equals(host)) {
			ftpServer = new FakeFtpServer();
			ftpServer.setServerControlPort(0); // use any free port

			UnixFakeFileSystem fileSystem = new UnixFakeFileSystem();
			fileSystem.add(new DirectoryEntry(remoteDirectory));
			ftpServer.setFileSystem(fileSystem);

			UserAccount userAccount = new UserAccount(username, password, remoteDirectory);
			ftpServer.addUserAccount(userAccount);

			ftpServer.start();
			port = ftpServer.getServerControlPort();
		}

		super.setUp();
	}

	@Override
	public void tearDown() throws Exception {
		ftpServer.stop();
		ftpServer = null;

		super.tearDown();
	}

	// This test doesn't work with the FTP STUB, it assumes that writing to a file removes the old file, which the STUB does not do.
	@Test
	@Override
	public void writableFileSystemTestTruncateFile() throws Exception {
		assumeFalse(host.equals("localhost"));
		super.writableFileSystemTestTruncateFile();
	}

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
