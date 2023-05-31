/*
   Copyright 2013 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import nl.nn.adapterframework.collection.CollectorPipeBase.Action;
import nl.nn.adapterframework.collection.CollectorSenderBase;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;

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
public class ZipWriterSender extends CollectorSenderBase<ZipWriter, MessageZipEntry> {

	@Override
	public void configure() throws ConfigurationException {
		setCollectionName("zipwriterhandle");
		super.configure();
		ZipWriter.validateParametersForAction(Action.WRITE, getParameterList());
	}

	/**
	 * Session key used to refer to zip session. Must be specified with another value if ZipWriterPipes are nested
	 * @ff.default zipwriterhandle
	 */
	@Deprecated
	@ConfigurationWarning("Replaced with attribute collection")
	public void setZipWriterHandle(String string) {
		setCollectionName(string);
	}
}
