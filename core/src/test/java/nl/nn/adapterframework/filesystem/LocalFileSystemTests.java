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

public class LocalFileSystemTests extends FileSystemTest<File,LocalFileSystem> {

	public TemporaryFolder folder;
	
	@Override
	@Before
	public void setup() throws IOException {
		folder = new TemporaryFolder();
		folder.create();
		super.setup();
	}

	@Override
	protected LocalFileSystem getFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setDirectory(folder.getRoot().getAbsolutePath());
		return result;
	}

	@Override
	protected File getFileHandle(String filename) {
		return new File(folder.getRoot().getAbsolutePath(),filename);
	}

	
	
	@Override
	public boolean _fileExists(File f) {
		return f.exists();
	}

	@Override
	public void _deleteFile(File f) {
		f.delete();
	}

	@Override
	public OutputStream _createFile(File f) throws IOException {
		f.createNewFile();
		return new FileOutputStream(f);
		
	}

	@Override
	public InputStream _readFile(File f) throws FileNotFoundException {
		return new FileInputStream(f);
	}



}
