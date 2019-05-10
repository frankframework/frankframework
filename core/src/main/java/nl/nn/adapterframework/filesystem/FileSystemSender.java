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

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * FileSystem Sender: Base class for all file system senders
 * 
 * 
 * <p><b>Actions:</b></p>
 * <p>The <code>list</code> action for listing a directory content. inputFolder could be set as an attribute or as a parameter in adapter to list specific folder content. If both are set then parameter will override the value of attribute. If not set then, it will list root folder content </p>
 * <p>The <code>download</code> action for downloading a file, requires filename as input. Returns a base64 encoded string containing the file content </p>
 * <p>The <code>move</code> action for moving a file to another folder requires destination folder as parameter "destination"</p>
 * <p>The <code>delete</code> action requires the filename as input </p>
 * <p>The <code>upload</code> action requires the file parameter to be set which should contain the fileContent to upload in either Stream, Bytes or String format</p>
 * <p>The <code>rename</code> action requires the destination parameter to be set which should contain the full path </p>
 * <p>The <code>mkdir</code> action for creating a directory requires directory name to be created as input. </p>
 * <p>The <code>rmdir</code> action for removing a directory requires directory name to be removed as input. </p>
 * 
 * <br/>
 */

public class FileSystemSender<F, FS extends IBasicFileSystem<F>> extends SenderWithParametersBase {
	
	public final String[] ACTIONS_BASIC= {"list", "download", "move", "delete"};
	public final String[] ACTIONS_WRITABLE_FS= {"upload", "rename", "mkdir", "rmdir"};

	private String action;
	private String inputFolder;

	private Set<String> actions = new LinkedHashSet<String>(Arrays.asList(ACTIONS_BASIC));

	private FS fileSystem;
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		getFileSystem().configure();
		if (getFileSystem() instanceof IWritableFileSystem) {
			actions.addAll(Arrays.asList(ACTIONS_WRITABLE_FS));
		}

		if (getAction() == null)
			throw new ConfigurationException(getLogPrefix() + "action must be specified");
		if (!actions.contains(getAction()))
			throw new ConfigurationException(getLogPrefix() + "unknown or invalid action [" + getAction()
					+ "] supported actions are " + actions.toString() + "");

		//Check if necessarily parameters are available
		ParameterList parameterList = getParameterList();
		if (getAction().equals("upload") && (parameterList == null || parameterList.findParameter("file") == null))
			throw new ConfigurationException(
					getLogPrefix() + "the upload action requires the file parameter to be present");
		if (getAction().equals("rename")
				&& (parameterList == null || parameterList.findParameter("destination") == null))
			throw new ConfigurationException(
					getLogPrefix() + "the rename action requires a destination parameter to be present");
	}
	
	@Override
	public void open() throws SenderException {
		try {
			FS fileSystem=getFileSystem();
			fileSystem.open();
			if (StringUtils.isNotEmpty(getInputFolder())) {
				if (!fileSystem.folderExists(getInputFolder())) {
					throw new SenderException("inputFolder ["+getInputFolder()+"] does not exist");
				}
			}
		} catch (FileSystemException e) {
			throw new SenderException("Cannot open fileSystem",e);
		}
	}
	
	@Override
	public void close() throws SenderException {
		try {
			getFileSystem().close();
		} catch (FileSystemException e) {
			throw new SenderException("Cannot close fileSystem",e);
		}
	}

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
		ParameterValueList pvl = null;
		
		try {
			if (prc != null && paramList != null) {
				pvl = prc.getValues(paramList);
			}
		} catch (ParameterException e) {
			throw new SenderException(
					getLogPrefix() + "Sender [" + getName() + "] caught exception evaluating parameters", e);
		}

		IBasicFileSystem<F> ifs = getFileSystem();
		F file;
		
		try {
			file = ifs.toFile(message);
		} catch (Exception e) {
			throw new SenderException(getLogPrefix() + "unable to get file", e);
		}
		
		try {
			if (action.equalsIgnoreCase("delete")) {
				ifs.deleteFile(file);
			} else if (action.equalsIgnoreCase("download")) {
				InputStream is = new Base64InputStream(ifs.readFile(file), true);
				String result = Misc.streamToString(is);
				is.close();

				return result;
			} else if (action.equalsIgnoreCase("list")) {
				String folder = inputFolder;
				if(paramList !=null && paramList.findParameter("inputFolder") != null ) {
					folder = (String) pvl.getParameterValue("inputFolder").getValue();
				}
				Iterator<F> fileList = ifs.listFiles(folder);
				int count = 0;
				XmlBuilder dirXml = new XmlBuilder("directory");
				while (fileList.hasNext()) {
					F fileObject = fileList.next();
					dirXml.addSubElement(getFileAsXmlBuilder(fileObject, "file"));
					count++;
				}
				dirXml.addAttribute("count", count);

				return dirXml.toXML();
			} else if (action.equalsIgnoreCase("upload")) {
				Object paramValue = pvl.getParameterValue("file").getValue();
				byte[] fileBytes = null;
				if (paramValue instanceof InputStream)
					fileBytes = Misc.streamToBytes((InputStream) paramValue);
				else if (paramValue instanceof byte[])
					fileBytes = (byte[]) paramValue;
				else if (paramValue instanceof String)
					fileBytes = ((String) paramValue).getBytes(Misc.DEFAULT_INPUT_STREAM_ENCODING);
				else
					throw new SenderException("expected InputStream, ByteArray or String but got ["
							+ paramValue.getClass().getName() + "] instead");
				OutputStream out = null;
				out = ((IWritableFileSystem<F>)ifs).createFile(file);
				out.write(fileBytes);
				out.close();

				return getFileAsXmlBuilder(file, "file").toXML();
			} else if (action.equalsIgnoreCase("mkdir")) {
				((IWritableFileSystem<F>)ifs).createFolder(message);
			} else if (action.equalsIgnoreCase("rmdir")) {
				((IWritableFileSystem<F>)ifs).removeFolder(message);
			} else if (action.equalsIgnoreCase("rename")) {
				String destination = (String) pvl.getParameterValue("destination").getValue();
				if (destination == null) {
					throw new SenderException("unknown destination [" + destination + "]");
				}
				((IWritableFileSystem<F>)ifs).renameFile(file, destination, false);
			} else if (action.equalsIgnoreCase("move")) {
				String destination = (String) pvl.getParameterValue("destination").getValue();
				if (destination == null) {
					throw new SenderException("destination folder not specified");
				}
				F moved=ifs.moveFile(file, destination, false);
				return getFileSystem().getName(moved);
			}
		} catch (Exception e) {
			throw new SenderException(getLogPrefix() + "unable to process ["+action+"] action for File [" + message + "]", e);
		}

		return message;
	}

	public XmlBuilder getFileAsXmlBuilder(F f, String elementName) throws FileSystemException {
		IBasicFileSystem<F> ifs = getFileSystem();
		XmlBuilder fileXml = new XmlBuilder(elementName);

		String name = ifs.getName(f);
		fileXml.addAttribute("name", name);
		if (!".".equals(name) && !"..".equals(name)) {
			long fileSize = ifs.getFileSize(f);
			fileXml.addAttribute("size", "" + fileSize);
			fileXml.addAttribute("fSize", "" + Misc.toFileSize(fileSize, true));
			try {
				fileXml.addAttribute("canonicalName", fileSystem.getCanonicalName(f));
			} catch (Exception e) {
				log.warn("cannot get canonicalName for file [" + name + "]", e);
				fileXml.addAttribute("canonicalName", name);
			}
			// Get the modification date of the file
			Date modificationDate = ifs.getModificationTime(f);
			//add date
			if (modificationDate != null) {
				String date = DateUtils.format(modificationDate, DateUtils.FORMAT_DATE);
				fileXml.addAttribute("modificationDate", date);

				// add the time
				String time = DateUtils.format(modificationDate, DateUtils.FORMAT_TIME_HMS);
				fileXml.addAttribute("modificationTime", time);
			}
		}
		
		Map<String, Object> additionalParameters = ifs.getAdditionalFileProperties(f);
		if(additionalParameters != null) {
			for (Map.Entry<String, Object> attribute : additionalParameters.entrySet()) {
				fileXml.addAttribute(attribute.getKey(), String.valueOf(attribute.getValue()));
			}
		}

		return fileXml;
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

	public void setFileSystem(FS fileSystem) {
		this.fileSystem = fileSystem;
	}

	protected void addActions(List<String> specificActions) {
		actions.addAll(specificActions);
	}

	@IbisDoc({ "possible values: list, download, delete, upload, rename, move, mkdir, rmdir", "" })
	public void setAction(String action) {
		this.action = action;
	}
	public String getAction() {
		return action;
	}

	@IbisDoc({"folder that is scanned for files when action=list. When not set, the root is scanned", ""})
	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}
	public String getInputFolder() {
		return inputFolder;
	}

}