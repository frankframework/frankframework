/*
   Copyright 2019-2022 WeAreFrank!

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
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.SupportsOutputStreaming;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.filesystem.FileSystemActor.FileSystemAction;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.Base64Pipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingSenderBase;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.util.SpringUtils;

/**
 * Base class for Senders that use a {@link IBasicFileSystem FileSystem}.
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
@ElementType(ElementTypes.ENDPOINT)
@SupportsOutputStreaming
public abstract class FileSystemSender<F, FS extends IBasicFileSystem<F>> extends StreamingSenderBase implements HasPhysicalDestination {

	private FS fileSystem;
	private FileSystemActor<F,FS> actor=new FileSystemActor<F,FS>();
	private final String FILESYSTEMACTOR = "nl.nn.adapterframework.filesystem.FileSystemActor";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		FS fileSystem = getFileSystem();
		SpringUtils.autowireByName(getApplicationContext(), fileSystem);
		fileSystem.configure();
		try {
			actor.configure(fileSystem, getParameterList(), this);
		} catch (ConfigurationException e) {
			throw new ConfigurationException(getLogPrefix(),e);
		}
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
	public MessageOutputStream provideOutputStream(PipeLineSession session, IForwardTarget next) throws StreamingException {
		return actor.provideOutputStream(session, next);
	}

	@Override
	public PipeRunResult sendMessage(Message message, PipeLineSession session, IForwardTarget next) throws SenderException, TimeoutException {
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
		return getFileSystem().getPhysicalDestinationName();
	}

	@Override
	public String getDomain() {
		return getFileSystem().getDomain();
	}

	public void setFileSystem(FS fileSystem) {
		this.fileSystem=fileSystem;
	}
	public FS getFileSystem() {
		return fileSystem;
	}

	protected void addActions(List<FileSystemAction> specificActions) {
		actor.addActions(specificActions);
	}



	@IbisDocRef({FILESYSTEMACTOR})
	public void setAction(FileSystemAction action) {
		actor.setAction(action);
	}
	public FileSystemAction getAction() {
		return actor.getAction();
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setFilename(String filename) {
		actor.setFilename(filename);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setDestination(String destination) {
		actor.setDestination(destination);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setInputFolder(String inputFolder) {
		actor.setInputFolder(inputFolder);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setCreateFolder(boolean createFolder) {
		actor.setCreateFolder(createFolder);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setOverwrite(boolean overwrite) {
		actor.setOverwrite(overwrite);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setRotateDays(int rotateDays) {
		actor.setRotateDays(rotateDays);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setRotateSize(int rotateSize) {
		actor.setRotateSize(rotateSize);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setNumberOfBackups(int numberOfBackups) {
		actor.setNumberOfBackups(numberOfBackups);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	@Deprecated
	public void setBase64(Base64Pipe.Direction base64) {
		actor.setBase64(base64);
	}

	@Deprecated
	@ConfigurationWarning("attribute 'wildCard' has been renamed to 'wildcard'")
	public void setWildCard(String wildcard) {
		setWildcard(wildcard);
	}
	@IbisDocRef({FILESYSTEMACTOR})
	public void setWildcard(String wildcard) {
		actor.setWildcard(wildcard);
	}

	@Deprecated
	@ConfigurationWarning("attribute 'excludeWildCard' has been renamed to 'excludeWildcard'")
	public void setExcludeWildCard(String excludeWildcard) {
		setExcludeWildcard(excludeWildcard);
	}
	@IbisDocRef({FILESYSTEMACTOR})
	public void setExcludeWildcard(String excludeWildcard) {
		actor.setExcludeWildcard(excludeWildcard);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setRemoveNonEmptyFolder(boolean removeNonEmptyFolder) {
		actor.setRemoveNonEmptyFolder(removeNonEmptyFolder);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setWriteLineSeparator(boolean writeLineSeparator) {
		actor.setWriteLineSeparator(writeLineSeparator);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setCharset(String charset) {
		actor.setCharset(charset);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setDeleteEmptyFolder(boolean deleteEmptyFolder) {
		actor.setDeleteEmptyFolder(deleteEmptyFolder);
	}

	@IbisDocRef({FILESYSTEMACTOR})
	public void setOutputFormat(DocumentFormat outputFormat) {
		actor.setOutputFormat(outputFormat);
	}
}