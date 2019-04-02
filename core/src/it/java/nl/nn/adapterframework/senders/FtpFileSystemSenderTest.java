package nl.nn.adapterframework.senders;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.After;
import org.junit.Before;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.FtpFileSystem;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;

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

	private FtpSession ftpSession;

	private int waitMillis = 0;

	{
		setWaitMillis(waitMillis);
	}

	@Override
	@Before
	public void setUp() throws ConfigurationException, IOException, FileSystemException {
		super.setUp();
		open();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		ftpSession.closeClient();
		ftpSession = null;
		super.tearDown();
	}

	private void open() throws FileSystemException, ConfigurationException {
		ftpSession = new FtpSession(); 
		ftpSession.setUsername(username);
		ftpSession.setPassword(password);
		ftpSession.setHost(host);
		ftpSession.setPort(port);
		ftpSession.configure();
		
		try {
			ftpSession.openClient("");
		} catch (FtpConnectException e) {
			throw new FileSystemException("Cannot connect to the FTP server with domain [" + host + "]", e);
		}
	}

	@Override
	protected FtpFileSystem getFileSystem() throws ConfigurationException {
		FtpFileSystem fileSystem = new FtpFileSystem();
		fileSystem.setHost(host);
		fileSystem.setUsername(username);
		fileSystem.setPassword(password);
		fileSystem.setRemoteDirectory(remoteDirectory);
		fileSystem.setPort(port);

		return fileSystem;
	}

	@Override
	protected boolean _fileExists(String filename) throws IOException, FileSystemException {
		try {
			FTPFile[] files = ftpSession.ftpClient.listFiles();
			for (FTPFile o : files) {
				if (o.isDirectory()) {
					if ((filename.endsWith("/") ? o.getName() + "/" : o.getName()).equals(filename)) {
						return true;
					}
				} else if (o.getName().equals(filename)) {
					return true;
				}
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
		return false;
	}

	@Override
	protected void _deleteFile(String filename) throws FileSystemException {
		try {
			ftpSession.ftpClient.deleteFile(filename);
		} catch (IOException e) {
			throw new FileSystemException("", e);
		}
	}

	private FilterOutputStream completePendingCommand(OutputStream os) {
		FilterOutputStream fos = new FilterOutputStream(os) {
			@Override
			public void close() throws IOException {
				super.close();
				ftpSession.ftpClient.completePendingCommand();
			}
		};
		return fos;
	}

	@Override
	protected OutputStream _createFile(String filename) throws IOException, FileSystemException {
		OutputStream out = ftpSession.ftpClient.storeFileStream(filename);
		return completePendingCommand(out);
	}

	@Override
	protected InputStream _readFile(String filename) throws IOException, FileSystemException {
		InputStream is = ftpSession.ftpClient.retrieveFileStream(filename);
		ftpSession.ftpClient.completePendingCommand();
		return is;
	}

	@Override
	public void _createFolder(String filename) throws FileSystemException {
		try {
			ftpSession.ftpClient.makeDirectory(filename);
		} catch (IOException e) {
			throw new FileSystemException("Cannot create directory", e);
		}
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		try {
			FTPFile[] files = ftpSession.ftpClient.listFiles();
			for (FTPFile o : files) {
				if (o.isDirectory()) {
					if ((o.getName() + "/").equals(folderName)) {
						return true;
					}
				} else if (o.getName().equals(folderName)) {
					return true;
				}
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
		return false;
	}

	@Override
	protected void _deleteFolder(String folderName) throws Exception {
		ftpSession.ftpClient.rmd(folderName);
	}
}