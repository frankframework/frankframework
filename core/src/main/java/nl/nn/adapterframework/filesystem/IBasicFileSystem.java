package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * 
 * @author Gerrit van Brakel
 *
 * @param <F> File representation
 */
public interface IBasicFileSystem<F> {

	public void configure() throws ConfigurationException;
	public void open() throws FileSystemException;
	public void close() throws FileSystemException;

	public F toFile(String filename) throws FileSystemException;
	public boolean exists(F f) throws FileSystemException;
	public Iterator<F> listFiles() throws FileSystemException;

	public boolean isFolder(F f) throws FileSystemException;
	public InputStream readFile(F f) throws FileSystemException, IOException;
	public void deleteFile(F f) throws FileSystemException;
	public void moveFile(F f, String destinationFolder) throws FileSystemException;

	public long getFileSize(F f, boolean isFolder) throws FileSystemException;
	public String getName(F f) throws FileSystemException;
	public String getCanonicalName(F f, boolean isFolder) throws FileSystemException;
	public Date getModificationTime(F f, boolean isFolder) throws FileSystemException;

	public Map<String, Object> getAdditionalFileProperties(F f) throws FileSystemException;

	

}
