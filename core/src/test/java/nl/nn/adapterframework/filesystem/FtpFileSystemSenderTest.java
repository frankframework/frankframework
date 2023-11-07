package nl.nn.adapterframework.filesystem;

import org.junit.jupiter.api.BeforeEach;

import nl.nn.adapterframework.ftp.FTPFileRef;
import nl.nn.adapterframework.senders.FtpFileSystemSender;
import nl.nn.adapterframework.testutil.junit.LocalFileServer;
import nl.nn.adapterframework.testutil.junit.LocalFileSystemMock;
import nl.nn.adapterframework.testutil.junit.LocalFileServer.FileSystemType;

/**
 * This test class is created to test both FtpFileSystem and FtpFileSystemSender classes.
 * 
 * @author Ali Sihab
 */
public class FtpFileSystemSenderTest extends FileSystemSenderTest<FtpFileSystemSender, FTPFileRef, FtpFileSystem> {

	private String username = "frankframework";
	private String password = "pass_123";
	private String host = "localhost";
	private int port = 21;
	private String remoteDirectory = "/home";

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
