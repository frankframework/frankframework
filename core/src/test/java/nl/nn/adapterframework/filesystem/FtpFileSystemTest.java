package nl.nn.adapterframework.filesystem;

import static org.junit.Assume.assumeFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.filesystem.LocalFileServer.FileSystemType;
import nl.nn.adapterframework.ftp.FTPFileRef;

/**
 * @author Niels Meijer
 */
public class FtpFileSystemTest extends FileSystemTest<FTPFileRef, FtpFileSystem> {

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

	// This test doesn't work with the FTP STUB, it assumes that writing to a file removes the old file, which the STUB does not do.
	@Test
	@Override
	public void writableFileSystemTestTruncateFile() throws Exception {
		assumeFalse(host.equals("localhost"));
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
