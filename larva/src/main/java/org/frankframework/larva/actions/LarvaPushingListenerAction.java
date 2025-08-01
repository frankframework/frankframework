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
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.TimeoutException;
import org.frankframework.http.WebServiceListener;
import org.frankframework.larva.ListenerMessage;
import org.frankframework.larva.ListenerMessageHandler;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class LarvaPushingListenerAction extends AbstractLarvaAction<IPushingListener> {
	private final ListenerMessageHandler<?> listenerMessageHandler;
	private ListenerMessage listenerMessage;

	public LarvaPushingListenerAction(IPushingListener listener, long timeout) {
		super(listener);

		listenerMessageHandler = new ListenerMessageHandler<>(timeout);
		listener.setHandler(listenerMessageHandler);
	}

	public ListenerMessageHandler getMessageHandler() {
		return listenerMessageHandler;
	}

	@Override
	public void configure() throws ConfigurationException {
		if(!(peek() instanceof WebServiceListener)) { // Requires a configuration as parent
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
	public void invokeSetters(Properties properties) {
		super.invokeSetters(properties);
		ClassUtils.invokeSetters(getMessageHandler(), properties); // Set timeout properties
	}

	@Override
	public void executeWrite(Message fileContent, String correlationId, Map<String, Object> parameters) {
		PipeLineSession context;
		if (listenerMessage != null) { // Reuse the context from the previous message
			context = listenerMessage.getContext();
		} else {
			context = getSession();
		}

		listenerMessageHandler.putResponseMessage(new ListenerMessage(fileContent, context));
	}

	@Override
	@SuppressWarnings("java:S1117")
	public Message executeRead(Properties properties) throws TimeoutException {
		ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessageWithDefaultTimeout();
		this.listenerMessage = listenerMessage;
		return listenerMessage.getMessage();
	}
}
