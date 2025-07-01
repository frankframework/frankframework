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
package org.frankframework.compression;

import jakarta.annotation.Nonnull;

import org.frankframework.collection.AbstractCollectorPipe.Action;
import org.frankframework.collection.AbstractCollectorSender;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;

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

	private boolean backwardsCompatibility = false;

	public ZipWriterSender() {
		setCollectionName("zipwriterhandle");
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ZipWriter.validateParametersForAction(Action.WRITE, getParameterList());
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		if(backwardsCompatibility) {
			super.sendMessage(message, session);
			return new SenderResult(message);
		}

		return super.sendMessage(message, session);
	}

	/**
	 * Session key used to refer to zip session. Must be specified with another value if ZipWriterPipes are nested
	 * @ff.default zipwriterhandle
	 */
	@Deprecated(forRemoval = true, since = "7.9.0")
	@ConfigurationWarning("Replaced with attribute collectionName")
	public void setZipWriterHandle(String string) {
		setCollectionName(string);
	}

	/**
	 * Input will be 'piped' to the output, and the message will be preserved. Avoid using this if possible.
	 */
	@Deprecated(forRemoval = true, since = "7.9.0")
	public void setBackwardsCompatibility(boolean backwardsCompatibility) {
		this.backwardsCompatibility = backwardsCompatibility;
	}
}
