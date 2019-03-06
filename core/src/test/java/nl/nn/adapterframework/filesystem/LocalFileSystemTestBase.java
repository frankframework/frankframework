package nl.nn.adapterframework.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.IFileSystem;
import nl.nn.adapterframework.filesystems.FileSystemTest;

public abstract class LocalFileSystemTestBase<F, FS extends IFileSystem<F>>
		extends FileSystemTest<F, FS> {

	public TemporaryFolder folder;

	@Override
	@Before
	public void setup() throws IOException, ConfigurationException, FileSystemException {
		folder = new TemporaryFolder();
		folder.create();
		super.setup();
	}

	protected File getFileHandle(String filename) {
		return new File(folder.getRoot().getAbsolutePath(), filename);
	}

	@Override
	public boolean _fileExists(String filename) {
		return getFileHandle(filename).exists();
	}

	@Override
	public void _deleteFile(String filename) {
		getFileHandle(filename).delete();
	}

	@Override
	public OutputStream _createFile(String filename) throws IOException {
		File f = getFileHandle(filename);
		f.createNewFile();
		return new FileOutputStream(f);
	}

	@Override
	public void _createFolder(String filename) throws IOException {
		File f = getFileHandle(filename);
		f.mkdir();
	}

	@Override
	public InputStream _readFile(String filename) throws FileNotFoundException {
		return new FileInputStream(getFileHandle(filename));
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		return _fileExists(folderName);
	}

	@Override
	protected void _deleteFolder(String folderName) throws Exception {
		deleteFile(folderName);
	}
}
