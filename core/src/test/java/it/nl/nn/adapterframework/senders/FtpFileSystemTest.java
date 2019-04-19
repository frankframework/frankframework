package it.nl.nn.adapterframework.senders;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.filesystem.FileSystemTest;
import nl.nn.adapterframework.filesystem.FtpFileSystem;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;

/**
 *  This test class is created to test both FtpFileSystem and FtpFileSystemSender classes.
 * @author alisihab
 *
 */
public class FtpFileSystemTest extends FileSystemTest<FTPFile, FtpFileSystem> {

	private String username = "test";
	private String password = "test";
	private String host = "10.0.0.190";
	private String remoteDirectory = "";
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
