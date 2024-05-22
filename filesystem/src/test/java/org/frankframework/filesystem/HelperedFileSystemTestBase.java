package org.frankframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.frankframework.util.CloseUtils;

public abstract class HelperedFileSystemTestBase extends FileSystemTestBase {

	protected IFileSystemTestHelper helper;

	protected abstract IFileSystemTestHelper getFileSystemTestHelper() throws IOException;

	/**
	 * Checks if a file with the specified name exists.
	 * @param folder to search in for the file, set to null for root folder.
	 * @param filename
	 */
	@Override
	protected boolean _fileExists(String folder, String filename) throws Exception {
		return helper._fileExists(folder,filename);
	}

	/**
	 * Checks if a folder with the specified name exists.
	 */
	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		return helper._folderExists(folderName);
	}

	/**
	 * Deletes the file with the specified name.
	 */
	@Override
	protected void _deleteFile(String folder, String filename) throws Exception {
		helper._deleteFile(folder, filename);
	}

	/**
	 * Creates a file with the specified name and returns output stream to be able to write that file.
	 */
	@Override
	protected OutputStream _createFile(String folder, String filename) throws Exception {
		return helper._createFile(folder, filename);
	}

	/**
	 * Returns an input stream of the file.
	 */
	@Override
	protected InputStream _readFile(String folder, String filename) throws Exception {
		return helper._readFile(folder, filename);
	}

	/**
	 * Creates a folder.
	 */
	@Override
	protected void _createFolder(String foldername) throws Exception {
		helper._createFolder(foldername);
	}

	/**
	 * Deletes the folder.
	 */
	@Override
	protected void _deleteFolder(String folderName) throws Exception {
		helper._deleteFolder(folderName);
	}

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		helper = getFileSystemTestHelper();
		helper.setUp();
		super.setUp();
	}

	@AfterEach
	@Override
	public void tearDown() {
		CloseUtils.closeSilently(helper);
		super.tearDown();
	}

}
