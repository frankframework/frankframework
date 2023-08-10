package nl.nn.adapterframework.filesystem.mock;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.IWithAttachments;
import nl.nn.adapterframework.stream.Message;

public class MockFileSystemWithAttachments extends MockFileSystem<MockFileWithAttachments> implements IWithAttachments<MockFileWithAttachments, MockAttachment> {

	@Override
	public Iterator<MockAttachment> listAttachments(MockFileWithAttachments f) throws FileSystemException {
		List<MockAttachment> list = f.getAttachments();
		return list==null?null:list.iterator();
	}

	@Override
	public String getAttachmentName(MockAttachment a) {
		return a.getName();
	}

	@Override
	public Message readAttachment(MockAttachment a) throws FileSystemException, IOException {
		return a.getContents()==null?null:new Message(a.getContents());
	}

	@Override
	public long getAttachmentSize(MockAttachment a) throws FileSystemException {
		return a.getContents()==null?0:a.getContents().length;
	}

	@Override
	public String getAttachmentContentType(MockAttachment a) throws FileSystemException {
		return a.getContents()==null?null:a.getContentType();
	}

	@Override
	public String getAttachmentFileName(MockAttachment a) throws FileSystemException {
		return a.getContents()==null?null:a.getFilename();
	}

	@Override
	public Map<String, Object> getAdditionalAttachmentProperties(MockAttachment a) throws FileSystemException {
		return a.getContents()==null?null:a.getAdditionalProperties();
	}

	@Override
	public MockFileWithAttachments getFileFromAttachment(MockAttachment a) throws FileSystemException {
		return null;
	}

}
