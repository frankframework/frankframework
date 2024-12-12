package org.frankframework.filesystem;

import java.io.InputStream;

public interface IFileSystemTestHelper extends AutoCloseable {

	void setUp() throws Exception;
	void tearDown() throws Exception;

	@Override
	default void close() throws Exception {
		tearDown();
	}

	/**
	 * Checks if a file with the specified name exists.
	 */
	boolean _fileExists(String folder, String filename) throws Exception;

	/**
	 * Checks if a folder with the specified name exists.
	 */
	boolean _folderExists(String folderName) throws Exception;

	/**
	 * Deletes the file with the specified name in the folder
	 */
	void _deleteFile(String folder, String filename) throws Exception;

	/**
	 * Creates a file with the specified name and returns output stream
	 * to be able to write that file.
	 */
	String createFile(String folder, String filename, String contents) throws Exception;

	/**
	 * Returns an input stream of the file
	 */
	InputStream _readFile(String folder, String filename) throws Exception;

	/**
	 * Creates a folder
	 */
	void _createFolder(String foldername) throws Exception;

	/**
	 * Deletes the folder
	 */
	void _deleteFolder(String folderName) throws Exception;


}
