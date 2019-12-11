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
import org.apache.commons.lang3.builder.ToStringBuilder;
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
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * {@link IPullingListener listener} that looks in a {@link IBasicFileSystem FileSystem} for files.
 * When a file is found, it is moved to an process-folder, so that it isn't found more then once.  
 * The name of the moved file is passed to the pipeline.  
 *
 *
 * @author Gerrit van Brakel
 */
public abstract class FileSystemListener<F, FS extends IBasicFileSystem<F>> implements IPullingListener<F> {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String inputFolder;
	private String inProcessFolder;
	private String processedFolder;
	private String errorFolder;

	private boolean createFolders=false;
	private boolean delete = false;
	private boolean overwrite = false;
	private boolean fileTimeSensitive=false;
//	private boolean overwrite = false;
	private String messageType="path";

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
		try {
			getFileSystem().open();
			// folders can only be checked at 'open()', because the checks need an opened file system.
			checkForExistenceOfFolder("inputFolder", getInputFolder());
			if (!checkForExistenceOfFolder("inProcessFolder", getInProcessFolder())) {
				ConfigurationWarnings.add(this, log, "attribute 'inProcessFolder' has not been set. This listener can only run in a single thread");			
			}
			checkForExistenceOfFolder("processedFolder", getProcessedFolder());
			checkForExistenceOfFolder("errorFolder",getErrorFolder());
		} catch (FileSystemException e) {
			throw new ListenerException("Cannot open fileSystem",e);
		}
	}
	
	protected boolean checkForExistenceOfFolder(String attributeName, String folderName) throws ListenerException, FileSystemException {
		FS fileSystem = getFileSystem();
		if (StringUtils.isNotEmpty(folderName)) {
			if (fileSystem.folderExists(folderName)) {
				return true;
			}
			if (isCreateFolders()) {
				fileSystem.createFolder(folderName);
				return true;
			} else { 
				String canonicalNameClause;
				try {
					canonicalNameClause=", canonical name ["+fileSystem.getCanonicalName(fileSystem.toFile(folderName))+"],";
				} catch (FileSystemException e) {
					canonicalNameClause=", (no canonical name: "+e.getMessage()+"),";
				}
				throw new ListenerException("The value for " +attributeName + " [" + folderName + "]"+canonicalNameClause+" is invalid. It is not a folder.");
			}
		}
		return false;
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
		String result=getFileSystem() instanceof HasPhysicalDestination?((HasPhysicalDestination)getFileSystem()).getPhysicalDestinationName()+" ":"";
		result+= "inputFolder [" + (getInputFolder() == null ? "" : getInputFolder()) + "] inProcessFolder [" + (getInProcessFolder() == null ? "" : getInProcessFolder()) +
				"] processedFolder [" + (getProcessedFolder() == null ? "" : getProcessedFolder()) + "] errorFolder [" + (getErrorFolder() == null ? "" : getErrorFolder()) + "]";
		return result;
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
					F inprocessFile = moveFile(file, getInProcessFolder());
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
	protected F moveFile(F file, String destinationFolder) throws ListenerException {
		FS fileSystem=getFileSystem();

		String filename=null;
		try {
			if (isOverwrite()) {
				F destinationFile = fileSystem.toFile(destinationFolder, fileSystem.getName(file));
				if (fileSystem.exists(destinationFile)) {
					log.debug("removing current destination file ["+fileSystem.getCanonicalName(destinationFile)+"]");
					fileSystem.deleteFile(destinationFile);
				}
			}
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
					moveFile(rawMessage, getErrorFolder());
					return;
				}
			}
			if (isDelete()) {
				fileSystem.deleteFile(rawMessage);
				return;
			}
			if (StringUtils.isNotEmpty(getProcessedFolder())) {
				moveFile(rawMessage, getProcessedFolder());
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
			if (StringUtils.isEmpty(getMessageType()) || getMessageType().equalsIgnoreCase("path")) {
				return getFileSystem().getCanonicalName(rawMessage);
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

	@IbisDoc({"8", "when set <code>true</code>, the destination file will be deleted if it already exists", "false"})
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}
	public boolean isOverwrite() {
		return overwrite;
	}


	@IbisDoc({"9", "when <code>true</code>, the file modification time is used in addition to the filename to determine if a file has been seen before", "false"})
	public void setFileTimeSensitive(boolean b) {
		fileTimeSensitive = b;
	}
	public boolean isFileTimeSensitive() {
		return fileTimeSensitive;
	}

	@IbisDoc({"10", "determines the contents of the message that is sent to the pipeline. Can be 'name', for the filename, 'path', for the full file path, 'contents' for the contents of the file. For any other value, the attributes of the file are searched and used", "path"})
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	public String getMessageType() {
		return messageType;
	}


	@IbisDoc({"11", "minimal age of file in milliseconds, to avoid receiving a file while it is still being written", "1000 [ms]"})
	public void setMinStableTime(long minStableTime) {
		this.minStableTime = minStableTime;
	}
	public long getMinStableTime() {
		return minStableTime;
	}

}
