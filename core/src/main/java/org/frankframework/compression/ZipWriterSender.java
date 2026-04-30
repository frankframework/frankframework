/*
   Copyright 2013 Nationale-Nederlanden, 2020-2026 WeAreFrank!

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

import org.frankframework.collection.AbstractCollectorPipe.Action;
import org.frankframework.collection.AbstractCollectorSender;
import org.frankframework.configuration.ConfigurationException;

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
public class ZipWriterSender extends AbstractCollectorSender<ZipWriter, MessageZipEntry> {

	public ZipWriterSender() {
		setCollectionName("zipwriterhandle");
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ZipWriter.validateParametersForAction(Action.WRITE, getParameterList());
	}
}
