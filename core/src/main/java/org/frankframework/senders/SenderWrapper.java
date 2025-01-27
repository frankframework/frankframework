/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2022, 2025 WeAreFrank!

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
package org.frankframework.senders;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISender;
import org.frankframework.core.ISenderWithParameters;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;

/**
 * Wrapper for senders, that allows to get input from a session variable, and to store output in a session variable.
 *
 * @ff.parameters any parameters defined on the SenderWrapper will be handed to the sender, if this is a {@link ISenderWithParameters}
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class SenderWrapper extends AbstractSenderWrapper {

	/** specification of sender to send messages with */
	private @Getter @Setter ISender sender;

	@Override
	protected boolean isSenderConfigured() {
		return getSender()!=null;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		getSender().configure();
	}
	@Override
	public void start() {
		getSender().start();
		super.start();
	}
	@Override
	public void stop() {
		super.stop();
		getSender().stop();
	}

	@Override
	public SenderResult doSendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		return sender.sendMessage(message, session);
	}

	@Override
	public boolean isSynchronous() {
		return getSender().isSynchronous();
	}
}
