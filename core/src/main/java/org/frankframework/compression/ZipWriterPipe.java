/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.compression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.collection.AbstractCollectorPipe;
import org.frankframework.collection.CollectionException;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.stream.PathMessage;
import org.frankframework.util.TemporaryDirectoryUtils;

/**
 * Pipe that creates a ZIP archive (on action close).
 * <p>
 * A chain of zipWriterPipes can be used to create a ZIP archive. You can use the pipe with different actions (see specified below).
 * Action <code>CLOSE</code> will generate the ZIP archive which is returned as the pipe ouput.
 * </p>
 *
 * @ff.parameter filename only for <code>action=WRITE</code>: the filename of the zip-entry
 * @ff.parameter contents only for <code>action=WRITE</code>: contents of the zip-entry, If not specified, the input is used.
 *
 * @author Gerrit van Brakel
 * @author Niels Meijer
 * @since  7.9
 */
public class ZipWriterPipe extends AbstractCollectorPipe<ZipWriter, MessageZipEntry> {

	private @Getter boolean includeFileHeaders = false;

	private boolean backwardsCompatibility = false;

	public ZipWriterPipe() {
		setCollectionName("zipwriterhandle");
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ZipWriter.validateParametersForAction(getAction(), getParameterList());
	}

	@Override
	protected ZipWriter createCollector(Message input, PipeLineSession session) throws CollectionException {
		String filename = ParameterValueList.getValue(getParameterValueList(input, session), ZipWriter.PARAMETER_FILENAME, "");
		if(backwardsCompatibility && input.isRequestOfType(String.class) && StringUtils.isEmpty(filename)) {
			try {
				filename = input.asString();
			} catch (IOException e) {
				throw new CollectionException("unable to convert inputstring to filename");
			}
		}
		return new ZipWriter(includeFileHeaders, filename);
	}

	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		if(backwardsCompatibility) {
			try {
				if(Action.STREAM == getAction()) {
					return handleStreamAction(input, session);
				}

				input.preserve();
				super.doPipe(input, session);

				return new PipeRunResult(getSuccessForward(), input);
			} catch (IOException e) {
				throw new PipeRunException(this, "unable to preserve message for action ["+getAction()+"]", e);
			}
		}

		return super.doPipe(input, session);
	}

	private PipeRunResult handleStreamAction(Message input, PipeLineSession session) throws PipeRunException, IOException {
		try {
			ParameterValueList pvl = getParameterValueList(input, session);
			String filename = ParameterValueList.getValue(pvl, ZipWriter.PARAMETER_FILENAME, "");
			if(StringUtils.isEmpty(filename)) {
				throw new PipeRunException(this, "filename may not be empty");
			}

			Path collectorsTempFolder = TemporaryDirectoryUtils.getTempDirectory("collectors");
			Path file = Files.createTempFile(collectorsTempFolder, "msg", ".zip");

			// Unfortunately we cannot call doAction(Action.WRITE, PathMessage, session);
			// directly because the ParameterList is resolved against the input message.
			// We have to change the input here, but want to keep the original PVL.
			PathMessage tempZipArchive = PathMessage.asTemporaryMessage(file);
			addPartToCollection(getCollection(session), tempZipArchive, session, pvl);

			//We must return a file location, not the reference or file itself
			return new PipeRunResult(getSuccessForward(), new Message(file.toString()));
		} catch (CollectionException e) {
			throw new PipeRunException(this, "unable to preserve message for action ["+getAction()+"]", e);
		}
	}

	/**
	 * Session key used to refer to zip session. Must be specified with another value if ZipWriterPipes are nested. Deprecated, use collectionName instead.
	 * @ff.default zipwriterhandle
	 */
	@Deprecated(forRemoval = true, since = "7.9.0")
	@ConfigurationWarning("Replaced with attribute collectionName")
	public void setZipWriterHandle(String string) {
		setCollectionName(string);
	}

	/**
	 * Only for action='write': If set to <code>true</code>, the fields 'crc-32', 'compressed size' and 'uncompressed size' in the zip entry file header are set explicitly (note: compression ratio is zero)
	 * @ff.default false
	 */
	public void setCompleteFileHeader(boolean b) {
		includeFileHeaders = b;
	}

	/**
	 * When action is OPEN: If input is a string, it's assumed it's the location where to save the Zip Archive.
	 * When action is WRITE: Input will be 'piped' to the output, and the message will be preserved.
	 * Avoid using this if possible.
	 */
	@Deprecated(forRemoval = true, since = "7.9.0")
	public void setBackwardsCompatibility(boolean backwardsCompatibility) {
		this.backwardsCompatibility = backwardsCompatibility;
	}
}
