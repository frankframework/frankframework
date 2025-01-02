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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.DirectoryStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.INamedObject;
import org.frankframework.core.PipeLineSession;
import org.frankframework.doc.DocumentedEnum;
import org.frankframework.doc.EnumLabel;
import org.frankframework.documentbuilder.ArrayBuilder;
import org.frankframework.documentbuilder.DocumentBuilderFactory;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.documentbuilder.INodeBuilder;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;

/**
 * Worker class for {@link AbstractFileSystemPipe} and {@link AbstractFileSystemSender}.
 *
 * @ff.parameter action Overrides attribute <code>action</code>
 * @ff.parameter filename Overrides attribute <code>filename</code>. If not present, the input message is used.
 * @ff.parameter destination Destination for action <code>rename</code> and <code>move</code>. Overrides attribute <code>destination</code>.
 * @ff.parameter contents Content for action <code>write</code> and <code>append</code>.
 * @ff.parameter inputFolder Folder for actions <code>list</code>, <code>mkdir</code> and <code>rmdir</code>. This is a sub folder of baseFolder. Overrides attribute <code>inputFolder</code>. If not present, the input message is used.
 * @ff.parameter typeFilter Filter for action <code>list</code>. Specify <code>FILES_ONLY</code>, <code>FOLDERS_ONLY</code> or <code>FILES_AND_FOLDERS</code>. By default, only files are listed.
 *
 * @author Gerrit van Brakel
 */
public class FileSystemActor<F, S extends IBasicFileSystem<F>> {
	protected Logger log = LogUtil.getLogger(this);
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	public static final String ACTION_CREATE="create";
	public static final String ACTION_LIST="list";
	public static final String ACTION_INFO="info";
	public static final String ACTION_READ1="read";
	public static final String ACTION_READ2="download";
	public static final String ACTION_READ_DELETE="readDelete";
	public static final String ACTION_MOVE="move";
	public static final String ACTION_COPY="copy";
	public static final String ACTION_DELETE="delete";
	public static final String ACTION_MKDIR="mkdir";
	public static final String ACTION_RMDIR="rmdir";
	public static final String ACTION_WRITE1="write";
	public static final String ACTION_WRITE2="upload";
	public static final String ACTION_APPEND="append";
	public static final String ACTION_RENAME="rename";
	public static final String ACTION_FORWARD="forward";
	public static final String ACTION_LIST_ATTACHMENTS="listAttachments";

	public static final String PARAMETER_ACTION="action";
	public static final String PARAMETER_CONTENTS1="contents";
	public static final String PARAMETER_CONTENTS2="file";
	public static final String PARAMETER_FILENAME="filename";
	public static final String PARAMETER_INPUTFOLDER="inputFolder";	// folder for actions list, mkdir and rmdir. This is a sub folder of baseFolder
	public static final String PARAMETER_DESTINATION="destination";	// destination for action rename and move
	public static final String PARAMETER_TYPEFILTER = "typeFilter";

	public static final FileSystemAction[] ACTIONS_BASIC= {FileSystemAction.LIST, FileSystemAction.INFO, FileSystemAction.READ, FileSystemAction.DOWNLOAD, FileSystemAction.READDELETE, FileSystemAction.MOVE, FileSystemAction.COPY, FileSystemAction.DELETE, FileSystemAction.MKDIR, FileSystemAction.RMDIR};
	public static final FileSystemAction[] ACTIONS_WRITABLE_FS= {FileSystemAction.CREATE, FileSystemAction.WRITE, FileSystemAction.UPLOAD, FileSystemAction.APPEND, FileSystemAction.RENAME};
	public static final FileSystemAction[] ACTIONS_MAIL_FS= {FileSystemAction.FORWARD};

	private @Getter FileSystemAction action;
	private @Getter String filename;
	private @Getter String destination;
	private @Getter String inputFolder; // folder for action=list
	private @Getter boolean createFolder; // for action create, write, move, rename and list
	private @Getter TypeFilter typeFilter = TypeFilter.FILES_ONLY;

	private @Getter int rotateDays=0;
	private @Getter int rotateSize=0;
	private @Getter boolean overwrite=false;
	private @Getter int numberOfBackups=0;
	private @Getter String wildcard=null;
	private @Getter String excludeWildcard=null;
	private @Getter boolean removeNonEmptyFolder=false;
	private @Getter boolean writeLineSeparator=false;
	private @Getter String charset;
	private @Getter boolean deleteEmptyFolder;
	private @Getter DocumentFormat outputFormat=DocumentFormat.XML;

	private final Set<FileSystemAction> actions = new LinkedHashSet<>(Arrays.asList(ACTIONS_BASIC));

	private INamedObject owner;
	private S fileSystem;
	private ParameterList parameterList;
	private boolean hasCustomFileAttributes = false;

	private byte[] eolArray=null;

	public enum FileSystemAction implements DocumentedEnum {
		/** list files in a folder/directory, specified by attribute <code>inputFolder</code>, parameter <code>inputFolder</code> or input message */
		@EnumLabel(ACTION_LIST) LIST,
		/** show info about a single file, specified by attribute <code>filename</code>, parameter <code>filename</code> or input message */
		@EnumLabel(ACTION_INFO) INFO,
		/** read a file, specified by attribute <code>filename</code>, parameter <code>filename</code> or input message */
		@EnumLabel(ACTION_READ1) READ,
		/** replaced by <code>read</code> */
		@EnumLabel(ACTION_READ2) @Deprecated DOWNLOAD,
		/** like <code>read</code>, but deletes the file after it has been read */
		@EnumLabel(ACTION_READ_DELETE) READDELETE,
		/** move a file, specified by attribute <code>filename</code>, parameter <code>filename</code> or input message, to a folder specified by attribute <code>destination</code> or parameter <code>destination</code> */
		@EnumLabel(ACTION_MOVE) MOVE,
		/** copy a file, specified by attribute <code>filename</code>, parameter <code>filename</code> or input message, to a folder specified by attribute <code>destination</code> or parameter <code>destination</code>  */
		@EnumLabel(ACTION_COPY) COPY,
		/** delete a file, specified by attribute <code>filename</code>, parameter <code>filename</code> or input message */
		@EnumLabel(ACTION_DELETE) DELETE,
		/** create a folder/directory, specified by attribute <code>inputFolder</code>, parameter <code>inputFolder</code> or input message */
		@EnumLabel(ACTION_MKDIR) MKDIR,
		/** remove a folder/directory, specified by attribute <code>inputFolder</code>, parameter <code>inputFolder</code> or input message */
		@EnumLabel(ACTION_RMDIR) RMDIR,
		/** Creates file and writes contents, specified by parameter <code>contents</code> or input message, to a file, specified by attribute <code>filename</code>, parameter <code>filename</code> or input message.
		 *  At least one of the parameters must be specified. The missing parameter defaults to the input message. For streaming operation, the parameter <code>filename</code> must be specified. */
		@EnumLabel(ACTION_WRITE1) WRITE,
		/** replaced by <code>write</code> */
		@EnumLabel(ACTION_WRITE2) @Deprecated UPLOAD,
		/** (only for filesystems that support 'append') append contents, specified by parameter <code>contents</code> or input message, to a file, specified by attribute <code>filename</code>, parameter <code>filename</code> or input message.
		 *  At least one of the parameters must be specified. The missing parameter defaults to the input message. For streaming operation, the parameter <code>filename</code> must be specified. */
		@EnumLabel(ACTION_APPEND) APPEND,
		/** create empty file, specified by attribute <code>filename</code>, parameter <code>filename</code> or input message */
		@EnumLabel(ACTION_CREATE) CREATE,
		/** change the name of a file, specified by attribute <code>filename</code>, parameter <code>filename</code> or input message, to the value specified by attribute <code>destination</code> or parameter <code>destination</code> */
		@EnumLabel(ACTION_RENAME) RENAME,
		/** (for MailFileSystems only:) forward an existing file, specified by parameter <code>contents</code> or input message, to a file, to an email address specified by attribute <code>destination</code> or parameter <code>destination</code> */
		@EnumLabel(ACTION_FORWARD) FORWARD,

		/** Specific to FileSystemSenderWithAttachments */
		@EnumLabel(ACTION_LIST_ATTACHMENTS) LISTATTACHMENTS

	}

	public void configure(S fileSystem, ParameterList parameterList, IConfigurable owner) throws ConfigurationException {
		this.owner=owner;
		this.fileSystem=fileSystem;
		this.parameterList=parameterList;

		if (fileSystem instanceof IWritableFileSystem) {
			actions.addAll(Arrays.asList(ACTIONS_WRITABLE_FS));
		}
		if (fileSystem instanceof IMailFileSystem) {
			actions.addAll(Arrays.asList(ACTIONS_MAIL_FS));
		}

		if (parameterList!=null && parameterList.hasParameter(PARAMETER_CONTENTS2) && !parameterList.hasParameter(PARAMETER_CONTENTS1)) {
			ConfigurationWarnings.add(owner, log, "parameter ["+PARAMETER_CONTENTS2+"] has been replaced with ["+PARAMETER_CONTENTS1+"]");
			parameterList.findParameter(PARAMETER_CONTENTS2).setName(PARAMETER_CONTENTS1);
		}

		if (action != null) {
			if (getAction() == FileSystemAction.DOWNLOAD) {
				ConfigurationWarnings.add(owner, log, "action ["+FileSystemAction.DOWNLOAD+"] has been replaced with ["+FileSystemAction.READ+"]");
				action=FileSystemAction.READ;
			} else if (getAction() == FileSystemAction.UPLOAD) {
				ConfigurationWarnings.add(owner, log, "action ["+FileSystemAction.UPLOAD+"] has been replaced with ["+FileSystemAction.WRITE+"]");
				action=FileSystemAction.WRITE;
			}
			checkConfiguration(getAction());
		} else if (parameterList == null || !parameterList.hasParameter(PARAMETER_ACTION)) {
			throw new ConfigurationException(ClassUtils.nameOf(owner)+": either attribute [action] or parameter ["+PARAMETER_ACTION+"] must be specified");
		}

		if (StringUtils.isNotEmpty(getInputFolder()) && parameterList!=null && parameterList.hasParameter(PARAMETER_INPUTFOLDER)) {
			ConfigurationWarnings.add(owner, log, "inputFolder configured via attribute [inputFolder] as well as via parameter ["+PARAMETER_INPUTFOLDER+"], parameter will be ignored");
		}

		if (!(fileSystem instanceof IWritableFileSystem)) {
			if (getNumberOfBackups()>0) {
				throw new ConfigurationException("FileSystem ["+ClassUtils.nameOf(fileSystem)+"] does not support setting attribute 'numberOfBackups'");
			}
			if (getRotateDays()>0) {
				throw new ConfigurationException("FileSystem ["+ClassUtils.nameOf(fileSystem)+"] does not support setting attribute 'rotateDays'");
			}
		}

		if (parameterList != null && !(fileSystem instanceof ISupportsCustomFileAttributes<?>)) {
			List<String> parametersWithAttributePrefix = parameterList.stream()
					.map(IParameter::getName)
					.filter(p -> p.startsWith(ISupportsCustomFileAttributes.FILE_ATTRIBUTE_PARAM_PREFIX))
					.toList();
			if (!parametersWithAttributePrefix.isEmpty()) {
				ConfigurationWarnings.add(owner, log, "Filesystem [" + ClassUtils.nameOf(fileSystem) + "] does not support setting custom file attribute meta-data: [" + parametersWithAttributePrefix + "]");
			}
		}

		if (fileSystem instanceof ISupportsCustomFileAttributes<?> scfa) {
			hasCustomFileAttributes = scfa.hasCustomFileAttributes(parameterList);
		}

		eolArray = LINE_SEPARATOR.getBytes(StreamUtil.DEFAULT_CHARSET);
	}

	private void checkConfiguration(FileSystemAction action2) throws ConfigurationException {
		if (!actions.contains(action2))
			throw new ConfigurationException(ClassUtils.nameOf(owner)+": unknown or invalid action [" + action2 + "] supported actions are " + actions);

		//Check if necessary parameters are available
		actionRequiresAtLeastOneOfTwoParametersOrAttribute(owner, parameterList, action2, FileSystemAction.WRITE,   PARAMETER_CONTENTS1, PARAMETER_FILENAME, "filename", getFilename());
		actionRequiresAtLeastOneOfTwoParametersOrAttribute(owner, parameterList, action2, FileSystemAction.MOVE,    PARAMETER_DESTINATION, null, "destination", getDestination());
		actionRequiresAtLeastOneOfTwoParametersOrAttribute(owner, parameterList, action2, FileSystemAction.COPY,    PARAMETER_DESTINATION, null, "destination", getDestination());
		actionRequiresAtLeastOneOfTwoParametersOrAttribute(owner, parameterList, action2, FileSystemAction.RENAME,  PARAMETER_DESTINATION, null, "destination", getDestination());
		actionRequiresAtLeastOneOfTwoParametersOrAttribute(owner, parameterList, action2, FileSystemAction.FORWARD, PARAMETER_DESTINATION, null, "destination", getDestination());
	}

	protected void actionRequiresAtLeastOneOfTwoParametersOrAttribute(INamedObject owner, ParameterList parameterList, FileSystemAction configuredAction, FileSystemAction action, String parameter1, String parameter2, String attributeName, String attributeValue) throws ConfigurationException {
		if (configuredAction == action) {
			boolean parameter1Set = parameterList != null && parameterList.hasParameter(parameter1);
			boolean parameter2Set = parameterList != null && parameterList.hasParameter(parameter2);
			boolean attributeSet  = StringUtils.isNotEmpty(attributeValue);
			if (!parameter1Set && !parameter2Set && !attributeSet) {
				throw new ConfigurationException(ClassUtils.nameOf(owner)+": the ["+action+"] action requires the parameter ["+parameter1+"] "+(parameter2!=null?"or parameter ["+parameter2+"] ":"")+(attributeName!=null?"or the attribute ["+attributeName+"] ": "")+"to be present");
			}
		}
	}

	public void open() throws FileSystemException {
		if (StringUtils.isNotEmpty(getInputFolder()) && !fileSystem.folderExists(getInputFolder()) && getAction() != FileSystemAction.MKDIR && getAction() != FileSystemAction.RMDIR) {
			if (isCreateFolder()) {
				log.debug("creating inputFolder [{}]", this::getInputFolder);
				fileSystem.createFolder(getInputFolder());
			} else {
				F file = fileSystem.toFile(getInputFolder());
				if (file != null && fileSystem.exists(file)) {
					throw new FileAlreadyExistsException("inputFolder ["+getInputFolder()+"], canonical name ["+fileSystem.getCanonicalNameOrErrorMessage(fileSystem.toFile(getInputFolder()))+"], does not exist as a folder, but is a file");
				}
				throw new FolderNotFoundException("inputFolder ["+getInputFolder()+"], canonical name ["+fileSystem.getCanonicalNameOrErrorMessage(fileSystem.toFile(getInputFolder()))+"], does not exist");
			}
		}
	}


	private String determineFilename(Message input, ParameterValueList pvl) throws FileSystemException {
		if (StringUtils.isNotEmpty(getFilename())) {
			return getFilename();
		}
		if (pvl!=null && pvl.contains(PARAMETER_FILENAME)) {
			return pvl.get(PARAMETER_FILENAME).asStringValue(null);
		}
		try {
			return input.asString();
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	private String determineDestination(ParameterValueList pvl) throws FileSystemException {
		if (StringUtils.isNotEmpty(getDestination())) {
			return getDestination();
		}
		if (pvl!=null && pvl.contains(PARAMETER_DESTINATION)) {
			String destination = pvl.get(PARAMETER_DESTINATION).asStringValue(null);
			if (StringUtils.isEmpty(destination)) {
				throw new FileSystemException("parameter ["+PARAMETER_DESTINATION+"] does not specify destination");
			}
			return destination;
		}
		throw new FileSystemException("no destination specified");
	}

	private F getFile(@Nonnull Message input, ParameterValueList pvl) throws FileSystemException {
		return fileSystem.toFile(determineFilename(input, pvl));
	}

	private F getFileAndCreateFolder(@Nonnull Message input, ParameterValueList pvl) throws FileSystemException {
		final String filenameWithFolder = determineFilename(input, pvl);
		String folder = FilenameUtils.getFullPathNoEndSeparator(filenameWithFolder);

		if (StringUtils.isNotBlank(folder) && !fileSystem.folderExists(folder)) {
			if (isCreateFolder()) {
				fileSystem.createFolder(folder);
			} else {
				throw new FolderNotFoundException("folder ["+folder+"] does not exist");
			}
		}

		return fileSystem.toFile(filenameWithFolder);
	}

	private String determineInputFolderName(Message input, ParameterValueList pvl) throws FileSystemException {
		if (StringUtils.isNotEmpty(getInputFolder())) {
			return getInputFolder();
		}
		if (pvl!=null && pvl.contains(PARAMETER_INPUTFOLDER)) {
			return pvl.get(PARAMETER_INPUTFOLDER).asStringValue(null);
		}
		try {
			if (input==null || StringUtils.isEmpty(input.asString())) {
				return null;
			}
			return input.asString();
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	// Get the type filter from the parameter list, if it is present. Otherwise, return the pipe attribute value.
	private TypeFilter determineTypeFilter(final ParameterValueList pvl) throws FileSystemException {
		if (pvl != null && pvl.contains(PARAMETER_TYPEFILTER)) {
			try {
				return EnumUtils.parse(TypeFilter.class, pvl.get(PARAMETER_TYPEFILTER).asStringValue(String.valueOf(getTypeFilter())));
			} catch (IllegalArgumentException e) {
				throw new FileSystemException("unable to resolve the value of parameter [" + PARAMETER_TYPEFILTER + "]");
			}
		}
		return getTypeFilter();
	}

	private FileSystemAction getAction(ParameterValueList pvl) throws FileSystemException, ConfigurationException {
		if (pvl != null && pvl.contains(PARAMETER_ACTION)) {
			try {
				action = EnumUtils.parse(FileSystemAction.class, pvl.get(PARAMETER_ACTION).asStringValue(String.valueOf(getAction())));
			} catch(IllegalArgumentException e) {
				throw new FileSystemException("unable to resolve the value of parameter ["+PARAMETER_ACTION+"]");
			}
			checkConfiguration(action);
		} else {
			action = getAction();
		}
		return action;
	}

	@SuppressWarnings("unchecked")
	public Message doAction(@Nonnull Message input, ParameterValueList pvl, @Nonnull PipeLineSession session) throws FileSystemException {
		FileSystemAction action = null;
		try {
			input.closeOnCloseOf(session, getClass().getSimpleName() + " of a " + fileSystem.getClass().getSimpleName()); // don't know if the input will be used

			action = getAction(pvl);

			switch(action) {
				case CREATE:{
					return createFile(input, pvl, null);
				}
				case DELETE: {
					return processAction(input, pvl, f -> { fileSystem.deleteFile(f); return f; });
				}
				case INFO: {
					F file=getFile(input, pvl);
					FileSystemUtils.checkSource(fileSystem, file, FileSystemAction.INFO);
					return new Message(FileSystemUtils.getFileInfo(fileSystem, file, getOutputFormat()));
				}
				case READ: {
					F file = getFile(input, pvl);
					return fileSystem.readFile(file, getCharset());
				}
				case READDELETE: {
					final F file = getFile(input, pvl);
					Message result = fileSystem.readFile(file, getCharset());
					// Make a copy of a local file, otherwise the file is deleted after this method returns.
					if (fileSystem instanceof LocalFileSystem) {
						result = result.copyMessage();
					} else {
						result.preserve();
					}
					fileSystem.deleteFile(file);
					deleteEmptyFolder(file);
					return result;
				}
				case LIST: {
					return processListAction(input, pvl);
				}
				case WRITE: {
					return createFile(input, pvl, getContents(input, pvl, charset));
				}
				case APPEND: {
					F file=getFile(input, pvl);
					if (getRotateDays()>0 && fileSystem.exists(file)) {
						FileSystemUtils.rolloverByDay((IWritableFileSystem<F>)fileSystem, file, getInputFolder(), getRotateDays());
						file=getFile(input, pvl); // re-obtain the file, as the object itself may have changed because of the rollover
					}
					if (getRotateSize()>0 && fileSystem.exists(file)) {
						FileSystemUtils.rolloverBySize((IWritableFileSystem<F>)fileSystem, file, getRotateSize(), getNumberOfBackups());
						file=getFile(input, pvl); // re-obtain the file, as the object itself may have changed because of the rollover
					}

					((IWritableFileSystem<F>)fileSystem).appendFile(file, getContents(input, pvl, charset));
					return new Message(FileSystemUtils.getFileInfo(fileSystem, file, getOutputFormat()));
				}
				case MKDIR: {
					String folder = determineInputFolderName(input, pvl);
					fileSystem.createFolder(folder);
					return new Message(folder);
				}
				case RMDIR: {
					String folder = determineInputFolderName(input, pvl);
					fileSystem.removeFolder(folder, isRemoveNonEmptyFolder());
					return new Message(folder);
				}
				case RENAME: {
					F source=getFile(input, pvl);
					String destinationName = determineDestination(pvl);
					F destination;
					if (destinationName.contains("/") || destinationName.contains("\\")) {
						destination = fileSystem.toFile(destinationName);
					} else {
						String folderPath = fileSystem.getParentFolder(source);
						destination = fileSystem.toFile(folderPath,destinationName);
					}
					F renamed = FileSystemUtils.renameFile((IWritableFileSystem<F>)fileSystem, source, destination, isOverwrite(), getNumberOfBackups());
					return new Message(fileSystem.getName(renamed));
				}
				case MOVE: {
					String destinationFolder = determineDestination(pvl);
					return processAction(input, pvl, f -> FileSystemUtils.moveFile(fileSystem, f, destinationFolder, isOverwrite(), getNumberOfBackups(), isCreateFolder(), false));
				}
				case COPY: {
					String destinationFolder = determineDestination(pvl);
					return processAction(input, pvl, f -> FileSystemUtils.copyFile(fileSystem, f, destinationFolder, isOverwrite(), getNumberOfBackups(), isCreateFolder(), false));
				}
				case FORWARD: {
					F file=getFile(input, pvl);
					FileSystemUtils.checkSource(fileSystem, file, FileSystemAction.FORWARD);
					String destinationAddress = determineDestination(pvl);
					((IMailFileSystem<F,?>)fileSystem).forwardMail(file, destinationAddress);
					return null;
				}
				default:
					throw new FileSystemException("action ["+action+"] is not supported!");
			}
		} catch (Exception e) {
			throw new FileSystemException("unable to process ["+action+"] action for File [" + determineFilename(input, pvl) + "]", e);
		}
	}

	private Message processListAction(Message input, ParameterValueList pvl) throws FileSystemException, IOException, SAXException {
		String folder = arrangeFolder(determineInputFolderName(input, pvl));
		typeFilter = determineTypeFilter(pvl);

		MessageBuilder messageBuilder = new MessageBuilder();
		ArrayBuilder directoryBuilder = DocumentBuilderFactory.startArrayDocument(getOutputFormat(), "directory", "file", messageBuilder, true);
		try (directoryBuilder; Stream<F> stream = FileSystemUtils.getFilteredStream(fileSystem, folder, getWildcard(), getExcludeWildcard(), typeFilter)) {
			Iterator<F> it = stream.iterator();
			while (it.hasNext()) {
				F file = it.next();
				try (INodeBuilder nodeBuilder = directoryBuilder.addElement()) {
					FileSystemUtils.getFileInfo(fileSystem, file, nodeBuilder);
				}
			}
		}
		return messageBuilder.build();
	}

	@SuppressWarnings("unchecked") //Casts to the required FileSystem type
	private Message createFile(@Nonnull Message input, ParameterValueList pvl, InputStream contents) throws FileSystemException, IOException {
		F file = getFileAndCreateFolder(input, pvl);
		if (fileSystem.exists(file)) {
			FileSystemUtils.prepareDestination((IWritableFileSystem<F>)fileSystem, file, isOverwrite(), getNumberOfBackups(), FileSystemAction.WRITE);
			file = getFile(input, pvl); // re-obtain the file, as the object itself may have changed because of the rollover
		}

		// Creates a file with custom file attributes if the fileSystem supports it and there are customFileAttributes to set
		if (hasCustomFileAttributes && fileSystem instanceof ISupportsCustomFileAttributes<?> cfa) {
			((ISupportsCustomFileAttributes<F>)fileSystem).createFile(file, contents, cfa.getCustomFileAttributes(pvl));
		} else {
			((IWritableFileSystem<F>)fileSystem).createFile(file, contents);
		}

		return new Message(FileSystemUtils.getFileInfo(fileSystem, file, getOutputFormat()));
	}

	private interface FileAction<F> {
		F execute(F f) throws FileSystemException;
	}

	/**
	 * Helper method to process delete, move and copy actions.
	 * @throws FileSystemException
	 * @throws SAXException
	 */
	private Message processAction(Message input, ParameterValueList pvl, FileAction<F> action) throws FileSystemException, IOException, SAXException {
		if(StringUtils.isNotEmpty(getWildcard()) || StringUtils.isNotEmpty(getExcludeWildcard())) {
			String folder = arrangeFolder(determineInputFolderName(input, pvl));
			MessageBuilder messageBuilder = new MessageBuilder();
			try (ArrayBuilder directoryBuilder = DocumentBuilderFactory.startArrayDocument(getOutputFormat(), action+"FilesList", "file", messageBuilder, true)) {
				try(Stream<F> stream = FileSystemUtils.getFilteredStream(fileSystem, folder, getWildcard(), getExcludeWildcard(), TypeFilter.FILES_ONLY)) {
					Iterator<F> it = stream.iterator();
					while(it.hasNext()) {
						F file = it.next();
						try (INodeBuilder nodeBuilder = directoryBuilder.addElement()){
							FileSystemUtils.getFileInfo(fileSystem, file, nodeBuilder);
							action.execute(file);
						}
					}
				}
			}
			deleteEmptyFolder(folder);
			return messageBuilder.build();
		}
		F file=getFile(input, pvl);
		F resultFile = action.execute(file);
		deleteEmptyFolder(file);
		return resultFile!=null ? new Message(fileSystem.getName(resultFile)) : Message.nullMessage();
	}

	private String arrangeFolder(String determinedFolderName) throws FileSystemException {
		if (determinedFolderName!=null && !determinedFolderName.equals(getInputFolder()) && !fileSystem.folderExists(determinedFolderName)) {
			if (isCreateFolder()) {
				fileSystem.createFolder(determinedFolderName);
			} else {
				F file = fileSystem.toFile(determinedFolderName);
				if (file!=null && fileSystem.exists(file)) {
					throw new FileNotFoundException("folder ["+determinedFolderName+"], does not exist as a folder, but is a file");
				}
				throw new FolderNotFoundException("folder ["+determinedFolderName+"], does not exist");
			}
		}
		return determinedFolderName;
	}

	private InputStream getContents(Message input, ParameterValueList pvl, String charset) throws IOException {
		final Message message;
		if (pvl != null && pvl.contains(PARAMETER_CONTENTS1)) {
			message = pvl.get(PARAMETER_CONTENTS1).asMessage();
		} else {
			message = input;
		}

		InputStream is = message.asInputStream(charset);
		if(isWriteLineSeparator()) {
			return new SequenceInputStream(is, new ByteArrayInputStream(eolArray));
		}
		return is;
	}

	private void deleteEmptyFolder(F f) throws FileSystemException {
		if(isDeleteEmptyFolder()) {
			deleteEmptyFolder(fileSystem.getParentFolder(f));
		}
	}

	private void deleteEmptyFolder(String folder) throws FileSystemException {
		if(isDeleteEmptyFolder()) {
			boolean isEmpty = false;
			try (DirectoryStream<F> stream = fileSystem.list(folder, TypeFilter.FILES_ONLY)) {
				isEmpty = !stream.iterator().hasNext();
			} catch(IOException e) {
				throw new FileSystemException("Cannot delete folder ["+folder+"]");
			} finally {
				if(isEmpty) {
					fileSystem.removeFolder(folder, false);
				}
			}
		}
	}

	protected void addActions(List<FileSystemAction> specificActions) {
		actions.addAll(specificActions);
	}

	/** If parameter [{@value #PARAMETER_ACTION}] is set, then the attribute action value will be overridden with the value of the parameter. */
	public void setAction(FileSystemAction action) {
		this.action = action;
	}

	/** Folder that is scanned for files when action={@value #ACTION_LIST}. When not set, the root is scanned */
	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}

	/**
	 * If <code>true</code>: if a non-existing folder is part of the fileName, it will be created.
	 * @ff.default false
	 */
	public void setCreateFolder(boolean createFolder) {
		this.createFolder = createFolder;
	}

	/**
	 * If set <code>true</code>, for actions {@value #ACTION_CREATE}, {@value #ACTION_WRITE1}, {@value #ACTION_MOVE}, {@value #ACTION_COPY} or {@value #ACTION_RENAME}, the destination file is overwritten if it already exists
	 * @ff.default false
	 */
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	/** Filename to operate on. If not set, the parameter {@value #PARAMETER_FILENAME} is used. If that is not set either, the input is used */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Destination for {@value #ACTION_MOVE}, {@value #ACTION_COPY} or {@value #ACTION_RENAME}. If not set, the parameter {@value #PARAMETER_DESTINATION} is used. If that is not set either, the input is used
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}


	/**
	 * For action={@value #ACTION_APPEND}: If set to a positive number, the file is rotated each day, and this number of files is kept. The inputFolder must point to the directory where the file resides
	 * @ff.default 0
	 */
	public void setRotateDays(int rotateDays) {
		this.rotateDays = rotateDays;
	}

	/**
	 * For action={@value #ACTION_APPEND}: If set to a positive number, the file is rotated when it has reached the specified size, and the number of files specified in numberOfBackups is kept. Size is specified in plain bytes, suffixes like 'K', 'M' or 'G' are not recognized. The inputFolder must point to the directory where the file resides
	 * @ff.default 0
	 */
	public void setRotateSize(int rotateSize) {
		this.rotateSize = rotateSize;
	}

	/**
	 * For the actions {@value #ACTION_WRITE1} and {@value #ACTION_APPEND}, with rotateSize>0: the number of backup files that is kept. The inputFolder must point to the directory where the file resides
	 * @ff.default 0
	 */
	public void setNumberOfBackups(int numberOfBackups) {
		this.numberOfBackups = numberOfBackups;
	}

	/**
	 * Filter of files to look for in inputFolder e.g. '*.inp'. Works with actions {@value #ACTION_MOVE}, {@value #ACTION_COPY}, {@value #ACTION_DELETE} and {@value #ACTION_LIST}
	 */
	public void setWildcard(String wildcard) {
		this.wildcard = wildcard;
	}

	/**
	 * Filter of files to be excluded when looking in inputFolder. Works with actions {@value #ACTION_MOVE}, {@value #ACTION_COPY}, {@value #ACTION_DELETE} and {@value #ACTION_LIST}
	 */
	public void setExcludeWildcard(String excludeWildcard) {
		this.excludeWildcard = excludeWildcard;
	}

	/** If set to <code>true</code> then the folder and the content of the non empty folder will be deleted. */
	public void setRemoveNonEmptyFolder(boolean removeNonEmptyFolder) {
		this.removeNonEmptyFolder = removeNonEmptyFolder;
	}

	/** If set to <code>true</code> then the system specific line separator will be appended to the file after executing the action. Works with actions {@value #ACTION_WRITE1} and {@value #ACTION_APPEND}
	 * @ff.default false
	 */
	public void setWriteLineSeparator(boolean writeLineSeparator) {
		this.writeLineSeparator = writeLineSeparator;
	}

	/** Charset to be used for {@value #ACTION_READ1} and {@value #ACTION_WRITE1} action */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	/** If set to true then the folder will be deleted if it is empty after processing the action. Works with actions {@value #ACTION_DELETE}, {@value #ACTION_READ_DELETE} and {@value #ACTION_MOVE} */
	public void setDeleteEmptyFolder(boolean deleteEmptyFolder) {
		this.deleteEmptyFolder = deleteEmptyFolder;
	}

	/**
	 * Sets the outputFormat. This ignored when reading a file. Is applicable to actions which return information about file(s). Relevant for:
	 * 'info', 'list', 'append', 'move', 'delete' and 'copy' actions.
	 * @ff.default XML
	 */
	public void setOutputFormat(DocumentFormat outputFormat) {
		this.outputFormat = outputFormat;
	}

	/**
	 * Filter for action <code>list</code>. Specify <code>FILES_ONLY</code>, <code>FOLDERS_ONLY</code> or <code>FILES_AND_FOLDERS</code>.
	 * @ff.default FILES_ONLY
	 */
	public void setTypeFilter(TypeFilter typeFilter) {
		this.typeFilter = typeFilter;
	}
}
