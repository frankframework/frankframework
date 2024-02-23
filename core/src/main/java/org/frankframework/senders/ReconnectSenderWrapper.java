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

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.statistics.HasStatistics;
import org.frankframework.statistics.StatisticsKeeperIterationHandler;
import org.frankframework.stream.Message;

import lombok.Setter;

/**
 * Wrapper for senders, that opens the wrapped sender at runtime before each sender action, and closes it afterwards.
 * This prevents (long) open connections inside Senders and possible connection failures.
 * 
 * <b>Example:</b>
 * <pre><code>
 *   &lt;SenderPipe&gt;
 *     &lt;ReconnectSenderWrapper&gt;
 *        &lt;EchoSender myAttribute="myValue" /&gt;
 *     &lt;/ReconnectSenderWrapper&gt;
 *   &lt;/SenderPipe&gt;
 * </code></pre>
 * </p>
 *
 * @author  Niels Meijer
 */
public class ReconnectSenderWrapper extends SenderWrapperBase {

	/** specification of sender to send messages with */
	private @Setter ISender sender;

	@Override
	protected boolean isSenderConfigured() {
		return sender != null;
	}

	@Override
	public void configure() throws ConfigurationException {
		sender.configure();
		super.configure();
	}

	@Override
	public void open() throws SenderException {
		try {
			sender.open();
			super.open();
		} finally {
			sender.close(); //Only open to test the connection, close afterwards if all is ok.
		}
	}

	@Override
	public SenderResult doSendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		try {
			sender.open();
			return sender.sendMessage(message, session);
		} finally {
			sender.close();
		}
	}

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, Action action) throws SenderException {
		if (sender instanceof HasStatistics) {
			((HasStatistics) sender).iterateOverStatistics(hski,data,action);
		}
	}

	@Override
	public boolean isSynchronous() {
		return sender.isSynchronous();
	}
}
