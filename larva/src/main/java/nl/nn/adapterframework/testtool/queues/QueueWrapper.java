/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.testtool.queues;

import java.util.HashMap;
import java.util.Properties;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.testtool.ListenerMessageHandler;

public class QueueWrapper extends HashMap<String, Object> {
	private static final String CONVERT_MESSAGE_TO_EXCEPTION_KEY = "convertExceptionToMessage";
	private static final String PIPELINESESSION_KEY = "session";
	private static final String MESSAGE_HANDLER_KEY = "listenerMessageHandler";
	private final String queueKey;

	private QueueWrapper(IConfigurable configurable) {
		super();

		queueKey = QueueUtils.firstCharToLower(configurable.getClass().getSimpleName());
		put(queueKey, configurable);

		if(configurable instanceof IPushingListener) {
			ListenerMessageHandler<?> handler = new ListenerMessageHandler<>();
			((IPushingListener) configurable).setHandler(handler);
			put(MESSAGE_HANDLER_KEY, handler);
		}
	}

	public static QueueWrapper create(IConfigurable configurable) {
		return new QueueWrapper(configurable);
	}

	public synchronized PipeLineSession getSession() {
		if(!containsKey(PIPELINESESSION_KEY)) {
			PipeLineSession session = new PipeLineSession();
			put(PIPELINESESSION_KEY, session);
			return session;
		}
		return (PipeLineSession) get(PIPELINESESSION_KEY);
	}

	public ListenerMessageHandler getMessageHandler() {
		return (ListenerMessageHandler) get(MESSAGE_HANDLER_KEY);
	}

	public QueueWrapper invokeSetters(Properties queueProperties) {
		return invokeSetters(-1, queueProperties);
	}

	public QueueWrapper invokeSetters(int defaultTimeout, Properties properties) {
		if(containsKey(MESSAGE_HANDLER_KEY)) {
			if(defaultTimeout > 0) {
				getMessageHandler().setTimeout(defaultTimeout);
			}

			QueueUtils.invokeSetters(getMessageHandler(), properties); //set timeout properties
		}

		String convertException = properties.getProperty(CONVERT_MESSAGE_TO_EXCEPTION_KEY);
		put(CONVERT_MESSAGE_TO_EXCEPTION_KEY, new Boolean(convertException));

		return this;
	}

	public IConfigurable get() {
		return (IConfigurable) get(queueKey);
	}

	public static QueueWrapper createQueue(String className, Properties properties, int defaultTimeout, String queueName) {
		Properties queueProperties = QueueUtils.getSubProperties(properties, queueName);
		return createQueue(className, queueProperties, defaultTimeout);
	}

	public static QueueWrapper createQueue(String className, Properties queueProperties, int defaultTimeout) {
		IConfigurable configurable = QueueUtils.createInstance(className);
		QueueUtils.invokeSetters(configurable, queueProperties);
		return create(configurable).invokeSetters(defaultTimeout, queueProperties);
	}

	public void configure() throws ConfigurationException {
		get().configure();
	}

	public void open() throws ConfigurationException {
		try {
			if(get() instanceof ISender) {
				((ISender) get()).open();
			}
			else if(get() instanceof IListener<?>) {
				((IListener<?>) get()).open();
			}
		} catch (SenderException | ListenerException e) {
			throw new ConfigurationException("error opening ["+get()+"]", e);
		}
	}
}
