/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.larva.queues;

import java.util.Map;
import java.util.Properties;

import lombok.Getter;

import org.frankframework.core.ISender;
import org.frankframework.core.ListenerException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.SenderThread;
import org.frankframework.stream.Message;

public class SenderAction extends AbstractLarvaAction<ISender> {
	private @Getter SenderThread senderThread;

	private Message jdbcInputMessage;

	public SenderAction(ISender sender) {
		super(sender);
	}

	@Override
	public void start() {
		peek().start();
	}

	@Override
	public void stop() {
		peek().stop();
	}

	@Override
	public int executeWrite(String stepDisplayName, Message fileContent, String correlationId, Map<String, Object> parameters) throws TimeoutException, SenderException, ListenerException {
		if (peek() instanceof FixedQuerySender) { // QuerySender is reversed, write is read. Just copy the input message here.
			jdbcInputMessage = fileContent;
			return LarvaTool.RESULT_OK;
		}

		SenderThread senderThread = new SenderThread(peek(), fileContent, getSession(), isConvertExceptionToMessage(), correlationId);
		senderThread.start();
		this.senderThread = senderThread;
		return LarvaTool.RESULT_OK;
	}

	@Override
	public Message executeRead(String step, String stepDisplayName, Properties properties, String fileName, Message fileContent) throws SenderException, TimeoutException, ListenerException {
		if (peek() instanceof FixedQuerySender) { // QuerySender is reversed, read is write. Execute the query here.
			try (Message input = Message.asMessage(jdbcInputMessage)) { // Uses the provided message or NULL
				return peek().sendMessageOrThrow(input, getSession());
			}
		}

		if (senderThread == null) {
			throw new SenderException("no SenderThread found, write step not executed?");
		}
		SenderException senderException = senderThread.getSenderException();
		if (senderException != null) {
			throw senderException;
		}
		TimeoutException timeoutException = senderThread.getTimeoutException();
		if (timeoutException != null) {
			throw timeoutException;
		}

		Message response = senderThread.getResponse();
		senderThread = null; // Cleanup
		return response;
	}
}
