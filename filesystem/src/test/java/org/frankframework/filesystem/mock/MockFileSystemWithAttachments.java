package org.frankframework.filesystem.mock;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.xml.sax.SAXException;

import org.frankframework.filesystem.FileSystemException;
import org.frankframework.filesystem.IMailFileSystem;
import org.frankframework.stream.Message;
import org.frankframework.xml.SaxElementBuilder;

public class MockFileSystemWithAttachments extends MockFileSystem<MockFileWithAttachments> implements IMailFileSystem<MockFileWithAttachments, MockAttachment> {

	@Override
	public @Nullable Iterator<MockAttachment> listAttachments(MockFileWithAttachments f) throws FileSystemException {
		List<MockAttachment> list = f.getAttachments();
		return list==null?null:list.iterator();
	}

	@Override
	public String getAttachmentName(MockAttachment a) {
		return a.getName();
	}

	@Override
	public @Nullable Message readAttachment(MockAttachment a) throws FileSystemException, IOException {
		return a.getContents()==null?null:new Message(a.getContents());
	}

	@Override
	public long getAttachmentSize(MockAttachment a) throws FileSystemException {
		return a.getContents()==null?0:a.getContents().length;
	}

	@Override
	public @Nullable String getAttachmentContentType(MockAttachment a) throws FileSystemException {
		return a.getContents()==null?null:a.getContentType();
	}

	@Override
	public @Nullable String getAttachmentFileName(MockAttachment a) throws FileSystemException {
		return a.getContents()==null?null:a.getFilename();
	}

	@Override
	public @Nullable Map<String, Object> getAdditionalAttachmentProperties(MockAttachment a) throws FileSystemException {
		return a.getContents()==null?null:a.getAdditionalProperties();
	}

	@Override
	public MockFileWithAttachments getFileFromAttachment(MockAttachment a) throws FileSystemException {
		return null;
	}

	@Override
	public String getSubject(MockFileWithAttachments emailMessage) throws FileSystemException {
		return "";
	}

	@Override
	public @Nullable Message getMimeContent(MockFileWithAttachments emailMessage) throws FileSystemException {
		return null;
	}

	@Override
	public void forwardMail(MockFileWithAttachments emailMessage, String destination) throws FileSystemException {

	}

	@Override
	public void extractEmail(MockFileWithAttachments emailMessage, SaxElementBuilder emailXml) throws FileSystemException, SAXException {

	}

	@Override
	public void extractAttachment(MockAttachment attachment, SaxElementBuilder attachmentsXml) throws FileSystemException, SAXException {

	}

	@Override
	public String getReplyAddressFields() {
		return "";
	}
}
