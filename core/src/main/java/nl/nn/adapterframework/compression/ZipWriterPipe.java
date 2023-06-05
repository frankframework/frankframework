/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package nl.nn.adapterframework.compression;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.collection.CollectionException;
import nl.nn.adapterframework.collection.CollectorPipeBase;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.PathMessage;
import nl.nn.adapterframework.util.FileUtils;

/**
 * Pipe that creates a ZipStream.
 *
 * For action=open, the Pipe will create a new zip, that will be written to a file or stream specified by the input message, that must be a:<ul>
 * <li>String specifying a filename</li>
 * <li>OutputStream</li>
 * <li>HttpResponse</li>
 * </ul>
 *
 * @ff.parameter filename with action=open: the filename if the input is a HttpResponse; with action=write: the entryfilename
 * @ff.parameter contents only for action=write: contents of the zipentry, If not specified, the input is used.
 *
 * @author  Gerrit van Brakel
 * @since   4.9.10
 */
public class ZipWriterPipe extends CollectorPipeBase<ZipWriter, MessageZipEntry> {

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
		if(backwardsCompatibility && input.asObject() instanceof String && StringUtils.isEmpty(filename)) {
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

			File collectorsTempFolder = FileUtils.getTempDirectory("collectors");
			File file = File.createTempFile("msg", ".zip", collectorsTempFolder);

//			doAction(Action.WRITE, PathMessage.asTemporaryMessage(file.toPath()), session);
			PathMessage tempZipArchive = PathMessage.asTemporaryMessage(file.toPath());
			addPartToCollection(getCollection(session), tempZipArchive, session, pvl);

			//We must return a file location, not the reference or file it self
			return new PipeRunResult(getSuccessForward(), new Message(file.getAbsolutePath()));
		} catch (CollectionException e) {
			throw new PipeRunException(this, "unable to preserve message for action ["+getAction()+"]", e);
		}
	}

	/**
	 * Session key used to refer to zip session. Must be specified with another value if ZipWriterPipes are nested
	 * @ff.default zipwriterhandle
	 */
	@Deprecated
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
	@Deprecated
	public void setBackwardsCompatibility(boolean backwardsCompatibility) {
		this.backwardsCompatibility = backwardsCompatibility;
	}
}
