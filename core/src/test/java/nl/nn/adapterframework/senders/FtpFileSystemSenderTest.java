package nl.nn.adapterframework.senders;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.FtpFileSystem;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;

public class FtpFileSystemSenderTest extends FileSystemSenderTest<FTPFile, FtpFileSystem> {

	FtpFileSystem ffs = new FtpFileSystem();
	private FtpSession ftpSession = new FtpSession();

	private String username = "";
	private String password = "";
	private String host = "";
	private String remoteDirectory = "FTPTest";
	private int port = 21;

	@Override
	public void setup() throws ConfigurationException, IOException, FileSystemException {
		super.setup();
		ftpSession.setUsername(username);
		ftpSession.setPassword(password);
		ftpSession.setHost(host);
		ftpSession.setPort(port);

		ftpSession.configure();
		open();
	}

	@Override
	protected FtpFileSystem getFileSystem() throws ConfigurationException {
		ffs.setHost(host);
		ffs.setUsername(username);
		ffs.setPassword(password);
		ffs.setRemoteDirectory(remoteDirectory);
		ffs.setPort(port);
		ffs.configure();

		return ffs;
	}

	@Override
	protected boolean _fileExists(String filename) throws IOException, FileSystemException {
		try {
			close();
			open();
			FTPFile[] files = ftpSession.ftpClient.listFiles();
			for (FTPFile o : files) {
				if (o.isDirectory()) {
					if ((o.getName() + "/").equals(filename)) {
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
			close();
			open();
			ftpSession.ftpClient.deleteFile(filename);
			close();
		} catch (IOException e) {
			throw new FileSystemException("", e);
		}
	}

	@Override
	protected OutputStream _createFile(String filename) throws IOException, FileSystemException {
		close();
		open();
		OutputStream out = ftpSession.ftpClient.storeFileStream(filename);
		return out;
	}

	@Override
	protected InputStream _readFile(String filename) throws IOException, FileSystemException {
		close();
		open();
		InputStream is = ftpSession.ftpClient.retrieveFileStream(filename);
		return is;
	}

	@Override
	public void _createFolder(String filename) throws FileSystemException {
		try {
			close();
			open();
			ftpSession.ftpClient.makeDirectory(filename);
		} catch (IOException e) {
			throw new FileSystemException("Cannot create directory", e);
		}
	}

	private void close() {
		ftpSession.closeClient();
	}

	private void open() throws FileSystemException {
		try {
			ftpSession.openClient("");
		} catch (FtpConnectException e) {
			throw new FileSystemException("Cannot open connection", e);
		}
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		try {
			close();
			open();
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
		_deleteFile(folderName);
	}
}