/*
   Copyright 2019 Integration Partners

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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.IOutputStreamingSupport;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Worker class for {@link FileSystemPipe} and {@link FileSystemSender}.
 * 
 * <table align="top">
 * <tr><th>Action</th><th>Description</th><th>Configuration</th></tr>
 * <tr><td>list</td><td>list files in a folder/directory</td><td>folder, taken from first available of:<ol><li>attribute <code>inputFolder</code></li><li>parameter <code>inputFolder</code></li><li>root folder</li></ol></td></tr>
 * <tr><td>read</td><td>read a file, returns an InputStream</td><td>filename: taken from parameter <code>filename</code> or input message</td><td>&nbsp;</td></tr>
 * <tr><td>move</td><td>move a file to another folder</td><td>filename: taken from parameter <code>filename</code> or input message<br/>parameter <code>destination</code></td></tr>
 * <tr><td>delete</td><td>delete a file</td><td>filename: taken from parameter <code>filename</code> or input message</td><td>&nbsp;</td></tr>
 * <tr><td>mkdir</td><td>create a folder/directory</td><td>folder: taken from parameter <code>foldername</code> or input message</td><td>&nbsp;</td></tr>
 * <tr><td>rmdir</td><td>remove a folder/directory</td><td>folder: taken from parameter <code>foldername</code> or input message</td><td>&nbsp;</td></tr>
 * <tr><td>write</td><td>write contents to a file<td>
 *  filename: taken from parameter <code>filename</code> or input message<br/>
 *  parameter <code>contents</code>: contents as either Stream, Bytes or String<br/>
 *  At least one of the parameters must be specified.<br/>
 *  The missing parameter defaults to the input message.<br/>
 *  For streaming operation, the parameter <code>filename</code> must be specified.
 *  </td><td>&nbsp;</td></tr>
 * <tr><td>append</td><td>append contents to a file<br/>(only for filesystems that support 'append')<td>
 *  filename: taken from parameter <code>filename</code> or input message<br/>
 *  parameter <code>contents</code>: contents as either Stream, Bytes or String<br/>
 *  At least one of the parameters must be specified.<br/>
 *  The missing parameter defaults to the input message.<br/>
 *  For streaming operation, the parameter <code>filename</code> must be specified.
 *  </td><td>&nbsp;</td></tr>
 * <tr><td>rename</td><td>change the name of a file</td><td>filename: taken from parameter <code>filename</code> or input message<br/>parameter <code>destination</code></td></tr>
 * <table>
 * 
 * @author Gerrit van Brakel
 */
public class FileSystemActor<F, FS extends IBasicFileSystem<F>> implements IOutputStreamingSupport {
	protected Logger log = LogUtil.getLogger(this);
	
	public final String ACTION_LIST="list";
	public final String ACTION_READ1="read";
	public final String ACTION_READ2="download";
	public final String ACTION_MOVE="move";
	public final String ACTION_DELETE="delete";
	public final String ACTION_MKDIR="mkdir";
	public final String ACTION_RMDIR="rmdir";
	public final String ACTION_WRITE1="write";
	public final String ACTION_WRITE2="upload";
	public final String ACTION_APPEND="append";
	public final String ACTION_RENAME="rename";
	
	public final String PARAMETER_CONTENTS1="contents";
	public final String PARAMETER_CONTENTS2="file";
	public final String PARAMETER_FILENAME="filename";
	public final String PARAMETER_INPUTFOLDER="inputFolder";	// folder for actions list, mkdir and rmdir. This is a sub folder of baseFolder
	public final String PARAMETER_DESTINATION="destination";	// destination for action rename and move
	
	public final String BASE64_ENCODE="encode";
	public final String BASE64_DECODE="decode";
	
	public final String[] ACTIONS_BASIC= {ACTION_LIST, ACTION_READ1, ACTION_READ2, ACTION_MOVE, ACTION_DELETE, ACTION_MKDIR, ACTION_RMDIR};
	public final String[] ACTIONS_WRITABLE_FS= {ACTION_WRITE1, ACTION_WRITE2, ACTION_APPEND, ACTION_RENAME};

	private String action;
	private String inputFolder; // folder for action=list

	private String base64;
	private int rotateDays=0;
	private int rotateSize=0;
	private int numberOfBackups=0;
	

	private Set<String> actions = new LinkedHashSet<String>(Arrays.asList(ACTIONS_BASIC));
	
	private FS fileSystem;
	private ParameterList parameterList;

	
	public void configure(FS fileSystem, ParameterList parameterList, INamedObject owner) throws ConfigurationException {
		this.fileSystem=fileSystem;
		this.parameterList=parameterList;
		if (fileSystem instanceof IWritableFileSystem) {
			actions.addAll(Arrays.asList(ACTIONS_WRITABLE_FS));
		}

		if (getAction() == null)
			throw new ConfigurationException("["+owner.getName()+"]: action must be specified");
		if (!actions.contains(getAction()))
			throw new ConfigurationException("["+owner.getName()+"]: unknown or invalid action [" + getAction() + "] supported actions are " + actions.toString() + "");

		if (getAction().equals(ACTION_READ2)) {
			ConfigurationWarnings.add(owner, log, "action ["+ACTION_READ2+"] has been replaced with ["+ACTION_READ1+"]");
			setAction(ACTION_READ1);
		}
		if (getAction().equals(ACTION_WRITE2)) {
			ConfigurationWarnings.add(owner, log, "action ["+ACTION_WRITE2+"] has been replaced with ["+ACTION_WRITE1+"]");
			setAction(ACTION_WRITE1);
		}
		
		if (StringUtils.isNotEmpty(getBase64())) {
			if (!(getBase64().equals(BASE64_ENCODE) || getBase64().equals(BASE64_DECODE))) {
				throw new ConfigurationException("attribute 'base64' can have value '"+BASE64_ENCODE+"' or '"+BASE64_DECODE+"' or can be left empty");
			}
		}
		
		if (parameterList!=null && parameterList.findParameter(PARAMETER_CONTENTS2) != null && parameterList.findParameter(PARAMETER_CONTENTS1) == null) {
			ConfigurationWarnings.add(owner, log, "parameter ["+PARAMETER_CONTENTS2+"] has been replaced with ["+PARAMETER_CONTENTS1+"]");
			parameterList.findParameter(PARAMETER_CONTENTS2).setName(PARAMETER_CONTENTS1);;
		}
		
		//Check if necessarily parameters are available
		actionRequiresAtLeastOneOfTwoParameters(parameterList, ACTION_WRITE1, PARAMETER_CONTENTS1, PARAMETER_FILENAME);
		actionRequiresParameter(parameterList, ACTION_MOVE, PARAMETER_DESTINATION);
		actionRequiresParameter(parameterList, ACTION_RENAME, PARAMETER_DESTINATION);
		
		if (StringUtils.isNotEmpty(getInputFolder()) && parameterList!=null && parameterList.findParameter(PARAMETER_INPUTFOLDER) != null) {
			ConfigurationWarnings.add(owner, log, "inputFolder configured via attribute [inputFolder] as well as via parameter ["+PARAMETER_INPUTFOLDER+"], parameter will be ignored");
		}
		
	}
	
	
	
	protected void actionRequiresParameter(ParameterList parameterList, String action, String parameter) throws ConfigurationException {
//		if (getAction().equals(action) && (parameterList == null || parameterList.findParameter(parameter) == null)) {
//			throw new ConfigurationException("the "+action+" action requires the parameter ["+parameter+"] to be present");
//		}
		actionRequiresAtLeastOneOfTwoParameters(parameterList, action, parameter, null);
	}

	protected void actionRequiresAtLeastOneOfTwoParameters(ParameterList parameterList, String action, String parameter1, String parameter2) throws ConfigurationException {
		if (getAction().equals(action)) {
			boolean parameter1Set = parameterList != null && parameterList.findParameter(parameter1) != null;
			boolean parameter2Set = parameterList != null && parameterList.findParameter(parameter2) != null;
			if (!parameter1Set && !parameter2Set) {
				throw new ConfigurationException("the "+action+" action requires the parameter ["+parameter1+"] "+(parameter2!=null?"or parameter ["+parameter2+"] ":"")+"to be present");
			}
		}
	}

	public void open() throws FileSystemException {
		if (StringUtils.isNotEmpty(getInputFolder())) {
			if (!fileSystem.folderExists(getInputFolder())) {
				throw new FileSystemException("inputFolder ["+getInputFolder()+"] does not exist");
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
//		if (StringUtils.isNotEmpty(configuredFileName)) {
//			return configuredFileName;
//		}
		if (pvl!=null && pvl.containsKey(PARAMETER_FILENAME)) {
			return pvl.getParameterValue(PARAMETER_FILENAME).asStringValue("");
		}
		try {
			return input.asString();
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
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
			return pvl.getParameterValue(PARAMETER_INPUTFOLDER).asStringValue("");
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
	
	public Object doAction(Message input, ParameterValueList pvl, IPipeLineSession session) throws FileSystemException, TimeOutException {
		try {
			if (action.equalsIgnoreCase(ACTION_DELETE)) {
				F file=getFile(input, pvl);
				fileSystem.deleteFile(file);
				return fileSystem.getName(file);
			} else if (action.equalsIgnoreCase(ACTION_READ1)) {
				F file=getFile(input, pvl);
				InputStream in = fileSystem.readFile(file);
				if (StringUtils.isNotEmpty(getBase64())) {
					in = new Base64InputStream(in, getBase64().equals(BASE64_ENCODE));
				}
				return in;
			} else if (action.equalsIgnoreCase(ACTION_LIST)) {
				String folder = determineInputFoldername(input, pvl);
				Iterator<F> fileList = fileSystem.listFiles(folder);
				int count = 0;
				XmlBuilder dirXml = new XmlBuilder("directory");
				while (fileList.hasNext()) {
					F fileObject = fileList.next();
					dirXml.addSubElement(getFileAsXmlBuilder(fileObject, "file"));
					count++;
				}
				dirXml.addAttribute("count", count);

				return dirXml.toXML();
			} else if (action.equalsIgnoreCase(ACTION_WRITE1)) {
				F file=getFile(input, pvl);
				if (getNumberOfBackups()>0 && fileSystem.exists(file)) {
					rolloverByNumber(file);
					file=getFile(input, pvl); // reobtain the file, as the object itself may have changed because of the rollover
				}
				try (OutputStream out = ((IWritableFileSystem<F>)fileSystem).createFile(file)) {
					writeContentsToFile(out, input, pvl);
				}
				return getFileAsXmlBuilder(file, "file").toXML();
			} else if (action.equalsIgnoreCase(ACTION_APPEND)) {
				F file=getFile(input, pvl);
				if (getRotateDays()>0 && fileSystem.exists(file)) {
					rolloverByDay(file);
					file=getFile(input, pvl); // reobtain the file, as the object itself may have changed because of the rollover
				}
				if (getRotateSize()>0 && fileSystem.exists(file)) {
					rolloverBySize(file);
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
				fileSystem.removeFolder(folder);
				return folder;
			} else if (action.equalsIgnoreCase(ACTION_RENAME)) {
				F file=getFile(input, pvl);
				String destination = (String) pvl.getParameterValue(PARAMETER_DESTINATION).getValue();
				if (destination == null) {
					throw new SenderException("unknown destination [" + destination + "]");
				}
				((IWritableFileSystem<F>)fileSystem).renameFile(file, destination, false);
				return destination;
			} else if (action.equalsIgnoreCase(ACTION_MOVE)) {
				F file=getFile(input, pvl);
				String destinationFolder = (String) pvl.getParameterValue(PARAMETER_DESTINATION).getValue();
				if (destinationFolder == null) {
					throw new SenderException("parameter ["+PARAMETER_DESTINATION+"] for destination folder does not specify destination");
				}
				F moved=fileSystem.moveFile(file, destinationFolder, false);
				return fileSystem.getName(moved);
			}
		} catch (Exception e) {
			throw new FileSystemException("unable to process ["+action+"] action for File [" + determineFilename(input, pvl) + "]", e);
		}

		return input;
	}
	
	private void rolloverByNumber(F file) throws FileSystemException {
		IWritableFileSystem<F> wfs = (IWritableFileSystem<F>)fileSystem;
		
		if (!fileSystem.exists(file)) {
			return;
		}
		
		String srcFilename = fileSystem.getName(file);
		int number = getNumberOfBackups();
		
		log.debug("Rotating files with a name starting with ["+srcFilename+"] and keeping ["+number+"] backups");
		F lastFile=fileSystem.toFile(srcFilename+"."+number);
		if (fileSystem.exists(lastFile)) {
			log.debug("deleting file  ["+srcFilename+"."+number+"]");
			fileSystem.deleteFile(lastFile);
		}
		
		for(int i=number-1;i>0;i--) {
			F source=fileSystem.toFile(srcFilename+"."+i);
			if (fileSystem.exists(source)) {
				log.debug("moving file ["+srcFilename+"."+i+"] to file ["+srcFilename+"."+(i+1)+"]");
				wfs.renameFile(source, srcFilename+"."+(i+1), true);
			} else {
				log.debug("file ["+srcFilename+"."+i+"] does not exist, no need to move");
			}
		}
		log.debug("moving file ["+srcFilename+"] to file ["+srcFilename+".1]");
		wfs.renameFile(file, srcFilename+".1", true);
	}
	
	private void rolloverBySize(F file) throws FileSystemException {
		if (fileSystem.getFileSize(file)>getRotateSize()) {
			rolloverByNumber(file);
		}
	}

	private void rolloverByDay(F file) throws FileSystemException {
		final long millisPerDay = 24 * 60 * 60 * 1000;
		
		Date lastModified = fileSystem.getModificationTime(file);
		Date sysTime = new Date();
		if (DateUtils.isSameDay(lastModified, sysTime) || lastModified.after(sysTime)) {
			return;
		}
		String srcFilename = fileSystem.getName(file);
		
		log.debug("Deleting files in folder ["+getInputFolder()+"] that have a name starting with ["+srcFilename+"] and are older than ["+getRotateDays()+"] days");
		long threshold = sysTime.getTime()- getRotateDays()*millisPerDay;
		Iterator<F> it = fileSystem.listFiles(getInputFolder());
		while(it.hasNext()) {
			F f=it.next();
			String filename=fileSystem.getName(f);
			if (filename!=null && filename.startsWith(srcFilename) && fileSystem.getModificationTime(f).getTime()<threshold) {
				log.debug("deleting file ["+filename+"]");
				fileSystem.deleteFile(f);
			}
		}

		String tgtFilename = srcFilename+"."+DateUtils.format(fileSystem.getModificationTime(file), DateUtils.shortIsoFormat);
		((IWritableFileSystem<F>)fileSystem).renameFile(file, tgtFilename, true);
	}

	private void writeContentsToFile(OutputStream out, Object input, ParameterValueList pvl) throws IOException, FileSystemException {
		Object contents;
		if (pvl!=null && pvl.containsKey(PARAMETER_CONTENTS1)) {
			 contents=pvl.getParameterValue(PARAMETER_CONTENTS1).getValue();
		} else {
			contents=input;
		}
		if (StringUtils.isNotEmpty(getBase64())) {
			out = new Base64OutputStream(out, getBase64().equals(BASE64_ENCODE));
		}
		if (contents instanceof InputStream) {
			Misc.streamToStream((InputStream)contents, out, true);
		} else if (contents instanceof byte[]) {
			out.write((byte[])contents);
		} else if (contents instanceof String) {
			out.write(((String) contents).getBytes(Misc.DEFAULT_INPUT_STREAM_ENCODING));
		} else {
			throw new FileSystemException("expected InputStream, ByteArray or String but got [" + contents.getClass().getName() + "] instead");
		}
		
	}
	
	
	@Override
	public boolean canStreamToTarget() {
		return false;
	}
	@Override
	public boolean canProvideOutputStream() {
		return (ACTION_WRITE1.equals(getAction()) || ACTION_APPEND.equals(getAction())) && parameterList.findParameter(PARAMETER_FILENAME)!=null;
	}

	@Override
	public MessageOutputStream provideOutputStream(String correlationID, IPipeLineSession session, MessageOutputStream target) throws StreamingException {
		ParameterResolutionContext prc = new ParameterResolutionContext(null, session);
		ParameterValueList pvl=null;
		
		try {
			if (parameterList != null) {
				pvl = prc.getValues(parameterList);
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
			MessageOutputStream stream = new MessageOutputStream(out,null);
			stream.setResponse(getFileAsXmlBuilder(file, "file").toXML());
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

	@IbisDoc({"1", "Possible values: list, read, delete, move, mkdir, rmdir, write, append, rename", "" })
	public void setAction(String action) {
		this.action = action;
	}
	public String getAction() {
		return action;
	}

	@IbisDoc({"2", "Folder that is scanned for files when action=list. When not set, the root is scanned", ""})
	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}
	public String getInputFolder() {
		return inputFolder;
	}

	@IbisDoc({"3", "Can be set to 'encode' or 'decode' for actions read, write and append. When set the stream is base64 encoded or decoded, respectively", ""})
	public void setBase64(String base64) {
		this.base64 = base64;
	}
	public String getBase64() {
		return base64;
	}

	@IbisDoc({"4", "for action=append: when set to a positive number, the file is rotated each day, and this number of files is kept", "0"})
	public void setRotateDays(int rotateDays) {
		this.rotateDays = rotateDays;
	}
	public int getRotateDays() {
		return rotateDays;
	}

	@IbisDoc({"5", "for action=append: when set to a positive number, the file is rotated when it has reached the specified size, and the number of files specified in numberOfBackups is kept", "0"})
	public void setRotateSize(int rotateSize) {
		this.rotateSize = rotateSize;
	}
	public int getRotateSize() {
		return rotateSize;
	}

	@IbisDoc({"6", "for action=write, and for action=append with rotateSize>0: the number of backup files that is kept", "0"})
	public void setNumberOfBackups(int numberOfBackups) {
		this.numberOfBackups = numberOfBackups;
	}
	public int getNumberOfBackups() {
		return numberOfBackups;
	}

}