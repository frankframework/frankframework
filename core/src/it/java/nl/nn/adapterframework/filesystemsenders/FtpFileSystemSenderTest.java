package nl.nn.adapterframework.filesystemsenders;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.FtpFileSystem;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;

public class FtpFileSystemSenderTest extends FileSystemSenderTest<FTPFile, FtpFileSystem> {

	FtpFileSystem ffs = new FtpFileSystem();

	private String username = "";
	private String password = "";
	private String host = "";
	private String remoteDirectory = "";
	private int port = 21;
	private int waitMilis = 0;

	private static class FTPConnection {
		private static FTPConnection ftpConnection;
		private static FtpSession ftpSession;

		private FTPConnection(String userName, String password, String host, int port) throws ConfigurationException {
			ftpSession = new FtpSession();
			ftpSession.setUsername(userName);
			ftpSession.setPassword(password);
			ftpSession.setHost(host);
			ftpSession.setPort(port);
			ftpSession.configure();
		}

		public static FTPConnection getInstance(String userName, String password, String host, int port)
				throws ConfigurationException {
			if (ftpConnection == null) {
				ftpConnection = new FTPConnection(userName, password, host, port);
			}
			return ftpConnection;

		}

		public static FTPClient getClient() throws FileSystemException {
			if (ftpSession.ftpClient == null || !ftpSession.ftpClient.isConnected()) {
				try {
					ftpSession.closeClient();
					ftpSession.openClient("");
					ftpSession.ftpClient.configure(new FTPClientConfig(FTPClientConfig.SYST_UNIX));
				} catch (FtpConnectException e) {
					throw new FileSystemException(e);
				}
			}

			return ftpSession.ftpClient;
		}
	}

	@Override
	public void setup() throws ConfigurationException, IOException, FileSystemException {
		super.setup();
		setWaitMilis(waitMilis);
		FTPConnection.getInstance(username, password, host, port);
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
			FTPFile[] files = FTPConnection.getClient().listFiles();
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
			FTPConnection.getClient().deleteFile(filename);
		} catch (IOException e) {
			throw new FileSystemException("", e);
		}
	}

	private FilterOutputStream completePendingCommand(OutputStream os) {
		FilterOutputStream fos = new FilterOutputStream(os) {
			@Override
			public void close() throws IOException {
				super.close();
				try {
					FTPConnection.getClient().completePendingCommand();
				} catch (FileSystemException e) {
					System.err.println(e);
				}
			}
		};
		return fos;
	}

	@Override
	protected OutputStream _createFile(String filename) throws IOException, FileSystemException {
		OutputStream out = FTPConnection.getClient().storeFileStream(filename);
		return completePendingCommand(out);
	}

	@Override
	protected InputStream _readFile(String filename) throws IOException, FileSystemException {
		InputStream is = FTPConnection.getClient().retrieveFileStream(filename);
		FTPConnection.getClient().completePendingCommand();
		return is;
	}

	@Override
	public void _createFolder(String filename) throws FileSystemException {
		try {
			FTPConnection.getClient().makeDirectory(filename);
		} catch (IOException e) {
			throw new FileSystemException("Cannot create directory", e);
		}
	}

	private void open() throws FileSystemException {
		FTPConnection.getClient();
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		try {
			FTPFile[] files = FTPConnection.getClient().listFiles();
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
		FTPConnection.getClient().rmd(folderName);
	}
}