package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public interface IWithAttachments<F,A> extends IBasicFileSystem<F> {
	
	public Iterator<A> listAttachments(F f) throws FileSystemException;

	public String getAttachmentName(A a);
	public A getAttachmentByName(F f, String name) throws FileSystemException;
	public InputStream readAttachment(F f, A a) throws FileSystemException, IOException;
	public long getAttachmentSize(A a) throws FileSystemException;
	public String getAttachmentContentType(A a) throws FileSystemException;
	public String getAttachmentFileName(A a) throws FileSystemException;

	public Map<String, Object> getAdditionalAttachmentProperties(A a) throws FileSystemException;

}
