package org.frankframework.filesystem;

import org.apache.sshd.server.SshServer;
import org.frankframework.filesystem.ftp.SftpFileRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * This test class is created to test both SFtpFileSystem and SFtpFileSystemSender classes.
 *
 * @author Niels Meijer
 *
 */
class SftpFileSystemActorTest extends FileSystemActorTest<SftpFileRef, SftpFileSystem> {

	private final String username = "frankframework";
	private final String password = "pass_123";
	private final String host = "localhost";
	private static int port = 22;
	private String remoteDirectory = "/home/frankframework/sftp";
	private SshServer sshd;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if("localhost".equals(host)) {
			remoteDirectory = "/"; // See getTestDirectoryFS(), '/' is the SFTP HOME directory.

			sshd = SftpFileSystemTestHelper.createSshServer(username, password, port);
			log.info("Starting SSH daemon at port {}", sshd.getPort());
			sshd.start();
			port = sshd.getPort();
		}

		super.setUp();
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
}
