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
package org.frankframework.larva.actions;

import java.util.Map;
import java.util.Properties;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.http.WebServiceListener;
import org.frankframework.larva.ListenerMessage;
import org.frankframework.larva.ListenerMessageHandler;
import org.frankframework.stream.Message;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class LarvaPushingListenerAction extends AbstractLarvaAction<IPushingListener> {
	private ListenerMessageHandler<?> listenerMessageHandler;
	private ListenerMessage listenerMessage;

	public LarvaPushingListenerAction(IPushingListener listener) {
		super(listener);

		listenerMessageHandler = new ListenerMessageHandler<>();
		listener.setHandler(listenerMessageHandler);
	}

	public ListenerMessageHandler getMessageHandler() {
		return listenerMessageHandler;
	}

	@Override
	public void configure() throws ConfigurationException {
		if(!(peek() instanceof WebServiceListener)) {// Requires a configuration as parent
			super.configure();
		}
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
	public void invokeSetters(int defaultTimeout, Properties properties) {
		super.invokeSetters(defaultTimeout, properties);

		if(defaultTimeout > 0) {
			getMessageHandler().setTimeout(defaultTimeout);
		}

		LarvaActionUtils.invokeSetters(getMessageHandler(), properties); // Set timeout properties
	}

	@Override
	public void executeWrite(Message fileContent, String correlationId, Map<String, Object> parameters) throws TimeoutException, SenderException, ListenerException {
		PipeLineSession context;
		if (listenerMessage != null) { // Reuse the context from the previous message
			context = listenerMessage.getContext();
		} else {
			context = getSession();
		}

		ListenerMessage listenerMessage = new ListenerMessage(fileContent, context);
		listenerMessageHandler.putResponseMessage(listenerMessage);
	}

	@Override
	public Message executeRead(Properties properties) throws SenderException, TimeoutException, ListenerException {
		ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessage(0);

		if (listenerMessage != null) {
			this.listenerMessage = listenerMessage;
			return listenerMessage.getMessage();
		}
		throw new ListenerException("no message found in queue [" + peek() + "]");
	}
}
