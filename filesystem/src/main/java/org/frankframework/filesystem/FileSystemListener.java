/*
   Copyright 2019-2024 WeAreFrank!

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
package org.frankframework.filesystem;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IProvidesMessageBrowsers;
import org.frankframework.core.IPullingListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.ProcessState;
import org.frankframework.documentbuilder.DocumentBuilderFactory;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.documentbuilder.ObjectBuilder;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.SpringUtils;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

/**
 * {@link IPullingListener listener} that looks in a {@link IBasicFileSystem FileSystem} for files.
 * When a file is found, it is moved to a process-folder, so that it isn't found more than once.
 * The name of the moved file is passed to the pipeline.
 *
 * @author Gerrit van Brakel
 */
public abstract class FileSystemListener<F, FS extends IBasicFileSystem<F>> implements IPullingListener<F>, HasPhysicalDestination, IProvidesMessageBrowsers<F> {
	protected Logger log = LogUtil.getLogger(this);
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	public static final String ORIGINAL_FILENAME_KEY = "originalFilename";
	public static final String FILENAME_KEY = "filename";
	public static final String FILEPATH_KEY = "filepath";

	private static final Set<String> KEYS_COPIED_TO_MESSAGE_CONTEXT;
	static {
		KEYS_COPIED_TO_MESSAGE_CONTEXT = new HashSet<>();
		KEYS_COPIED_TO_MESSAGE_CONTEXT.add(PipeLineSession.MESSAGE_ID_KEY);
		KEYS_COPIED_TO_MESSAGE_CONTEXT.add(PipeLineSession.CORRELATION_ID_KEY);
		KEYS_COPIED_TO_MESSAGE_CONTEXT.add(FILENAME_KEY);
		KEYS_COPIED_TO_MESSAGE_CONTEXT.add(FILEPATH_KEY);
	}

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

	private final @Getter FS fileSystem;

	private Set<ProcessState> knownProcessStates;
	private Map<ProcessState,Set<ProcessState>> targetProcessStates = new EnumMap<>(ProcessState.class);

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

	@Nonnull
	@Override
	public Map<String,Object> openThread() throws ListenerException {
		return new HashMap<>();
	}

	@Override
	public void closeThread(@Nonnull Map<String, Object> threadContext) throws ListenerException {
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
	public synchronized RawMessageWrapper<F> getRawMessage(@Nonnull Map<String, Object> threadContext) throws ListenerException {
		log.trace("Get Raw Message");
		FS fileSystem=getFileSystem();
		log.trace("Getting raw message from FS {}", fileSystem.getClass().getSimpleName());
		try(Stream<F> ds = FileSystemUtils.getFilteredStream(fileSystem, getInputFolder(), getWildcard(), getExcludeWildcard(), TypeFilter.FILES_ONLY)) {
			Optional<F> fo = findFirstStableFile(ds);
			if (fo.isEmpty()) {
				return null;
			}
			F file = fo.get();
			String originalFilename;
			if (StringUtils.isNotEmpty(getInProcessFolder())) {
				originalFilename = fileSystem.getName(file);
				threadContext.put(ORIGINAL_FILENAME_KEY, originalFilename);
			} else {
				originalFilename = null;
			}
			if (StringUtils.isNotEmpty(getLogFolder())) {
				FileSystemUtils.copyFile(fileSystem, file, getLogFolder(), isOverwrite(), getNumberOfBackups(), isCreateFolders(), false);
			}
			return wrapRawMessage(file, originalFilename, threadContext);
		} catch (FileSystemException e) {
			throw new ListenerException(e);
		}
	}

	// Can throw FileSystemException from a Lambda
	private Optional<F> findFirstStableFile(Stream<F> ds) throws FileSystemException {
		long stabilityLimit = getMinStableTime();
		if (stabilityLimit <= 0L) {
			return ds.findFirst();
		}
		long latestAcceptableFileModTime = System.currentTimeMillis() - stabilityLimit;

		return ds.filter(file -> isFileOlderThan(file, latestAcceptableFileModTime))
				.findFirst();
	}

	@SneakyThrows(FileSystemException.class) // SneakyThrows because it's used in a Lambda
	private boolean isFileOlderThan(F file, long timeInMillis) {
		long filemodtime=fileSystem.getModificationTime(file).getTime();
		return filemodtime <= timeInMillis;
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<F> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
		log.debug("After Message Processed - begin");
		FS fileSystem=getFileSystem();
		if (rawMessage instanceof MessageWrapper wrapper) {
			if (StringUtils.isNotEmpty(getLogFolder()) || StringUtils.isNotEmpty(getErrorFolder()) || StringUtils.isNotEmpty(getProcessedFolder())) {
				log.warn("cannot write [{}] to logFolder, errorFolder or processedFolder after manual retry from errorStorage", wrapper.getId());
			}
		} else {
			F file = rawMessage.getRawMessage();
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
	public Message extractMessage(@Nonnull RawMessageWrapper<F> rawMessage, @Nonnull Map<String, Object> context) throws ListenerException {
		log.debug("Extract message from raw message");
		try {
			F file = rawMessage.getRawMessage();
			if (StringUtils.isEmpty(getMessageType()) || getMessageType().equalsIgnoreCase("name")) {
				return new Message(getFileSystem().getName(file));
			}
			if (StringUtils.isEmpty(getMessageType()) || getMessageType().equalsIgnoreCase("path")) {
				return new Message(getFileSystem().getCanonicalName(file));
			}
			if (getMessageType().equalsIgnoreCase("contents")) {
				return getFileSystem().readFile(file, getCharset());
			}
			if (getMessageType().equalsIgnoreCase("info")) {
				return new Message(FileSystemUtils.getFileInfo(getFileSystem(), file, getOutputFormat()));
			}

			Map<String,Object> attributes = getFileSystem().getAdditionalFileProperties(file);
			if (attributes != null) {
				Object result = attributes.get(getMessageType());
				if (result != null) {
					return Message.asMessage(result);
				}
			}
			log.warn("no attribute [{}] found for file [{}]", getMessageType(), getFileSystem().getName(file));
			return null;
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	public @Nonnull Map<String, Object> extractMessageProperties(@Nonnull F rawMessage, @Nullable String originalFilename) throws ListenerException {
		Map<String, Object> messageProperties = new HashMap<>();
		String filename=null;
		try {
			FS fs = getFileSystem();
			filename = fs.getName(rawMessage);
			Map <String,Object> attributes = fs.getAdditionalFileProperties(rawMessage);
			String messageId = null;
			if (StringUtils.isNotEmpty(getMessageIdPropertyKey())) {
				if (attributes != null) {
					messageId = (String)attributes.get(getMessageIdPropertyKey());
				}
				if (StringUtils.isEmpty(messageId)) {
					log.warn("no attribute [{}] found, will use filename as messageId", getMessageIdPropertyKey());
				}
			}
			if (StringUtils.isEmpty(messageId)) {
				messageId = originalFilename;
			}
			if (StringUtils.isEmpty(messageId)) {
				messageId = fs.getName(rawMessage);
			}
			if (isFileTimeSensitive()) {
				messageId += "-" + DateFormatUtils.format(fs.getModificationTime(rawMessage), DateFormatUtils.FULL_ISO_TIMESTAMP_NO_TZ_FORMATTER);
			}
			PipeLineSession.updateListenerParameters(messageProperties, messageId, messageId);
			if (attributes!=null) {
				messageProperties.putAll(attributes);
			}
			if (!"path".equals(getMessageType())) {
				messageProperties.put(FILEPATH_KEY, fs.getCanonicalName(rawMessage));
			}
			if (!"name".equals(getMessageType())) {
				messageProperties.put(FILENAME_KEY, fs.getName(rawMessage));
			}
			if (StringUtils.isNotEmpty(getStoreMetadataInSessionKey())) {
				ObjectBuilder metadataBuilder = DocumentBuilderFactory.startObjectDocument(DocumentFormat.XML, "metadata");

				if (attributes != null) {
					attributes.forEach((k,v) -> {
						try {
							metadataBuilder.add(k, v == null ? null : v.toString());
						} catch (SAXException e) {
							log.warn("cannot add property [{}] value [{}]", k, v, e);
						}
					});
				}

				metadataBuilder.close();
				messageProperties.put(getStoreMetadataInSessionKey(), metadataBuilder.toString());
			}
			return messageProperties;
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
				F movedFile = getFileSystem().moveFile(message.getRawMessage(), getStateFolder(toState), false);
				String newName = getFileSystem().getCanonicalName(movedFile)+"-"+(DateFormatUtils.format(getFileSystem().getModificationTime(movedFile), DateFormatUtils.FULL_ISO_TIMESTAMP_NO_TZ_FORMATTER).replace(":", "_"));
				F renamedFile = getFileSystem().toFile(newName);
				int i=1;
				while(getFileSystem().exists(renamedFile)) {
					renamedFile=getFileSystem().toFile(newName+"-"+i);
					if(i>5) {
						log.warn("Cannot rename file [{}] with the timestamp suffix. File moved to [{}] folder with the original name", ()->message, ()->getStateFolder(toState));
						return wrap(movedFile, message);
					}
					i++;
				}
				//noinspection unchecked
				return wrap(FileSystemUtils.renameFile((IWritableFileSystem<F>) getFileSystem(), movedFile, renamedFile, false, 0), message);
			}
			return wrap(getFileSystem().moveFile(message.getRawMessage(), getStateFolder(toState), false), message);
		} catch (FileSystemException e) {
			throw new ListenerException("Cannot change processState to ["+toState+"] for ["+getFileSystem().getName(message.getRawMessage())+"]", e);
		}
	}

	private RawMessageWrapper<F> wrap(F file, RawMessageWrapper<F> originalMessage) throws ListenerException {
		// Do not modify original message context. We do not have threadContext so pass copy of message context as substitute.
		String originalFilename = (String) originalMessage.getContext().get(ORIGINAL_FILENAME_KEY);
		return wrapRawMessage(file, originalFilename, new HashMap<>(originalMessage.getContext()));
	}

	private RawMessageWrapper<F> wrapRawMessage(F file, String originalFilename, Map<String, Object> threadContext) throws ListenerException {
		Map<String, Object> messageProperties = extractMessageProperties(file, originalFilename);
		threadContext.putAll(messageProperties);
		Map<String, Object> messageContext = threadContext.entrySet().stream()
				.filter(entry -> KEYS_COPIED_TO_MESSAGE_CONTEXT.contains(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		return new RawMessageWrapper<>(file, messageContext);
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

	/** Name of the listener */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/** Folder that is scanned for files. If not set, the root is scanned */
	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}

	/** Folder where files are stored <i>while</i> being processed */
	public void setInProcessFolder(String inProcessFolder) {
		this.inProcessFolder = inProcessFolder;
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

	/** Filter of files to look for in inputFolder e.g. '*.inp'. */
	public void setWildcard(String wildcard) {
		this.wildcard = wildcard;
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
