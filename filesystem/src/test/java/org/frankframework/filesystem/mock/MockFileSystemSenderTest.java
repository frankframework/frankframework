package org.frankframework.filesystem.mock;

import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.FileSystemSender;
import org.frankframework.filesystem.FileSystemSenderTest;
import org.frankframework.filesystem.IFileSystemTestHelper;

public class MockFileSystemSenderTest extends FileSystemSenderTest <FileSystemSender<MockFile, MockFileSystem<MockFile>>, MockFile,MockFileSystem<MockFile>>{

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	public FileSystemSender<MockFile, MockFileSystem<MockFile>> createFileSystemSender() {
		FileSystemSender<MockFile,MockFileSystem<MockFile>> result=new FileSystemSender<>() {};
		result.setFileSystem(((MockFileSystemTestHelper<MockFile>)helper).getFileSystem());
		return result;
	}

	@Test
	@Override
	public void fileSystemSenderListFoldersActionTestInFolder() {
		// Not supported
	}

	@Test
	@Override
	public void fileSystemSenderListFilesAndFoldersActionTestInFolder() {
		// Not supported
	}

}
