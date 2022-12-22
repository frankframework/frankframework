package nl.nn.adapterframework.filesystem;

import java.nio.file.Path;

import org.junit.rules.TemporaryFolder;

public class LocalFileSystemMessageBrowserTest extends FileSystemMessageBrowserTest<Path, LocalFileSystem>{

	public TemporaryFolder folder;


	@Override
	protected LocalFileSystem createFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setRoot(folder.getRoot().getAbsolutePath());
		return result;
	}

	@Override
	public void setUp() throws Exception {
		folder = new TemporaryFolder();
		folder.create();
		super.setUp();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}


}
