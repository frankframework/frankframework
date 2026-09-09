package org.frankframework.filesystem.mock;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.frankframework.filesystem.IMailFileSystem;
import org.frankframework.stream.Message;
import org.frankframework.xml.SaxElementBuilder;

public class MockFileSystemWithAttachments extends MockFileSystem<MockFileWithAttachments> implements IMailFileSystem<MockFileWithAttachments, MockAttachment> {

	@Override
	public Iterator<MockAttachment> listAttachments(MockFileWithAttachments f) {
		List<MockAttachment> list = f.getAttachments();
		return list==null?null:list.iterator();
	}

	@Override
	public String getAttachmentName(MockAttachment a) {
		return a.getName();
	}

	@Override
	public Message readAttachment(MockAttachment a) {
		return a.getContents()==null?null:new Message(a.getContents());
	}

	@Override
	public long getAttachmentSize(MockAttachment a) {
		return a.getContents()==null?0:a.getContents().length;
	}

	@Override
	public String getAttachmentContentType(MockAttachment a) {
		return a.getContents()==null?null:a.getContentType();
	}

	@Override
	public String getAttachmentFileName(MockAttachment a) {
		return a.getContents()==null?null:a.getFilename();
	}

	@Override
	public Map<String, Object> getAdditionalAttachmentProperties(MockAttachment a) {
		return a.getContents()==null?null:a.getAdditionalProperties();
	}

	@Override
	public MockFileWithAttachments getFileFromAttachment(MockAttachment a) {
		return null;
	}

	@Override
	public String getSubject(MockFileWithAttachments emailMessage) {
		return "";
	}

	@Override
	public Message getMimeContent(MockFileWithAttachments emailMessage) {
		return null;
	}

	@Override
	public void forwardMail(MockFileWithAttachments emailMessage, String destination) {

	}

	@Override
	public void extractEmail(MockFileWithAttachments emailMessage, SaxElementBuilder emailXml) {

	}

	@Override
	public void extractAttachment(MockAttachment attachment, SaxElementBuilder attachmentsXml) {

	}

	@Override
	public String getReplyAddressFields() {
		return "";
	}
}
