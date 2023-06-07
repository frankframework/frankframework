package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import nl.nn.adapterframework.ftp.SftpFileRef;

/**
 * This test class is created to test both SFtpFileSystem and SFtpFileSystemSender classes.
 * 
 * @author Niels Meijer
 *
 */
public class SftpFileSystemTest extends FileSystemTest<SftpFileRef, SftpFileSystem> {

	private String username = "wearefrank";
	private String password = "pass_123";
	private String host = "localhost";
	private int port = 22;
	private String remoteDirectory = "/home/wearefrank/dir";

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if("localhost".equals(host)) {
			//TODO setup mock
		}

		super.setUp();
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new SftpFileSystemTestHelper(username, password, host, remoteDirectory, port);
	}

	@Override
	public SftpFileSystem createFileSystem() {
		SftpFileSystem fileSystem = new SftpFileSystem();
		fileSystem.setHost(host);
		fileSystem.setUsername(username);
		fileSystem.setPassword(password);
		fileSystem.setRemoteDirectory(remoteDirectory);
		fileSystem.setPort(port);
		fileSystem.setStrictHostKeyChecking(false);
//		fileSystem.setVerifyHostname(false);

		return fileSystem;
	}

	@Test
	public void testSFTPFileRefSetRelative() {
		assertEquals("test123", new SftpFileRef("test123").getName());
		assertEquals("folder/test123", new SftpFileRef("folder/test123").getName());
	}

	@Test
	public void testSFTPFileRefSetFolder() {
		SftpFileRef ref1 = new SftpFileRef("test123");
		ref1.setFolder("folder");
		assertEquals("folder/test123", ref1.getName());
	}

	@Test
	public void testSFTPFileRefRelativeWithSetFolder() {
		SftpFileRef ref2 = new SftpFileRef("folder1/test123");
		ref2.setFolder("folder2");
		assertEquals("folder2/folder1/test123", ref2.getName());
	}

	@Test
	public void testSFTPFileRefWindowsSlash() {
		SftpFileRef ref2 = new SftpFileRef("folder1\\test123");
		ref2.setFolder("folder2");
		assertEquals("folder2/folder1/test123", ref2.getName());
	}
}
