/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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
package org.frankframework.batch;

import java.util.List;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISender;
import org.frankframework.core.ISenderWithParameters;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;

/**
 * Translate a record into XML, then send it using a sender.
 *
 * @ff.parameters any parameters defined on the recordHandler will be handed to the sender, if this is a {@link ISenderWithParameters ISenderWithParameters}
 *
 * @author  John Dekker
 * @deprecated Warning: non-maintained functionality.
 */
public class RecordXml2Sender extends RecordXmlTransformer {

	private @Getter ISender sender = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (sender==null) {
			throw new ConfigurationException(ClassUtils.nameOf(this)+" has no sender");
		}
		sender.configure();
	}

	@Override
	public void open() throws SenderException {
		super.open();
		sender.start();
	}
	@Override
	public void close() throws SenderException {
		super.close();
		sender.stop();
	}

	@Override
	public String handleRecord(PipeLineSession session, List<String> parsedRecord) throws Exception {
		String xml = super.handleRecord(session,parsedRecord);
		try (Message message = getSender().sendMessageOrThrow(new Message(xml), session)) {
			return message.asString();
		}
	}


	/** Sender that needs to handle the (XML) record */
	public void setSender(ISender sender) {
		this.sender = sender;
	}
}
