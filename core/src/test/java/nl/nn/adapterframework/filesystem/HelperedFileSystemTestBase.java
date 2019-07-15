package nl.nn.adapterframework.filesystem;

import java.io.InputStream;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Before;

public abstract class HelperedFileSystemTestBase extends FileSystemTestBase {

	protected IFileSystemTestHelper helper;
	
	/**
	 * Returns the file system 
	 * @return fileSystem
	 * @throws ConfigurationException
	 */
	protected abstract IFileSystemTestHelper getFileSystemTestHelper();

	/**
	 * Checks if a file with the specified name exists.
	 * @param folder to search in for the file, set to null for root folder. 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	@Override
	protected boolean _fileExists(String folder, String filename) throws Exception {
		return helper._fileExists(folder,filename);
	}
	
	/**
	 * Checks if a folder with the specified name exists.
	 * @param folderName
	 * @return
	 * @throws Exception
	 */
	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		return helper._folderExists(folderName);
	}
	
	/**
	 * Deletes the file with the specified name
	 * @param folder 
	 * @param filename
	 * @throws Exception
	 */
	@Override
	protected void _deleteFile(String folder, String filename) throws Exception {
		helper._deleteFile(folder, filename);
	}
	
	/**
	 * Creates a file with the specified name and returns output stream 
	 * to be able to write that file.
	 * @param folder 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	@Override
	protected OutputStream _createFile(String folder, String filename) throws Exception {
		return helper._createFile(folder, filename);
	}

	/**
	 * Returns an input stream of the file 
	 * @param folder 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	@Override
	protected InputStream _readFile(String folder, String filename) throws Exception {
		return helper._readFile(folder, filename);
	}
	
	/**
	 * Creates a folder 
	 * @param filename
	 * @throws Exception
	 */
	@Override
	protected void _createFolder(String foldername) throws Exception {
		helper._createFolder(foldername);
	}

	/**
	 * Deletes the folder 
	 * @param filename
	 * @throws Exception
	 */
	@Override
	protected void _deleteFolder(String folderName) throws Exception {
		helper._deleteFolder(folderName);
	}

	@Before
	public void setUp() throws Exception {
		helper = getFileSystemTestHelper();
		helper.setUp();
	}
	
	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

}
