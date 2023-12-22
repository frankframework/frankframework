package org.frankframework.filesystem.mock;

import java.util.Map;

import org.frankframework.filesystem.IFileSystemWithAttachmentsTestHelper;

public class MockFileSystemWithAttachmentsTestHelper extends MockFileSystemTestHelper<MockFileWithAttachments> implements IFileSystemWithAttachmentsTestHelper<MockAttachment> {

	public MockFileSystemWithAttachmentsTestHelper() {
		super(new MockFileSystemWithAttachments());
	}

	@Override
	public MockFileSystemWithAttachments getFileSystem() {
		return (MockFileSystemWithAttachments)super.getFileSystem();
	}

	@Override
	public MockAttachment createAttachment(String name, String filename, String contentType, byte[] contents) {
		MockAttachment a = new MockAttachment(name, filename, contentType);
		a.setContents(contents);
		return a;
	}

	@Override
	public void addAttachment(String foldername, String filename, MockAttachment attachment) {
		MockFolder folder = foldername==null?getFileSystem():getFileSystem().getFolders().get(foldername);
		MockFileWithAttachments file = (MockFileWithAttachments)folder.getFiles().get(filename);
		file.addAttachment(attachment);
	}

	@Override
	protected MockFileWithAttachments createNewFile(MockFolder folder, String filename) {
		return new MockFileWithAttachments(filename,folder);
	}

	@Override
	public void setProperty(MockAttachment attachment, String key, Object value) {
		attachment.setAdditionalProperties(key, value);
	}

	@Override
	public Object getProperty(MockAttachment attachment, String key) {
		Map<String,Object> properties=attachment.getAdditionalProperties();
		if (properties==null) {
			return null;
		}
		return properties.get(key);
	}

}
