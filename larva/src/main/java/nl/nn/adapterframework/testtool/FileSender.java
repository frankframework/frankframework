/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.testtool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.helper.ProjectHelperImpl;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Dir2Xml;

/**
 * File sender for the Test Tool.
 * 
 * @author Jaco de Groot
 */
public class FileSender implements IConfigurable {
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter String name;

	private String filename;
	private File file;
	private String encoding = "UTF-8";
	private boolean checkDelete = true;
	private long timeOut = 3000;
	private long interval = 100;
	private boolean overwrite = false;
	private boolean deletePath = false;
	private boolean createPath = false;
	private boolean runAnt = false;

	@Override
	public void configure() throws ConfigurationException {
		String scenarioDirectory = null;
		try {
			URL scenarioDirectoryURL = ClassUtils.getResourceURL(this, ".");
			scenarioDirectory = new File(scenarioDirectoryURL.toURI()).getAbsolutePath();
		} catch (URISyntaxException e) {
			throw new ConfigurationException("Could not find scenario root directory", e);
		}
		String absPath = TestTool.getAbsolutePath(scenarioDirectory, filename);
		file = new File(absPath);
	}

	/**
	 * Send the message to the specified file. After writing the message to
	 * file, this method will check if the file is deleted by another party
	 * (detect reading of the file).
	 * 
	 * @param message  the message to write to file
	 */
	public void sendMessage(String message) throws TimeoutException, SenderException {
		if (runAnt) {
			runAntScript();
		} else {
			if (deletePath) {
				if (file.exists()) {
					recursiveDelete(file);
				}
			} else {
				if (createPath) {
					if (file.exists()) {
						throw new SenderException("Path '" + file + "' already exists.");
					}
					file.mkdirs();
				} else {
					if (!overwrite && file.exists()) {
						throw new SenderException("File '" + file + "' already exists.");
					}
					String pathname = file.getParent();
					File path = new File(pathname);
					if (!path.exists()) {
						path.mkdirs();
					}

					try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
						fileOutputStream.write(message.getBytes(encoding));
					} catch(Exception e) {
						throw new SenderException("Exception writing file '" + filename + "': " + e.getMessage(), e);
					}
					long startTime = System.currentTimeMillis();
					while (checkDelete && file.exists() && System.currentTimeMillis() < startTime + timeOut) {
						try {
							Thread.sleep(interval);
						} catch(InterruptedException e) {
							throw new SenderException("Exception waiting for deletion of file '" + filename + "': " + e.getMessage(), e);
						}
					}
					if (checkDelete && file.exists()) {
						throw new TimeoutException("Time out waiting for deletion of file '" + filename + "'.");
					}
				}
			}
		}
	}

	public String getMessage() throws IOException {
		Dir2Xml dx=new Dir2Xml();
		dx.setPath(file.getAbsolutePath());
		return dx.getRecursiveDirList();
	}

	private void runAntScript() {
		Project ant = new Project();
		DefaultLogger consoleLogger = new DefaultLogger();
		consoleLogger.setErrorPrintStream(System.err);
		consoleLogger.setOutputPrintStream(System.out);
		consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
		ant.addBuildListener(consoleLogger);

		// iterate over appConstants and add them as properties
		AppConstants appConstants = AppConstants.getInstance();
		@SuppressWarnings("unchecked")
		Enumeration<String> enums = (Enumeration<String>) appConstants.propertyNames();
		while (enums.hasMoreElements()) {
			String key = enums.nextElement();
			ant.setProperty(key, appConstants.getResolvedProperty(key));
		}

		ant.init();
		ProjectHelper helper = new ProjectHelperImpl();
		helper.parse(ant, file);
		ant.executeTarget(ant.getDefaultTarget());
	}

	private boolean recursiveDelete(File path) {
		String[] dirList = path.list();
		for (int i = 0; i < dirList.length; i++) {
			File newFile = new File(path, dirList[i]);
			if (newFile.isDirectory()) {
				recursiveDelete(path);
			} else {
				newFile.delete();
			}
		}
		return path.delete();
	}

	/**
	 * Set the filename to write the message to.
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Set the encoding to use when writing the file.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Set check delete.
	 */
	public void setCheckDelete(boolean checkDelete) {
		this.checkDelete = checkDelete;
	}

	/**
	 * Set the time out in milliseconds waiting for deletion of the file.
	 */
	public void setTimeOut(long timeOut) {
		this.timeOut = timeOut;
	}

	/**
	 * Set the interval time in milliseconds between checks for file deletion.
	 */
	public void setInterval(long interval) {
		this.interval = interval;
	}

	/**
	 * Set the overwrite file.
	 */
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public void setDeletePath(boolean deletePath) {
		this.deletePath = deletePath;
	}
	
	public void setCreatePath(boolean createPath) {
		this.createPath = createPath;
	}

	public void setRunAnt(boolean runAnt) {
		this.runAnt = runAnt;
	}

}
