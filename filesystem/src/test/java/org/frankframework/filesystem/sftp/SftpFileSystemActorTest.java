package org.frankframework.filesystem.sftp;

import org.apache.sshd.server.SshServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import lombok.extern.log4j.Log4j2;

import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.WritableFileSystemActorTest;

/**
 * This test class is created to test both SFtpFileSystem and SFtpFileSystemSender classes.
 *
 * @author Niels Meijer
 *
 */
@Log4j2
class SftpFileSystemActorTest extends WritableFileSystemActorTest<SftpFileRef, SftpFileSystem> {

	private static final String username = "frankframework";
	private static final String password = "pass_123";
	private static final String host = "localhost";
	private static int port = 22;
	private static String remoteDirectory = "/home/frankframework/sftp";
	private static SshServer sshd;

	@BeforeAll
	public static void setUpOnce() throws Exception {
		if("localhost".equals(host)) {
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
}
