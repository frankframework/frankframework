package nl.nn.adapterframework.filesystem;

import java.io.File;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalFileSystemTest extends FileSystemTest<File, LocalFileSystem>{

	public TemporaryFolder folder;


	@Override
	protected LocalFileSystem createFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setRoot(folder.getRoot().getAbsolutePath());
		return result;
	}

	@Override
	@Before
	public void setUp() throws Exception {
		folder = new TemporaryFolder();
		folder.create();
		super.setUp();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}
	
	@Ignore
	@Override
	@Test
	public void writableFileSystemTestRenameTo() throws Exception {
		// Ignored because cannot rename temporary file
		super.writableFileSystemTestRenameTo();
	}
	
	@Ignore
	@Override
	@Test
	public void writableFileSystemTestRenameToExisting() throws Exception {
		// Ignored because foreach test different temp folder is created
		// create file creates destination file in different folder 
		// so that renameTo method returns false in exists file check
		// and does not throw the exception. 
		super.writableFileSystemTestRenameToExisting();
	}

}
