package nl.nn.adapterframework.filesystem;

import org.junit.Ignore;
import org.junit.Test;

public class MockFileSystemActorTest extends FileSystemActorExtraTest <MockFile,MockFileSystem<MockFile>>{

	
	@Override
	protected IFileSystemTestHelperFullControl getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	protected MockFileSystem<MockFile> createFileSystem() {
		return ((MockFileSystemTestHelper<MockFile>)helper).getFileSystem();
	}

	@Ignore("does not support throwing exceptions by attempting to remove non empty folder.")
	@Override
	@Test
	public void fileSystemActorDeleteActionWithDeleteEmptyFolderRootContainsEmptyFoldersTest() throws Exception {
		
	}
}
