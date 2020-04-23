package nl.nn.adapterframework.filesystem;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Before;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.Samba2Sender;
import nl.nn.adapterframework.stream.Message;

public class Samba2FileSystemTestHelper implements IFileSystemTestHelper {

	protected Samba2Sender fileSystemSender;
		
	public Samba2FileSystemTestHelper(String shareFolder, String username, String password, String host, Integer port, String domain) {
		fileSystemSender = new Samba2Sender();
		fileSystemSender.setShare(shareFolder);
		fileSystemSender.setUsername(username);
		fileSystemSender.setPassword(password);
		fileSystemSender.setDomain(host);
		fileSystemSender.setPort(port);
	}
	
	@Override
	@Before
	public void setUp() throws ConfigurationException, IOException, FileSystemException, SenderException, TimeOutException {
		// not necessary
	}

	@Override
	public void tearDown() throws Exception {
		// not necessary
	}
	
	private boolean isPresent(String name) throws Exception {
		fileSystemSender.open();
		boolean r = fileSystemSender.getFileSystem().exists(name);
		fileSystemSender.close();
		return r;
	}
	
	@Override
	public boolean _fileExists(String folder, String fileName) throws Exception {
		return isPresent(fileName);
	}

	@Override
	public void _deleteFile(String folder, String fileName) throws Exception {
		this.prepareAction("delete", fileName);
		fileSystemSender.sendMessage(new Message("delete"), null);
		fileSystemSender.close();
	}

	@Override
	public OutputStream _createFile(String folder, String fileName) throws Exception {
		this.prepareAction("upload", fileName);
		Message message = fileSystemSender.sendMessage(new Message("upload"), null);
		OutputStream os = new ByteArrayOutputStream();
		os.write(message.asByteArray());
		return os;
	}

	@Override
	public InputStream _readFile(String folder, String fileName) throws FileNotFoundException, Exception {
		this.prepareAction("read", fileName);
		Message message = fileSystemSender.sendMessage(new Message(fileName), null);
		InputStream is = message.asInputStream();
		return is;
	}

	@Override
	public boolean _folderExists(String folderName) throws Exception {
		return isPresent(folderName);
	}

	@Override
	public void _createFolder(String folderName) throws IOException, ConfigurationException, SenderException, TimeOutException {
		this.prepareAction("mkdir", folderName);
		fileSystemSender.sendMessage(new Message(folderName), null);
		fileSystemSender.close();
	}
	
	@Override
	public void _deleteFolder(String folderName) throws Exception {
		this.prepareAction("rmdir", folderName);
		fileSystemSender.sendMessage(new Message(folderName), null);
		fileSystemSender.close();
	}
	
	private void prepareAction(String action, String name) throws ConfigurationException, SenderException {
		fileSystemSender.setAction(action);
		fileSystemSender.setFilename(name);
		fileSystemSender.configure();
		fileSystemSender.open();
	}

}
