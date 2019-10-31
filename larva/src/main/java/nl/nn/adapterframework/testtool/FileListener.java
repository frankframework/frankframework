package nl.nn.adapterframework.testtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.util.FileUtils;

/**
 * File listener for the Test Tool.
 * 
 * @author Jaco de Groot
 */
public class FileListener {
	private String filename;
	private String filename2;
	private String directory;
	private String wildcard;
	private long waitBeforeRead = -1;
	private long timeOut = 3000;
	private long interval = 100;

	/**
	 * Read the message from the specified file. If the file doesn't exist,
	 * this methods waits a specified time before it attempts to read the file.
	 * 
	 * @return The message read from the specified file
	 */
	public String getMessage() throws TimeOutException, ListenerException {
		String result = null;
		if (waitBeforeRead != -1) {
			try {
				Thread.sleep(waitBeforeRead);
			} catch(InterruptedException e) {
				throw new ListenerException("Exception waiting before reading the file: " + e.getMessage(), e);
			}
		}
		File file = null;
		if (filename == null) {
			File[] files = FileUtils.getFiles(directory, wildcard, null, 0);
			if (files.length > 0) {
				file = files[0];
			}
		} else {
			file = new File(filename);
		}
		if (filename2 != null) {
			try {
				File file2 = new File(filename2);
				boolean equal = FileUtils.isFileBinaryEqual(file, file2);
				result = Boolean.toString(equal);
			} catch (IOException e) {
				throw new ListenerException("Exception comparing files '"
						+ filename + "' and '" + filename2 + "': "
						+ e.getMessage(), e);
			}
		} else {
			long startTime = System.currentTimeMillis();
			while ((file == null || !file.exists()) && System.currentTimeMillis() < startTime + timeOut) {
				try {
					Thread.sleep(interval);
				} catch(InterruptedException e) {
					throw new ListenerException("Exception waiting for file: " + e.getMessage(), e);
				}
				if (filename == null) {
					File[] files = FileUtils.getFiles(directory, wildcard, null, 0);
					if (files.length > 0) {
						file = files[0];
					}
				}
			}
			if (file != null && file.exists()) {
				StringBuffer stringBuffer = new StringBuffer();
				FileInputStream fileInputStream = null;
				try {
					fileInputStream = new FileInputStream(file);
				} catch(IOException e) {
					throw new ListenerException("Exception opening file '" + file.getAbsolutePath() + "': " + e.getMessage(), e);
				}
				byte[] buffer = new byte[1024];
				try {
					int length = fileInputStream.read(buffer);
					while (length != -1) {
						stringBuffer.append(new String(buffer, 0, length, "UTF-8"));
						length = fileInputStream.read(buffer);
					}
				} catch(IOException e) {
					try {
						fileInputStream.close();
					} catch(Exception e2) {
					}
					throw new ListenerException("Exception reading file '" + file.getAbsolutePath() + "': " + e.getMessage(), e);
				}
				try {
					fileInputStream.close();
				} catch(IOException e) {
					throw new ListenerException("Exception closing file '" + file.getAbsolutePath() + "': " + e.getMessage(), e);
				}
				result = stringBuffer.toString();
				if (!file.delete()) {
					throw new ListenerException("Could not delete file '" + file.getAbsolutePath() + "'.");
				}
			} else {
				throw new TimeOutException("Time out waiting for file.");
			}
		}
		return result;
	}

	/**
	 * Set the filename of the file to read the message from.
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * When used, filename and filename2 are binary compared (returns 'true' or
	 * 'false' instead of the file content).
	 */
	public void setFilename2(String filename2) {
		this.filename2 = filename2;
	}

	public String getFilename2() {
		return filename2;
	}

	/**
	 * Set the directory to read the file from.
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	/**
	 * Set the wildcard to find the file to read the message from.
	 * 
	 * @param wildcard to search for files in a directory
	 */
	public void setWildcard(String wildcard) {
		this.wildcard = wildcard;
	}

	/**
	 * Set the time to wait in milliseconds before starting to read the file.
	 * Set to -1 (default) to start reading the file directly.  
	 */
	public void setWaitBeforeRead(long waitBeforeRead) {
		this.waitBeforeRead = waitBeforeRead;
	}

	/**
	 * Set the time out in milliseconds waiting for creation of the file.
	 */
	public void setTimeOut(long timeOut) {
		this.timeOut = timeOut;
	}

	/**
	 * Get the time out in milliseconds waiting for creation of the file.
	 */
	public long getTimeOut() {
		return timeOut;
	}

	/**
	 * Set the interval time in milliseconds between checks for creation of the
	 * file.
	 */
	public void setInterval(long interval) {
		this.interval = interval;
	}

}
