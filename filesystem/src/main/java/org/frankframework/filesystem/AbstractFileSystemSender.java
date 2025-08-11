/*
   Copyright 2019-2024 WeAreFrank!

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
package org.frankframework.filesystem;

import java.util.List;

import jakarta.annotation.Nonnull;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.DestinationType;
import org.frankframework.core.DestinationType.Type;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Forward;
import org.frankframework.doc.ReferTo;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.senders.AbstractSenderWithParameters;
import org.frankframework.stream.Message;
import org.frankframework.util.SpringUtils;

/**
 * Base class for Senders that use a {@link IBasicFileSystem FileSystem}.
 *
 * @see FileSystemActor
 *
 * @ff.parameter action Overrides attribute <code>action</code>.
 * @ff.parameter filename Overrides attribute <code>filename</code>. If not present, the input message is used.
 * @ff.parameter destination Destination for action <code>rename</code> and <code>move</code>. Overrides attribute <code>destination</code>.
 * @ff.parameter contents Content for action <code>write</code> and <code>append</code>.
 * @ff.parameter inputFolder Folder for actions <code>list</code>, <code>mkdir</code> and <code>rmdir</code>. This is a sub folder of baseFolder. Overrides attribute <code>inputFolder</code>. If not present, the input message is used.
 * @ff.parameter typeFilter Filter for action <code>list</code>. Specify <code>FILES_ONLY</code>, <code>FOLDERS_ONLY</code> or <code>FILES_AND_FOLDERS</code>. By default, only files are listed.
 *
 * @author Gerrit van Brakel
 */
@DestinationType(Type.FILE_SYSTEM)
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.ENDPOINT)
@Forward(name = "fileNotFound", description = "if the input file was expected to exist, but was not found")
@Forward(name = "folderNotFound", description = "if the folder does not exist")
@Forward(name = "fileAlreadyExists", description = "if a file that should have been created as new already exists, or if a file already exists when it should have been created as folder")
@Forward(name = "folderAlreadyExists", description = "if a folder is to be created that already exists")
public abstract class AbstractFileSystemSender<F, S extends IBasicFileSystem<F>> extends AbstractSenderWithParameters implements HasPhysicalDestination {

	private S fileSystem;
	private final FileSystemActor<F,S> actor = new FileSystemActor<>();

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		S fileSystem = getFileSystem();
		SpringUtils.autowireByName(getApplicationContext(), fileSystem);
		fileSystem.configure();
		try {
			actor.configure(fileSystem, getParameterList(), this);
		} catch (ConfigurationException e) {
			throw new ConfigurationException(getLogPrefix(),e);
		}
	}

	@Override
	public void start() {
		try {
			S fileSystem=getFileSystem();
			fileSystem.open();
			actor.open();
		} catch (FileSystemException e) {
			throw new LifecycleException("Cannot open fileSystem",e);
		}
	}

	@Override
	public void stop() {
		try {
			getFileSystem().close();
		} catch (FileSystemException e) {
			throw new LifecycleException("Cannot close fileSystem",e);
		}
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		ParameterValueList pvl = null;

		try {
			if (paramList !=null) {
				pvl = paramList.getValues(message, session);
			}
		} catch (ParameterException e) {
			throw new SenderException("Sender [" + getName() + "] caught exception evaluating parameters", e);
		}

		try {
			Message result = actor.doAction(message, pvl, session);
			return new SenderResult(result);
		} catch (FileSystemException e) {
			String forwardName = e.getForward().getForwardName();
			log.info("error from FileSystemActor, will call forward name [{}]",forwardName, e);
			return new SenderResult(false, Message.nullMessage(), e.getMessage(), forwardName);
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		return getFileSystem().getPhysicalDestinationName();
	}

	public void setFileSystem(S fileSystem) {
		this.fileSystem=fileSystem;
	}
	public S getFileSystem() {
		return fileSystem;
	}

	protected void addActions(List<FileSystemActor.FileSystemAction> specificActions) {
		actor.addActions(specificActions);
	}

	@ReferTo(FileSystemActor.class)
	public void setAction(FileSystemActor.FileSystemAction action) {
		actor.setAction(action);
	}
	public FileSystemActor.FileSystemAction getAction() {
		return actor.getAction();
	}

	@ReferTo(FileSystemActor.class)
	public void setFilename(String filename) {
		actor.setFilename(filename);
	}

	@ReferTo(FileSystemActor.class)
	public void setDestination(String destination) {
		actor.setDestination(destination);
	}

	@ReferTo(FileSystemActor.class)
	public void setInputFolder(String inputFolder) {
		actor.setInputFolder(inputFolder);
	}

	@ReferTo(FileSystemActor.class)
	public void setCreateFolder(boolean createFolder) {
		actor.setCreateFolder(createFolder);
	}

	@ReferTo(FileSystemActor.class)
	public void setOverwrite(boolean overwrite) {
		actor.setOverwrite(overwrite);
	}

	@ReferTo(FileSystemActor.class)
	public void setRotateDays(int rotateDays) {
		actor.setRotateDays(rotateDays);
	}

	@ReferTo(FileSystemActor.class)
	public void setRotateSize(int rotateSize) {
		actor.setRotateSize(rotateSize);
	}

	@ReferTo(FileSystemActor.class)
	public void setNumberOfBackups(int numberOfBackups) {
		actor.setNumberOfBackups(numberOfBackups);
	}

	@ReferTo(FileSystemActor.class)
	public void setWildcard(String wildcard) {
		actor.setWildcard(wildcard);
	}

	@ReferTo(FileSystemActor.class)
	public void setExcludeWildcard(String excludeWildcard) {
		actor.setExcludeWildcard(excludeWildcard);
	}

	@ReferTo(FileSystemActor.class)
	public void setRemoveNonEmptyFolder(boolean removeNonEmptyFolder) {
		actor.setRemoveNonEmptyFolder(removeNonEmptyFolder);
	}

	@ReferTo(FileSystemActor.class)
	public void setWriteLineSeparator(boolean writeLineSeparator) {
		actor.setWriteLineSeparator(writeLineSeparator);
	}

	@ReferTo(FileSystemActor.class)
	public void setCharset(String charset) {
		actor.setCharset(charset);
	}

	@ReferTo(FileSystemActor.class)
	public void setDeleteEmptyFolder(boolean deleteEmptyFolder) {
		actor.setDeleteEmptyFolder(deleteEmptyFolder);
	}

	@ReferTo(FileSystemActor.class)
	public void setOutputFormat(DocumentFormat outputFormat) {
		actor.setOutputFormat(outputFormat);
	}

	@ReferTo(FileSystemActor.class)
	public void setTypeFilter(TypeFilter typeFilter) {
		actor.setTypeFilter(typeFilter);
	}
}
