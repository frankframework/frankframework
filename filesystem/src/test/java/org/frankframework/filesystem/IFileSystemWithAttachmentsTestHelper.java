package org.frankframework.filesystem;

public interface IFileSystemWithAttachmentsTestHelper<A> extends IFileSystemTestHelper {

	A createAttachment(String name, String filename, String contentType, byte[] contents);
	void addAttachment(String folder, String filename, A attachment);
	void setProperty(A attachment, String key, Object value);
	Object getProperty(A attachment, String key);

}
