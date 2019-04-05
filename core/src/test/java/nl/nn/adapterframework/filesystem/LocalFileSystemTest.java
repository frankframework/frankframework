package nl.nn.adapterframework.filesystem;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

public class LocalFileSystemTest extends LocalFileSystemTestBase<File,LocalFileSystem> {

	@Override
	protected LocalFileSystem getFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setRoot(folder.getRoot().getAbsolutePath());
		return result;
	}
	
	@Ignore
	@Override
	@Test
	public void fileSystemTestRenameTo() throws Exception {
		// Ignored because cannot rename temporary file
		super.fileSystemTestRenameTo();
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
