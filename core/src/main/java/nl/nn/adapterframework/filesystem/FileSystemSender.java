/*
   Copyright 2019, 2020 WeAreFrank!

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

import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingSenderBase;

/**
 * Base class for Senders that use a {@link IBasicFileSystem FileSystem}.
 * 
 * <table align="top" border="1">
 * <tr><th>Action</th><th>Description</th><th>Configuration</th></tr>
 * <tr><td>list</td><td>list files in a folder/directory</td><td>folder, taken from first available of:<ol><li>attribute <code>inputFolder</code></li><li>parameter <code>inputFolder</code></li><li>root folder</li></ol></td></tr>
 * <tr><td>info</td><td>show info about a single file</td><td>filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message</li><li>root folder</li></ol></td></tr>
 * <tr><td>read</td><td>read a file, returns an InputStream</td><td>filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message</td><td>&nbsp;</td></tr>
 * <tr><td>readDelete</td><td>like read, but deletes the file after it has been read</td><td>filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message</td><td>&nbsp;</td></tr>
 * <tr><td>move</td><td>move a file to another folder</td><td>filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message<br/>destination: taken from attribute <code>destination</code> or parameter <code>destination</code></td></tr>
 * <tr><td>copy</td><td>copy a file to another folder</td><td>filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message<br/>destination: taken from attribute <code>destination</code> or parameter <code>destination</code></td></tr>
 * <tr><td>delete</td><td>delete a file</td><td>filename: taken from attribute <code>filename</code>, parameter <code>filename</code> or input message</td><td>&nbsp;</td></tr>
 * <tr><td>mkdir</td><td>create a folder/directory</td><td>folder: taken from parameter <code>foldername</code> or input message</td><td>&nbsp;</td></tr>
 * <tr><td>rmdir</td><td>remove a folder/directory</td><td>folder: taken from parameter <code>foldername</code> or input message</td><td>&nbsp;</td></tr>
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
 * <table>
 * 
 * @author Gerrit van Brakel
 */
public class FileSystemSender<F, FS extends IBasicFileSystem<F>> extends StreamingSenderBase implements HasPhysicalDestination {
	
	private FS fileSystem;
	private FileSystemActor<F,FS> actor=new FileSystemActor<F,FS>();
	private final String FILESYSTEMACTOR = "nl.nn.adapterframework.filesystem.FileSystemActor";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		getFileSystem().configure();
		actor.configure(fileSystem, getParameterList(), this);
	}
	
	@Override
	public void open() throws SenderException {
		try {
			FS fileSystem=getFileSystem();
			fileSystem.open();
			actor.open();
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
	public MessageOutputStream provideOutputStream(IPipeLineSession session, IForwardTarget next) throws StreamingException {
		return actor.provideOutputStream(session, next);
	}

	@Override
	public PipeRunResult sendMessage(Message message, IPipeLineSession session, IForwardTarget next) throws SenderException, TimeOutException {
		ParameterValueList pvl = null;
		
		try {
			if (paramList !=null) {
				pvl=paramList.getValues(message, session);
			}
		} catch (ParameterException e) {
			throw new SenderException(
					getLogPrefix() + "Sender [" + getName() + "] caught exception evaluating parameters", e);
		}

		try {
			Object result=actor.doAction(message, pvl, session);
			if (result instanceof PipeRunResult) {
				return (PipeRunResult)result;
			}
			return new PipeRunResult(null, result);
		} catch (FileSystemException e) {
			throw new SenderException(e);
		}
	}


	@Override
	public String getPhysicalDestinationName() {
		if (getFileSystem() instanceof HasPhysicalDestination) {
			return ((HasPhysicalDestination)getFileSystem()).getPhysicalDestinationName();
		}
		return null;
	}


	public void setFileSystem(FS fileSystem) {
		this.fileSystem=fileSystem;
	}
	public FS getFileSystem() {
		return fileSystem;
	}

	protected void addActions(List<String> specificActions) {
		actor.addActions(specificActions);
	}



	@IbisDocRef({"1", FILESYSTEMACTOR})
	public void setAction(String action) {
		actor.setAction(action);
	}
	public String getAction() {
		return actor.getAction();
	}

	@IbisDocRef({"2", FILESYSTEMACTOR})
	public void setFilename(String filename) {
		actor.setFilename(filename);
	}

	@IbisDocRef({"2", FILESYSTEMACTOR})
	public void setDestination(String destination) {
		actor.setDestination(destination);
	}

	@IbisDocRef({"3", FILESYSTEMACTOR})
	public void setInputFolder(String inputFolder) {
		actor.setInputFolder(inputFolder);
	}
	
	@IbisDocRef({"4", FILESYSTEMACTOR})
	public void setCreateFolder(boolean createFolder) {
		actor.setCreateFolder(createFolder);
	}

	@IbisDocRef({"5", FILESYSTEMACTOR})
	public void setBase64(String base64) {
		actor.setBase64(base64);
	}

	@IbisDocRef({"6", FILESYSTEMACTOR})
	public void setRotateDays(int rotateDays) {
		actor.setRotateDays(rotateDays);
	}

	@IbisDocRef({"7", FILESYSTEMACTOR})
	public void setRotateSize(int rotateSize) {
		actor.setRotateSize(rotateSize);
	}

	@IbisDocRef({"8", FILESYSTEMACTOR})
	public void setNumberOfBackups(int numberOfBackups) {
		actor.setNumberOfBackups(numberOfBackups);
	}

}