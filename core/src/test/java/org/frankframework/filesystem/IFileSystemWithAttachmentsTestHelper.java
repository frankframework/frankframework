package org.frankframework.filesystem;

public interface IFileSystemWithAttachmentsTestHelper<A> extends IFileSystemTestHelper {

	public A createAttachment(String name, String filename, String contentType, byte[] contents);
	public void addAttachment(String folder, String filename, A attachment);
	public void setProperty(A attachment, String key, Object value);
	public Object getProperty(A attachment, String key);

}
