package org.frankframework.filesystem.sftp;

import java.io.IOException;

import org.apache.sshd.server.SshServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.WritableFileSystemListenerTest;
import org.frankframework.receivers.SftpFileSystemListener;

@Log4j2
public class SftpFileSystemListenerTest extends WritableFileSystemListenerTest<SftpFileRef, SftpFileSystem> {

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
	public SftpFileSystemListener createFileSystemListener() {
		SftpFileSystemListener listener = new SftpFileSystemListener();
		listener.setHost(host);
		listener.setUsername(username);
		listener.setPassword(password);
		listener.setRemoteDirectory(remoteDirectory);
		listener.setPort(port);
		listener.setStrictHostKeyChecking(false);

		return listener;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() throws IOException {
		return new SftpFileSystemTestHelper(username, password, host, remoteDirectory, port);
	}

	@Override
	@Test
	@Disabled("Our SFTP test server does not appear to support getting the modification-time, so we cannot do this test")
	public void fileListenerTestGetIdFromRawMessageFileTimeSensitive() {
		// Ignore for this subclass
	}
}
