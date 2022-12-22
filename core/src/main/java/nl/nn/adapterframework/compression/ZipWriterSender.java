/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
import nl.nn.adapterframework.collection.CollectorSender;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Sender that writes an entry to a ZipStream, similar to ZipWriterPipe with action='write'.
 * Filename and contents are taken from parameters. If one of the parameters is not present, the input message
 * is used for either filename or contents.
 *
 * @ff.parameter filename filename of the zipentry
 * @ff.parameter contents contents of the zipentry
 *
 * @author  Gerrit van Brakel
 * @since   4.9.10
 */
public class ZipWriterSender extends CollectorSender<IZipWritingElement, ZipWriter> implements IZipWritingElement {

	private @Getter boolean closeInputstreamOnExit=true;
	private @Getter boolean closeOutputstreamOnExit=true;
	private @Getter String charset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	private @Getter boolean completeFileHeader=false;

	public ZipWriterSender() {
		super();
		setCollection("zipwriterhandle");
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ZipWriter.configure(getAction(), getParameterList());
	}

	@Override
	public ZipWriter openCollection(Message input, PipeLineSession session, ParameterValueList pvl) throws CollectionException {
		return ZipWriter.openCollection(input, session, pvl, this);
	}



	/**
	 * Only for action='write': If set to <code>false</code>, the inputstream is not closed after the zip entry is written
	 * @ff.default true
	 */
	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}

	/**
	 * Only for action='open': If set to <code>false</code>, the outputstream is not closed after the zip creation is finished
	 * @ff.default true
	 */
	public void setCloseOutputstreamOnExit(boolean b) {
		closeOutputstreamOnExit = b;
	}
	@Deprecated
	@ConfigurationWarning("attribute 'closeStreamOnExit' has been renamed to 'closeOutputstreamOnExit'")
	public void setCloseStreamOnExit(boolean b) {
		setCloseOutputstreamOnExit(b);
	}

	/**
	 * Only for action='write': Charset used to write strings to zip entries
	 * @ff.default utf-8
	 */
	public void setCharset(String string) {
		charset = string;
	}

	/**
	 * Session key used to refer to zip session. Must be specified with another value if ZipWriterPipes are nested
	 * @ff.default zipwriterhandle
	 */
	@Deprecated
	@ConfigurationWarning("Replaced with attribute collection")
	public void setZipWriterHandle(String string) {
		setCollection(string);
	}

	/**
	 * Only for action='write': If set to <code>true</code>, the fields 'crc-32', 'compressed size' and 'uncompressed size' in the zip entry file header are set explicitly (note: compression ratio is zero)
	 * @ff.default false
	 */
	public void setCompleteFileHeader(boolean b) {
		completeFileHeader = b;
	}



}
