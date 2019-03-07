package nl.nn.adapterframework.filesystem;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

public class LocalFileSystemTest extends LocalFileSystemTestBase<File,LocalFileSystem> {

	@Override
	protected LocalFileSystem getFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setDirectory(folder.getRoot().getAbsolutePath());
		return result;
	}
	
	@Ignore
	@Override
	@Test
	public void testRenameTo() throws Exception {
		// TODO Auto-generated method stub
		super.testRenameTo();
	}
}
