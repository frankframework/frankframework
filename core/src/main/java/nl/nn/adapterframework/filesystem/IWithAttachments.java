package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public interface IWithAttachments<F,A> extends IBasicFileSystem<F> {
	
	public Iterator<A> listAttachments(F f) throws FileSystemException;

	public String getAttachmentName(F f, A a);
	public InputStream readAttachment(F f, A a) throws FileSystemException, IOException;
	public long getAttachmentSize(F f, A a) throws FileSystemException;
	public String getAttachmentContentType(F f, A a) throws FileSystemException;
	public String getAttachmentFileName(F f, A a) throws FileSystemException;

	public Map<String, Object> getAdditionalAttachmentProperties(F f, A a) throws FileSystemException;

}
