package nl.nn.adapterframework.filesystem;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.ftp.FTPFileRef;
import nl.nn.adapterframework.senders.FtpFileSystemSender;

/**
 *  This test class is created to test both FtpFileSystem and FtpFileSystemSender classes.
 * @author alisihab
 *
 */
public class FtpFileSystemSenderTest extends FileSystemSenderTest<FtpFileSystemSender, FTPFileRef, FtpFileSystem> {

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
