package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.rules.TemporaryFolder;

public class LocalFileSystemUtilTest extends FileSystemUtilsTest <Path,LocalFileSystem>{
	TemporaryFolder temp;

	@Before
	@Override
	public void setUp() throws Exception {
		temp = new TemporaryFolder();
		temp.create();
		super.setUp();
	}
	@Override
	protected LocalFileSystem createFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setRoot(temp.getRoot().getAbsolutePath());
		return result;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() throws IOException {
		return new LocalFileSystemTestHelper(temp);
	}
}
