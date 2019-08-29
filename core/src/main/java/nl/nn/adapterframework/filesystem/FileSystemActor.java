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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
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
 * <tr><td>read</td><td>read a file, returns an InputStream</td><td>filename: taken from input message</td><td>&nbsp;</td></tr>
 * <tr><td>move</td><td>move a file to another folder</td><td>filename: taken from input message<br/>parameter <code>destination</code></td></tr>
 * <tr><td>delete</td><td>delete a file</td><td>filename: taken from input message</td><td>&nbsp;</td></tr>
 * <tr><td>mkdir</td><td>create a folder/directory</td><td>folder: taken from input message</td><td>&nbsp;</td></tr>
 * <tr><td>rmdir</td><td>remove a folder/directory</td><td>folder: taken from input message</td><td>&nbsp;</td></tr>
 * <tr><td>write</td><td>write contents to a file<td>filename: taken from input message<br/>parameter <code>contents</code>: contents as either Stream, Bytes or String</td><td>&nbsp;</td></tr>
 * <tr><td>rename</td><td>change the name of a file</td><td>filename: taken from input message<br/>parameter <code>destination</code></td></tr>
 * <table>
 * 
 * @author Gerrit van Brakel
 */
public class FileSystemActor<F, FS extends IBasicFileSystem<F>> {
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
	public final String ACTION_RENAME="rename";
	
	public final String PARAMETER_CONTENTS1="contents";
	public final String PARAMETER_CONTENTS2="file";
	public final String PARAMETER_INPUTFOLDER="inputFolder";			// folder for actions list, mkdir and rmdir. This is a sub folder of baseFolder
	public final String PARAMETER_DESTINATION="destination";	// destination for action rename and move
	
	
	public final String[] ACTIONS_BASIC= {ACTION_LIST, ACTION_READ1, ACTION_READ2, ACTION_MOVE, ACTION_DELETE, ACTION_MKDIR, ACTION_RMDIR};
	public final String[] ACTIONS_WRITABLE_FS= {ACTION_WRITE1, ACTION_WRITE2, ACTION_RENAME};

	private String action;
	private String inputFolder; // folder for action=list

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
		
		if (parameterList!=null && parameterList.findParameter(PARAMETER_CONTENTS2) != null && parameterList.findParameter(PARAMETER_CONTENTS1) == null) {
			ConfigurationWarnings.add(owner, log, "parameter ["+PARAMETER_CONTENTS2+"] has been replaced with ["+PARAMETER_CONTENTS1+"]");
			parameterList.findParameter(PARAMETER_CONTENTS2).setName(PARAMETER_CONTENTS1);;
		}
		
		//Check if necessarily parameters are available
		actionRequiresParameter(parameterList, ACTION_WRITE1, PARAMETER_CONTENTS1);
		actionRequiresParameter(parameterList, ACTION_MOVE, PARAMETER_DESTINATION);
		actionRequiresParameter(parameterList, ACTION_RENAME, PARAMETER_DESTINATION);
		
		if (StringUtils.isNotEmpty(getInputFolder()) && parameterList!=null && parameterList.findParameter(PARAMETER_INPUTFOLDER) != null) {
			ConfigurationWarnings.add(owner, log, "inputFolder configured via attribute [inputFolder] as well as via parameter ["+PARAMETER_INPUTFOLDER+"], parameter will be ignored");
		}
		
	}
	
	
	
	protected void actionRequiresParameter(ParameterList parameterList, String action, String parameter) throws ConfigurationException {
		if (getAction().equals(action) && (parameterList == null || parameterList.findParameter(parameter) == null)) {
			throw new ConfigurationException("the "+action+" action requires the parameter ["+parameter+"] to be present");
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
	
	private String determineFilename(Object input, ParameterValueList pvl) {
//		if (StringUtils.isNotEmpty(configuredFileName)) {
//			return configuredFileName;
//		}
//		if (pvl!=null && pvl.containsKey(PARAMETER_FILE)) {
//			return pvl.getParameterValue(PARAMETER_FILE).asStringValue("");
//		}
		return input.toString();
	}

	private F getFile(Object input, ParameterValueList pvl) throws FileSystemException {
		String filename=determineFilename(input, pvl);
		return fileSystem.toFile(filename);
	}
	
	private String determineInputFoldername(Object input, ParameterValueList pvl) {
		if (StringUtils.isNotEmpty(getInputFolder())) {
			return getInputFolder();
		}
		if (pvl!=null && pvl.containsKey(PARAMETER_INPUTFOLDER)) {
			return pvl.getParameterValue(PARAMETER_INPUTFOLDER).asStringValue("");
		}
		if (input==null || StringUtils.isEmpty(input.toString())) {
			return null;
		}
		return input.toString();
	}
	
	public Object doAction(Object input, ParameterValueList pvl) throws FileSystemException, TimeOutException {
		try {
			if (action.equalsIgnoreCase(ACTION_DELETE)) {
				F file=getFile(input, pvl);
				fileSystem.deleteFile(file);
			} else if (action.equalsIgnoreCase(ACTION_READ1)) {
				F file=getFile(input, pvl);
				return fileSystem.readFile(file);
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
				Object contents=pvl.getParameterValue(PARAMETER_CONTENTS1).getValue();
				F file=getFile(input, pvl);
				try (OutputStream out = ((IWritableFileSystem<F>)fileSystem).createFile(file)) {
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
				return getFileAsXmlBuilder(file, "file").toXML();
			} else if (action.equalsIgnoreCase(ACTION_MKDIR)) {
				String folder = determineInputFoldername(input, pvl);
				fileSystem.createFolder(folder);
			} else if (action.equalsIgnoreCase(ACTION_RMDIR)) {
				String folder = determineInputFoldername(input, pvl);
				fileSystem.removeFolder(folder);
			} else if (action.equalsIgnoreCase(ACTION_RENAME)) {
				F file=getFile(input, pvl);
				String destination = (String) pvl.getParameterValue(PARAMETER_DESTINATION).getValue();
				if (destination == null) {
					throw new SenderException("unknown destination [" + destination + "]");
				}
				((IWritableFileSystem<F>)fileSystem).renameFile(file, destination, false);
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

	@IbisDoc({"1", "possible values: list, read, delete, move, mkdir, rmdir, write, rename", "" })
	public void setAction(String action) {
		this.action = action;
	}
	public String getAction() {
		return action;
	}

	@IbisDoc({"2", "folder that is scanned for files when action=list. When not set, the root is scanned", ""})
	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}
	public String getInputFolder() {
		return inputFolder;
	}

}