package it.nl.nn.adapterframework.senders;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.FtpFileSystem;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;

/**
 *  This test class is created to test both FtpFileSystem and FtpFileSystemSender classes.
 * @author alisihab
 *
 */
public class FtpFileSystemSenderTest extends FileSystemSenderTest<FTPFile, FtpFileSystem> {

	private String username = "";
	private String password = "";
	private String host = "";
	private String remoteDirectory = "";
	private int port = 21;

	private int waitMillis = 0;

	{
		setWaitMillis(waitMillis);
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new FtpFileSystemTestHelper(username, password, host, remoteDirectory, port);
	}

	@Override
	protected FtpFileSystem getFileSystem() {
		FtpFileSystem fileSystem = new FtpFileSystem();
		fileSystem.setHost(host);
		fileSystem.setUsername(username);
		fileSystem.setPassword(password);
		fileSystem.setRemoteDirectory(remoteDirectory);
		fileSystem.setPort(port);

		return fileSystem;
	}
}