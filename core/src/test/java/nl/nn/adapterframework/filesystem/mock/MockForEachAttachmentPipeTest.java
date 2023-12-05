package nl.nn.adapterframework.filesystem.mock;

import nl.nn.adapterframework.filesystem.ForEachAttachmentPipe;
import nl.nn.adapterframework.filesystem.ForEachAttachmentPipeTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;

public class MockForEachAttachmentPipeTest extends ForEachAttachmentPipeTest <ForEachAttachmentPipe<MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments>, MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments>{

	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemWithAttachmentsTestHelper();
	}

	@Override
	public ForEachAttachmentPipe<MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments> createForEachAttachmentPipe() {
		ForEachAttachmentPipe<MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments> pipe= new ForEachAttachmentPipe<MockFileWithAttachments, MockAttachment, MockFileSystemWithAttachments>();
		pipe.setFileSystem(((MockFileSystemWithAttachmentsTestHelper)helper).getFileSystem());
		return pipe;
	}

}
