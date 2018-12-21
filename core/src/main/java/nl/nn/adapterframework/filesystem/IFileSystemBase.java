package nl.nn.adapterframework.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;


public interface IFileSystemBase<F> {

	public F toFile(String filename);
	public Iterator<F> listFiles();
	public boolean exists(F f);
	public OutputStream createFile(F f) throws IOException;
	public OutputStream appendFile(F f) throws FileNotFoundException;
	public InputStream readFile(F f) throws FileNotFoundException;
	public void deleteFile(F f);
	public String getInfo(F f);
	
}
