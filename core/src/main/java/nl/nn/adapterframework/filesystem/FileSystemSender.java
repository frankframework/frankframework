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
import java.util.List;

import org.apache.commons.codec.binary.Base64InputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IOutputStreamProvider;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.Misc;

/**
 * Base class for Senders that use a {@link IBasicFileSystem FileSystem}.
 * 
 * <table align="top">
 * <tr><th>Action</th><th>Description</th><th>Configuration</th></tr>
 * <tr><td>list</td><td>list files in a folder/directory</td><td>folder, taken from first available of:<ol><li>attribute <code>inputFolder</code></li><li>parameter <code>inputFolder</code></li><li>root folder</li></ol></td></tr>
 * <tr><td>read</td><td>read a file, returns a base64 encoded string containing the file content</td><td>filename: taken from input message</td><td>&nbsp;</td></tr>
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
public class FileSystemSender<F, FS extends IBasicFileSystem<F>> extends SenderWithParametersBase implements IOutputStreamProvider {
	
	private FS fileSystem;
	private FileSystemActor<F,FS> actor=new FileSystemActor<F,FS>();
	
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
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		ParameterValueList pvl = null;
		
		try {
			if (prc != null && paramList != null) {
				pvl = prc.getValues(paramList);
			}
		} catch (ParameterException e) {
			throw new SenderException(
					getLogPrefix() + "Sender [" + getName() + "] caught exception evaluating parameters", e);
		}

		try {
			Object result = actor.doAction(message, pvl, prc.getSession());
			if (result==null) {
				return null;
			} else {
				if (result instanceof InputStream) {
					try (InputStream is = new Base64InputStream((InputStream)result, true)) {
						return Misc.streamToString(is);
					} catch (IOException e) {
						throw new SenderException(e);
					}
				} else {
					return result.toString();
				}
			}
		} catch (FileSystemException e) {
			throw new SenderException(e);
		}
	}


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

	@IbisDoc({"1", "possible values: list, read, delete, move, mkdir, rmdir, write, rename", "" })
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

	@IbisDoc({"3", "Only for action 'write': When set, an {@link OutputStream} will be provided in this session variable, that the next pipe can use to write it's output to.", ""})
	@Override
	public void setCreateStreamSessionKey(String createStreamSessionKey) {
		actor.setCreateStreamSessionKey(createStreamSessionKey);
	}
	@Override
	public String getCreateStreamSessionKey() {
		return actor.getCreateStreamSessionKey();
	}

}