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

public abstract class LocalFileSystemTestBase<Ff,FS extends IFileSystem<Ff>> extends FileSystemTest<Ff,FS> {

	public TemporaryFolder folder;
	
	@Override
	@Before
	public void setup() throws IOException, ConfigurationException {
		folder = new TemporaryFolder();
		folder.create();
		super.setup();
	}


	protected File getFileHandle(String filename) {
		return new File(folder.getRoot().getAbsolutePath(),filename);
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
	public InputStream _readFile(String filename) throws FileNotFoundException {
		return new FileInputStream(getFileHandle(filename));
	}



}
