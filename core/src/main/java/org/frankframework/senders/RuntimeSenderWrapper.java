/*
   Copyright 2024 WeAreFrank!

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
import org.frankframework.statistics.HasStatistics;
import org.frankframework.statistics.StatisticsKeeperIterationHandler;
import org.frankframework.stream.Message;

/**
 * Wrapper for senders, that opens 'runtime' before each sender action, and closes afterwards. Prevents long open connections inside Senders and possible connection failures. 
 *
 * @ff.parameters any parameters defined on the SenderWrapper will be handed to the sender, if this is a {@link ISenderWithParameters}
 *
 * @author  Niels Meijer
 */
public class RuntimeSenderWrapper extends SenderWrapperBase {

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
	public void open() throws SenderException {
		try {
			getSender().open();
			super.open();
		} finally {
			getSender().close(); //Only open to test the connection, close afterwards if all is ok.
		}
	}

	@Override
	public SenderResult doSendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		try {
			getSender().open();
			return sender.sendMessage(message, session);
		} finally {
			getSender().close();
		}
	}

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, Action action) throws SenderException {
		if (getSender() instanceof HasStatistics) {
			((HasStatistics)getSender()).iterateOverStatistics(hski,data,action);
		}
	}

	@Override
	public boolean isSynchronous() {
		return getSender().isSynchronous();
	}
}
