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

import org.frankframework.core.IPullingListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class PullingListenerAction extends AbstractLarvaAction<IPullingListener> {

	public PullingListenerAction(IPullingListener listener) {
		super(listener);
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
		throw new ListenerException("no write step for pulling listener [" + peek() + "]");
	}

	@Override
	public Message executeRead(String step, String stepDisplayName, Properties properties, String fileName, Message fileContent) throws SenderException, TimeoutException, ListenerException {
		Map<String, Object> threadContext = null;
		IPullingListener pullingListener = peek();
		try {
			threadContext = pullingListener.openThread();
			RawMessageWrapper rawMessage = pullingListener.getRawMessage(threadContext);
			if (rawMessage != null) {
				return pullingListener.extractMessage(rawMessage, threadContext);
			}
		} finally {
			if (threadContext != null) {
				pullingListener.closeThread(threadContext);
			}
			threadContext = null;
		}
		throw new ListenerException("No message found in queue [" + peek() + "]");
	}

}
