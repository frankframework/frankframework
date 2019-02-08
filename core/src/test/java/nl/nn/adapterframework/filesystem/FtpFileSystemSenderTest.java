package nl.nn.adapterframework.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.Ignore;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpSession;

@Ignore
public class FtpFileSystemSenderTest extends FileSystemSenderTest<FTPFile, FtpFileSystem> {

	private FtpFileSystem ffs;

	// TODO: Add local connection parameters.

	private String localFilePath = "";
	private String share = null;
	private String relativePath = "DummyFolder/";
	private String username = "";
	private String password = "";
	private String host = "";
	private int port = 22;

	@Override
	protected FtpFileSystem getFileSystem() throws ConfigurationException {
		ffs = new FtpFileSystem();
		FtpSession session = ffs.getFtpSession();

		session.setUsername(username);
		session.setPassword(password);
		session.setHost(host);
		session.setPort(port);
		ffs.configure();

		return ffs;
	}

	@Override
	protected boolean _fileExists(String filename) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void _deleteFile(String filename) {
		// TODO Auto-generated method stub

	}

	@Override
	protected OutputStream _createFile(String filename) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected InputStream _readFile(String filename) throws FileNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void _createFolder(String filename) throws IOException {
		// TODO Auto-generated method stub

	}
}