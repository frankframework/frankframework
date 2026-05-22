/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020-2026 WeAreFrank!

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

import lombok.Getter;

import org.frankframework.collection.AbstractCollectorPipe;
import org.frankframework.collection.CollectionException;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

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
		return new ZipWriter(includeFileHeaders, filename);
	}

	/**
	 * Only for action='write': If set to <code>true</code>, the fields 'crc-32', 'compressed size' and 'uncompressed size' in the zip entry file header are set explicitly (note: compression ratio is zero)
	 * @ff.default false
	 */
	public void setCompleteFileHeader(boolean b) {
		includeFileHeaders = b;
	}
}
