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

import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingPipe;

/**
 * Base class for Pipes that use a {@link IBasicFileSystem FileSystem}.
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
public class FileSystemPipe<F, FS extends IBasicFileSystem<F>> extends StreamingPipe implements HasPhysicalDestination {
	
	private FileSystemActor<F, FS> actor = new FileSystemActor<F, FS>();
	private FS fileSystem;
	
	
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		getFileSystem().configure();
		actor.configure(fileSystem, getParameterList(), this);
	}
	
	@Override
	public void start() throws PipeStartException {
		super.start();
		try {
			FS fileSystem=getFileSystem();
			fileSystem.open();
			actor.open();
		} catch (FileSystemException e) {
			throw new PipeStartException("Cannot open fileSystem",e);
		}
	}
	
	@Override
	public void stop() {
		try {
			getFileSystem().close();
		} catch (FileSystemException e) {
			log.warn("Cannot close fileSystem",e);
		} finally {
			super.stop();
		}
	}

	@Override
	public boolean canProvideOutputStream() {
		return super.canProvideOutputStream() && actor.canProvideOutputStream();
	}
	@Override
	public boolean requiresOutputStream() {
		return super.requiresOutputStream() && actor.requiresOutputStream();  
	}
	
	@Override
	public MessageOutputStream provideOutputStream(String correlationID, IPipeLineSession session, MessageOutputStream target) throws StreamingException {
		return actor.provideOutputStream(correlationID, session, target);
	}


	@Override
	public PipeRunResult doPipe (Object input, IPipeLineSession session, MessageOutputStream target) throws PipeRunException {
		ParameterList paramList = getParameterList();
		ParameterResolutionContext prc = new ParameterResolutionContext(input, session);
		ParameterValueList pvl=null;
		
		try {
			if (paramList != null) {
				pvl = prc.getValues(paramList);
			}
		} catch (ParameterException e) {
			throw new PipeRunException(this,getLogPrefix(session) + "Pipe [" + getName() + "] caught exception evaluating parameters", e);
		}

		Object result;
		try {
			result = actor.doAction(new Message(input), pvl, session);
		} catch (FileSystemException | TimeOutException e) {
			if (getForwards().containsKey("exception")) {
				return new PipeRunResult(getForwards().get("exception"), e.getMessage());
			}
			throw new PipeRunException(this, "cannot perform action", e);
		}
		if (result!=null) {
			return new PipeRunResult(getForward(), result);
		}
		return new PipeRunResult(getForward(), input);
	}

	@Override
	public String getPhysicalDestinationName() {
		if (getFileSystem() instanceof HasPhysicalDestination) {
			return ((HasPhysicalDestination)getFileSystem()).getPhysicalDestinationName();
		}
		return null;
	}

	public FS getFileSystem() {
		return fileSystem;
	}

	protected void setFileSystem(FS fileSystem) {
		this.fileSystem = fileSystem;
	}

	protected void addActions(List<String> specificActions) {
		actor.addActions(specificActions);
	}

	@IbisDoc({"1", "possible values: list, info, read, delete, move, mkdir, rmdir, write, rename", "" })
	public void setAction(String action) {
		actor.setAction(action);
	}
	public String getAction() {
		return actor.getAction();
	}

	@IbisDoc({"2", "folder that is scanned for files when action=list. When not set, the root is scanned", ""})
	public void setInputFolder(String inputFolder) {
		actor.setInputFolder(inputFolder);
	}
	public String getInputFolder() {
		return actor.getInputFolder();
	}
	
	@IbisDoc({"3", "filename to operate on. When not set, the parameter filename is used. When that is not set either, the input is used", ""})
	public void setFilename(String filename) {
		actor.setFilename(filename);
	}
	public String getFilename() {
		return actor.getFilename();
	}

	@IbisDoc({"3", "Can be set to 'encode' or 'decode' for actions read, write and append. When set the stream is base64 encoded or decoded, respectively", ""})
	public void setBase64(String base64) {
		actor.setBase64(base64);
	}

	@IbisDoc({"4", "for action=append: when set to a positive number, the file is rotated each day, and this number of files is kept", "0"})
	public void setRotateDays(int rotateDays) {
		actor.setRotateDays(rotateDays);
	}

	@IbisDoc({"5", "for action=append: when set to a positive number, the file is rotated when it has reached the specified size, and the number of files specified in numberOfBackups is kept", "0"})
	public void setRotateSize(int rotateSize) {
		actor.setRotateSize(rotateSize);
	}

	@IbisDoc({"6", "for action=write, and for action=append with rotateSize>0: the number of backup files that is kept", "0"})
	public void setNumberOfBackups(int numberOfBackups) {
		actor.setNumberOfBackups(numberOfBackups);
	}

}