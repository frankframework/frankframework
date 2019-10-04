package nl.nn.adapterframework.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.rules.TemporaryFolder;

public class LocalFileSystemTestHelper implements IFileSystemTestHelper {

	public TemporaryFolder folder;


	public LocalFileSystemTestHelper(TemporaryFolder folder) {
		this.folder=folder;
	}

	@Override
	public void setUp() throws Exception {
		// not necessary
	}

	@Override
	public void tearDown() throws Exception {
		// not necessary
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
		try {
			f.createNewFile();
		} catch (IOException e) {
			throw new IOException("Cannot create file ["+f.getAbsolutePath()+"]",e);
		}
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
	public boolean _folderExists(String folderName) throws Exception {
		return _fileExists(null,folderName);
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		_deleteFile(null, folderName);
	}
}
