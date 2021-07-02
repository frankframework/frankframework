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

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.IOutputStreamingSupport;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Worker class for {@link FileSystemPipe} and {@link FileSystemSender}.
 * 
 * <table align="top" border="1">
 * <tr><th>Action</th><th>Description</th><th>Configuration</th></tr>
 * <tr><td>list</td><td>list files in a folder/directory</td><td>folder, taken from first available of:<ol><li>attribute <code>inputFolder</code></li><li>parameter <code>inputFolder</code></li><li>input message</li></ol></td></tr>
 * <tr><td>info</td><td>show info about a single file</td><td>filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message</li><li>root folder</li></ol></td></tr>
 * <tr><td>read</td><td>read a file, returns an InputStream</td><td>filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message</td><td>&nbsp;</td></tr>
 * <tr><td>readDelete</td><td>like read, but deletes the file after it has been read</td><td>filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message</td><td>&nbsp;</td></tr>
 * <tr><td>move</td><td>move a file to another folder</td><td>filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message<br/>destination: taken from attribute <code>destination</code> or parameter <code>destination</code></td></tr>
 * <tr><td>copy</td><td>copy a file to another folder</td><td>filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message<br/>destination: taken from attribute <code>destination</code> or parameter <code>destination</code></td></tr>
 * <tr><td>delete</td><td>delete a file</td><td>filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message</td><td>&nbsp;</td></tr>
 * <tr><td>mkdir</td><td>create a folder/directory</td><td>folder, taken from first available of:<ol><li>attribute <code>inputFolder</code></li><li>parameter <code>inputFolder</code></li><li>input message</li></ol></td><td>&nbsp;</td></tr>
 * <tr><td>rmdir</td><td>remove a folder/directory</td><td>folder, taken from first available of:<ol><li>attribute <code>inputFolder</code></li><li>parameter <code>inputFolder</code></li><li>input message</li></ol></td><td>&nbsp;</td></tr>
 * <tr><td>write</td><td>write contents to a file<td>
 *  filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message<br/>
 *  parameter <code>contents</code>: contents as either Stream, Bytes or String<br/>
 *  At least one of the parameters must be specified.<br/>
 *  The missing parameter defaults to the input message.<br/>
 *  For streaming operation, the parameter <code>filename</code> must be specified.
 *  </td><td>&nbsp;</td></tr>
 * <tr><td>append</td><td>append contents to a file<br/>(only for filesystems that support 'append')<td>
 *  filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message<br/>
 *  parameter <code>contents</code>: contents as either Stream, Bytes or String<br/>
 *  At least one of the parameters must be specified.<br/>
 *  The missing parameter defaults to the input message.<br/>
 *  For streaming operation, the parameter <code>filename</code> must be specified.
 *  </td><td>&nbsp;</td></tr>
 * <tr><td>rename</td><td>change the name of a file</td><td>filename: taken from parameter <code>filename</code> or input message<br/>destination: taken from attribute <code>destination</code> or parameter <code>destination</code></td></tr>
 * <tr><td>forward</td><td>(for MailFileSystems only:) forward an existing file to an email address</td><td>filename: taken from parameter <code>filename</code> or input message<br/>destination (an email address in this case): taken from attribute <code>destination</code> or parameter <code>destination</code></td></tr>
 * <table>
 * 
 * @author Gerrit van Brakel
 */
public class FileSystemActor<F, FS extends IBasicFileSystem<F>> implements IOutputStreamingSupport {
	protected Logger log = LogUtil.getLogger(this);

	public final String ACTION_LIST="list";
	public final String ACTION_INFO="info";
	public final String ACTION_READ1="read";
	public final String ACTION_READ2="download";
	public final String ACTION_READ_DELETE="readDelete";
	public final String ACTION_MOVE="move";
	public final String ACTION_COPY="copy";
	public final String ACTION_DELETE="delete";
	public final String ACTION_MKDIR="mkdir";
	public final String ACTION_RMDIR="rmdir";
	public final String ACTION_WRITE1="write";
	public final String ACTION_WRITE2="upload";
	public final String ACTION_APPEND="append";
	public final String ACTION_RENAME="rename";
	public final String ACTION_FORWARD="forward";

	public final String PARAMETER_ACTION="action";
	public final String PARAMETER_CONTENTS1="contents";
	public final String PARAMETER_CONTENTS2="file";
	public final String PARAMETER_FILENAME="filename";
	public final String PARAMETER_INPUTFOLDER="inputFolder";	// folder for actions list, mkdir and rmdir. This is a sub folder of baseFolder
	public final String PARAMETER_DESTINATION="destination";	// destination for action rename and move
	
	public final String BASE64_ENCODE="encode";
	public final String BASE64_DECODE="decode";
	
	public final String[] ACTIONS_BASIC= {ACTION_LIST, ACTION_INFO, ACTION_READ1, ACTION_READ2, ACTION_READ_DELETE, ACTION_MOVE, ACTION_COPY, ACTION_DELETE, ACTION_MKDIR, ACTION_RMDIR};
	public final String[] ACTIONS_WRITABLE_FS= {ACTION_WRITE1, ACTION_WRITE2, ACTION_APPEND, ACTION_RENAME};
	public final String[] ACTIONS_MAIL_FS= {ACTION_FORWARD};

	private @Getter String action;
	private @Getter String filename;
	private @Getter String destination;
	private @Getter String inputFolder; // folder for action=list
	private @Getter boolean createFolder; // for action move, rename and list

	private @Getter String base64;
	private @Getter int rotateDays=0;
	private @Getter int rotateSize=0;
	private @Getter boolean overwrite=false;
	private @Getter int numberOfBackups=0;
	private @Getter String wildcard=null;
	private @Getter String excludeWildcard=null;
	private @Getter boolean removeNonEmptyFolder=false;
	private @Getter boolean writeLineSeparator=false;
	private @Getter String charset;

	private Set<String> actions = new LinkedHashSet<String>(Arrays.asList(ACTIONS_BASIC));
	
	private INamedObject owner;
	private FS fileSystem;
	private ParameterList parameterList;

	private byte[] eolArray=null;

	
	public void configure(FS fileSystem, ParameterList parameterList, IConfigurable owner) throws ConfigurationException {
		this.owner=owner;
		this.fileSystem=fileSystem;
		this.parameterList=parameterList;
		
		if (fileSystem instanceof IWritableFileSystem) {
			actions.addAll(Arrays.asList(ACTIONS_WRITABLE_FS));
		}
		if (fileSystem instanceof IMailFileSystem) {
			actions.addAll(Arrays.asList(ACTIONS_MAIL_FS));
		}

		if (parameterList!=null && parameterList.findParameter(PARAMETER_CONTENTS2) != null && parameterList.findParameter(PARAMETER_CONTENTS1) == null) {
			ConfigurationWarnings.add(owner, log, "parameter ["+PARAMETER_CONTENTS2+"] has been replaced with ["+PARAMETER_CONTENTS1+"]");
			parameterList.findParameter(PARAMETER_CONTENTS2).setName(PARAMETER_CONTENTS1);
		}

		if (StringUtils.isNotEmpty(getAction())) {
			if (getAction().equals(ACTION_READ2)) {
				ConfigurationWarnings.add(owner, log, "action ["+ACTION_READ2+"] has been replaced with ["+ACTION_READ1+"]");
				setAction(ACTION_READ1);
			}
			if (getAction().equals(ACTION_WRITE2)) {
				ConfigurationWarnings.add(owner, log, "action ["+ACTION_WRITE2+"] has been replaced with ["+ACTION_WRITE1+"]");
				setAction(ACTION_WRITE1);
			}
			checkConfiguration(getAction());
		} else if (parameterList == null || parameterList.findParameter(PARAMETER_ACTION) == null) {
			throw new ConfigurationException(ClassUtils.nameOf(owner)+" ["+owner.getName()+"]: either attribute [action] or parameter ["+PARAMETER_ACTION+"] must be specified");
		}

		if (StringUtils.isNotEmpty(getBase64()) && !(getBase64().equals(BASE64_ENCODE) || getBase64().equals(BASE64_DECODE))) {
			throw new ConfigurationException("attribute 'base64' can have value '"+BASE64_ENCODE+"' or '"+BASE64_DECODE+"' or can be left empty");
		}

		if (StringUtils.isNotEmpty(getInputFolder()) && parameterList!=null && parameterList.findParameter(PARAMETER_INPUTFOLDER) != null) {
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
		eolArray = System.getProperty("line.separator").getBytes();
	}

	private void checkConfiguration(String action) throws ConfigurationException {
		if (!actions.contains(action))
			throw new ConfigurationException(ClassUtils.nameOf(owner)+" ["+owner.getName()+"]: unknown or invalid action [" + action + "] supported actions are " + actions.toString() + "");

		//Check if necessary parameters are available
		actionRequiresAtLeastOneOfTwoParametersOrAttribute(owner, parameterList, action, ACTION_WRITE1,  PARAMETER_CONTENTS1, PARAMETER_FILENAME, "filename", getFilename());
		actionRequiresAtLeastOneOfTwoParametersOrAttribute(owner, parameterList, action, ACTION_MOVE,    PARAMETER_DESTINATION, null, "destination", getDestination());
		actionRequiresAtLeastOneOfTwoParametersOrAttribute(owner, parameterList, action, ACTION_COPY,    PARAMETER_DESTINATION, null, "destination", getDestination());
		actionRequiresAtLeastOneOfTwoParametersOrAttribute(owner, parameterList, action, ACTION_RENAME,  PARAMETER_DESTINATION, null, "destination", getDestination());
		actionRequiresAtLeastOneOfTwoParametersOrAttribute(owner, parameterList, action, ACTION_FORWARD, PARAMETER_DESTINATION, null, "destination", getDestination());
	}
	
//	protected void actionRequiresParameter(INamedObject owner, ParameterList parameterList, String action, String parameter) throws ConfigurationException {
//		if (getAction().equals(action) && (parameterList == null || parameterList.findParameter(parameter) == null)) {
//			throw new ConfigurationException("the "+action+" action requires the parameter ["+parameter+"] to be present");
//		}
//		actionRequiresAtLeastOneOfTwoParametersOrAttribute(owner, parameterList, action, parameter, null, null, null);
//	}

	protected void actionRequiresAtLeastOneOfTwoParametersOrAttribute(INamedObject owner, ParameterList parameterList, String configuredAction, String action, String parameter1, String parameter2, String attributeName, String attributeValue) throws ConfigurationException {
		if (configuredAction.equals(action)) {
			boolean parameter1Set = parameterList != null && parameterList.findParameter(parameter1) != null;
			boolean parameter2Set = parameterList != null && parameterList.findParameter(parameter2) != null;
			boolean attributeSet  = StringUtils.isNotEmpty(attributeValue);
			if (!parameter1Set && !parameter2Set && !attributeSet) {
				throw new ConfigurationException(ClassUtils.nameOf(owner)+" ["+owner.getName()+"]: the "+action+" action requires the parameter ["+parameter1+"] "+(parameter2!=null?"or parameter ["+parameter2+"] ":"")+(attributeName!=null?"or the attribute ["+attributeName+"] ": "")+"to be present");
			}
		}
	}

	public void open() throws FileSystemException {
		if (StringUtils.isNotEmpty(getInputFolder()) && !fileSystem.folderExists(getInputFolder()) && !ACTION_MKDIR.equals(getAction())) {
			if (isCreateFolder()) {
				log.debug("creating inputFolder ["+getInputFolder()+"]");
				fileSystem.createFolder(getInputFolder());
			} else {
				F file = fileSystem.toFile(getInputFolder());
				if (file!=null && fileSystem.exists(file)) {
					throw new FileNotFoundException("inputFolder ["+getInputFolder()+"], canonical name ["+fileSystem.getCanonicalName(fileSystem.toFile(getInputFolder()))+"], does not exist as a folder, but is a file");
				} else {
					throw new FileNotFoundException("inputFolder ["+getInputFolder()+"], canonical name ["+fileSystem.getCanonicalName(fileSystem.toFile(getInputFolder()))+"], does not exist");
				}
			}
		}
	}
	
//	@Override
//	public void close() throws SenderException {
//		try {
//			getFileSystem().close();
//		} catch (FileSystemException e) {
//			throw new SenderException("Cannot close fileSystem",e);
//		}
//	}
	
	private String determineFilename(Message input, ParameterValueList pvl) throws FileSystemException {
		if (StringUtils.isNotEmpty(getFilename())) {
			return getFilename();
		}
		if (pvl!=null && pvl.containsKey(PARAMETER_FILENAME)) {
			return pvl.getParameterValue(PARAMETER_FILENAME).asStringValue(null);
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
		if (pvl!=null && pvl.containsKey(PARAMETER_DESTINATION)) {
			String destination = pvl.getParameterValue(PARAMETER_DESTINATION).asStringValue(null);
			if (StringUtils.isEmpty(destination)) {
				throw new FileSystemException("parameter ["+PARAMETER_DESTINATION+"] does not specify destination");
			}
			return destination;
		}
		throw new FileSystemException("no destination specified");
	}

	private F getFile(Message input, ParameterValueList pvl) throws FileSystemException {
		String filename=determineFilename(input, pvl);
		return fileSystem.toFile(filename);
	}
	
	private String determineInputFoldername(Message input, ParameterValueList pvl) throws FileSystemException {
		if (StringUtils.isNotEmpty(getInputFolder())) {
			return getInputFolder();
		}
		if (pvl!=null && pvl.containsKey(PARAMETER_INPUTFOLDER)) {
			return pvl.getParameterValue(PARAMETER_INPUTFOLDER).asStringValue(null);
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
	
	public Object doAction(Message input, ParameterValueList pvl, PipeLineSession session) throws FileSystemException, TimeOutException {
		try {
			if(input != null) {
				input.closeOnCloseOf(session); // don't know if the input will be used
			}

			String action;
			if (pvl != null && pvl.containsKey(PARAMETER_ACTION)) {
				action = pvl.getParameterValue(PARAMETER_ACTION).asStringValue(getAction());
				if(StringUtils.isEmpty(action)) {
					throw new FileSystemException("unable to resolve the value of parameter ["+PARAMETER_ACTION+"]");
				}
				checkConfiguration(action);
			} else {
				action = getAction();
			}

			if (action.equalsIgnoreCase(ACTION_DELETE)) {
				return processAction(input, pvl, f -> { fileSystem.deleteFile(f); return f; });
			} else if (action.equalsIgnoreCase(ACTION_INFO)) {
				F file=getFile(input, pvl);
				FileSystemUtils.checkSource(fileSystem, file, "inspect");
				return getFileAsXmlBuilder(file, "file").toXML();
			} else if (action.equalsIgnoreCase(ACTION_READ1)) {
				F file=getFile(input, pvl);
				Message in = fileSystem.readFile(file, getCharset());
				if (StringUtils.isNotEmpty(getBase64())) {
					return new Base64InputStream(in.asInputStream(), getBase64().equals(BASE64_ENCODE));
				}
				return in;
			} else if (action.equalsIgnoreCase(ACTION_READ_DELETE)) {
				F file=getFile(input, pvl);
				InputStream in = new FilterInputStream(fileSystem.readFile(file, getCharset()).asInputStream()) {

					@Override
					public void close() throws IOException {
						super.close();
						try {
							fileSystem.deleteFile(file);
						} catch (FileSystemException e) {
							throw new IOException("Could not delete file", e);
						}
					}

					@Override
					protected void finalize() throws Throwable {
						try {
							close();
						} catch (Exception e) {
							log.warn("Could not close file", e);
						}
						super.finalize();
					}
					
				};
				if (StringUtils.isNotEmpty(getBase64())) {
					in = new Base64InputStream(in, getBase64().equals(BASE64_ENCODE));
				}
				return in;
			} else if (action.equalsIgnoreCase(ACTION_LIST)) {
				String folder = arrangeFolder(determineInputFoldername(input, pvl));
				XmlBuilder dirXml = new XmlBuilder("directory");
				try(Stream<F> stream = FileSystemUtils.getFilteredStream(fileSystem, folder, getWildcard(), getExcludeWildcard())) {
					int count = 0;
					Iterator<F> it = stream.iterator();
					while(it.hasNext()) {
						F file = it.next();
						dirXml.addSubElement(getFileAsXmlBuilder(file, "file"));
						count++;
					}
					dirXml.addAttribute("count", count);
				}
				return dirXml.toXML();

			} else if (action.equalsIgnoreCase(ACTION_WRITE1)) {
				F file=getFile(input, pvl);
				if (fileSystem.exists(file)) {
					FileSystemUtils.prepareDestination((IWritableFileSystem<F>)fileSystem, file, isOverwrite(), getNumberOfBackups(), ACTION_WRITE1);
					file=getFile(input, pvl); // reobtain the file, as the object itself may have changed because of the rollover
				}
				try (OutputStream out = ((IWritableFileSystem<F>)fileSystem).createFile(file)) {
					writeContentsToFile(out, input, pvl);
				}
				return getFileAsXmlBuilder(file, "file").toXML();
			} else if (action.equalsIgnoreCase(ACTION_APPEND)) {
				F file=getFile(input, pvl);
				if (getRotateDays()>0 && fileSystem.exists(file)) {
					FileSystemUtils.rolloverByDay((IWritableFileSystem<F>)fileSystem, file, getInputFolder(), getRotateDays());
					file=getFile(input, pvl); // reobtain the file, as the object itself may have changed because of the rollover
				}
				if (getRotateSize()>0 && fileSystem.exists(file)) {
					FileSystemUtils.rolloverBySize((IWritableFileSystem<F>)fileSystem, file, getRotateSize(), getNumberOfBackups());
					file=getFile(input, pvl); // reobtain the file, as the object itself may have changed because of the rollover
				}
				try (OutputStream out = ((IWritableFileSystem<F>)fileSystem).appendFile(file)) {
					writeContentsToFile(out, input, pvl);
				}
				return getFileAsXmlBuilder(file, "file").toXML();
			} else if (action.equalsIgnoreCase(ACTION_MKDIR)) {
				String folder = determineInputFoldername(input, pvl);
				fileSystem.createFolder(folder);
				return folder;
			} else if (action.equalsIgnoreCase(ACTION_RMDIR)) {
				String folder = determineInputFoldername(input, pvl);
				fileSystem.removeFolder(folder, isRemoveNonEmptyFolder());
				return folder;
			} else if (action.equalsIgnoreCase(ACTION_RENAME)) {
				F source=getFile(input, pvl);
				String destinationName = determineDestination(pvl);
				F destination;
				if (destinationName.contains("/") || destinationName.contains("\\")) {
					destination = fileSystem.toFile(destinationName);
				} else {
					String sourceName = fileSystem.getCanonicalName(source);
					File sourceAsFile = new File(sourceName);
					String folderPath = sourceAsFile.getParent();
					destination = fileSystem.toFile(folderPath,destinationName);
				}
				F renamed = FileSystemUtils.renameFile((IWritableFileSystem<F>)fileSystem, source, destination, isOverwrite(), getNumberOfBackups());
				return fileSystem.getName(renamed);
			} else if (action.equalsIgnoreCase(ACTION_MOVE)) {
				String destinationFolder = determineDestination(pvl);
				return processAction(input, pvl, f -> FileSystemUtils.moveFile(fileSystem, f, destinationFolder, isOverwrite(), getNumberOfBackups(), isCreateFolder()));
			} else if (action.equalsIgnoreCase(ACTION_COPY)) {
				String destinationFolder = determineDestination(pvl);
				return processAction(input, pvl, f -> FileSystemUtils.copyFile(fileSystem, f, destinationFolder, isOverwrite(), getNumberOfBackups(), isCreateFolder()));
			} else if (action.equalsIgnoreCase(ACTION_FORWARD)) {
				F file=getFile(input, pvl);
				FileSystemUtils.checkSource(fileSystem, file, "forward");
				String destinationAddress = determineDestination(pvl);
				((IMailFileSystem<F,?>)fileSystem).forwardMail(file, destinationAddress);
				return null;
			}
		} catch (Exception e) {
			throw new FileSystemException("unable to process ["+action+"] action for File [" + determineFilename(input, pvl) + "]", e);
		}

		return input;
	}

	
	private interface FileAction<F> {
		public F execute(F f) throws FileSystemException;
	}
	/**
	 * Helper method to process delete, move and copy actions.
	 * @throws FileSystemException 
	 * @throws IOException 
	 */
	private String processAction(Message input, ParameterValueList pvl, FileAction<F> action) throws FileSystemException, IOException {
		if(StringUtils.isNotEmpty(getWildcard()) || StringUtils.isNotEmpty(getExcludeWildcard())) { 
			String folder = arrangeFolder(determineInputFoldername(input, pvl));
			XmlBuilder dirXml = new XmlBuilder(getAction()+"FilesList");
			try(Stream<F> stream = FileSystemUtils.getFilteredStream(fileSystem, folder, getWildcard(), getExcludeWildcard())) {
				Iterator<F> it = stream.iterator();
				while(it.hasNext()) {
					F file = it.next();
					XmlBuilder item = getFileAsXmlBuilder(file, "file");
					if(action.execute(file) != null) {
						dirXml.addSubElement(item);
					}
				}
			}
			return dirXml.toXML();
		} else {
			F file=getFile(input, pvl);
			return fileSystem.getName(action.execute(file));
		}
	}

	private String arrangeFolder(String determinedFolderName) throws FileSystemException {
		if (determinedFolderName!=null && !determinedFolderName.equals(getInputFolder()) && !fileSystem.folderExists(determinedFolderName)) {
			if (isCreateFolder()) {
				fileSystem.createFolder(determinedFolderName);
			} else {
				F file = fileSystem.toFile(determinedFolderName);
				if (file!=null && fileSystem.exists(file)) {
					throw new FileNotFoundException("folder ["+determinedFolderName+"], does not exist as a folder, but is a file");
				} else {
					throw new FileNotFoundException("folder ["+determinedFolderName+"], does not exist");
				}
			}
		}
		return determinedFolderName;
	}

	private void writeContentsToFile(OutputStream out, Message input, ParameterValueList pvl) throws IOException, FileSystemException {
		Object contents;
		if (pvl!=null && pvl.containsKey(PARAMETER_CONTENTS1)) {
			 contents=pvl.getParameterValue(PARAMETER_CONTENTS1).getValue();
		} else {
			contents=input;
		}
		if (StringUtils.isNotEmpty(getBase64())) {
			out = new Base64OutputStream(out, getBase64().equals(BASE64_ENCODE));
		}
		if (contents instanceof Message) {
			Misc.streamToStream(((Message)contents).asInputStream(), out);
		} else if (contents instanceof InputStream) {
			Misc.streamToStream((InputStream)contents, out);
		} else if (contents instanceof byte[]) {
			out.write((byte[])contents);
		} else if (contents instanceof String) {
			out.write(((String) contents).getBytes(StringUtils.isNotEmpty(getCharset()) ? getCharset() : StreamUtil.DEFAULT_INPUT_STREAM_ENCODING));
		} else {
			throw new FileSystemException("expected Message, InputStream, ByteArray or String but got [" + contents.getClass().getName() + "] instead");
		}
		if(isWriteLineSeparator()) {
			out.write(eolArray);
		}
	}
	
	
	protected boolean canProvideOutputStream() {
		return (ACTION_WRITE1.equals(getAction()) || ACTION_APPEND.equals(getAction())) && parameterList.findParameter(PARAMETER_FILENAME)!=null;
	}

	@Override
	public boolean supportsOutputStreamPassThrough() {
		return false;
	}

	@SuppressWarnings("resource")
	@Override
	public MessageOutputStream provideOutputStream(PipeLineSession session, IForwardTarget next) throws StreamingException {
		if (!canProvideOutputStream()) {
			return null;
		}
		ParameterValueList pvl=null;
		
		try {
			if (parameterList != null) {
				pvl = parameterList.getValues(null, session);
			}
		} catch (ParameterException e) {
			throw new StreamingException("caught exception evaluating parameters", e);
		}
		try {
			F file=getFile(null, pvl);
			OutputStream out;
			if (ACTION_APPEND.equals(getAction())) {
				out = ((IWritableFileSystem<F>)fileSystem).appendFile(file);
			} else {
				out = ((IWritableFileSystem<F>)fileSystem).createFile(file);
			}
			MessageOutputStream stream = new MessageOutputStream(owner, out, next);
			stream.setResponse(new Message(getFileAsXmlBuilder(file, "file").toXML()));
			return stream;
		} catch (FileSystemException | IOException e) {
			throw new StreamingException("cannot obtain OutputStream", e);
		}
	}


	public XmlBuilder getFileAsXmlBuilder(F f, String rootElementName) throws FileSystemException {
		XmlBuilder fileXml = new XmlBuilder(rootElementName);

		String name = fileSystem.getName(f);
		fileXml.addAttribute("name", name);
		if (!".".equals(name) && !"..".equals(name)) {
			long fileSize = fileSystem.getFileSize(f);
			fileXml.addAttribute("size", "" + fileSize);
			fileXml.addAttribute("fSize", "" + Misc.toFileSize(fileSize, true));
			try {
				fileXml.addAttribute("canonicalName", fileSystem.getCanonicalName(f));
			} catch (Exception e) {
				log.warn("cannot get canonicalName for file [" + name + "]", e);
				fileXml.addAttribute("canonicalName", name);
			}
			// Get the modification date of the file
			Date modificationDate = fileSystem.getModificationTime(f);
			//add date
			if (modificationDate != null) {
				String date = DateUtils.format(modificationDate, DateUtils.FORMAT_DATE);
				fileXml.addAttribute("modificationDate", date);

				// add the time
				String time = DateUtils.format(modificationDate, DateUtils.FORMAT_TIME_HMS);
				fileXml.addAttribute("modificationTime", time);
			}
		}
		
		Map<String, Object> additionalParameters = fileSystem.getAdditionalFileProperties(f);
		if(additionalParameters != null) {
			for (Map.Entry<String, Object> attribute : additionalParameters.entrySet()) {
				fileXml.addAttribute(attribute.getKey(), String.valueOf(attribute.getValue()));
			}
		}

		return fileXml;
	}



	protected void addActions(List<String> specificActions) {
		actions.addAll(specificActions);
	}

	@IbisDoc({"1", "Possible values: "+ACTION_LIST+", "+ACTION_INFO+", "+ACTION_READ1+", "+ACTION_READ_DELETE+", "+ACTION_MOVE+", "+ACTION_COPY+", "+ACTION_DELETE+", "+ACTION_MKDIR+", "+ACTION_RMDIR+", "+ACTION_WRITE1+", "+ACTION_APPEND+", "+ACTION_RENAME+". If parameter ["+PARAMETER_ACTION+"] is set, then the attribute action value will be overridden with the value of the parameter.", "" })
	public void setAction(String action) {
		this.action = action;
	}

	@IbisDoc({"2", "Folder that is scanned for files when action="+ACTION_LIST+". When not set, the root is scanned", ""})
	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}

	@IbisDoc({"3", "when set to <code>true</code>, the folder to move or copy to is created if it does not exist", "false"})
	public void setCreateFolder(boolean createFolder) {
		this.createFolder = createFolder;
	}

	@IbisDoc({"4", "when set to <code>true</code>, for actions "+ACTION_MOVE+", "+ACTION_COPY+" or "+ACTION_RENAME+", the destination file is overwritten if it already exists", "false"})
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	@IbisDoc({"5", "filename to operate on. When not set, the parameter "+PARAMETER_FILENAME+" is used. When that is not set either, the input is used", ""})
	public void setFilename(String filename) {
		this.filename = filename;
	}

	@IbisDoc({"5", "destination for "+ACTION_MOVE+", "+ACTION_COPY+" or "+ACTION_RENAME+". If not set, the parameter "+PARAMETER_DESTINATION+" is used. When that is not set either, the input is used", ""})
	public void setDestination(String destination) {
		this.destination = destination;
	}


	@IbisDoc({"6", "for action="+ACTION_APPEND+": when set to a positive number, the file is rotated each day, and this number of files is kept. The inputFolder must point to the directory where the file resides", "0"})
	public void setRotateDays(int rotateDays) {
		this.rotateDays = rotateDays;
	}

	@IbisDoc({"7", "for action="+ACTION_APPEND+": when set to a positive number, the file is rotated when it has reached the specified size, and the number of files specified in numberOfBackups is kept. Size is specified in plain bytes, suffixes like 'K', 'M' or 'G' are not recognized. The inputFolder must point to the directory where the file resides", "0"})
	public void setRotateSize(int rotateSize) {
		this.rotateSize = rotateSize;
	}

	@IbisDoc({"8", "for action="+ACTION_WRITE1+", and for action="+ACTION_APPEND+" with rotateSize>0: the number of backup files that is kept. The inputFolder must point to the directory where the file resides", "0"})
	public void setNumberOfBackups(int numberOfBackups) {
		this.numberOfBackups = numberOfBackups;
	}

	@IbisDoc({"9", "Can be set to 'encode' or 'decode' for actions "+ACTION_READ1+", "+ACTION_WRITE1+" and "+ACTION_APPEND+". When set the stream is base64 encoded or decoded, respectively", ""})
	@Deprecated
	public void setBase64(String base64) {
		this.base64 = base64;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'wildCard' has been renamed to 'wildcard'")
	public void setWildCard(String wildcard) {
		setWildcard(wildcard);
	}
	@IbisDoc({"10", "Filter of files to look for in inputFolder e.g. '*.inp'. Works with actions "+ACTION_MOVE+", "+ACTION_COPY+", "+ACTION_DELETE+" and "+ACTION_LIST, ""})
	public void setWildcard(String wildcard) {
		this.wildcard = wildcard;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'excludeWildCard' has been renamed to 'excludeWildcard'")
	public void setExcludeWildCard(String excludeWildcard) {
		setExcludeWildcard(excludeWildcard);
	}
	@IbisDoc({"11", "Filter of files to be excluded when looking in inputFolder. Works with actions "+ACTION_MOVE+", "+ACTION_COPY+", "+ACTION_DELETE+" and "+ACTION_LIST, ""})
	public void setExcludeWildcard(String excludeWildcard) {
		this.excludeWildcard = excludeWildcard;
	}

	@IbisDoc({"12", "If set to true then the folder and the content of the non empty folder will be deleted."})
	public void setRemoveNonEmptyFolder(boolean removeNonEmptyFolder) {
		this.removeNonEmptyFolder = removeNonEmptyFolder;
	}

	@IbisDoc({"13", "If set to true then the system specific line separator will be appended to the file after executing the action. Works with actions "+ACTION_WRITE1+", and for action="+ACTION_APPEND, "false"})
	public void setWriteLineSeparator(boolean writeLineSeparator) {
		this.writeLineSeparator = writeLineSeparator;
	}

	@IbisDoc({"14", "Charset to be used for read and write action"})
	public void setCharset(String charset) {
		this.charset = charset;
	}
}
