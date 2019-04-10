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
import nl.nn.adapterframework.filesystem.IWritableFileSystem;
import nl.nn.adapterframework.filesystem.FileSystemTest;

public abstract class LocalFileSystemTestBase<F, FS extends IWritableFileSystem<F>> extends FileSystemTest<F, FS> {

	public TemporaryFolder folder;

	@Override
	@Before
	public void setUp() throws IOException, ConfigurationException, FileSystemException {
		folder = new TemporaryFolder();
		folder.create();
		super.setUp();
	}

	protected File getFileHandle(String filename) {
		return new File(folder.getRoot().getAbsolutePath(), filename);
	}
	protected File getFileHandle(String subfolder, String filename) {
		if (subfolder==null) {
			return getFileHandle(filename);
		}
		return new File(folder.getRoot().getAbsolutePath()+"/"+subfolder, filename);
	}

	@Override
	public boolean _fileExists(String subfolder, String filename) {
		return getFileHandle(subfolder,filename).exists();
	}

	@Override
	public void _deleteFile(String folder, String filename) {
		getFileHandle(folder, filename).delete();
	}

	@Override
	public OutputStream _createFile(String folder, String filename) throws IOException {
		File f = getFileHandle(folder, filename);
		f.createNewFile();
		return new FileOutputStream(f);
	}

	@Override
	public void _createFolder(String filename) throws IOException {
		File f = getFileHandle(filename);
		f.mkdir();
	}

	@Override
	public InputStream _readFile(String folder, String filename) throws FileNotFoundException {
		return new FileInputStream(getFileHandle(folder, filename));
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		return _fileExists(folderName);
	}

	@Override
	protected void _deleteFolder(String folderName) throws Exception {
		deleteFile(null, folderName);
	}
}
