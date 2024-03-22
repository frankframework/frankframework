package org.frankframework.filesystem.mock;

import java.util.LinkedList;
import java.util.List;

public class MockFileWithAttachments extends MockFile {

	private List<MockAttachment> attachments;

	public MockFileWithAttachments(String name, MockFolder owner) {
		super(name,owner);
	}

	public List<MockAttachment> getAttachments() {
		return attachments;
	}

	public void addAttachment(MockAttachment attachment) {
		if (attachments==null) {
			attachments=new LinkedList<>();
		}
		attachments.add(attachment);
	}
}
