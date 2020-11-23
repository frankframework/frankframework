package nl.nn.adapterframework.filesystem;

import org.apache.commons.net.ftp.FTPFile;

/**
 *  This test class is created to test both FtpFileSystem and FtpFileSystemSender classes.
 * @author alisihab
 *
 */
public class FtpFileSystemTest extends FileSystemTest<FTPFile, FtpFileSystem> {

	private String username = "wearefrank";
	private String password = "pass_123";
	private String host = "localhost";
	private String remoteDirectory = "/home/wearefrank/dir";
	private int port = 21;

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
}
