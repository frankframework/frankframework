package org.frankframework.filesystem.sftp;

import org.apache.sshd.server.SshServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import lombok.extern.log4j.Log4j2;

import org.frankframework.filesystem.FileSystemSenderTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.senders.SftpFileSystemSender;

@Log4j2
public class SftpFileSystemSenderTest extends FileSystemSenderTest<SftpFileSystemSender, SftpFileRef, SftpFileSystem> {

	private static final String username = "frankframework";
	private static final String password = "pass_123";
	private static final String host = "localhost";
	private static int port = 22;
	private static String remoteDirectory = "/home/frankframework/sftp";
	private static SshServer sshd;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new SftpFileSystemTestHelper(username, password, host, remoteDirectory, port);
	}

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
	public SftpFileSystemSender createFileSystemSender() {
		SftpFileSystemSender fileSystemSender = new SftpFileSystemSender();
		fileSystemSender.setHost(host);
		fileSystemSender.setUsername(username);
		fileSystemSender.setPassword(password);
		fileSystemSender.setRemoteDirectory(remoteDirectory);
		fileSystemSender.setPort(port);
		fileSystemSender.setStrictHostKeyChecking(false);

		return fileSystemSender;
	}
}
