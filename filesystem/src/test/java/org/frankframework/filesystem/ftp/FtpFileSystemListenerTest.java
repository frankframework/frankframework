package org.frankframework.filesystem.ftp;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.LocalFileSystemTestHelper;
import org.frankframework.filesystem.WritableFileSystemListenerTest;
import org.frankframework.receivers.FtpFileSystemListener;
import org.frankframework.testutil.junit.LocalFileServer;
import org.frankframework.testutil.junit.LocalFileSystemMock;

public class FtpFileSystemListenerTest extends WritableFileSystemListenerTest<FTPFileRef, FtpFileSystem> {

	private static final String username = "frankframework";
	private static final String password = "pass_123";
	private static final String host = "localhost";
	private int port = 21;
	private final String remoteDirectory = "/home";

	@LocalFileSystemMock
	private static LocalFileServer fs;


	@BeforeAll
	public static void setUpOnce() throws Exception {
		if ("localhost".equals(host)) {
			fs.startServer(LocalFileServer.FileSystemType.FTP);
		}
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if ("localhost".equals(host)) {
			port = fs.getPort();
		}
		super.setUp();
	}

	@AfterAll
	public static void tearDownOnce() throws Exception {
		if (fs != null) {
			fs.close();
		}
	}

	@Override
	public FtpFileSystemListener createFileSystemListener() {
		FtpFileSystemListener listener = new FtpFileSystemListener();
		listener.setHost(host);
		listener.setUsername(username);
		listener.setPassword(password);
		listener.setRemoteDirectory(remoteDirectory);
		listener.setPort(port);

		return listener;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() throws IOException {
		if ("localhost".equals(host)) {
			return new LocalFileSystemTestHelper(fs.getTestDirectory());
		}
		return new FtpFileSystemTestHelper(username, password, host, remoteDirectory, port);
	}
}
