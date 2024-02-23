package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.hostbased.StaticHostBasedAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.frankframework.ftp.SftpFileRef;
import org.frankframework.testutil.TestAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This test class is created to test both SFtpFileSystem and SFtpFileSystemSender classes.
 *
 * @author Niels Meijer
 *
 */
class SftpFileSystemTest extends FileSystemTest<SftpFileRef, SftpFileSystem> {

	private final String username = "demo";
	private final String password = "demo";
	private final String host = "localhost";
	private int port = 22;
	private String remoteDirectory = "/home/frankframework/sftp";
	protected TestAppender testAppender;

	private SshServer sshd;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		startNewSshDaemon();
		super.setUp();
	}

	private void startNewSshDaemon() throws IOException {
		if("localhost".equals(host)) {
			remoteDirectory = "/"; // See getTestDirectoryFS(), '/' is the SFTP HOME directory.

			sshd = createSshServer(username, password);
			sshd.start();
			port = sshd.getPort();
		}
	}

	static SshServer createSshServer(String username, String password) throws IOException {
		SshServer sshd = SshServer.setUpDefaultServer();
		sshd.setHost("localhost");
		sshd.setPasswordAuthenticator((uname, psswrd, session) -> username.equals(uname) && password.equals(psswrd));
		sshd.setHostBasedAuthenticator(new StaticHostBasedAuthenticator(true));

		SftpSubsystemFactory sftpFactory = new SftpSubsystemFactory();
		sshd.setSubsystemFactories(Collections.singletonList(sftpFactory));
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

		sshd.setFileSystemFactory(getTestDirectoryFS());
		return sshd;
	}

	/**
	 * Creates the folder '../target/sftpTestFS' in which the tests will be executed.
	 * This 'virtual FS' will pretend that the mentioned folder is the SFTP HOME directory.
	 */
	private static FileSystemFactory getTestDirectoryFS() throws IOException {
		File targetFolder = new File(".", "target");
		File sftpTestFS = new File(targetFolder.getCanonicalPath(), "sftpTestFS");
		sftpTestFS.mkdir();
		assertTrue(sftpTestFS.exists());

		return new VirtualFileSystemFactory(sftpTestFS.toPath());
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		if(sshd != null) {
			if(sshd.isStarted()) sshd.stop();
			sshd.close(true);
			sshd = null;
		}
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
