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

import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.AdapterAware;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;

/**
 * Wrapper for senders, that opens the wrapped sender at runtime before each sender action, and closes it afterwards.
 * This prevents (long) open connections inside Senders and possible connection failures.
 *
 * <b>Example:</b>
 * <pre>{@code
 * <SenderPipe>
 *     <ReconnectSenderWrapper>
 *         <EchoSender myAttribute="myValue" />
 *     </ReconnectSenderWrapper>
 * </SenderPipe>
 * }</pre>
 * </p>
 *
 * @author  Niels Meijer
 */
public class ReconnectSenderWrapper extends AbstractSenderWrapper {

	/** specification of sender to send messages with */
	private @Setter ISender sender;

	@Override
	protected boolean isSenderConfigured() {
		return sender != null;
	}

	@Override
	public void configure() throws ConfigurationException {
		sender.configure();
		if(sender instanceof AdapterAware aware) {
			aware.setAdapter(adapter);
		}

		super.configure();
	}

	@Override
	public void start() {
		try {
			sender.start();
			super.start();
		} finally {
			sender.stop(); // Only open to test the connection, close afterwards if all is ok.
		}
	}

	@Override
	public SenderResult doSendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		sender.start();
		session.scheduleCloseOnSessionExit(new AutoCloseableSenderWrapper(sender), this.toString());
		return sender.sendMessage(message, session);
	}

	public class AutoCloseableSenderWrapper implements AutoCloseable {
		private final ISender sender;

		public AutoCloseableSenderWrapper(ISender sender) {
			this.sender = sender;
		}

		@Override
		public void close() {
			try {
				log.debug("Closing sender after use: [{}]", sender.getName());
				sender.stop();
			} catch (LifecycleException e) {
				log.warn("Error closing sender: [{}]", sender.getName(), e);
			}
		}

	}
	@Override
	public boolean isSynchronous() {
		return sender.isSynchronous();
	}
}
