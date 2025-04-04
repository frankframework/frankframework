package org.frankframework.filesystem.ftp;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.FileSystemTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.LocalFileSystemTestHelper;
import org.frankframework.testutil.junit.LocalFileServer;
import org.frankframework.testutil.junit.LocalFileServer.FileSystemType;
import org.frankframework.testutil.junit.LocalFileSystemMock;

/**
 * @author Niels Meijer
 */
@Tag("slow")
public class FtpFileSystemTest extends FileSystemTest<FTPFileRef, FtpFileSystem> {

	private static final String username = "frankframework";
	private static final String password = "pass_123";
	private static final String host = "localhost";
	private int port = 21;
	private final String remoteDirectory = "/home";

	@LocalFileSystemMock
	private static LocalFileServer fs;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		if ("localhost".equals(host)) {
			return new LocalFileSystemTestHelper(fs.getTestDirectory());
		}
		return new FtpFileSystemTestHelper(username, password, host, remoteDirectory, port);
	}

	@BeforeAll
	public static void setUpOnce() throws Exception {
		if ("localhost".equals(host)) {
			fs.startServer(FileSystemType.FTP);
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

	// This test doesn't work with the FTP STUB, it assumes that writing to a file removes the old file, which the STUB does not do.
	@Test
	@Override
	public void writableFileSystemTestTruncateFile() throws Exception {
		assumeFalse("localhost".equals(host));
		super.writableFileSystemTestTruncateFile();
	}

	@Override
	public FtpFileSystem createFileSystem() {
		FtpFileSystem fileSystem = new FtpFileSystem();
		fileSystem.setHost(host);
		fileSystem.setUsername(username);
		fileSystem.setPassword(password);
		fileSystem.setRemoteDirectory(remoteDirectory);
		fileSystem.setPort(port);

		return fileSystem;
	}
}
