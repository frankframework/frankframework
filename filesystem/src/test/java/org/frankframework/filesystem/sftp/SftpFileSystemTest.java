package org.frankframework.filesystem.sftp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.sshd.server.SshServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import org.frankframework.filesystem.FileSystemTest;
import org.frankframework.filesystem.IFileSystemTestHelper;

/**
 * This test class is created to test both SFtpFileSystem and SFtpFileSystemSender classes.
 *
 * @author Niels Meijer
 *
 */
@Log4j2
@Tag("slow")
class SftpFileSystemTest extends FileSystemTest<SftpFileRef, SftpFileSystem> {

	private static final String username = "demo";
	private static final String password = "demo";
	private static final String host = "localhost";
	private static int port = 22;
	private static String remoteDirectory = "/home/frankframework/sftp";
	private static SshServer sshd;

	@BeforeAll
	public static void setUpOnce() throws Exception {
		if ("localhost".equals(host)) {
			remoteDirectory = "/"; // See getTestDirectoryFS(), '/' is the SFTP HOME directory.

			sshd = SftpFileSystemTestHelper.createSshServer(username, password, port);
			log.info("Starting SSH daemon at port {}", sshd.getPort());
			sshd.start();
			port = sshd.getPort();
		}
	}

	@AfterAll
	public static void tearDownOnce() throws Exception {
		if(sshd != null) {
			if(sshd.isStarted()) sshd.stop();
			sshd.close(true);
			sshd = null;
		}
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

		return fileSystem;
	}

	@Test
	void testSFTPFileRefSetRelative() {
		assertEquals("test123", new SftpFileRef("test123").getName());
		assertEquals("folder/test123", new SftpFileRef("folder/test123").getName());
	}

	@Test
	void testSFTPFileRefSetFolder() {
		SftpFileRef ref1 = new SftpFileRef("test123", "folder");
		assertEquals("folder/test123", ref1.getName());
	}

	@Test
	void testSFTPFileRefRelativeWithSetFolder() {
		SftpFileRef ref2 = new SftpFileRef("folder1/test123", "folder2");
		assertEquals("folder2/test123", ref2.getName());
	}

	@Test
	void testSFTPFileRefWindowsSlash() {
		SftpFileRef ref2 = new SftpFileRef("folder1\\test123", "folder2");
		assertEquals("folder2/test123", ref2.getName());
	}

	@Test
	void testRemoveMultipleFolders() throws Exception {
		fileSystem.configure();
		fileSystem.open();
		fileSystem.createFolder("piet");
		fileSystem.changeDirectory("piet");
		fileSystem.createFolder("1/2/3/4/5");
		assertTrue(fileSystem.folderExists("1/2/3/4"));
		assertTrue(fileSystem.folderExists("/piet/1/2/3/4"));
		fileSystem.removeFolder("1/2/3", true);
		assertFalse(fileSystem.folderExists("1/2/3"));
		assertTrue(fileSystem.folderExists("1/2"));
	}

}
