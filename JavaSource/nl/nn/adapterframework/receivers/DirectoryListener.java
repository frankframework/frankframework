/*
 * $Log: DirectoryListener.java,v $
 * Revision 1.17  2011-05-25 08:11:24  L190409
 * Added attribute minStableTime. Removed waiting loops. Waiting will now be performed by (and configured at) Receiver, where required.
 *
 * Revision 1.16  2009/01/14 10:54:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added fileTimeSensitive attribute
 *
 * Revision 1.15  2008/10/24 07:03:34  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * outputDirectory attribute is optional instead of obliged
 *
 * Revision 1.14  2008/08/13 13:41:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * prepare to react better on stop command
 *
 * Revision 1.13  2008/07/15 12:50:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added excludeWildcard attribute
 *
 * Revision 1.12  2008/04/03 07:18:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * now implements HasPhysicalDestination
 *
 * Revision 1.11  2008/02/28 16:24:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use PipeLineSession.setListenerParameters()
 *
 * Revision 1.10  2008/02/19 09:41:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.9  2008/02/15 13:59:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attributes processedDirectory, numberOfBackups, overwrite and delete
 *
 * Revision 1.8  2007/10/03 08:59:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 * Revision 1.7  2007/07/26 16:24:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.6  2007/07/10 15:18:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

/**
 * File {@link nl.nn.adapterframework.core.IPullingListener listener} that looks in a directory for files 
 * according to a <code>wildcard</code> and a <code>excludeWildcard</code>.  
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
 * <tr><td>{@link #setExcludeWildcard(String) excludeWildcard}</td><td>Filter of files to be excluded when looking in inputDirectory</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileTimeSensitive(boolean) fileTimeSensitive}</td><td>when <code>true</code>, the file modification time is used in addition to the filename to determine if a file has been seen before</td><td>false</td></tr>
 * <tr><td>{@link #setOutputDirectory(String) outputDirectory}</td><td>Directory where files are stored <i>while</i> being processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputFilenamePattern(String) outputFilenamePattern}</td><td>Pattern for the name using the MessageFormat.format method. Params: 0=inputfilename, 1=inputfile extension, 2=unique uuid, 3=current date</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProcessedDirectory(String) processedDirectory}</td><td>Directory where files are stored <i>after</i> being processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNumberOfBackups(int) numberOfBackups}</td><td>number of copies held of a file with the same name. Backup files have a dot and a number suffixed to their name. If set to 0, no backups will be kept.</td><td>5</td></tr>
 * <tr><td>{@link #setOverwrite(boolean) overwrite}</td><td>when set <code>true</code>, the destination file will be deleted if it already exists</td><td>false</td></tr>
 * <tr><td>{@link #setDelete(boolean) delete}</td><td>when set <code>true</code>, the file processed will deleted after being processed, and not stored</td><td>false</td></tr>
 * <tr><td>{@link #setMinStableTime(long) minStableTime}</td><td>minimal age of file in milliseconds, to avoid receiving a file while it is still being written</td><td>1000 [ms]</td></tr>
 * <tr><td>{@link #setPassWithoutDirectory(boolean) passWithoutDirectory}</td><td>pass the filename without the <code>outputDirectory</code> to the pipeline</td><td>false</td></tr>
 * <tr><td>{@link #setCreateInputDirectory(boolean) createInputDirectory}</td><td>when set to <code>true</code>, the directory to look for files is created if it does not exist</td><td>false</td></tr>
 * <tr><td>{@link #setResponseTime(long) responseTime}</td><td>Waittime to wait between polling. N.B. not used anymore. Please use pollInterval on the Receiver instead</td><td>10000 [ms]</td></tr>
 * <tr><td>{@link #setNumberOfAttempts(int) numberOfAttempts}</td><td>maximum number of move attempts before throwing an exception. N.B. not used anymore. Please use maxRetries on the Receiver instead</td><td>1</td></tr>
 * <tr><td>{@link #setWaitBeforeRetry(long) waitBeforeRetry}</td><td>time waited after unsuccesful try. N.B. not used anymore.</td><td>1000 [ms]</td></tr>
 * </table>
 * </p>
 *
 * @author  John Dekker
 * @version Id
 */
public class DirectoryListener implements IPullingListener, INamedObject, HasPhysicalDestination {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String inputDirectory;
	private String wildcard;
	private String excludeWildcard;
	private String outputDirectory;
	private String outputFilenamePattern;
	private long responseTime = 0;
	private boolean passWithoutDirectory = false;
	protected boolean createInputDirectory = false;

	private String processedDirectory;
	private int numberOfBackups = 0;
	private boolean overwrite = false;
	private boolean delete = false;
	private int numberOfAttempts = 1;
	private long waitBeforeRetry = 1000;
	private boolean fileTimeSensitive=false;

	private long minStableTime = 1000;
	
	/**
	 * Configure does some basic checks (directoryProcessedFiles is a directory,  inputDirectory is a directory, wildcard is filled etc.);
	 *
	 */
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getInputDirectory()))
			throw new ConfigurationException("no value specified for inputDirectory");
		if (StringUtils.isEmpty(getWildcard()))
			throw new ConfigurationException("no value specified for wildcard");
		if (StringUtils.isEmpty(getOutputDirectory())) {
			//throw new ConfigurationException("no value specified for outputDirectory");
			//TODO: instead of an outputDirectory a script to remove processed files is permitted
		} else {
			File dir = new File(getOutputDirectory());
			if (!dir.isDirectory()) {
				throw new ConfigurationException("The value for outputDirectory [" + getOutputDirectory() + "] is invalid. It is not a directory ");
			}
		}
		File inp = new File(getInputDirectory());
		if (!inp.exists() && createInputDirectory) {
			if (!inp.mkdirs()) {
				throw new ConfigurationException(getInputDirectory() + " could not be created");
			}
		}
		if (!inp.isDirectory()) {
			throw new ConfigurationException("The value for inputDirectory [" + getInputDirectory() + "] is invalid. It is not a directory.");
		}
		if (getResponseTime()>0) {
			String msg="The use of the attribute responseTime [" + getResponseTime() + "] is no longer used; Please set the attribute pollInterval on the receiver instead, which is specified in seconds instead of milliseconds";
			ConfigurationWarnings.getInstance().add(log,msg);
		}
		if (getNumberOfAttempts()>1) {
			String msg="The use of the attribute numberOfAttempts [" + getNumberOfAttempts() + "] is no longer used; Please set the attribute maxRetries on the receiver instead";
			ConfigurationWarnings.getInstance().add(log,msg);
		}
	}

	public void open() throws ListenerException {
	}

	public Map openThread() throws ListenerException {
		return null;
	}


	public void close() throws ListenerException {
	}

	public void closeThread(Map threadContext) throws ListenerException {
	}


	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map context) throws ListenerException {
		if (isDelete() || StringUtils.isNotEmpty(getProcessedDirectory())) {
			String filename=getStringFromRawMessage(rawMessage, context);
			File f=new File(filename);
			try {
				FileUtils.moveFileAfterProcessing(f, getProcessedDirectory(), isDelete(), isOverwrite(), getNumberOfBackups()); 
			} catch (Exception e) {
				throw new ListenerException("Could not move file ["+filename+"]",e);
			}
		}
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
			File rename2 = new File(getOutputDirectory(), FileUtils.getFilename(null, session, file, getOutputFilenamePattern()));
			newFilename = FileUtils.moveFile(file, rename2, isOverwrite(), getNumberOfBackups(), getNumberOfAttempts(), getWaitBeforeRetry());

			if (newFilename == null) {
				throw new ListenerException(getName() + " was unable to rename file [" + file.getAbsolutePath() + "] to [" + getOutputDirectory() + "]");
			}

			if (passWithoutDirectory) {
				File newFile = new File(newFilename);
				newFilename = newFile.getName();
			}
			return newFilename;
		}
		catch(Exception e) {
			throw new ListenerException(getName() + " was unable to rename file [" + file.getAbsolutePath() + "] to [" + getOutputDirectory() + "]", e);
		}
	}

	/**
	 * Returns a string of the rawMessage
	 */
	public String getStringFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
		return rawMessage.toString();
	}

	/**
	 * Returns the name of the file in process (the {@link #archiveFile(File) archived} file) concatenated with the
	 * record number. As te {@link #archiveFile(File) archivedFile} method always renames to a 
	 * unique file, the combination of this filename and the recordnumber is unique, enabling tracing in case of errors
	 * in the processing of the file.
	 * Override this method for your specific needs! 
	 */
	public String getIdFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
		String filename= rawMessage.toString();
		String correlationId=filename;
		if (isFileTimeSensitive()) {
			try {
				File f=new File(filename);
				correlationId+="-"+DateUtils.format(f.lastModified());
			} catch (Exception e) {
				throw new ListenerException("Could not get filetime from filename ["+filename+"]",e);
			}
		}
		PipeLineSession.setListenerParameters(threadContext, correlationId, correlationId, null, null);
		return correlationId;
	}
	/**
	 * Retrieves a single record from a file. If the file is empty or fully processed, it looks wether there
	 * is a new file to process and returns the first record.
	 */
	public synchronized Object getRawMessage(Map threadContext) throws ListenerException {
		File inputFile = FileUtils.getFirstMatchingFile(getInputDirectory(), getWildcard(), getExcludeWildcard(), getMinStableTime());
		if (inputFile == null) {
			return null;
		}

		String inputFileName=null;
		try {
			inputFileName = inputFile.getCanonicalPath();
		} catch (IOException e) {
			throw new ListenerException("Error while getting canonical path", e);
		}
//		String tsReceived=inputFile.lastModified()
		if (StringUtils.isNotEmpty(getOutputDirectory())) {
			String inprocessFile = archiveFile(getSession(threadContext), inputFile);
			if (inprocessFile == null) { // moving was unsuccessful, probably becausing writing was not finished
				return null;
			}
			return inprocessFile;
		} else {
			if (passWithoutDirectory) {
				return inputFile.getName();
			}
			return inputFileName;
		}
	}
	
	private PipeLineSession getSession(Map threadContext) {
		PipeLineSession session = new PipeLineSession();
		if(threadContext != null)
			session.putAll(threadContext);
		return session;
	}


	public String getPhysicalDestinationName() {
		return "wildcard pattern ["+getWildcard()+"] "+(getExcludeWildcard()==null?"":"excluding ["+getExcludeWildcard()+"] ")+"inputDirectory ["+ getInputDirectory()+"] outputDirectory ["+ getOutputDirectory()+"]";
	}

	
	
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
		ts.append("name", getName());
		ts.append("inputDirectory", getInputDirectory());
		ts.append("wildcard", getWildcard());
		ts.append("excludeWildcard", getExcludeWildcard());
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

	public void setExcludeWildcard(String excludeWildcard) {
		this.excludeWildcard = excludeWildcard;
	}

	public String getExcludeWildcard() {
		return excludeWildcard;
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
	 * @deprecated
	 */
	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}
	/**
	 * @deprecated
	 */
	public long getResponseTime() {
		return responseTime;
	}

	public void setMinStableTime(long minStableTime) {
		this.minStableTime = minStableTime;
	}
	public long getMinStableTime() {
		return minStableTime;
	}

	/**
	 * @deprecated
	 */
	public void setNumberOfAttempts(int i) {
		numberOfAttempts = i;
	}
	/**
	 * @deprecated
	 */
	public int getNumberOfAttempts() {
		return numberOfAttempts;
	}


	/**
	 * @deprecated
	 */
	public void setWaitBeforeRetry(long l) {
		waitBeforeRetry = l;
	}
	/**
	 * @deprecated
	 */
	public long getWaitBeforeRetry() {
		return waitBeforeRetry;
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

	public void setProcessedDirectory(String processedDirectory) {
		this.processedDirectory = processedDirectory;
	}
	public String getProcessedDirectory() {
		return processedDirectory;
	}
	
	public void setNumberOfBackups(int i) {
		numberOfBackups = i;
	}
	public int getNumberOfBackups() {
		return numberOfBackups;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}
	public boolean isOverwrite() {
		return overwrite;
	}

	public void setDelete(boolean b) {
		delete = b;
	}
	public boolean isDelete() {
		return delete;
	}

	public void setFileTimeSensitive(boolean b) {
		fileTimeSensitive = b;
	}
	public boolean isFileTimeSensitive() {
		return fileTimeSensitive;
	}

}
