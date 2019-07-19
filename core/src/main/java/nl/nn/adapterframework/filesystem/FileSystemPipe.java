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
import nl.nn.adapterframework.pipes.FixedForwardPipe;

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

public class FileSystemPipe<F, FS extends IBasicFileSystem<F>> extends FixedForwardPipe implements HasPhysicalDestination {
	
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
	public PipeRunResult doPipe (Object input, IPipeLineSession session) throws PipeRunException {
		ParameterList paramList = getParameterList();
		ParameterResolutionContext prc = new ParameterResolutionContext(input.toString(), session);
		ParameterValueList pvl=null;
		
		try {
			if (paramList != null) {
				pvl = prc.getValues(paramList);
			}
		} catch (ParameterException e) {
			throw new PipeRunException(this,getLogPrefix(session) + "Sender [" + getName() + "] caught exception evaluating parameters", e);
		}

		Object result;
		try {
			result = actor.doAction(input, pvl);
		} catch (FileSystemException | TimeOutException e) {
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

	@IbisDoc({ "possible values: list, read, delete, write, rename, move, mkdir, rmdir", "" })
	public void setAction(String action) {
		actor.setAction(action);
	}
	public String getAction() {
		return actor.getAction();
	}

	@IbisDoc({"folder that is scanned for files when action=list. When not set, the root is scanned", ""})
	public void setInputFolder(String inputFolder) {
		actor.setInputFolder(inputFolder);
	}
	public String getInputFolder() {
		return actor.getInputFolder();
	}

}