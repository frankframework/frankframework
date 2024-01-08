package org.frankframework.filesystem;

import org.apache.sshd.server.SshServer;
import org.frankframework.ftp.SftpFileRef;
import org.frankframework.senders.SftpFileSystemSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class SftpFileSystemSenderTest extends FileSystemSenderTest<SftpFileSystemSender, SftpFileRef, SftpFileSystem> {

	private final String username = "frankframework";
	private final String password = "pass_123";
	private final String host = "localhost";
	private int port = 22;
	private String remoteDirectory = "/home/frankframework/sftp";

	private SshServer sshd;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new SftpFileSystemTestHelper(username, password, host, remoteDirectory, port);
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if("localhost".equals(host)) {
			remoteDirectory = "/"; // See getTestDirectoryFS(), '/' is the SFTP HOME directory.

			sshd = SftpFileSystemTest.createSshServer(username, password);

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
