/*
 * $Log: DirectoryListener.java,v $
 * Revision 1.6  2007-07-10 15:18:40  europe\L190409
 * fixed typo
 *
 * Revision 1.5  2007/05/21 12:21:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added createInputDirectory attribute
 *
 * Revision 1.3  2007/02/12 14:03:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.2  2006/09/25 13:21:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic reorganization of code
 *
 * Revision 1.1  2006/08/23 11:35:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved DirectoryListener from batch to receivers package
 *
 * Revision 1.6  2006/08/22 12:48:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added overwrite-attribute
 *
 * Revision 1.5  2006/05/19 09:28:37  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.3  2006/01/05 13:51:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.2  2005/10/24 09:59:22  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.1  2005/10/11 13:00:21  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.receivers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import nl.nn.adapterframework.util.LogUtil;

/**
 * File {@link nl.nn.adapterframework.core.IPullingListener listener} that looks in a directory for files according to a wildcard. 
 * When a file is found, it is moved to an outputdirectory, so that it isn't found more then once.  
 * The name of the moved file is passed to the pipeline.  
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.batch.DirectoryListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputDirectory(String) inputDirectory}</td><td>Directory to look for files</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWildcard(String) wildcard}</td><td>Filter of files to look for in inputDirectory</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputDirectory(String) outputDirectory}</td><td>Directory where to move the files to</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputFilenamePattern(String) outputFilenamePattern}</td><td>Pattern for the name using the MessageFormat.format method. Params: 0=inputfilename, 1=inputfile extension, 2=unique uuid, 3=current date</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setResponseTime(long) responseTime}</td><td>Waittime to wait between polling</td><td>10000 [ms]</td></tr>
 * <tr><td>{@link #setNumberOfAttempts(int) numberOfAttempts}</td><td>maximum number of move attempts before throwing an exception</td><td>10</td></tr>
 * <tr><td>{@link #setWaitBeforeRetry(long) waitBeforeRetry}</td><td>time waited after unsuccesful try</td><td>1000</td></tr>
 * <tr><td>{@link #setOverwrite(boolean) overwrite}</td><td>overwrite the file in the output directory if it already exist</td><td>false</td></tr>
 * <tr><td>{@link #setPassWithoutDirectory(boolean) passWithoutDirectory}</td><td>pass the filename without the <code>outputDirectory</code> to the pipeline</td><td>false</td></tr>
 * <tr><td>{@link #setCreateInputDirectory(boolean) createInputDirectory}</td><td>when set to <code>true</code>, the directory to look for files is created if it does not exist</td><td>false</td></tr>
 *  * </table>
 * </p>
 *
 * @version Id
 * @author  John Dekker
 */
public class DirectoryListener implements IPullingListener, INamedObject {
	public static final String version = "$RCSfile: DirectoryListener.java,v $  $Revision: 1.6 $ $Date: 2007-07-10 15:18:40 $";
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String inputDirectory;
	private String wildcard;
	private String outputDirectory;
	private String outputFilenamePattern;
	private long responseTime = 10000;
	private int numberOfAttempts = 10;
	private long waitBeforeRetry = 1000;
	private boolean overwrite = false;
	private boolean passWithoutDirectory = false;
	protected boolean createInputDirectory = false;

	//TODO: check if this is thread-safe
	private String inputFileName=null;
	
	/**
	 * Configure does some basic checks (directoryProcessedFiles is a directory,  inputDirectory is a directory, wildcard is filled etc.);
	 *
	 */
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getInputDirectory()))
			throw new ConfigurationException("no value specified for [inputDirectory]");
		if (StringUtils.isEmpty(getWildcard()))
			throw new ConfigurationException("no value specified for [wildcard]");
		if (StringUtils.isEmpty(getOutputDirectory()))
			throw new ConfigurationException("no value specified for [inprocessDirectory]");
		File dir = new File(getOutputDirectory());
		if (!dir.isDirectory()) {
			throw new ConfigurationException("The value for [directoryProcessedFiles] :[ " + getOutputDirectory() + "] is invalid. It is not a directory ");
		}
		File inp = new File(getInputDirectory());
		if (!inp.exists() && createInputDirectory) {
			if (!inp.mkdirs()) {
				throw new ConfigurationException(getInputDirectory() + " could not be created");
			}
		}
		if (!inp.isDirectory()) {
			throw new ConfigurationException("The value for [inputDirectory] :[ " + getInputDirectory() + "] is invalid. It is not a directory ");

		}
	}

	public void open() throws ListenerException {
	}

	public HashMap openThread() throws ListenerException {
		return null;
	}


	public void close() throws ListenerException {
	}

	public void closeThread(HashMap threadContext) throws ListenerException {
	}


	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, HashMap context) throws ListenerException {
	}

	/**
	 * Moves a file to another directory and places a UUID in the name.
	 * @return String with the name of the (renamed and moved) file
	 * 
	 */
	protected String archiveFile(PipeLineSession session, File file) throws ListenerException {
		// Move file to new directory
		String newFilename = null;
		
		try {
			File rename2 = new File(getOutputDirectory(), FileUtils.getFilename(null, session, file, outputFilenamePattern));
			if (overwrite) {
				if (rename2.exists()) {
					log.debug("File exist: " + rename2);
					if (!rename2.delete()) {
						log.error("Could not delete file: " + rename2);
					}
				}
			}
			newFilename = FileUtils.moveFile(file, rename2, numberOfAttempts, waitBeforeRetry);

			if (newFilename == null) {
				throw new ListenerException(getName() + " was unable to rename file [" + file.getAbsolutePath() + "] to [" + outputDirectory + "]");
			}

			if (passWithoutDirectory) {
				File newFile = new File(newFilename);
				newFilename = newFile.getName();
			}
			return newFilename;
		}
		catch(Exception e) {
			throw new ListenerException(getName() + " was unable to rename file [" + file.getAbsolutePath() + "] to [" + outputDirectory + "]", e);
		}
	}

	/**
	 * Returns a string of the rawMessage
	 */
	public String getStringFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
		return rawMessage.toString();
	}

	/**
	 * Returns the name of the file in process (the {@link #archiveFile(File) archived} file) concatenated with the
	 * record number. As te {@link #archiveFile(File) archivedFile} method always renames to a 
	 * unique file, the combination of this filename and the recordnumber is unique, enabling tracing in case of errors
	 * in the processing of the file.
	 * Override this method for your specific needs! 
	 */
	public String getIdFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
		String correlationId = inputFileName;
		threadContext.put("cid", correlationId);
		return correlationId;
	}
	/**
	 * Retrieves a single record from a file. If the file is empty or fully processed, it looks wether there
	 * is a new file to process and returns the first record.
	 */
	public synchronized Object getRawMessage(HashMap threadContext) throws ListenerException {
		File inputFile = FileUtils.getFirstMatchingFile(inputDirectory, wildcard);
		if (inputFile == null) {
			return waitAWhile();
		}
		
		try {
			inputFileName = inputFile.getCanonicalPath();
		}
		catch (IOException e) {
			throw new ListenerException("Error while getting canonical path", e);
		}
		String inprocessFile = archiveFile(getSession(threadContext), inputFile);
		if (inprocessFile == null) { // moving was unsuccessful, probably becausing writing was not finished
			return waitAWhile();
		}
		return inprocessFile;
	}
	
	private PipeLineSession getSession(HashMap threadContext) {
		PipeLineSession session = new PipeLineSession();
		if(threadContext != null)
			session.putAll(threadContext);
		return session;
	}

	private Object waitAWhile() throws ListenerException {
		try {
			Thread.sleep(responseTime);
			return null;
		}
		catch(InterruptedException e) {		
			throw new ListenerException("Interrupted while listening", e);
		}
		
	}

	
	
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
		ts.append("name", getName());
		ts.append("inputDirectory", getInputDirectory());
		ts.append("wildcard", getWildcard());
		result += ts.toString();
		return result;

	}


	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}


	/**
	 * set the directory name to look for files in.
	 * @see #setWildcard(String)
	 */
	public void setInputDirectory(String inputDirectory) {
		this.inputDirectory = inputDirectory;
	}
	public String getInputDirectory() {
		return inputDirectory;
	}


	/**
	 * set the {@link nl.nn.adapterframework.util.WildCardFilter wildcard}  to look for files in the specifiek directory, e.g. "*.inp"
	 */
	public void setWildcard(String wildcard) {
		this.wildcard = wildcard;
	}
	/**
	* get the {@link nl.nn.adapterframework.util.WildCardFilter wildcard}  to look for files in the specifiek directory, e.g. "*.inp"
	*/
	public String getWildcard() {
		return wildcard;
	}


	/**
	 * Sets the directory to store processed files in
	 * @param directoryProcessedFiles The directoryProcessedFiles to set
	 */
	public void setOutputDirectory(String inprocessDirectory) {
		this.outputDirectory = inprocessDirectory;
	}
	/**
	 * Returns the directory in whiche processed files are stored.
	 * @return String
	 */
	public String getOutputDirectory() {
		return outputDirectory;
	}


	public void setOutputFilenamePattern(String string) {
		outputFilenamePattern = string;
	}
	public String getOutputFilenamePattern() {
		return outputFilenamePattern;
	}


	/**
	 * set the time to delay when no records are to be processed and this class has to look for the arrival of a new file
	 */
	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}
	public long getResponseTime() {
		return responseTime;
	}


	public void setNumberOfAttempts(int i) {
		numberOfAttempts = i;
	}
	public int getNumberOfAttempts() {
		return numberOfAttempts;
	}


	public void setWaitBeforeRetry(long l) {
		waitBeforeRetry = l;
	}
	public long getWaitBeforeRetry() {
		return waitBeforeRetry;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public void setPassWithoutDirectory(boolean b) {
		passWithoutDirectory = b;
	}

	public boolean isPassWithoutDirectory() {
		return passWithoutDirectory;
	}

	public void setCreateInputDirectory(boolean b) {
		createInputDirectory = b;
	}

	public boolean isCreateInputDirectory() {
		return createInputDirectory;
	}
}
