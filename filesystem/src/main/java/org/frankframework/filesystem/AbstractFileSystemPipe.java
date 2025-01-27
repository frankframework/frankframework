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
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Forward;
import org.frankframework.doc.ReferTo;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.SpringUtils;

/**
 * Base class for Pipes that use a {@link IBasicFileSystem FileSystem}.
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
@Forward(name = "fileNotFound", description = "the input file was expected to exist, but was not found")
@Forward(name = "folderNotFound", description = "the folder does not exist")
@Forward(name = "fileAlreadyExists", description = "a file that should have been created as new already exists, or if a file already exists when it should have been created as folder")
@Forward(name = "folderAlreadyExists", description = "a folder is to be created that already exists.")
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.ENDPOINT)
public abstract class AbstractFileSystemPipe<F, FS extends IBasicFileSystem<F>> extends FixedForwardPipe implements HasPhysicalDestination {

	private final FileSystemActor<F, FS> actor = new FileSystemActor<>();
	@Getter private FS fileSystem;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		FS fileSystem = getFileSystem();
		SpringUtils.autowireByName(getApplicationContext(), fileSystem);
		fileSystem.configure();
		actor.configure(fileSystem, getParameterList(), this);
	}

	@Override
	public void start() {
		super.start();
		try {
			FS fileSystem = getFileSystem();
			fileSystem.open();
			actor.open();
		} catch (FileSystemException e) {
			throw new LifecycleException("Pipe [" + getName() + "]: Cannot open fileSystem",e);
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
	@Nullable
	public PipeRunResult doPipe(@Nonnull Message message, @Nonnull PipeLineSession session) throws PipeRunException {
		ParameterList paramList = getParameterList();
		ParameterValueList pvl=null;
		try {
			if (paramList != null) {
				pvl = paramList.getValues(message, session);
			}
		} catch (ParameterException e) {
			throw new PipeRunException(this,"Pipe caught exception evaluating parameters", e);
		}

		Message result;
		try {
			result = actor.doAction(message, pvl, session);
		} catch (FileSystemException e) {
			String forwardName = e.getForward().getForwardName();

			Map<String, PipeForward> forwards = getForwards();
			if (forwards.containsKey(forwardName)) {
				return new PipeRunResult(getForwards().get(forwardName), e.getMessage());
			} else if (forwards.containsKey(PipeForward.EXCEPTION_FORWARD_NAME)) {
				return new PipeRunResult(getForwards().get(PipeForward.EXCEPTION_FORWARD_NAME), e.getMessage());
			}
			throw new PipeRunException(this, "cannot perform action", e);
		}
		if (!Message.isNull(result)) {
			return new PipeRunResult(getSuccessForward(), result);
		}
		return new PipeRunResult(getSuccessForward(), message);
	}

	@Override
	public String getPhysicalDestinationName() {
		return getFileSystem().getPhysicalDestinationName();
	}

	@Override
	public String getDomain() {
		return getFileSystem().getDomain();
	}

	protected void setFileSystem(FS fileSystem) {
		this.fileSystem = fileSystem;
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
