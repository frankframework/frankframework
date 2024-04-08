package nl.nn.adapterframework.filesystem;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.ftp.FTPFileRef;

/**
 * This test class is created to test both FtpFileSystem and FtpFileSystemSender classes.
 *
 * @author Jacobjob Koster
 */
class FtpFileSystemActorTest extends FileSystemActorTest<FTPFileRef, FtpFileSystem> {

	private final String username = "frankframework";
	private final String password = "pass_123";
	private final String host = "localhost";
	private int port = 22;
	private final String remoteDirectory = "/home";

	@LocalFileSystemMock
	private static LocalFileServer fs;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if ("localhost".equals(host)) {
			fs.startServer(LocalFileServer.FileSystemType.FTP);
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
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new FtpFileSystemTestHelper(username, password, host, remoteDirectory, port);
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

	@Override
	@Test
	@Disabled("Line-endings differences breaking the test. Test is gone in 8.0 anyway")
	public void fileSystemActorWriteActionBase64Encode() throws Exception {
		fileSystemActorWriteActionBase64Encode(false, true);
	}

	@Override
	@Test
	@Disabled("Line-endings differences breaking the test. Test is gone in 8.0 anyway")
	public void fileSystemActorWriteActionBase64EncodeStreaming() throws Exception {
		fileSystemActorWriteActionBase64Encode(true, true);
	}

}
