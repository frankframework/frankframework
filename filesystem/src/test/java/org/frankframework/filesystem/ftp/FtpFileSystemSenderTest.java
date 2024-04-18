package org.frankframework.filesystem.ftp;

import org.frankframework.filesystem.FileSystemSenderTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.LocalFileSystemTestHelper;
import org.frankframework.senders.FtpFileSystemSender;
import org.frankframework.testutil.junit.LocalFileServer;
import org.frankframework.testutil.junit.LocalFileServer.FileSystemType;
import org.frankframework.testutil.junit.LocalFileSystemMock;
import org.junit.jupiter.api.BeforeEach;

/**
 * This test class is created to test both FtpFileSystem and FtpFileSystemSender classes.
 *
 * @author Ali Sihab
 */
public class FtpFileSystemSenderTest extends FileSystemSenderTest<FtpFileSystemSender, FTPFileRef, FtpFileSystem> {

	private final String username = "frankframework";
	private final String password = "pass_123";
	private final String host = "localhost";
	private int port = 21;
	private final String remoteDirectory = "/home";

	@LocalFileSystemMock
	private static LocalFileServer fs;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		if("localhost".equals(host)) {
			return new LocalFileSystemTestHelper(fs.getTestDirectory());
		}
		return new FtpFileSystemTestHelper(username, password, host, remoteDirectory, port);
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if("localhost".equals(host)) {
			fs.startServer(FileSystemType.FTP);
			port = fs.getPort();
		}

		super.setUp();
	}

	@Override
	public FtpFileSystemSender createFileSystemSender() {
		FtpFileSystemSender fileSystemSender = new FtpFileSystemSender();
		fileSystemSender.setHost(host);
		fileSystemSender.setUsername(username);
		fileSystemSender.setPassword(password);
		fileSystemSender.setRemoteDirectory(remoteDirectory);
		fileSystemSender.setPort(port);

		return fileSystemSender;
	}
}
