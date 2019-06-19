package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import liquibase.util.StreamUtil;
import nl.nn.adapterframework.util.LogUtil;

public abstract class FileSystemTestBase {
	protected Logger log = LogUtil.getLogger(this);
	
	protected boolean doTimingTests=false;

	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	public String FILE1 = "file1.txt";
	public String FILE2 = "file2.txt";
	public String DIR1 = "testDirectory";
	public String DIR2 = "testDirectory2";

	private long waitMillis = 0;
	
	
	/**
	 * Checks if a file with the specified name exists.
	 * @param folder to search in for the file, set to null for root folder. 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	protected abstract boolean _fileExists(String folder, String filename) throws Exception;
	
	/**
	 * Checks if a folder with the specified name exists.
	 * @param folderName
	 * @return
	 * @throws Exception
	 */
	protected abstract boolean _folderExists(String folderName) throws Exception;
	
	/**
	 * Deletes the file with the specified name
	 * @param folder 
	 * @param filename
	 * @throws Exception
	 */
	protected abstract void _deleteFile(String folder, String filename) throws Exception;
	
	/**
	 * Creates a file with the specified name and returns output stream 
	 * to be able to write that file.
	 * @param folder 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	protected abstract OutputStream _createFile(String folder, String filename) throws Exception;

	/**
	 * Returns an input stream of the file 
	 * @param folder 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	protected abstract InputStream _readFile(String folder, String filename) throws Exception;
	
	/**
	 * Creates a folder 
	 * @param filename
	 * @throws Exception
	 */
	protected abstract void _createFolder(String foldername) throws Exception;
	
	/**
	 * Deletes the folder 
	 * @param filename
	 * @throws Exception
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
		OutputStream out = _createFile(folder, filename);
		if (contents != null)
			out.write(contents.getBytes());
		out.close();
	}

	public String readFile(String folder, String filename) throws Exception {
		InputStream in = _readFile(folder, filename);
		String content = StreamUtil.getReaderContents(new InputStreamReader(in));
		in.close();
		return content;
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

	protected void equalsCheck(String content, String actual) {
		assertEquals(content, actual);
	}


	protected void existsCheck(String filename) throws Exception {
		assertTrue("Expected file [" + filename + "] to be present", _fileExists(filename));
	}


	public void setWaitMillis(long waitMillis) {
		this.waitMillis = waitMillis;
	}

}