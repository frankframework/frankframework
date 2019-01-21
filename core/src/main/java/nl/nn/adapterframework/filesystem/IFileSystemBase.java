package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import nl.nn.adapterframework.configuration.ConfigurationException;


public interface IFileSystemBase<F> {

	public void configure() throws ConfigurationException;
	
	public F toFile(String filename) throws FileSystemException;
	public Iterator<F> listFiles() throws FileSystemException;
	public boolean exists(F f) throws FileSystemException;
	public OutputStream createFile(F f) throws FileSystemException,IOException;
	public OutputStream appendFile(F f) throws FileSystemException,IOException;
	public InputStream readFile(F f) throws FileSystemException,IOException;
	public void deleteFile(F f) throws FileSystemException;
	public String getInfo(F f) throws FileSystemException;
	
}
