/*
   Copyright 2019-2021 WeAreFrank!

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * {@link IPullingListener listener} that looks in a {@link IBasicFileSystem FileSystem} for files.
 * When a file is found, it is moved to an process-folder, so that it isn't found more then once.  
 * The name of the moved file is passed to the pipeline.  
 *
 *
 * @author Gerrit van Brakel
 */
public abstract class FileSystemListener<F, FS extends IBasicFileSystem<F>> implements IPullingListener<F>, HasPhysicalDestination, IProvidesMessageBrowsers<F> {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	
	public final String ORIGINAL_FILENAME_KEY = "originalFilename";

	private String name;
	private String inputFolder;
	private String inProcessFolder;
	private String processedFolder;
	private String errorFolder;
	private String holdFolder;
	private String logFolder;

	private boolean createFolders=false;
	private boolean delete = false;
	private boolean overwrite = false;
	private int numberOfBackups=0;
	private boolean fileTimeSensitive=false;
	private String messageType="path";
	private String messageIdPropertyKey = null;
	
	private boolean disableMessageBrowsers = false;
	

	private long minStableTime = 1000;
//	private Long fileListFirstFileFound;

	private FS fileSystem;
	
	private Set<ProcessState> knownProcessStates;
	private Map<ProcessState,Set<ProcessState>> targetProcessStates = new HashMap<>();
	private String wildCard;
	private String excludeWildCard;

	protected abstract FS createFileSystem();

	public FileSystemListener() {
		fileSystem=createFileSystem();
	}

	@Override
	public void configure() throws ConfigurationException {
		FS fileSystem = getFileSystem();
		fileSystem.configure();
		if (getNumberOfBackups()>0 && !(fileSystem instanceof IWritableFileSystem)) {
			throw new ConfigurationException("FileSystem ["+ClassUtils.nameOf(fileSystem)+"] does not support setting attribute 'numberOfBackups'");
		}
		knownProcessStates = ProcessState.getMandatoryKnownStates();
		for (ProcessState state: ProcessState.values()) {
			if (StringUtils.isNotEmpty(getStateFolder(state))) {
				knownProcessStates.add(state);
			}
		}
		targetProcessStates = ProcessState.getTargetProcessStates(knownProcessStates);
	}

	@Override
	public Set<ProcessState> knownProcessStates() {
		return knownProcessStates;
	}

	@Override
	public Map<ProcessState,Set<ProcessState>> targetProcessStates() {
		return targetProcessStates;
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
			checkForExistenceOfFolder("logFolder",getLogFolder());
		} catch (FileSystemException e) {
			throw new ListenerException("Cannot open fileSystem",e);
		}
	}

	protected boolean checkForExistenceOfFolder(String attributeName, String folderName) throws ListenerException {
		FS fileSystem = getFileSystem();
		if (StringUtils.isNotEmpty(folderName)) {
			try {
				if (fileSystem.folderExists(folderName)) {
					return true;
				}
			} catch (FileSystemException e) {
				throw new ListenerException("Cannot determine presence of  " +attributeName + " [" + folderName + "]",e);
			}
			if (isCreateFolders()) {
				try {
					fileSystem.createFolder(folderName);
				} catch (FileSystemException e) {
					throw new ListenerException("Cannot create " +attributeName + " [" + folderName + "]",e);
				}
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

	@Override
	public String getPhysicalDestinationName() {
		String result=getFileSystem() instanceof HasPhysicalDestination?((HasPhysicalDestination)getFileSystem()).getPhysicalDestinationName()+" ":"";
		result+= "inputFolder [" + (getInputFolder() == null ? "" : getInputFolder()) + "] inProcessFolder [" + (getInProcessFolder() == null ? "" : getInProcessFolder()) +
				"] processedFolder [" + (getProcessedFolder() == null ? "" : getProcessedFolder()) + "] errorFolder [" + (getErrorFolder() == null ? "" : getErrorFolder()) + "] logFolder [" + (getLogFolder() == null ? "" : getLogFolder()) + "]";
		return result;
	}

	public FS getFileSystem() {
		return fileSystem;
	}

	@Override
	public synchronized F getRawMessage(Map<String,Object> threadContext) throws ListenerException {
		FS fileSystem=getFileSystem();
		try(Stream<F> ds = FileSystemUtils.getFilteredStream(fileSystem, getInputFolder(), getWildCard(), getExcludeWildCard())) {
			if (ds==null) {
				return null;
			}
			Iterator<F> it = ds.iterator();
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
				if (threadContext!=null && StringUtils.isNotEmpty(getInProcessFolder())) {
					threadContext.put(ORIGINAL_FILENAME_KEY, fileSystem.getName(file));
				}
				return file;
			}
		} catch (IOException | FileSystemException e) {
			throw new ListenerException(e);
		}

		return null;
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessageOrWrapper, Map<String,Object> context) throws ListenerException {
		FS fileSystem=getFileSystem();
		if ((rawMessageOrWrapper instanceof MessageWrapper)) { 
			// if it is a MessageWrapper, it comes from an errorStorage, and then the state cannot be managed using folders by the listener itself.
			MessageWrapper<?> wrapper = (MessageWrapper<?>)rawMessageOrWrapper;
			if (StringUtils.isNotEmpty(getLogFolder()) || StringUtils.isNotEmpty(getErrorFolder()) || StringUtils.isNotEmpty(getProcessedFolder())) {
				log.warn("cannot write ["+wrapper.getId()+"] to logFolder, errorFolder or processedFolder after manual retry from errorStorage");
			}
		} else {
			@SuppressWarnings("unchecked") 
			F rawMessage = (F)rawMessageOrWrapper; // if it is not a wrapper, than it must be an F 
			try {
				if (StringUtils.isNotEmpty(getLogFolder())) {
					FileSystemUtils.copyFile(fileSystem, rawMessage, getLogFolder(), isOverwrite(), getNumberOfBackups(), isCreateFolders());
				}
				if (!processResult.isSuccessful()) {
					if (StringUtils.isNotEmpty(getErrorFolder())) {
						FileSystemUtils.moveFile(fileSystem, rawMessage, getErrorFolder(), isOverwrite(), getNumberOfBackups(), isCreateFolders());
						return;
					}
				}
				if (isDelete()) {
					fileSystem.deleteFile(rawMessage);
					return;
				}
				if (StringUtils.isNotEmpty(getProcessedFolder())) {
					FileSystemUtils.moveFile(fileSystem, rawMessage, getProcessedFolder(), isOverwrite(), getNumberOfBackups(), isCreateFolders());
				}
			} catch (FileSystemException e) {
				throw new ListenerException("Could not move or delete file ["+fileSystem.getName(rawMessage)+"]",e);
			}
		}
	}

	/**
	 * Returns returns the filename, or the contents
	 */
	@Override
	public Message extractMessage(F rawMessage, Map<String,Object> threadContext) throws ListenerException {
		try {
			if (StringUtils.isEmpty(getMessageType()) || getMessageType().equalsIgnoreCase("name")) {
				return new Message(getFileSystem().getName(rawMessage));
			}
			if (StringUtils.isEmpty(getMessageType()) || getMessageType().equalsIgnoreCase("path")) {
				return new Message(getFileSystem().getCanonicalName(rawMessage));
			}
			if (getMessageType().equalsIgnoreCase("contents")) {
				return getFileSystem().readFile(rawMessage);
			}
			Map<String,Object> attributes = getFileSystem().getAdditionalFileProperties(rawMessage);
			if (attributes!=null) {
				Object result=attributes.get(getMessageType());
				if (result!=null) {
					return Message.asMessage(result);
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
			filename=fileSystem.getName(rawMessage);
			Map <String,Object> attributes = fileSystem.getAdditionalFileProperties(rawMessage);
			String messageId = null;
			if (StringUtils.isNotEmpty(getMessageIdPropertyKey())) {
				if (attributes != null) {
					messageId = (String)attributes.get(getMessageIdPropertyKey());
				}
				if (StringUtils.isEmpty(messageId)) {
					log.warn("no attribute ["+getMessageIdPropertyKey()+"] found, will use filename as messageId");
				}
			}
			if (StringUtils.isEmpty(messageId) && threadContext!=null) {
				messageId = (String)threadContext.get(ORIGINAL_FILENAME_KEY);
			}
			if (StringUtils.isEmpty(messageId)) {
				messageId = fileSystem.getName(rawMessage);
			}
			if (isFileTimeSensitive()) {
				messageId+="-"+DateUtils.format(fileSystem.getModificationTime(file));
			}
			if (threadContext!=null) {
				PipeLineSessionBase.setListenerParameters(threadContext, messageId, messageId, null, null);
				if (attributes!=null) {
					threadContext.putAll(attributes);
				}
				if (!"path".equals(getMessageType())) {
					threadContext.put("filepath", fileSystem.getCanonicalName(rawMessage));
				}
				if (!"name".equals(getMessageType())) {
					threadContext.put("filename", fileSystem.getName(rawMessage));
				}
			}
			return messageId;
		} catch (Exception e) {
			throw new ListenerException("Could not get filetime for filename ["+filename+"]",e);
		}
	}

	@Override
	public F changeProcessState(F message, ProcessState toState) throws ListenerException {
		try {
			if (!fileSystem.exists(message) || !knownProcessStates().contains(toState)) {
				return null; // if message and/or toState does not exist, the message can/will not be moved to it, so return null.
			}
			return getFileSystem().moveFile(message, getStateFolder(toState), false);
		} catch (FileSystemException e) {
			throw new ListenerException("Cannot change processState to ["+toState+"] for ["+getFileSystem().getName(message)+"]", e);
		}
	}

	public String getStateFolder(ProcessState state) {
		switch (state) {
		case AVAILABLE:
			return getInputFolder();
		case INPROCESS:
			return getInProcessFolder();
		case DONE:
			return getProcessedFolder();
		case ERROR:
			return getErrorFolder();
		case HOLD:
			return getHoldFolder();
		default:
			throw new IllegalStateException("Unknown state ["+state+"]");
		}
	}
	
	@Override
	public IMessageBrowser<F> getMessageBrowser(ProcessState state) {
		if (isDisableMessageBrowsers() || !knownProcessStates().contains(state)) {
			return null;
		}
		return new FileSystemMessageBrowser<F, FS>(fileSystem, getStateFolder(state), getMessageIdPropertyKey());
	}	
	
	@Override
	@IbisDoc({"1", "Name of the listener", ""})
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public String getName() {
		return name;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'inputDirectory' has been replaced by 'inputFolder'")
	public void setInputDirectory(String inputDirectory) {
		setInputFolder(inputDirectory);
	}

	@IbisDoc({"2", "Folder that is scanned for files. If not set, the root is scanned", ""})
	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}
	public String getInputFolder() {
		return inputFolder;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'outputDirectory' has been replaced by 'inProcessFolder'")
	public void setOutputDirectory(String outputDirectory) {
		setInProcessFolder(outputDirectory);
	}

	@IbisDoc({"3", "Folder where files are stored <i>while</i> being processed", ""})
	public void setInProcessFolder(String inProcessFolder) {
		this.inProcessFolder = inProcessFolder;
	}
	public String getInProcessFolder() {
		return inProcessFolder;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'processedDirectory' has been replaced by 'processedFolder'")
	public void setProcessedDirectory(String processedDirectory) {
		setProcessedFolder(processedDirectory);
	}

	@IbisDoc({"4", "Folder where files are stored <i>after</i> being processed", ""})
	public void setProcessedFolder(String processedFolder) {
		this.processedFolder = processedFolder;
	}
	public String getProcessedFolder() {
		return processedFolder;
	}

	@IbisDoc({"5", "Folder where files are stored <i>after</i> being processed, in case the exit-state was not equal to <code>success</code>", ""})
	public void setErrorFolder(String errorFolder) {
		this.errorFolder = errorFolder;
	}
	public String getErrorFolder() {
		return errorFolder;
	}

	@IbisDoc({"6", "Folder where messages from the error folder can be put on Hold, temporarily", ""})
	public void setHoldFolder(String holdFolder) {
		this.holdFolder = holdFolder;
	}
	public String getHoldFolder() {
		return holdFolder;
	}

	@IbisDoc({"6", "Folder where a copy of every file that is received is stored", ""})
	public void setLogFolder(String logFolder) {
		this.logFolder = logFolder;
	}
	public String getLogFolder() {
		return logFolder;
	}

	@IbisDoc({"7", "If set to <code>true</code>, the folders to look for files and to move files to when being processed and after being processed are created if they are specified and do not exist", "false"})
	public void setCreateFolders(boolean createFolders) {
		this.createFolders = createFolders;
	}
	public boolean isCreateFolders() {
		return createFolders;
	}
	
	@Deprecated
	@ConfigurationWarning("attribute 'createInputDirectory' has been replaced by 'createFolders'")
	public void setCreateInputDirectory(boolean createInputDirectory) {
		setCreateFolders(createInputDirectory);
	}

	@IbisDoc({"8", "If set <code>true</code>, the file processed will be deleted after being processed, and not stored", "false"})
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

	
	@IbisDoc({"9", "Number of copies held of a file with the same name. Backup files have a dot and a number suffixed to their name. If set to 0, no backups will be kept.", "0"})
	public void setNumberOfBackups(int i) {
		numberOfBackups = i;
	}
	public int getNumberOfBackups() {
		return numberOfBackups;
	}

	@IbisDoc({"10", "If set <code>true</code>, the destination file will be deleted if it already exists", "false"})
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}
	public boolean isOverwrite() {
		return overwrite;
	}

	@IbisDoc({"11", "Determines the contents of the message that is sent to the pipeline. Can be 'name', for the filename, 'path', for the full file path, 'contents' for the contents of the file. For any other value, the attributes of the file are searched and used", "path"})
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	public String getMessageType() {
		return messageType;
	}

	@IbisDoc({"12", "If <code>true</code>, the file modification time is used in addition to the filename to determine if a file has been seen before", "false"})
	public void setFileTimeSensitive(boolean b) {
		fileTimeSensitive = b;
	}
	public boolean isFileTimeSensitive() {
		return fileTimeSensitive;
	}

	@IbisDoc({"13", "Minimal age of file in milliseconds, to avoid receiving a file while it is still being written", "1000 [ms]"})
	public void setMinStableTime(long minStableTime) {
		this.minStableTime = minStableTime;
	}
	public long getMinStableTime() {
		return minStableTime;
	}

	@IbisDoc({"14", "Key of Property to use as messageId. If not set, the filename of the file as it was received in the inputFolder is used as the messageId", "for MailFileSystems: Message-ID"})
	public void setMessageIdPropertyKey(String messageIdPropertyKey) {
		this.messageIdPropertyKey = messageIdPropertyKey;
	}
	public String getMessageIdPropertyKey() {
		return messageIdPropertyKey;
	}

	@IbisDoc({"15", "If set <code>true</code>, no browsers for process folders are generated", "false"})
	public void setDisableMessageBrowsers(boolean disableMessageBrowsers) {
		this.disableMessageBrowsers = disableMessageBrowsers;
	}
	public boolean isDisableMessageBrowsers() {
		return disableMessageBrowsers;
	}

	@IbisDoc({"16", "Filter of files to look for in inputFolder e.g. '*.inp'.", ""})
	public void setWildCard(String wildCard) {
		this.wildCard = wildCard;
	}
	public String getWildCard() {
		return wildCard;
	}

	@IbisDoc({"17", "Filter of files to be excluded when looking in inputFolder.", ""})
	public void setExcludeWildCard(String excludeWildCard) {
		this.excludeWildCard = excludeWildCard;
	}
	public String getExcludeWildCard() {
		return excludeWildCard;
	}

}
