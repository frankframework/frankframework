package org.frankframework.filesystem.mock;

import org.frankframework.filesystem.FileSystemListener;
import org.frankframework.filesystem.FileSystemListenerExtraTest;
import org.frankframework.filesystem.IFileSystemTestHelperFullControl;

public class MockFileSystemListenerTest extends FileSystemListenerExtraTest <MockFile,MockFileSystem<MockFile>>{


	@Override
	protected IFileSystemTestHelperFullControl getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	public FileSystemListener<MockFile, MockFileSystem<MockFile>> createFileSystemListener() {
		FileSystemListener<MockFile,MockFileSystem<MockFile>> result=new FileSystemListener<>(){

			@Override
			protected MockFileSystem<MockFile> createFileSystem() {
				return ((MockFileSystemTestHelper<MockFile>)helper).getFileSystem();
			}

		};
		return result;
	}

}
