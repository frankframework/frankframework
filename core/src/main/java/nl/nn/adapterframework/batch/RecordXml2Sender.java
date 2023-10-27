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
package nl.nn.adapterframework.batch;

import java.util.List;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

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
		sender.open();
	}
	@Override
	public void close() throws SenderException {
		super.close();
		sender.close();
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
