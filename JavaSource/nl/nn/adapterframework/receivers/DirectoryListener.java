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
package nl.nn.adapterframework.receivers;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
 * <tr><td>{@link #setFileList(int) fileList}</td><td>When set a list if files in xml format (&lt;files&gt;&lt;file&gt;/file/name&lt;/file&gt;&lt;file&gt;/another/file/name&lt;/file&gt;&lt;/files&gt;) is passed to the pipleline instead of 1 file name when the specified amount of files is present in the input directory. When set to -1 the list of files is passed to the pipleline whenever one of more files are present.</td><td></td></tr>
 * <tr><td>{@link #setFileListForcedAfter(long) fileListForcedAfter}</td><td>When set along with fileList a list of files is passed to the pipleline when the specified amount of ms has passed since the first file for a new list of files was found even if the amount of files specified by fileList isn't present in the input directory yet</td><td></td></tr>
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
 * @version $Id$
 */
public class DirectoryListener implements IPullingListener, INamedObject, HasPhysicalDestination {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String inputDirectory;
	private String wildcard;
	private String excludeWildcard;
	private boolean fileTimeSensitive=false;
	private Integer fileList;
	private Long fileListForcedAfter;
	private Long fileListFirstFileFound;
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
		if (StringUtils.isNotEmpty(getProcessedDirectory())) {
			File dir = new File(getProcessedDirectory());
			if (!dir.isDirectory()) {
				throw new ConfigurationException("The value for processedDirectory [" + getProcessedDirectory() + "] is invalid. It is not a directory ");
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
			if (getFileList() != null) {
				try {
					XmlUtils.parseXml(new AfterMessageProcessedHandler(),(String)rawMessage);
				} catch (Exception e) {
					throw new ListenerException("Could not move files ["+rawMessage+"]",e);
				}
			} else {
				String filename=getStringFromRawMessage(rawMessage, context);
				moveFileAfterProcessing(filename);
			}
		}
	}

	private void moveFileAfterProcessing(String filename) throws ListenerException {
		File f=new File(filename);
		try {
			FileUtils.moveFileAfterProcessing(f, getProcessedDirectory(), isDelete(), isOverwrite(), getNumberOfBackups()); 
		} catch (Exception e) {
			throw new ListenerException("Could not move file ["+filename+"]",e);
		}
	}

	/**
	 * Moves a file to another directory and places a UUID in the name.
	 * @return String with the name of the (renamed and moved) file
	 * 
	 */
	protected String archiveFile(IPipeLineSession session, File file) throws ListenerException {
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
		if (getFileList() == null) {
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
			PipeLineSessionBase.setListenerParameters(threadContext, correlationId, correlationId, null, null);
			return correlationId;
		} else {
			return null;
		}
	}
	/**
	 * Retrieves a single record from a file. If the file is empty or fully processed, it looks wether there
	 * is a new file to process and returns the first record.
	 */
	public synchronized Object getRawMessage(Map threadContext) throws ListenerException {
		File[] inputFiles = FileUtils.getFiles(getInputDirectory(), getWildcard(), getExcludeWildcard(), getMinStableTime());
		if (inputFiles.length == 0) {
			return null;
		} else if (getFileList() != null) {
			if (fileListFirstFileFound == null) {
				fileListFirstFileFound = System.currentTimeMillis();
			}
			if (inputFiles.length >= getFileList()
					|| (getFileListForcedAfter() != null
					&& System.currentTimeMillis() > fileListFirstFileFound + getFileListForcedAfter())) {
				XmlBuilder filesXml = new XmlBuilder("files");
				int max = getFileList();
				if (inputFiles.length < max || max == -1) {
					max = inputFiles.length;
				}
				for (int i = 0; i < max; i++) {
					XmlBuilder fileXml = new XmlBuilder("file");
					fileXml.setValue(getInputFileName(inputFiles[i], threadContext));
					filesXml.addSubElement(fileXml);
				}
				fileListFirstFileFound = null;
				return filesXml.toXML();
			} else {
				return null;
			}
		} else {
			return getInputFileName(inputFiles[0], threadContext);
		}
	}

	private String getInputFileName(File inputFile, Map threadContext) throws ListenerException {
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
	
	private IPipeLineSession getSession(Map threadContext) {
		IPipeLineSession session = new PipeLineSessionBase();
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

	public void setFileList(Integer fileList) {
		this.fileList = fileList;
	}

	public Integer getFileList() {
		return fileList;
	}

	public void setFileListForcedAfter(Long fileListForcedAfter) {
		this.fileListForcedAfter = fileListForcedAfter;
	}

	public Long getFileListForcedAfter() {
		return fileListForcedAfter;
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

	class AfterMessageProcessedHandler extends DefaultHandler {
		boolean fileStartElementFound = false;
		StringBuffer fileName;

		public void startElement(String uri, String localName, String qName, Attributes attributes)	throws SAXException {
			if ("file".equals(localName)) {
				fileStartElementFound = true;
				fileName = new StringBuffer();
			}
		}

		public void characters(char[] ch, int start, int length) {
			if (fileStartElementFound) {
				fileName.append(ch, start, length);
			}
		}

		public void endElement(String uri, String localName, String qname) throws SAXException {
			if ("file".equals(localName)) {
				fileStartElementFound = false;
				try {
					moveFileAfterProcessing(fileName.toString());
				} catch (ListenerException e) {
					throw new SAXException(e);
				}
				fileName = null;
			}
		}
	}

}
