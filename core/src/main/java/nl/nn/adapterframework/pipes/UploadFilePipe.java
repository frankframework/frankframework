/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Uploads a zip file (inputstream in a sessionKey) and unzips it to a directory.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>name of the key in the <code>PipeLineSession</code> which contains the inputstream</td><td>file</td></tr>
 * <tr><td>{@link #setDirectory(String) directory}</td><td>base directory where files are unzipped to</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirectorySessionKey(String) directorySessionKey}</td><td>the session key that contains the base directory where files are unzipped to</td><td>destination</td></tr>
 * </table>
 * </p>
 * 
 * @author Peter Leeuwenburgh
 */

public class UploadFilePipe extends FixedForwardPipe {

	private String directory;
	protected String directorySessionKey = "destination";
	private String sessionKey = "file";

	public void configure() throws ConfigurationException {
		super.configure();
/*
		if (StringUtils.isEmpty(getDirectory())) {
			throw new ConfigurationException("no value specified for directory");
		} else {
			File dir = new File(getDirectory());
			if (!dir.isDirectory()) {
				throw new ConfigurationException("The value for directory ["
						+ getDirectory()
						+ "] is invalid. It is not a directory ");
			}
		}
*/
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
			throws PipeRunException {
		InputStream inputStream = (InputStream) session.get(getSessionKey());
		if (inputStream == null) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "got null value from session under key ["
					+ getSessionKey() + "]");
		}

		File dir;
		if (StringUtils.isNotEmpty(getDirectory())) {
			dir = new File(getDirectory());
		} else {
			if (StringUtils.isNotEmpty(getDirectorySessionKey())) {
				dir = new File((String) session.get(getDirectorySessionKey()));
			} else {
				dir = new File(input.toString());
			}
		}

		if (!dir.exists()) {
			if (dir.mkdirs()) {
				log.debug(getLogPrefix(session) + "created directory ["
						+ dir.getPath() + "]");
			} else {
				log.warn(getLogPrefix(session) + "directory [" + dir.getPath()
						+ "] could not be created");
			}
		}
		
		String fileName;
		try {
			fileName = (String) session.get("fileName");
			if (FileUtils.extensionEqualsIgnoreCase(fileName, "zip")) {
				FileUtils.unzipStream(inputStream, dir);
			} else {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "file extension ["
						+ FileUtils.getFileNameExtension(fileName)
						+ "] should be 'zip'");
			}
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ " Exception on uploading and unzipping/writing file", e);
		}

		return new PipeRunResult(getForward(), dir.getPath());
	}

	public void setDirectory(String string) {
		directory = string;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectorySessionKey(String string) {
		directorySessionKey = string;
	}

	public String getDirectorySessionKey() {
		return directorySessionKey;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(String string) {
		sessionKey = string;
	}
}