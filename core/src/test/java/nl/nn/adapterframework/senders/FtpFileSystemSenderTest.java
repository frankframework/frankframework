package nl.nn.adapterframework.senders;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.FtpFileSystem;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;

public class FtpFileSystemSenderTest extends FileSystemSenderTest<FTPFile, FtpFileSystem> {

	FtpFileSystem ffs = new FtpFileSystem();
	private FtpSession ftpSession = new FtpSession();

	private String username = "test";
	private String password = "test";
	private String host = "10.0.0.190";
	private int port = 21;

	@Override
	public void setup() throws ConfigurationException, IOException {
		super.setup();
		ftpSession.setUsername(username);
		ftpSession.setPassword(password);
		ftpSession.setHost(host);
		ftpSession.setPort(port);

		ftpSession.configure();
	}

	@Override
	protected FtpFileSystem getFileSystem() throws ConfigurationException {
		FtpSession session = ffs.getFtpSession();
		session.setUsername(username);
		session.setPassword(password);
		session.setHost(host);
		session.setPort(port);

		ffs.configure();

		return ffs;
	}

	@Override
	protected boolean _fileExists(String filename) throws IOException {
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
			e.printStackTrace();
		}
		return false;
	}

	@Override
	protected void _deleteFile(String filename) {
		try {
			close();
			open();
			ftpSession.ftpClient.deleteFile(filename);
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected OutputStream _createFile(String filename) throws IOException {
		close();
		open();
		OutputStream out = ftpSession.ftpClient.storeFileStream(filename);
		return out;
	}

	@Override
	protected InputStream _readFile(String filename) throws IOException {
		close();
		open();
		InputStream is = ftpSession.ftpClient.retrieveFileStream(filename);
		return is;
	}

	@Override
	public void _createFolder(String filename) throws IOException {
		try {
			close();
			open();
			ftpSession.ftpClient.makeDirectory(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void close() {
		ftpSession.closeClient();
	}

	private void open() {
		try {
			ftpSession.openClient("");
		} catch (FtpConnectException e) {
			e.printStackTrace();
		}
	}
}