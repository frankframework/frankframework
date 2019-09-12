/*
   Copyright 2019 Integration Parners

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
package nl.nn.adapterframework.filesystem;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.receivers.DirectoryListener;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * {@link IPullingListener listener} that looks in a {@link IBasicFileSystem FileSystem} for files.
 * When a file is found, it is moved to an process-folder, so that it isn't found more then once.  
 * The name of the moved file is passed to the pipeline.  
 *
 *
 * @author Gerrit van Brakel, after {@link DirectoryListener} by John Dekker
 */
public abstract class FileSystemListener<F, FS extends IBasicFileSystem<F>> implements IPullingListener<F>, IFileSystemListener<F> {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String inputFolder;
	private String inProcessFolder;
	private String processedFolder;
	private String errorFolder;

	private boolean createFolders=false;
	private boolean delete = false;
	private boolean fileTimeSensitive=false;
//	private boolean overwrite = false;
	private String messageType="name";

	private long minStableTime = 1000;
//	private Long fileListFirstFileFound;
	
	private FS fileSystem;

	protected abstract FS createFileSystem();
	
	public FileSystemListener() {
		fileSystem=createFileSystem();
	}
	
	@Override
	public void configure() throws ConfigurationException {
		FS fileSystem = getFileSystem();
		fileSystem.configure();
	}

	@Override
	public void open() throws ListenerException {
		FS fileSystem = getFileSystem();
		try {
			getFileSystem().open();
			// folders can only be checked at 'open()', because the checks need an opened file system.
			if (StringUtils.isNotEmpty(getInputFolder())) {
				if (!fileSystem.folderExists(getInputFolder())) {
					if (isCreateFolders()) {
						fileSystem.createFolder(getInputFolder());
					} else { 
						throw new ListenerException("The value for inputFolder [" + getInputFolder() + "] is invalid. It is not a folder.");
					}
				}
			}
			if (StringUtils.isNotEmpty(getInProcessFolder())) {
				if (!fileSystem.folderExists(getInProcessFolder())) {
					if (isCreateFolders()) {
						fileSystem.createFolder(getInProcessFolder());
					} else { 
						throw new ListenerException("The value for inProcessFolder [" + getInProcessFolder() + "] is invalid. It is not a folder.");
					}
				}
			} else {
				ConfigurationWarnings.add(this, log, "attribute 'inProcessFolder' has not been set. This listener can only run in a single thread");			
			}
			if (StringUtils.isNotEmpty(getProcessedFolder())) {
				if (!fileSystem.folderExists(getProcessedFolder())) {
					if (isCreateFolders()) {
						fileSystem.createFolder(getProcessedFolder());
					} else { 
						throw new ListenerException("The value for processedFolder [" + getProcessedFolder() + "] is invalid. It is not a folder.");
					}
				}
			}
			if (StringUtils.isNotEmpty(getErrorFolder())) {
				if (!fileSystem.folderExists(getErrorFolder())) {
					if (isCreateFolders()) {
						fileSystem.createFolder(getErrorFolder());
					} else { 
						throw new ListenerException("The value for errorFolder [" + getErrorFolder() + "] is invalid. It is not a folder.");
					}
				}
			}
		} catch (FileSystemException e) {
			throw new ListenerException("Cannot open fileSystem",e);
		}
	}
	
	@Override
	public void close() throws ListenerException {
		try {
			getFileSystem().close();
		} catch (FileSystemException e) {
			throw new ListenerException("Cannot close fileSystem",e);
		}
	}

	@Override
	public Map<String,Object> openThread() throws ListenerException {
		return null;
	}

	@Override
	public void closeThread(Map<String,Object> threadContext) throws ListenerException {
		// nothing special here
	}

	public String getPhysicalDestinationName() {
		if (getFileSystem() instanceof HasPhysicalDestination) {
			return ((HasPhysicalDestination)getFileSystem()).getPhysicalDestinationName();
		}
		return null;
	}

	public FS getFileSystem() {
		return fileSystem;
	}


	@Override
	public synchronized F getRawMessage(Map<String,Object> threadContext) throws ListenerException {
		try {
			FS fileSystem=getFileSystem();
			Iterator<F> it = fileSystem.listFiles(getInputFolder());
			if (it==null || !it.hasNext()) {
				return null;
			}
			
			long stabilityLimit = getMinStableTime();
			if (stabilityLimit>0) {
				stabilityLimit=System.currentTimeMillis()-stabilityLimit;
			}
			while (it.hasNext()) {
				F file = it.next();
				if (stabilityLimit>0) {
					long filemodtime=fileSystem.getModificationTime(file).getTime();
					if (filemodtime>stabilityLimit) {
						continue;
					}
				}
				if (StringUtils.isNotEmpty(getInProcessFolder())) {
					F inprocessFile = moveFileToInProcess(file, getInProcessFolder());
					return inprocessFile;
				} 
				return file;
			}
			return null;
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}


	/**
	 * Used to be: Moves a file to another directory and places a UUID in the name.
	 * Now is:  Moves a file
	 * @return String with the name of the (renamed and moved) file
	 * 
	 */
	protected F moveFileToInProcess(F file, String destinationFolder) throws ListenerException {
		FS fileSystem=getFileSystem();

		String filename=null;
		try {
			filename=fileSystem.getName(file);
			F newFile = fileSystem.moveFile(file, destinationFolder, isCreateFolders());
			if (newFile == null) {
				throw new ListenerException(getName() + " was unable to move file [" + filename + "] to [" + destinationFolder + "]");
			}
			return newFile;
		} catch(FileSystemException e) {
			throw new ListenerException(getName() + " was unable to move file [" + filename + "] to [" + destinationFolder + "]", e);
		}
	}


	@Override
	public void afterMessageProcessed(PipeLineResult processResult, F rawMessage, Map<String,Object> context) throws ListenerException {
		FS fileSystem=getFileSystem();
		try {
			if (!PipeLineExit.EXIT_STATE_SUCCESS.equals(processResult.getState())) {
				if (StringUtils.isNotEmpty(getErrorFolder())) {
					fileSystem.moveFile(rawMessage, getErrorFolder(), isCreateFolders());
					return;
				}
			}
			if (isDelete()) {
				fileSystem.deleteFile(rawMessage);
				return;
			}
			if (StringUtils.isNotEmpty(getProcessedFolder())) {
				fileSystem.moveFile(rawMessage, getProcessedFolder(), isCreateFolders());
			}
		} catch (FileSystemException e) {
			throw new ListenerException("Could not move or delete file ["+fileSystem.getName(rawMessage)+"]",e);
		}
	}

	/**
	 * Returns returns the filename, or the contents
	 */
	@Override
	public String getStringFromRawMessage(F rawMessage, Map<String,Object> threadContext) throws ListenerException {
		try {
			if (StringUtils.isEmpty(getMessageType()) || getMessageType().equalsIgnoreCase("name")) {
				return getFileSystem().getName(rawMessage);
			}
			if (getMessageType().equalsIgnoreCase("contents")) {
				return StreamUtil.streamToString(getFileSystem().readFile(rawMessage), null, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			}
			Map<String,Object> attributes = getFileSystem().getAdditionalFileProperties(rawMessage);
			if (attributes!=null) {
				Object result=attributes.get(getMessageType());
				if (result!=null) {
					return result.toString();
				}
			}
			log.warn("no attribute ["+getMessageType()+"] found for file ["+getFileSystem().getName(rawMessage)+"]");
			return null;
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	/**
	 * Returns the name of the file in process (the {@link #moveFileToInProcess(Object file, String destination) archived} file) concatenated with the
	 * record number. As the {@link #moveFileToInProcess(Object file, String destination) archivedFile} method always renames to a 
	 * unique file, the combination of this filename and the recordnumber is unique, enabling tracing in case of errors
	 * in the processing of the file.
	 * Override this method for your specific needs! 
	 */
	@Override
	public String getIdFromRawMessage(F rawMessage, Map<String, Object> threadContext) throws ListenerException {
	    String filename=null;
		try {
			FS fileSystem = getFileSystem();
			F file = rawMessage;
			filename=fileSystem.getName(file);
			String messageId=fileSystem.getCanonicalName(file);
			if (isFileTimeSensitive()) {
				messageId+="-"+DateUtils.format(fileSystem.getModificationTime(file));
			}
			PipeLineSessionBase.setListenerParameters(threadContext, messageId, messageId, null, null);
			Map <String,Object> attributes = fileSystem.getAdditionalFileProperties(rawMessage);
			if (attributes!=null) {
				threadContext.putAll(attributes);
			}
			return messageId;
		} catch (Exception e) {
			throw new ListenerException("Could not get filetime for filename ["+filename+"]",e);
		}
	}

	
	
//	public String toString() {
//		String result = super.toString();
//		ToStringBuilder ts = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
//		ts.append("name", getName());
//		ts.append("inputDirectory", getInputDirectory());
//		ts.append("wildcard", getWildcard());
//		ts.append("excludeWildcard", getExcludeWildcard());
//		result += ts.toString();
//		return result;
//	}


	@Override
	@IbisDoc({"1", "name of the listener", ""})
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @Deprecated replaced by inProcessFolder
	 */
	public void setInputDirectory(String inputDirectory) {
		ConfigurationWarnings.add(this, log, "attribute 'inputDirectory' has been replaced by 'inputFolder'");
		setInputFolder(inputDirectory);
	}

	@IbisDoc({"2", "folder that is scanned for files. When not set, the root is scanned", ""})
	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}
	public String getInputFolder() {
		return inputFolder;
	}


//	/**
//	 * set the {@link nl.nn.adapterframework.util.WildCardFilter wildcard}  to look for files in the specifiek directory, e.g. "*.inp"
//	 */
//	@IbisDoc({"filter of files to look for in inputdirectory", ""})
//	public void setWildcard(String wildcard) {
//		this.wildcard = wildcard;
//	}
//	/**
//	* get the {@link nl.nn.adapterframework.util.WildCardFilter wildcard}  to look for files in the specifiek directory, e.g. "*.inp"
//	*/
//	public String getWildcard() {
//		return wildcard;
//	}
//
//	@IbisDoc({"filter of files to be excluded when looking in inputdirectory", ""})
//	public void setExcludeWildcard(String excludeWildcard) {
//		this.excludeWildcard = excludeWildcard;
//	}
//
//	public String getExcludeWildcard() {
//		return excludeWildcard;
//	}

//	@IbisDoc({"when set a list of files in xml format (&lt;files&gt;&lt;file&gt;/file/name&lt;/file&gt;&lt;file&gt;/another/file/name&lt;/file&gt;&lt;/files&gt;) is passed to the pipleline instead of 1 file name when the specified amount of files is present in the input directory. when set to -1 the list of files is passed to the pipleline whenever one of more files are present.", ""})
//	public void setFileList(Integer fileList) {
//		this.fileList = fileList;
//	}
//	public Integer getFileList() {
//		return fileList;
//	}
//	
//	@IbisDoc({"when set along with filelist a list of files is passed to the pipleline when the specified amount of ms has passed since the first file for a new list of files was found even if the amount of files specified by filelist isn't present in the input directory yet", ""})
//	public void setFileListForcedAfter(Long fileListForcedAfter) {
//		this.fileListForcedAfter = fileListForcedAfter;
//	}
//	public Long getFileListForcedAfter() {
//		return fileListForcedAfter;
//	}

	/**
	 * @Deprecated replaced by inProcessFolder
	 */
	public void setOutputDirectory(String outputDirectory) {
		ConfigurationWarnings.add(this, log, "attribute 'outputDirectory' has been replaced by 'inProcessFolder'");
		setInProcessFolder(outputDirectory);
	}

	@IbisDoc({"3", "folder where files are stored <i>while</i> being processed", ""})
	public void setInProcessFolder(String inProcessFolder) {
		this.inProcessFolder = inProcessFolder;
	}
	public String getInProcessFolder() {
		return inProcessFolder;
	}

	/**
	 * @Deprecated replaced by processedFolder
	 */
	public void setProcessedDirectory(String processedDirectory) {
		ConfigurationWarnings.add(this, log, "attribute 'processedDirectory' has been replaced by 'processedFolder'");
		setProcessedFolder(processedDirectory);
	}

	@IbisDoc({"4", "folder where files are stored <i>after</i> being processed", ""})
	public void setProcessedFolder(String processedFolder) {
		this.processedFolder = processedFolder;
	}
	public String getProcessedFolder() {
		return processedFolder;
	}

	@IbisDoc({"5", "folder where files are stored <i>after</i> being processed, in case the exit-state was not equal to <code>success</code>", ""})
	public void setErrorFolder(String errorFolder) {
		this.errorFolder = errorFolder;
	}
	public String getErrorFolder() {
		return errorFolder;
	}

	@IbisDoc({"6", "when set to <code>true</code>, the folders to look for files and to move files to when being processed and after being processed are created if they are specified and do not exist", "false"})
	public void setCreateFolders(boolean createFolders) {
		this.createFolders = createFolders;
	}
	public boolean isCreateFolders() {
		return createFolders;
	}
	
	@Override
	public void setCreateInputDirectory(boolean createInputDirectory) {
		ConfigurationWarnings.add(this, log, "attribute 'createInputDirectory' has been replaced by 'createFolders'");
		setCreateFolders(createInputDirectory);
	}

	@IbisDoc({"7", "when set <code>true</code>, the file processed will deleted after being processed, and not stored", "false"})
	public void setDelete(boolean b) {
		delete = b;
	}
	public boolean isDelete() {
		return delete;
	}

//	@IbisDoc({"pattern for the name using the messageformat.format method. params: 0=inputfilename, 1=inputfile extension, 2=unique uuid, 3=current date", ""})
//	public void setOutputFilenamePattern(String string) {
//		outputFilenamePattern = string;
//	}
//	public String getOutputFilenamePattern() {
//		return outputFilenamePattern;
//	}

//	@IbisDoc({"pass the filename without the <code>outputdirectory</code> to the pipeline", "false"})
//	public void setPassWithoutDirectory(boolean b) {
//		passWithoutDirectory = b;
//	}
//
//	public boolean isPassWithoutDirectory() {
//		return passWithoutDirectory;
//	}

	
//	@IbisDoc({"number of copies held of a file with the same name. backup files have a dot and a number suffixed to their name. if set to 0, no backups will be kept.", "5"})
//	public void setNumberOfBackups(int i) {
//		numberOfBackups = i;
//	}
//	public int getNumberOfBackups() {
//		return numberOfBackups;
//	}

//	@IbisDoc({"when set <code>true</code>, the destination file will be deleted if it already exists", "false"})
//	public void setOverwrite(boolean overwrite) {
//		this.overwrite = overwrite;
//	}
//	public boolean isOverwrite() {
//		return overwrite;
//	}


	@IbisDoc({"8", "when <code>true</code>, the file modification time is used in addition to the filename to determine if a file has been seen before", "false"})
	public void setFileTimeSensitive(boolean b) {
		fileTimeSensitive = b;
	}
	public boolean isFileTimeSensitive() {
		return fileTimeSensitive;
	}

	@Override
	@IbisDoc({"9", "determines the contents of the message that is sent to the pipeline. Can be 'name', for the filename, 'contents' for the contents of the file. For any other value, the attributes of the file are searched and used", "name"})
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	public String getMessageType() {
		return messageType;
	}


	@IbisDoc({"10", "minimal age of file in milliseconds, to avoid receiving a file while it is still being written", "1000 [ms]"})
	public void setMinStableTime(long minStableTime) {
		this.minStableTime = minStableTime;
	}
	public long getMinStableTime() {
		return minStableTime;
	}

}
