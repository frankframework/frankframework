package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.XmlBuilder;

public interface IFileSystemBase<F> {

	public void configure() throws ConfigurationException;
	public void open() throws FileSystemException;
	public void close() throws FileSystemException;

	public F toFile(String filename) throws FileSystemException;
	public Iterator<F> listFiles() throws FileSystemException;
	
	public boolean exists(F f) throws FileSystemException;
	public OutputStream createFile(F f) throws FileSystemException, IOException;
	public OutputStream appendFile(F f) throws FileSystemException, IOException;
	public InputStream readFile(F f) throws FileSystemException, IOException;
	public void deleteFile(F f) throws FileSystemException;
	public void renameTo(F f, String destination) throws FileSystemException;

	public long getFileSize(F f, boolean isFolder) throws FileSystemException;
	public String getName(F f) throws FileSystemException;
	public String getCanonicalName(F f, boolean isFolder) throws FileSystemException;
	public Date getModificationTime(F f, boolean isFolder) throws FileSystemException;

	public void augmentDirectoryInfo(XmlBuilder dirInfo, F f);
}
