/*
   Copyright 2019-2023 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.document.DocumentBuilderFactory;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.stream.document.ObjectBuilder;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.SpringUtils;

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
	private @Getter @Setter ApplicationContext applicationContext;

	public final String ORIGINAL_FILENAME_KEY = "originalFilename";
	public final String FILENAME_KEY = "filename";
	public final String FILEPATH_KEY = "filepath";

	private @Getter String name;
	private @Getter String inputFolder;
	private @Getter String inProcessFolder;
	private @Getter String processedFolder;
	private @Getter String errorFolder;
	private @Getter String holdFolder;
	private @Getter String logFolder;

	private @Getter boolean createFolders=false;
	private @Getter boolean delete = false;
	private @Getter boolean overwrite = false;
	private @Getter int numberOfBackups=0;
	private @Getter boolean fileTimeSensitive=false;
	private @Getter String messageType="path";
	private @Getter String messageIdPropertyKey = null;
	private @Getter String storeMetadataInSessionKey;

	private @Getter boolean disableMessageBrowsers = false;
	private @Getter String wildcard;
	private @Getter String excludeWildcard;
	private @Getter String charset;

	private @Getter long minStableTime = 1000;
	private @Getter DocumentFormat outputFormat=DocumentFormat.XML;

	private @Getter FS fileSystem;

	private Set<ProcessState> knownProcessStates;
	private Map<ProcessState,Set<ProcessState>> targetProcessStates = new HashMap<>();

	protected abstract FS createFileSystem();

	public FileSystemListener() {
		fileSystem=createFileSystem();
	}

	@Override
	public void configure() throws ConfigurationException {
		log.debug("Configuring FileSystemListener");
		FS fileSystem = getFileSystem();
		SpringUtils.autowireByName(getApplicationContext(), fileSystem);
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
		log.debug("Opening FileSystemListener");
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
			}
			String canonicalNameClause;
			try {
				canonicalNameClause=", canonical name ["+fileSystem.getCanonicalName(fileSystem.toFile(folderName))+"],";
			} catch (FileSystemException e) {
				canonicalNameClause=", (no canonical name: "+e.getMessage()+"),";
			}
			throw new ListenerException("The value for " +attributeName + " [" + folderName + "]"+canonicalNameClause+" is invalid. It is not a folder.");
		}
		return false;
	}

	@Override
	public void close() throws ListenerException {
		log.debug("Closing the FS");
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
		StringBuilder destination = new StringBuilder(getFileSystem().getPhysicalDestinationName());
		if(getInputFolder() != null) destination.append(" inputFolder [").append(getInputFolder()).append("]");
		if(getInProcessFolder() != null) destination.append(" inProcessFolder [").append(getInProcessFolder()).append("]");
		if(getProcessedFolder() != null) destination.append(" processedFolder [").append(getProcessedFolder()).append("]");
		if(getErrorFolder() != null) destination.append(" errorFolder [").append(getErrorFolder()).append("]");
		if(getLogFolder() != null) destination.append(" logFolder [").append(getLogFolder()).append("]");

		log.trace("Physical destination name: [{}]", destination);
		return destination.toString();
	}

	@Override
	public String getDomain() {
		return getFileSystem().getDomain();
	}

	@Override
	public synchronized RawMessageWrapper<F> getRawMessage(Map<String,Object> threadContext) throws ListenerException {
		log.trace("Get Raw Message");
		FS fileSystem=getFileSystem();
		log.trace("Getting raw message from FS {}", fileSystem.getClass().getSimpleName());
		try(Stream<F> ds = FileSystemUtils.getFilteredStream(fileSystem, getInputFolder(), getWildcard(), getExcludeWildcard())) {
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
				if (StringUtils.isNotEmpty(getLogFolder())) {
					FileSystemUtils.copyFile(fileSystem, file, getLogFolder(), isOverwrite(), getNumberOfBackups(), isCreateFolders(), false);
				}
				return wrapRawMessage(file, threadContext);
			}
		} catch (IOException | FileSystemException e) {
			throw new ListenerException(e);
		}

		return null;
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<F> rawMessage, Map<String,Object> context) throws ListenerException {
		log.debug("After Message Processed - begin");
		FS fileSystem=getFileSystem();
		if ((rawMessage instanceof MessageWrapper)) {
			// TODO: Check if below is still correct!
			// TODO: Check test coverage of this code, in old and new branches.
			// if it is a MessageWrapper, it comes from an errorStorage, and then the state cannot be managed using folders by the listener itself.
			MessageWrapper<?> wrapper = (MessageWrapper<?>) rawMessage;
			if (StringUtils.isNotEmpty(getLogFolder()) || StringUtils.isNotEmpty(getErrorFolder()) || StringUtils.isNotEmpty(getProcessedFolder())) {
				log.warn("cannot write ["+wrapper.getId()+"] to logFolder, errorFolder or processedFolder after manual retry from errorStorage");
			}
		} else {
			F file = rawMessage.getRawMessage(); // if it is not a MessageWrapper, then it must be a RawMessageWrapper<F>
			try {
				if (isDelete() && (processResult.isSuccessful() || StringUtils.isEmpty(getErrorFolder()))) {
					fileSystem.deleteFile(file);
				}
			} catch (FileSystemException e) {
				throw new ListenerException("Could not copy or delete file ["+fileSystem.getName(file)+"]",e);
			}
		}
		log.debug("After Message Processed - end");
	}

	/**
	 * Returns the filename, or the contents
	 */
	@Override
	public Message extractMessage(RawMessageWrapper<F> rawMessage, Map<String,Object> threadContext) throws ListenerException {
		log.debug("Extract message from raw message");
		try {
			if (StringUtils.isEmpty(getMessageType()) || getMessageType().equalsIgnoreCase("name")) {
				return new Message(getFileSystem().getName(rawMessage.getRawMessage()));
			}
			if (StringUtils.isEmpty(getMessageType()) || getMessageType().equalsIgnoreCase("path")) {
				return new Message(getFileSystem().getCanonicalName(rawMessage.getRawMessage()));
			}
			if (getMessageType().equalsIgnoreCase("contents")) {
				return getFileSystem().readFile(rawMessage.getRawMessage(), getCharset());
			}
			if (getMessageType().equalsIgnoreCase("info")) {
				return new Message(FileSystemUtils.getFileInfo(getFileSystem(), rawMessage.getRawMessage(), getOutputFormat()));
			}

			Map<String,Object> attributes = getFileSystem().getAdditionalFileProperties(rawMessage.getRawMessage());
			if (attributes!=null) {
				Object result=attributes.get(getMessageType());
				if (result!=null) {
					return Message.asMessage(result);
				}
			}
			log.warn("no attribute ["+getMessageType()+"] found for file ["+getFileSystem().getName(rawMessage.getRawMessage())+"]");
			return null;
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

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
				PipeLineSession.updateListenerParameters(threadContext, messageId, messageId, null, null);
				if (attributes!=null) {
					threadContext.putAll(attributes);
				}
				if (!"path".equals(getMessageType())) {
					threadContext.put(FILEPATH_KEY, fileSystem.getCanonicalName(rawMessage));
				}
				if (!"name".equals(getMessageType())) {
					threadContext.put(FILENAME_KEY, fileSystem.getName(rawMessage));
				}
			}
			if (StringUtils.isNotEmpty(getStoreMetadataInSessionKey())) {
				ObjectBuilder metadataBuilder = DocumentBuilderFactory.startObjectDocument(DocumentFormat.XML, "metadata");

				if (attributes!=null) {
					attributes.forEach((k,v) -> {
						try {
							metadataBuilder.add(k, v==null?null:v.toString());
						} catch (SAXException e) {
							log.warn("cannot add property [{}] value [{}]", k, v, e);
						}
					});
				}

				metadataBuilder.close();
				threadContext.put(getStoreMetadataInSessionKey(), metadataBuilder.toString());
			}
			return messageId;
		} catch (Exception e) {
			throw new ListenerException("Could not get filetime for filename ["+filename+"]",e);
		}
	}

	// result is guaranteed if toState==ProcessState.INPROCESS
	@Override
	public RawMessageWrapper<F> changeProcessState(RawMessageWrapper<F> message, ProcessState toState, String reason) throws ListenerException {
		log.debug("Change message process state to [{}] for message [{}]", toState, message);
		try {
			if (!getFileSystem().exists(message.getRawMessage()) || !knownProcessStates().contains(toState)) {
				return null; // if message and/or toState does not exist, the message can/will not be moved to it, so return null.
			}
			if (toState==ProcessState.DONE || toState==ProcessState.ERROR) {
				return wrap(FileSystemUtils.moveFile(getFileSystem(), message.getRawMessage(), getStateFolder(toState), isOverwrite(), getNumberOfBackups(), isCreateFolders(), false), message);
			}
			if (toState==ProcessState.INPROCESS && isFileTimeSensitive() && getFileSystem() instanceof IWritableFileSystem) {
				F movedFile = getFileSystem().moveFile(message.getRawMessage(), getStateFolder(toState), false, true);
				String newName = getFileSystem().getCanonicalName(movedFile)+"-"+(DateUtils.format(getFileSystem().getModificationTime(movedFile)).replace(":", "_"));
				F renamedFile = getFileSystem().toFile(newName);
				int i=1;
				while(getFileSystem().exists(renamedFile)) {
					renamedFile=getFileSystem().toFile(newName+"-"+i);
					if(i>5) {
						log.warn("Cannot rename file ["+message+"] with the timestamp suffix. File moved to ["+getStateFolder(toState)+"] folder with the original name");
						return wrap(movedFile, message);
					}
					i++;
				}
				return wrap(FileSystemUtils.renameFile((IWritableFileSystem<F>) getFileSystem(), movedFile, renamedFile, false, 0), message);
			}
			return wrap(getFileSystem().moveFile(message.getRawMessage(), getStateFolder(toState), false, toState==ProcessState.INPROCESS), message);
		} catch (FileSystemException e) {
			throw new ListenerException("Cannot change processState to ["+toState+"] for ["+getFileSystem().getName(message.getRawMessage())+"]", e);
		}
	}

	private RawMessageWrapper<F> wrap(F file, RawMessageWrapper<F> originalMessage) throws ListenerException {
		return wrapRawMessage(file, originalMessage.getContext());
	}

	public RawMessageWrapper<F> wrapRawMessage(F file, Map<String, Object> context) throws ListenerException {
		return new RawMessageWrapper<>(file, this.getIdFromRawMessage(file, context), (String) context.get(PipeLineSession.correlationIdKey), context);
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
		return new FileSystemMessageBrowser<F, FS>(getFileSystem(), getStateFolder(state), getMessageIdPropertyKey());
	}

	@Override
	/** Name of the listener */
	public void setName(String name) {
		this.name = name;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'inputDirectory' has been replaced by 'inputFolder'")
	public void setInputDirectory(String inputDirectory) {
		setInputFolder(inputDirectory);
	}

	/** Folder that is scanned for files. If not set, the root is scanned */
	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'outputDirectory' has been replaced by 'inProcessFolder'")
	public void setOutputDirectory(String outputDirectory) {
		setInProcessFolder(outputDirectory);
	}

	/** Folder where files are stored <i>while</i> being processed */
	public void setInProcessFolder(String inProcessFolder) {
		this.inProcessFolder = inProcessFolder;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'processedDirectory' has been replaced by 'processedFolder'")
	public void setProcessedDirectory(String processedDirectory) {
		setProcessedFolder(processedDirectory);
	}

	/** Folder where files are stored <i>after</i> being processed */
	public void setProcessedFolder(String processedFolder) {
		this.processedFolder = processedFolder;
	}

	/** Folder where files are stored <i>after</i> being processed, in case the exit-state was not equal to <code>success</code> */
	public void setErrorFolder(String errorFolder) {
		this.errorFolder = errorFolder;
	}

	/** Folder where messages from the error folder can be put on Hold, temporarily */
	public void setHoldFolder(String holdFolder) {
		this.holdFolder = holdFolder;
	}

	/** Folder where a copy of every file that is received is stored */
	public void setLogFolder(String logFolder) {
		this.logFolder = logFolder;
	}

	/**
	 * If set to <code>true</code>, the folders to look for files and to move files to when being processed and after being processed are created if they are specified and do not exist
	 * @ff.default false
	 */
	public void setCreateFolders(boolean createFolders) {
		this.createFolders = createFolders;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'createInputDirectory' has been replaced by 'createFolders'")
	public void setCreateInputDirectory(boolean createInputDirectory) {
		setCreateFolders(createInputDirectory);
	}

	/**
	 * If set <code>true</code>, the file processed will be deleted after being processed, and not stored
	 * @ff.default false
	 */
	public void setDelete(boolean b) {
		delete = b;
	}

	/**
	 * Number of copies held of a file with the same name. Backup files have a dot and a number suffixed to their name. If set to 0, no backups will be kept.
	 * @ff.default 0
	 */
	public void setNumberOfBackups(int i) {
		numberOfBackups = i;
	}

	/**
	 * If set <code>true</code>, the destination file will be deleted if it already exists
	 * @ff.default false
	 */
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	/**
	 * Determines the contents of the message that is sent to the pipeline. Can be 'name', for the filename, 'path', for the full file path, 'contents' for the contents of the file, 'info' for file information. For any other value, the attributes of the file are searched and used
	 * @ff.default path
	 */
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	/**
	 * If <code>true</code>, the file modification time is used in addition to the filename to determine if a file has been seen before
	 * @ff.default false
	 */
	public void setFileTimeSensitive(boolean b) {
		fileTimeSensitive = b;
	}

	/**
	 * Minimal age of file <i>in milliseconds</i>, to avoid receiving a file while it is still being written
	 * @ff.default 1000
	 */
	public void setMinStableTime(long minStableTime) {
		this.minStableTime = minStableTime;
	}

	/**
	 * Key of Property to use as messageId. If not set, the filename of the file as it was received in the inputFolder is used as the messageId
	 * @ff.default for MailFileSystems: Message-ID
	 */
	public void setMessageIdPropertyKey(String messageIdPropertyKey) {
		this.messageIdPropertyKey = messageIdPropertyKey;
	}

	/**
	 * If set <code>true</code>, no browsers for process folders are generated
	 * @ff.default false
	 */
	public void setDisableMessageBrowsers(boolean disableMessageBrowsers) {
		this.disableMessageBrowsers = disableMessageBrowsers;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'wildCard' has been renamed to 'wildcard'")
	public void setWildCard(String wildcard) {
		setWildcard(wildcard);
	}
	/** Filter of files to look for in inputFolder e.g. '*.inp'. */
	public void setWildcard(String wildcard) {
		this.wildcard = wildcard;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'excludeWildCard' has been renamed to 'excludeWildcard'")
	public void setExcludeWildCard(String excludeWildcard) {
		setExcludeWildcard(excludeWildcard);
	}
	/** Filter of files to be excluded when looking in inputFolder. */
	public void setExcludeWildcard(String excludeWildcard) {
		this.excludeWildcard = excludeWildcard;
	}

	/** If set, an XML with all message properties is provided under this key */
	public void setStoreMetadataInSessionKey(String storeMetadataInSessionKey) {
		this.storeMetadataInSessionKey = storeMetadataInSessionKey;
	}

	/** Charset to be used for extracting the contents */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * OutputFormat of message for messageType=info
	 * @ff.default XML
	 */
	public void setOutputFormat(DocumentFormat outputFormat) {
		this.outputFormat = outputFormat;
	}
}
