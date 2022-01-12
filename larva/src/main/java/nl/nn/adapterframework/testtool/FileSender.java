package nl.nn.adapterframework.testtool;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Dir2Xml;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.helper.ProjectHelperImpl;

/**
 * File sender for the Test Tool.
 * 
 * @author Jaco de Groot
 */
public class FileSender {
	private String filename;
	private String encoding = "UTF-8";
	private boolean checkDelete = true;
	private long timeOut = 3000;
	private long interval = 100;
	private boolean overwrite = false;
	private boolean deletePath = false;
	private boolean createPath = false;
	private boolean runAnt = false;
	
	/**
	 * Send the message to the specified file. After writing the message to
	 * file, this method will check if the file is deleted by another party
	 * (detect reading of the file).
	 * 
	 * @param message  the message to write to file
	 */
	public void sendMessage(String message) throws TimeOutException, SenderException {
		if (runAnt) {
			runAntScript();
		} else {
			if (deletePath) {
				File file = new File(filename);
				if (file.exists()) {
					recursiveDelete(filename);
				}
			} else {
				if (createPath) {
					File file = new File(filename);
					if (file.exists()) {
						throw new SenderException("Path '" + filename + "' already exists.");
					}
					file.mkdirs();
				} else {
					File file = new File(filename);
					if (!overwrite && file.exists()) {
						throw new SenderException("File '" + filename + "' already exists.");
					}
					String pathname = file.getParent();
					File path = new File(pathname);
					if (!path.exists()) {
						path.mkdirs();
					}
					try {
						FileOutputStream fileOutputStream = new FileOutputStream(file);
						fileOutputStream.write(message.getBytes(encoding));
						fileOutputStream.close();
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
						throw new TimeOutException("Time out waiting for deletion of file '" + filename + "'.");
					}
				}
			}
		}
	}

	public String getMessage() throws TimeOutException, SenderException {
		Dir2Xml dx=new Dir2Xml();
		dx.setPath(filename);
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
		helper.parse(ant, new File(filename));
		ant.executeTarget(ant.getDefaultTarget());
	}

	private boolean recursiveDelete(String path) {
		File file = new File(path);
		String[] dirList = file.list();
		for (int i = 0; i < dirList.length; i++) {
			String newPath = path + File.separator + dirList[i];
			File newFile = new File(newPath);
			if (newFile.isDirectory()) {
				recursiveDelete(newPath);
			} else {
				newFile.delete();
			}
		}
		return file.delete();
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
