package org.frankframework.filesystem.mock;

import org.frankframework.filesystem.AbstractFileSystemListener;
import org.frankframework.filesystem.FileSystemListenerExtraTest;
import org.frankframework.filesystem.IFileSystemTestHelperFullControl;

public class MockFileSystemListenerTest extends FileSystemListenerExtraTest <MockFile,MockFileSystem<MockFile>>{


	@Override
	protected IFileSystemTestHelperFullControl getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	public AbstractFileSystemListener<MockFile, MockFileSystem<MockFile>> createFileSystemListener() {
		AbstractFileSystemListener<MockFile,MockFileSystem<MockFile>> result=new AbstractFileSystemListener<>(){

			@Override
			protected MockFileSystem<MockFile> createFileSystem() {
				return ((MockFileSystemTestHelper<MockFile>)helper).getFileSystem();
			}

		};
		return result;
	}

}
