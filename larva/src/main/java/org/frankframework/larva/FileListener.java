/*
   Copyright 2021-2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.larva;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.ListenerException;
import org.frankframework.core.TimeoutException;
import org.frankframework.util.FileUtils;
import org.frankframework.util.StreamUtil;

/**
 * File listener for the Test Tool.
 *
 * @author Jaco de Groot
 */
public class FileListener implements IConfigurable, AutoCloseable {
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter String name;

	private String filename;
	@Getter private String filename2;
	private String directory;
	private String wildcard;
	private long waitBeforeRead = -1;
	/**
	 * -- GETTER --
	 *  Get the timeout in milliseconds waiting for creation of the file.
	 */
	@Getter private long timeout = 3000;
	private long interval = 100;

	@Override
	public void configure() throws ConfigurationException {
		if (filename == null && directory == null) {
			throw new ConfigurationException("Could not find filename or directory property");
		} else if (directory != null && wildcard == null) {
			throw new ConfigurationException("Could not find wildcard property");
		}

		close();
	}

	@Override
	public void close() throws ConfigurationException {
		if (getFilename2() != null) {
			return;
		}

		long oldTimeOut = getTimeout();
		try {
			setTimeout(0);
			try {
				String message = getMessage();
				if (message != null) {
					throw new ConfigurationException("Found remaining message on fileListener ["+getName()+"]");
				}
			} catch(ListenerException e) {
				throw new ConfigurationException("Could read message from fileListener ["+getName()+"]: " + e.getMessage(), e);
			} catch (TimeoutException e) {
				//Simply means no message was found
			}
		} finally {
			setTimeout(oldTimeOut);
		}
	}

	/**
	 * Read the message from the specified file. If the file doesn't exist,
	 * this methods waits a specified time before it attempts to read the file.
	 *
	 * @return The message read from the specified file
	 */
	public String getMessage() throws TimeoutException, ListenerException {
		String result;
		if (waitBeforeRead != -1) {
			try {
				Thread.sleep(waitBeforeRead);
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
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
		if (file != null && filename2 != null) {
			try {
				File file2 = new File(filename2);
				boolean equal = isFileBinaryEqual(file, file2);
				result = Boolean.toString(equal);
			} catch (IOException e) {
				throw new ListenerException("Exception comparing files '"
						+ filename + "' and '" + filename2 + "': "
						+ e.getMessage(), e);
			}
		} else {
			long startTime = System.currentTimeMillis();
			while ((file == null || !file.exists()) && System.currentTimeMillis() < startTime + timeout) {
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
				StringBuilder stringBuilder = new StringBuilder();
				try (InputStream fileInputStream = new FileInputStream(file)) {
					byte[] buffer = new byte[StreamUtil.BUFFER_SIZE];
					int length = fileInputStream.read(buffer);
					while (length != -1) {
						stringBuilder.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
						length = fileInputStream.read(buffer);
					}
				} catch(IOException e) {
					throw new ListenerException("Exception reading file '" + file.getAbsolutePath() + "': " + e.getMessage(), e);
				}
				result = stringBuilder.toString();
				try {
					Files.delete(file.toPath());
				} catch (IOException e) {
					throw new ListenerException("Could not delete file '" + file.getAbsolutePath() + "'.", e);
				}
			} else {
				throw new TimeoutException("Time out waiting for file.");
			}
		}
		return result;
	}

	static boolean isFileBinaryEqual(File first, File second) throws IOException {
		if ((!first.exists()) || (!second.exists()) || (!first.isFile()) || (!second.isFile())) {
			return false;
		}
		if (first.length() != second.length()) {
			return false;
		}
		if (first.getCanonicalPath().equals(second.getCanonicalPath())) {
			return true;
		}

		try (InputStream bufFirstInput = new BufferedInputStream(new FileInputStream(first));
			 InputStream bufSecondInput = new BufferedInputStream(new FileInputStream(second))) {
			boolean retval;
			while (true) {
				int firstByte = bufFirstInput.read();
				int secondByte = bufSecondInput.read();
				if (firstByte != secondByte) {
					retval = false;
					break;
				}
				// End of file, must be end of both files.
				if (firstByte < 0) {
					retval = true;
					break;
				}
			}
			return retval;
		}
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
	 * Set the timeout in milliseconds waiting for creation of the file.
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Set the interval time in milliseconds between checks for creation of the
	 * file.
	 */
	public void setInterval(long interval) {
		this.interval = interval;
	}

}
