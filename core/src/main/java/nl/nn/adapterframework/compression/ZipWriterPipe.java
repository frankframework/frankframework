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

import lombok.Getter;
import nl.nn.adapterframework.collection.CollectionException;
import nl.nn.adapterframework.collection.CollectorPipeBase;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;

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

	private @Getter boolean includeFileHeaders=false;

	@Override
	public void configure() throws ConfigurationException {
		setCollectionName("zipwriterhandle");
		super.configure();
		ZipWriter.configure(getAction(), getParameterList());
	}

	@Override
	protected ZipWriter createCollector() throws CollectionException {
		return new ZipWriter(includeFileHeaders);
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
}
