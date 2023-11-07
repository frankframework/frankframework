package nl.nn.adapterframework.filesystem;

import java.io.InputStream;
import java.io.OutputStream;

public interface IFileSystemTestHelper {

	public void setUp() throws Exception;
	public void tearDown() throws Exception;

		/**
	 * Checks if a file with the specified name exists.
	 */
	public boolean _fileExists(String folder, String filename) throws Exception;

	/**
	 * Checks if a folder with the specified name exists.
	 */
	public boolean _folderExists(String folderName) throws Exception;

	/**
	 * Deletes the file with the specified name in the folder
	 */
	public void _deleteFile(String folder, String filename) throws Exception;

	/**
	 * Creates a file with the specified name and returns output stream 
	 * to be able to write that file.
	 */
	public OutputStream _createFile(String folder, String filename) throws Exception;

	/**
	 * Returns an input stream of the file 
	 */
	public InputStream _readFile(String folder, String filename) throws Exception;

	/**
	 * Creates a folder 
	 */
	public void _createFolder(String foldername) throws Exception;

	/**
	 * Deletes the folder 
	 */
	public void _deleteFolder(String folderName) throws Exception;


}
