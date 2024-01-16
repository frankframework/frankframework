/*
   Copyright 2019-2023 WeAreFrank!

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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.PipeStartException;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.doc.ReferTo;

import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.stream.document.DocumentFormat;
import org.frankframework.util.SpringUtils;

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
@ElementType(ElementTypes.ENDPOINT)
public abstract class FileSystemPipe<F, FS extends IBasicFileSystem<F>> extends FixedForwardPipe implements HasPhysicalDestination {

	private final FileSystemActor<F, FS> actor = new FileSystemActor<>();
	private FS fileSystem;
	private final String FILESYSTEMACTOR = "org.frankframework.filesystem.FileSystemActor";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		FS fileSystem = getFileSystem();
		SpringUtils.autowireByName(getApplicationContext(), fileSystem);
		fileSystem.configure();
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
	@Nullable
	public PipeRunResult doPipe (@Nonnull Message message, @Nonnull PipeLineSession session) throws PipeRunException {
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
			Map<String, PipeForward> forwards = getForwards();
			if (forwards!=null && forwards.containsKey(PipeForward.EXCEPTION_FORWARD_NAME)) {
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

	public FS getFileSystem() {
		return fileSystem;
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
}
