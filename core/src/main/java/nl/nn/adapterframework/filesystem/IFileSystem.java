package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.OutputStream;

public interface IFileSystem<F> extends IBasicFileSystem<F> {

	public OutputStream createFile(F f) throws FileSystemException, IOException;
	public OutputStream appendFile(F f) throws FileSystemException, IOException;
	/**
	 * Renames the file to a new name in the same folder.
	 */
	public void renameFile(F f, String newName) throws FileSystemException;

	public void createFolder(F f) throws FileSystemException;
	public void removeFolder(F f) throws FileSystemException;

}
