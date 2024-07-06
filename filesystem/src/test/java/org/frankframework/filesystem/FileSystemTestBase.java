package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;

import org.frankframework.core.ConfiguredTestBase;
import org.frankframework.stream.Message;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;

public abstract class FileSystemTestBase extends ConfiguredTestBase {

	protected boolean doTimingTests=false;

	public static final String FILE1 = "file1.txt";
	public static final String FILE2 = "file2.txt";
	public static final String DIR1 = "testDirectory";
	public static final String DIR2 = "testDirectory2";

	private long waitMillis = 0;


	/**
	 * Checks if a file with the specified name exists.
	 * @param folder to search in for the file, set to null for root folder.
	 * @param filename
	 */
	protected abstract boolean _fileExists(String folder, String filename) throws Exception;

	/**
	 * Checks if a folder with the specified name exists.
	 */
	protected abstract boolean _folderExists(String folderName) throws Exception;

	/**
	 * Deletes the file with the specified name
	 */
	protected abstract void _deleteFile(String folder, String filename) throws Exception;

	/**
	 * Creates a file with the specified name and returns output stream
	 */
	protected abstract OutputStream _createFile(String folder, String filename) throws Exception;

	/**
	 * Returns an input stream of the file
	 */
	protected abstract InputStream _readFile(String folder, String filename) throws Exception;

	/**
	 * Creates a folder
	 */
	protected abstract void _createFolder(String foldername) throws Exception;

	/**
	 * Deletes the folder
	 */
	protected abstract void _deleteFolder(String folderName) throws Exception;

	protected boolean _fileExists(String filename) throws Exception {
		return _fileExists(null, filename);
	}


	public void deleteFile(String folder, String filename) throws Exception {
		if (_fileExists(folder,filename)) {
			_deleteFile(folder, filename);
		}
	}

	public void createFile(String folder, String filename, String contents) throws Exception {
		try (OutputStream out = _createFile(folder, filename)) {
			if (contents != null) {
				out.write(contents.getBytes());
			}
		}
	}

	public String readFile(String folder, String filename) throws Exception {
		try (InputStream in = _readFile(folder, filename)) {
			return StreamUtil.streamToString(in);
		}
	}

	/**
	 * Pause current thread. Since creating an object takes a bit time
	 * this method can be used to make sure object is created in the server.
	 * Added for Amazon S3 sender.
	 * @throws FileSystemException
	 */
	public void waitForActionToFinish() throws FileSystemException {
		try {
			Thread.sleep(waitMillis);
		} catch (InterruptedException e) {
			throw new FileSystemException("Cannot pause the thread. Be aware that there may be timing issue to check files on the server.", e);
		}
	}

	protected void existsCheck(String filename) throws Exception {
		assertTrue(_fileExists(filename), "Expected file [" + filename + "] to be present");
	}

	protected void assertFileExistsWithContents(String folder, String filename, String contents) throws Exception {
		assertTrue(_fileExists(folder, filename),"file ["+filename+"] does not exist in folder ["+folder+"]");
		String actualContents = readFile(folder, filename);
		assertEquals(contents, actualContents==null?null:actualContents.trim(), filename);
	}

	protected void assertFileDoesNotExist(String folder, String filename) throws Exception {
		assertFalse(_fileExists(folder, filename), filename+" should not exist");
	}

	protected void assertFileCountEquals(Message result, int expectedFileCount) throws Exception {
		TransformerPool tp = TransformerPool.getXPathTransformerPool(null, "count(*/file[@type='file'])", OutputType.TEXT, false, null);
		int resultCount = Integer.parseInt(tp.transform(result, null, false));
		assertEquals(expectedFileCount, resultCount, "file count mismatch");
	}

	protected void assertFolderCountEquals(Message result, int expectedFolderCount) throws Exception {
		TransformerPool tp = TransformerPool.getXPathTransformerPool(null, "count(*/file[@type='folder']) ", OutputType.TEXT, false, null);
		int resultCount = Integer.parseInt(tp.transform(result, null, false));
		assertEquals(expectedFolderCount, resultCount, "folder count mismatch");
	}

	public void setWaitMillis(long waitMillis) {
		this.waitMillis = waitMillis;
	}

}
