package org.frankframework.filesystem.mock;

import org.frankframework.filesystem.ForEachAttachmentPipe;
import org.frankframework.filesystem.ForEachAttachmentPipeTest;
import org.frankframework.filesystem.IFileSystemTestHelper;

public class MockForEachAttachmentPipeTest extends ForEachAttachmentPipeTest <ForEachAttachmentPipe<MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments>, MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments>{


	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemWithAttachmentsTestHelper();
	}

	@Override
	public ForEachAttachmentPipe<MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments> createForEachAttachmentPipe() {
		ForEachAttachmentPipe<MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments> pipe= new ForEachAttachmentPipe<>();
		pipe.setFileSystem(((MockFileSystemWithAttachmentsTestHelper)helper).getFileSystem());
		return pipe;
	}

}
