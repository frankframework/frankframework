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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

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
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.documentbuilder.ObjectBuilder;
import org.frankframework.documentbuilder.XmlDocumentBuilder;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.SpringUtils;
import org.frankframework.xml.XmlWriter;

/**
 * {@link IPullingListener listener} that looks in a {@link IBasicFileSystem FileSystem} for files.
 * When a file is found, it is moved to an in-process folder, so that it isn't found more than once.
 * <br/>
 * The information specified by {@link #setMessageType(IMessageType)} is then passed to the pipeline.
 *
 * @ff.info To avoid problems with duplicate filenames in folders like the {@code errorFolder} or {@code processedFolder},
 * you should configure either {@code overwrite="true"}, configure {@code numberOfBackups} to a value larger than 0, or
 * configure an {@code inProcessFolder} and {@code fileTimeSensitive="true"}.
 * These options can be used together as well.
 *
 * @ff.warning In addition to the above, prior to release 9.0 it was not sufficient to configure {@code inProcessFolder} and {@code fileTimeSensitive}
 * to avoid potential duplicate filename errors. Prior to release 9.0, it is recommended to configure {@code numberOfBackups} to avoid these issues.
 *
 */
public abstract class AbstractFileSystemListener<F, FS extends IBasicFileSystem<F>> implements IPullingListener<F>, HasPhysicalDestination, IProvidesMessageBrowsers<F> {
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

	public interface IMessageType {
		String name();
	}

	public enum MessageType implements IMessageType {
		NAME,
		PATH,
		CONTENTS,
		INFO
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
	private @Getter @Setter IMessageType messageType = MessageType.PATH;
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

	public AbstractFileSystemListener() {
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
		if (isFileTimeSensitive() && !(fileSystem instanceof IWritableFileSystem)) {
			throw new ConfigurationException("FileSystem ["+ClassUtils.nameOf(fileSystem)+"] does not support setting attribute 'fileTimeSensitive'");
		}
		knownProcessStates = ProcessState.getMandatoryKnownStates();
		for (ProcessState state: ProcessState.values()) {
			if (StringUtils.isNotEmpty(getStateFolder(state))) {
				knownProcessStates.add(state);
			}
		}
		targetProcessStates = ProcessState.getTargetProcessStates(knownProcessStates);
		if (!(knownProcessStates.contains(ProcessState.INPROCESS) && isFileTimeSensitive()) && !(isOverwrite() || getNumberOfBackups() > 0)) {
			ConfigurationWarnings.add(this, log, "It is recommended to either configure an in-process folder with 'fileTimeSensitive=true', or configure 'overwrite' or 'numberOfBackups', to avoid problems when files with the same name are processed.");
		}
		if (!knownProcessStates.contains(ProcessState.INPROCESS) && isFileTimeSensitive()) {
			ConfigurationWarnings.add(this, log, "Configuring 'fileTimeSensitive' has no effect when no 'In Process' folder is configured.");
		}
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
	public void start() {
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
			throw new LifecycleException("Cannot open fileSystem", e);
		}
	}

	protected boolean checkForExistenceOfFolder(String attributeName, String folderName) throws FileSystemException {
		FS fileSystem = getFileSystem();
		if (StringUtils.isNotEmpty(folderName)) {
			if (fileSystem.folderExists(folderName)) {
				return true;
			}

			if (isCreateFolders()) {
				fileSystem.createFolder(folderName);
				return true;
			}

			String canonicalNameClause;
			try {
				canonicalNameClause=", canonical name ["+fileSystem.getCanonicalName(fileSystem.toFile(folderName))+"],";
			} catch (FileSystemException e) {
				canonicalNameClause=", (no canonical name: "+e.getMessage()+"),";
			}
			throw new FileSystemException("The value for " +attributeName + " [" + folderName + "]"+canonicalNameClause+" is invalid. It is not a folder.");
		}
		return false;
	}

	@Override
	public void stop() {
		log.debug("Closing the FS");
		try {
			getFileSystem().close();
		} catch (FileSystemException e) {
			throw new LifecycleException("Cannot close fileSystem",e);
		}
	}

	@Nonnull
	@Override
	public Map<String,Object> openThread() {
		return new HashMap<>();
	}

	@Override
	public void closeThread(@Nonnull Map<String, Object> threadContext) {
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
		FS fileSystem = getFileSystem();
		log.trace("Getting raw message from FS {}", () -> fileSystem.getClass().getSimpleName());

		try(Stream<F> ds = FileSystemUtils.getFilteredStream(fileSystem, getInputFolder(), getWildcard(), getExcludeWildcard(), TypeFilter.FILES_ONLY)) {
			Optional<F> optionalFile = findFirstStableFile(ds);
			if (optionalFile.isEmpty()) {
				return null;
			}
			F file = optionalFile.get();
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
		FS fs = getFileSystem();
		if (pipeLineSession.get(PipeLineSession.MANUAL_RETRY_KEY, false)) {
			if (StringUtils.isNotEmpty(getLogFolder()) || StringUtils.isNotEmpty(getErrorFolder()) || StringUtils.isNotEmpty(getProcessedFolder())) {
				// TODO: Why not actually? Is this the case, or is this a holdover from the past?
				log.warn("cannot write [{}] to logFolder, errorFolder or processedFolder after manual retry from errorStorage", pipeLineSession.getMessageId());
			}
		} else {
			F file = rawMessage.getRawMessage();
			try {
				if (isDelete() && (processResult.isSuccessful() || StringUtils.isEmpty(getErrorFolder()))) {
					fs.deleteFile(file);
				}
			} catch (FileSystemException e) {
				throw new ListenerException("Could not copy or delete file ["+fs.getName(file)+"]",e);
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

			return switch (getMessageType().name()) {
				case "NAME" -> new Message(getFileSystem().getName(file));
				case "PATH" -> new Message(getFileSystem().getCanonicalName(file));
				case "CONTENTS" -> getFileSystem().readFile(file, getCharset());
				case "INFO" -> new Message(FileSystemUtils.getFileInfo(getFileSystem(), file, getOutputFormat()));
				default -> throw new ListenerException("Unknown messageType [" + getMessageType().name() + "]");
			};
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	public @Nonnull Map<String, Object> extractMessageProperties(@Nonnull F rawMessage, @Nullable String originalFilename) throws ListenerException {
		Map<String, Object> messageProperties = new HashMap<>();
		FS fs = getFileSystem();
		String filename = fs.getName(rawMessage);
		try {
			Map <String,Object> attributes = fs.getAdditionalFileProperties(rawMessage);
			String messageId = deriveMessageId(rawMessage, originalFilename, attributes);
			PipeLineSession.updateListenerParameters(messageProperties, messageId, messageId);
			if (attributes!=null) {
				messageProperties.putAll(attributes);
			}
			if (getMessageType() != MessageType.PATH) {
				messageProperties.put(FILEPATH_KEY, fs.getCanonicalName(rawMessage));
			}
			if (getMessageType() != MessageType.NAME) {
				messageProperties.put(FILENAME_KEY, fs.getName(rawMessage));
			}
			if (StringUtils.isNotEmpty(getStoreMetadataInSessionKey())) {
				String xml = buildAttributeXml(attributes);
				messageProperties.put(getStoreMetadataInSessionKey(), xml);
			}
			return messageProperties;
		} catch (Exception e) {
			throw new ListenerException("Could not get properties for filename ["+filename+"]",e);
		}
	}

	private String deriveMessageId(@Nonnull F rawMessage, @Nullable String originalFilename, Map<String, Object> attributes) throws FileSystemException {
		String messageId = null;
		if (StringUtils.isNotEmpty(getMessageIdPropertyKey())) {
			if (attributes != null) {
				messageId = (String) attributes.get(getMessageIdPropertyKey());
			}
			if (StringUtils.isEmpty(messageId)) {
				log.warn("no attribute [{}] found, will use filename as messageId", getMessageIdPropertyKey());
			}
		}
		if (StringUtils.isEmpty(messageId)) {
			messageId = originalFilename;
		}
		if (StringUtils.isEmpty(messageId)) {
			messageId = getFileSystem().getName(rawMessage);
		}
		if (isFileTimeSensitive()) {
			messageId += "-" + getFormatFileModificationDate(rawMessage);
		}
		return messageId;
	}

	private String buildAttributeXml(Map<String, Object> attributes) throws SAXException {
		XmlWriter writer = new XmlWriter();
		try (XmlDocumentBuilder xmlBuilder = new XmlDocumentBuilder("metadata", writer, true)) {
			if (attributes != null) {
				ObjectBuilder metadataBuilder = xmlBuilder.startObject();
				attributes.forEach((k, v) -> {
					try {
						metadataBuilder.add(k, v == null ? null : v.toString());
					} catch (SAXException e) {
						log.warn("cannot add property [{}] value [{}]", k, v, e);
					}
				});
			}
		}
		return writer.toString();
	}

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
				return wrap(renameFileWithTimeStamp(message, toState, movedFile), message);
			}
			return wrap(getFileSystem().moveFile(message.getRawMessage(), getStateFolder(toState), false), message);
		} catch (FileSystemException e) {
			throw new ListenerException("Cannot change processState to ["+toState+"] for ["+getFileSystem().getName(message.getRawMessage())+"]", e);
		}
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	private F renameFileWithTimeStamp(RawMessageWrapper<F> message, ProcessState toState, F movedFile) throws FileSystemException {

		String fileModificationDate = getFormatFileModificationDate(movedFile).replace(":", "_");

		String fullName = getFileSystem().getName(movedFile);
		if (fullName.contains(fileModificationDate)) {
			return movedFile;
		}
		String extension = FilenameUtils.getExtension(fullName);
		if (StringUtils.isNotEmpty(extension)) {
			extension = "." + extension;
		}
		String newName = FilenameUtils.getBaseName(fullName) + "-" + fileModificationDate;

		String parentFolder = getFileSystem().getParentFolder(movedFile);
		F renamedFile = getFileSystem().toFile(parentFolder, newName + extension);
		int i=1;
		int maxNrInBackups = getNumberOfBackups() > 0 ? getNumberOfBackups() : 5; // This should not fail when numberOfBackups is not configured but numberOfBackups originally did not apply here
		while(getFileSystem().exists(renamedFile)) {
			renamedFile=getFileSystem().toFile(parentFolder, newName+"-"+i + extension);
			if (i > maxNrInBackups) {
				log.warn("Cannot rename file [{}] with the timestamp suffix. File moved to [{}] folder with the original name", ()-> message, ()->getStateFolder(toState));
				return movedFile;
			}
			i++;
		}
		return FileSystemUtils.renameFile((IWritableFileSystem<F>) getFileSystem(), movedFile, renamedFile, false, 0);
	}

	@Nonnull
	private String getFormatFileModificationDate(F movedFile) throws FileSystemException {
		return DateFormatUtils.format(getFileSystem().getModificationTime(movedFile), DateFormatUtils.FULL_ISO_TIMESTAMP_NO_TZ_FORMATTER);
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
		return switch (state) {
			case AVAILABLE -> getInputFolder();
			case INPROCESS -> getInProcessFolder();
			case DONE -> getProcessedFolder();
			case ERROR -> getErrorFolder();
			case HOLD -> getHoldFolder();
		};
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
	 * If <code>true</code>, the file modification time is used in addition to the filename to determine if a file has been seen previously.
	 * <br/>
	 * This setting is only supported for filesystem listeners that implement {@link IWritableFileSystem}.
	 *
	 * @ff.info This setting is only effective when an {@code inProcessFolder} has been configured.
	 *
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

	/**
	 * If set, an XML with all message properties is provided under this key.
	 * Also stored in the {@value PipeLineSession#ORIGINAL_MESSAGE_KEY} metadata.
	 */
	@Deprecated(since = "9.0")
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
