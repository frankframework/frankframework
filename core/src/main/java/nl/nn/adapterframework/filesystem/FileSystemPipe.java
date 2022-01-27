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

import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.filesystem.FileSystemActor.FileSystemAction;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.Base64Pipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingPipe;

/**
 * Base class for Pipes that use a {@link IBasicFileSystem FileSystem}.
 * 
 * @see FileSystemActor
 * 
 * @ff.parameter action overrides attribute <code>action</code>.
 * @ff.parameter filename overrides attribute <code>filename</code>. If not present, the input message is used.
 * @ff.parameter destination destination for action <code>rename</code> and <code>move</code>. Overrides attribute <code>destination</code>.
 * @ff.parameter contents contents for action <code>write</code> and <code>append</code>.
 * @ff.parameter inputFolder folder for actions <code>list</code>, <code>mkdir</code> and <code>rmdir</code>. This is a sub folder of baseFolder. Overrides attribute <code>inputFolder</code>. If not present, the input message is used.
 * 
 * @author Gerrit van Brakel
 */
public abstract class FileSystemPipe<F, FS extends IBasicFileSystem<F>> extends StreamingPipe implements HasPhysicalDestination {

	private FileSystemActor<F, FS> actor = new FileSystemActor<F, FS>();
	private FS fileSystem;
	private final String FILESYSTEMACTOR = "nl.nn.adapterframework.filesystem.FileSystemActor";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		getFileSystem().configure();
		try { 
			actor.configure(fileSystem, getParameterList(), this);
		} catch (ConfigurationException e) {
			throw new ConfigurationException(getLogPrefix(null),e);
		}
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
	public boolean supportsOutputStreamPassThrough() {
		return false;
	}

	@Override
	protected MessageOutputStream provideOutputStream(PipeLineSession session) throws StreamingException {
		return actor.provideOutputStream(session, getNextPipe());
	}

	@Override
	public PipeRunResult doPipe (Message message, PipeLineSession session) throws PipeRunException {
		ParameterList paramList = getParameterList();
		ParameterValueList pvl=null;
		try {
			if (paramList != null) {
				pvl = paramList.getValues(message, session);
			}
		} catch (ParameterException e) {
			throw new PipeRunException(this,getLogPrefix(session) + "Pipe [" + getName() + "] caught exception evaluating parameters", e);
		}

		Object result;
		try {
			result = actor.doAction(message, pvl, session);
		} catch (FileSystemException | TimeoutException e) {
			Map<String, PipeForward> forwards = getForwards();
			if (forwards!=null && forwards.containsKey(PipeForward.EXCEPTION_FORWARD_NAME)) {
				return new PipeRunResult(getForwards().get(PipeForward.EXCEPTION_FORWARD_NAME), e.getMessage());
			}
			throw new PipeRunException(this, "cannot perform action", e);
		}
		if (result!=null) {
			return new PipeRunResult(getSuccessForward(), result);
		}
		return new PipeRunResult(getSuccessForward(), message);
	}

	@Override
	public String getPhysicalDestinationName() {
		return getFileSystem().getPhysicalDestinationName();
	}

	public FS getFileSystem() {
		return fileSystem;
	}

	protected void setFileSystem(FS fileSystem) {
		this.fileSystem = fileSystem;
	}

	protected void addActions(List<FileSystemAction> specificActions) {
		actor.addActions(specificActions);
	}

	@IbisDocRef({"1", FILESYSTEMACTOR})
	public void setAction(FileSystemAction action) {
		actor.setAction(action);
	}
	public FileSystemAction getAction() {
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
	public void setOverwrite(boolean overwrite) {
		actor.setOverwrite(overwrite);
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

	@IbisDocRef({"9", FILESYSTEMACTOR})
	@Deprecated
	public void setBase64(Base64Pipe.Direction base64) {
		actor.setBase64(base64);
	}

	@Deprecated
	@ConfigurationWarning("attribute 'wildCard' has been renamed to 'wildcard'")
	public void setWildCard(String wildcard) {
		setWildcard(wildcard);
	}
	@IbisDocRef({"10", FILESYSTEMACTOR})
	public void setWildcard(String wildcard) {
		actor.setWildcard(wildcard);
	}

	@Deprecated
	@ConfigurationWarning("attribute 'excludeWildCard' has been renamed to 'excludeWildcard'")
	public void setExcludeWildCard(String excludeWildcard) {
		setExcludeWildcard(excludeWildcard);
	}
	@IbisDocRef({"11", FILESYSTEMACTOR})
	public void setExcludeWildcard(String excludeWildcard) {
		actor.setExcludeWildcard(excludeWildcard);
	}

	@IbisDocRef({"12", FILESYSTEMACTOR})
	public void setRemoveNonEmptyFolder(boolean removeNonEmptyFolder) {
		actor.setRemoveNonEmptyFolder(removeNonEmptyFolder);
	}

	@IbisDocRef({"13", FILESYSTEMACTOR})
	public void setWriteLineSeparator(boolean writeLineSeparator) {
		actor.setWriteLineSeparator(writeLineSeparator);
	}

	@IbisDocRef({"14", FILESYSTEMACTOR})
	public void setCharset(String charset) {
		actor.setCharset(charset);
	}
}