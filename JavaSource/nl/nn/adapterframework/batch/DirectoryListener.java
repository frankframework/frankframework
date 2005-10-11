/*
 * $Log: DirectoryListener.java,v $
 * Revision 1.1  2005-10-11 13:00:21  europe\m00f531
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

/**
 * File {@link nl.nn.adapterframework.core.IPullingListener listener} that looks in a directory for files according to a wildcard. 
 * When a file is found, it is moved to an outputdirectory, so that it isn't found more then once.  
 * The name of the moved file is passed to the pipeline.  
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.DirectoryListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputDirectory(String) inputDirectory}</td><td>Directory to look for files</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWildcard(String) wildcard}</td><td>Filter of files to look for in inputDirectory</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputDirectory(String) outputDirectory}</td><td>Directory where to move the files to</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputFilenamePattern(String) outputFilenamePattern}</td><td>Pattern for the name using the MessageFormat.format method. Params: 0=inputfilename, 1=inputfile extension, 2=unique uuid, 3=current date</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setResponseTime(long) responseTime}</td><td>Waittime to wait between polling</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNumberOfAttempts(int) numberOfAttempts}</td><td>maximum number of move attempts before throwing an exception</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWaitBeforeRetry(long) waitBeforeRetry}</td><td>Number of move attempts</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 *
 * @version Id
 * @author  John Dekker
 */
public class DirectoryListener implements IPullingListener, INamedObject {
	public static final String version = "$RCSfile: DirectoryListener.java,v $  $Revision: 1.1 $ $Date: 2005-10-11 13:00:21 $";

	protected Logger log = Logger.getLogger(this.getClass());
	private String name;
	private String inputDirectory;
	private String inputFileName;
	private String outputFilenamePattern;
	private String outputDirectory;
	private String wildcard;
	private long responseTime = 10000;
	private long waitBeforeRetry = 1000;
	private int numberOfAttempts = 10;

	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, HashMap context) throws ListenerException {
	}

	/**
	 * Moves a file to another directory and places a UUID in the name.
	 * @return String with the name of the (renamed and moved) file
	 * 
	 */
	protected String archiveFile(File file) throws ListenerException {
		// Move file to new directory
		String newFilename = null;
		
		try {
			File rename2 = new File(getOutputDirectory(), FileUtils.getFilename(file, outputFilenamePattern, true));
			newFilename = FileUtils.moveFile(file, rename2, numberOfAttempts, waitBeforeRetry);

			if (newFilename == null) {
				throw new ListenerException(getName() + " was unable to rename file [" + file.getAbsolutePath() + "] to [" + outputDirectory + "]");
			}
			return newFilename;
		}
		catch(Exception e) {
			throw new ListenerException(getName() + " was unable to rename file [" + file.getAbsolutePath() + "] to [" + outputDirectory + "]", e);
		}
	}

	public void close() throws ListenerException {
	}

	public void closeThread(HashMap threadContext) throws ListenerException {

	}
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
		if (!inp.isDirectory()) {
			throw new ConfigurationException("The value for [inputDirectory] :[ " + getInputDirectory() + "] is invalid. It is not a directory ");

		}
	}
	/**
	 * Returns the directory in whiche processed files are stored.
	 * @return String
	 */
	public String getOutputDirectory() {
		return outputDirectory;
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
	public String getInputDirectory() {
		return inputDirectory;
	}
	public String getName() {
		return name;
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
			throw new ListenerException("Error while gettin canonical path", e);
		}
		String inprocessFile = archiveFile(inputFile);
		if (inprocessFile == null) { // moving was unsuccessful, probably becausing writing was not finished
			return waitAWhile();
		}
		return inprocessFile;
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

	public long getResponseTime() {
		return responseTime;
	}
	
	/**
	 * Returns a string of the rawMessage
	 */
	public String getStringFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
		return rawMessage.toString();
	}
	/**
	* get the {@link nl.nn.adapterframework.util.WildCardFilter wildcard}  to look for files in the specifiek directory, e.g. "*.inp"
	*/
	public String getWildcard() {
		return wildcard;
	}

	public void open() throws ListenerException {
	}

	public HashMap openThread() throws ListenerException {

		return null;
	}
	/**
	 * Sets the directory to store processed files in
	 * @param directoryProcessedFiles The directoryProcessedFiles to set
	 */
	public void setOutputDirectory(String inprocessDirectory) {
		this.outputDirectory = inprocessDirectory;
	}
	/**
	 * set the directory name to look for files in.
	 * @see #setWildcard(String)
	 */
	public void setInputDirectory(String inputDirectory) {
		this.inputDirectory = inputDirectory;
	}
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * set the time to delay when no records are to be processed and this class has to look for the arrival of a new file
	 */
	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}
	/**
	 * set the {@link nl.nn.adapterframework.util.WildCardFilter wildcard}  to look for files in the specifiek directory, e.g. "*.inp"
	 */
	public void setWildcard(String wildcard) {
		this.wildcard = wildcard;
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
	public String getOutputFilenamePattern() {
		return outputFilenamePattern;
	}

	public void setOutputFilenamePattern(String string) {
		outputFilenamePattern = string;
	}

	public int getNumberOfAttempts() {
		return numberOfAttempts;
	}

	public long getWaitBeforeRetry() {
		return waitBeforeRetry;
	}

	public void setNumberOfAttempts(int i) {
		numberOfAttempts = i;
	}

	public void setWaitBeforeRetry(long l) {
		waitBeforeRetry = l;
	}

}
