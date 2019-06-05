package nl.nn.adapterframework.filesystem;

public interface IFileSystemWithAttachmentsTestHelper<A> extends IFileSystemTestHelper {

	public A createAttachment(String name, String filename, String contentType, byte[] contents);
	public void addAttachment(String folder, String filename, A attachment);
	
}
