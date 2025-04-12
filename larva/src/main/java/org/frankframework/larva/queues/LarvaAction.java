/*
   Copyright 2022-2025 WeAreFrank!

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

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.IListener;
import org.frankframework.core.IPullingListener;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.ISender;
import org.frankframework.core.IWithParameters;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.http.WebServiceListener;
import org.frankframework.jdbc.AbstractJdbcQuerySender.QueryType;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.larva.FileListener;
import org.frankframework.larva.FileSender;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.ListenerMessage;
import org.frankframework.larva.ListenerMessageHandler;
import org.frankframework.larva.SenderThread;
import org.frankframework.larva.XsltProviderListener;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.IParameter;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.StringUtil;

/**
 * This class is used to create and manage the lifecycle of Larva queues.
 * 
 * This class is a wrapper around the IConfigurable interface and handles the read and write operations.
 * 
 * @author Niels Meijer
 */
@Log4j2
public class LarvaAction {

	@Serial
	private static final long serialVersionUID = 1L;

	private static final String CONVERT_MESSAGE_TO_EXCEPTION_KEY = "convertExceptionToMessage";
	private static final String PIPELINESESSION_KEY = "session";
	private static final String MESSAGE_HANDLER_KEY = "listenerMessageHandler";
	private static final String LISTENER_MESSAGE_KEY = "listenerMessage";
	private static final String SENDER_THREAD_KEY = "defaultSenderThread";
	private static final String JDBC_INPUT_MESSAGE = "jdbcInputMessage";

	private final HashMap<String, Object> removeMeMap = new HashMap<>();

	private final String queueKey;

	public LarvaAction(IConfigurable configurable) {
		super();

		queueKey = StringUtil.lcFirst(ClassUtils.classNameOf(configurable));
		log.trace("registering FrankElement [{}] under key [{}]", configurable, queueKey);
		removeMeMap.put(queueKey, configurable);

		if (configurable instanceof IPushingListener listener) {
			ListenerMessageHandler<?> handler = new ListenerMessageHandler<>();
			listener.setHandler(handler);
			log.trace("registering PushingListener handler [{}] as [{}]", handler, MESSAGE_HANDLER_KEY);
			removeMeMap.put(MESSAGE_HANDLER_KEY, handler);
		}
	}

	public synchronized PipeLineSession getSession() {
		if (!removeMeMap.containsKey(PIPELINESESSION_KEY)) {
			PipeLineSession session = new PipeLineSession();
			removeMeMap.put(PIPELINESESSION_KEY, session);
			return session;
		}
		return (PipeLineSession) removeMeMap.get(PIPELINESESSION_KEY);
	}

	public ListenerMessageHandler getMessageHandler() {
		ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler) removeMeMap.get(MESSAGE_HANDLER_KEY);
		if (listenerMessageHandler == null && peek() instanceof IPushingListener) {
			throw new NoSuchElementException("No ListenerMessageHandler found");
		}
		return listenerMessageHandler;
	}

	public SenderThread getSenderThread() {
		return (SenderThread) removeMeMap.get(SENDER_THREAD_KEY);
	}

	private SenderThread removeSenderThread() {
		return (SenderThread) removeMeMap.remove(SENDER_THREAD_KEY);
	}

	public void invokeSetters(int defaultTimeout, Properties properties) {
		LarvaActionUtils.invokeSetters(peek(), properties);

		if(removeMeMap.containsKey(MESSAGE_HANDLER_KEY)) {
			if(defaultTimeout > 0) {
				getMessageHandler().setTimeout(defaultTimeout);
			}

			LarvaActionUtils.invokeSetters(getMessageHandler(), properties); // Set timeout properties
		}

		String convertException = properties.getProperty(CONVERT_MESSAGE_TO_EXCEPTION_KEY);
		removeMeMap.put(CONVERT_MESSAGE_TO_EXCEPTION_KEY, Boolean.valueOf(convertException));

		mapParameters(properties);

		// Keeps properties backwards compatible
		if (peek() instanceof FixedQuerySender jdbcSender) {
			String queryType = properties.getProperty("queryType", "select");
			jdbcSender.setQueryType(EnumUtils.parse(QueryType.class, queryType));

			String readQuery = properties.getProperty("readQuery");
			jdbcSender.setQuery(readQuery);
		}
	}

	private void mapParameters(Properties properties) {
		if(peek() instanceof IWithParameters withParameters) {
			Map<String, IParameter> paramPropertiesMap = LarvaActionUtils.createParametersMapFromParamProperties(properties, getSession());
			paramPropertiesMap.values().forEach(withParameters::addParameter);
		}
	}

	private IConfigurable peek() {
		return (IConfigurable) removeMeMap.get(queueKey);
	}

	public void configure() throws ConfigurationException {
		if(!(peek() instanceof WebServiceListener)) {// Requires a configuration as parent
			peek().configure();
		}
	}

	public void open() throws ConfigurationException {
		try {
			if(peek() instanceof ISender) {
				((ISender) peek()).start();
			}
			else if(peek() instanceof IListener<?>) {
				((IListener<?>) peek()).start();
			}
		} catch (LifecycleException e) {
			throw new ConfigurationException("error opening [" + peek() + "]", e);
		}
	}

	public void close() throws Exception {
		IConfigurable configurable = peek();
		if(configurable instanceof AutoCloseable autoCloseable) {
			autoCloseable.close();
		}
		else if(configurable instanceof ISender sender) {
			sender.stop();
		}
		else if(configurable instanceof IListener<?> listener) {
			listener.stop();
		}
		if (removeMeMap.containsKey(PIPELINESESSION_KEY)) {
			getSession().close();
		}
	}

	public int executeWrite(String stepDisplayName, Message fileContent, String correlationId, Map<String, Object> parameters) throws TimeoutException, SenderException, ListenerException {
		if (peek() instanceof FileSender fileSender) {
			fileSender.sendMessage(fileContent);
			return LarvaTool.RESULT_OK;
		}
		if (peek() instanceof XsltProviderListener xsltProviderListener) {
			xsltProviderListener.processRequest(fileContent, parameters);
			return LarvaTool.RESULT_OK;
		}
		if (peek() instanceof ISender sender) {
			if (sender instanceof FixedQuerySender) {
				getSession().put(JDBC_INPUT_MESSAGE, fileContent);
				return LarvaTool.RESULT_OK;
			}

			Boolean convertExceptionToMessage = (Boolean) removeMeMap.get(CONVERT_MESSAGE_TO_EXCEPTION_KEY);
			PipeLineSession session = getSession();
			SenderThread senderThread = new SenderThread(sender, fileContent, session, convertExceptionToMessage, correlationId);
			senderThread.start();
			removeMeMap.put(SENDER_THREAD_KEY, senderThread);
			return LarvaTool.RESULT_OK;
		}
		if (peek() instanceof IPushingListener<?>) {
			ListenerMessageHandler listenerMessageHandler = getMessageHandler();
			PipeLineSession context;
			ListenerMessage requestListenerMessage = (ListenerMessage) removeMeMap.get(LISTENER_MESSAGE_KEY);
			if (requestListenerMessage != null) {
				context = requestListenerMessage.getContext();
			} else {
				context = new PipeLineSession();
			}
			ListenerMessage listenerMessage = new ListenerMessage(fileContent, context);
			listenerMessageHandler.putResponseMessage(listenerMessage);
			return LarvaTool.RESULT_OK;
		}
		throw new SenderException("Could not perform executeWrite() for queue [" + peek() + "]");
	}

	public Message executeRead(String step, String stepDisplayName, Properties properties, String fileName, Message fileContent) throws SenderException, TimeoutException, ListenerException {
		if (peek() instanceof FileSender fileSender) {
			return fileSender.getMessage();
		}
		if (peek() instanceof FileListener fileListener) {
			return fileListener.getMessage();
		}
		if (peek() instanceof XsltProviderListener xsltProviderListener) {
			return xsltProviderListener.getResult();
		}
		if (peek() instanceof IPullingListener pullingListener) {
			Map<String, Object> threadContext = null;
			try {
				threadContext = pullingListener.openThread();
				RawMessageWrapper rawMessage = pullingListener.getRawMessage(threadContext);
				if (rawMessage != null) {
					Message message = pullingListener.extractMessage(rawMessage, threadContext);
					String correlationId = rawMessage.getId(); // NB: Historically this code extracted message-ID then used that as correlation-ID.
					removeMeMap.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
					return message;
				}
			} finally {
				if (threadContext != null) {
					pullingListener.closeThread(threadContext);
				}
			}
			throw new ListenerException("No message found in queue [" + peek() + "]");
		}
		if (peek() instanceof IPushingListener<?>) {
			ListenerMessageHandler<?> listenerMessageHandler = getMessageHandler();
			ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessage(0);

			if (listenerMessage != null) {
				removeMeMap.put(LISTENER_MESSAGE_KEY, listenerMessage);
				return listenerMessage.getMessage();
			}
			throw new ListenerException("No message found in queue [" + peek() + "]");
		}
		if (peek() instanceof ISender sender) {
			if (sender instanceof FixedQuerySender) {
				try (Message input = getSession().getMessage(JDBC_INPUT_MESSAGE)) { // Uses the provided message or NULL
					return sender.sendMessageOrThrow(input, getSession());
				}
			}

			SenderThread senderThread = removeSenderThread();
			if (senderThread == null) {
				throw new SenderException("No SenderThread found, no corresponding write request?");
			}
			SenderException senderException = senderThread.getSenderException();
			if (senderException != null) {
				throw senderException;
			}
			TimeoutException timeoutException = senderThread.getTimeoutException();
			if (timeoutException != null) {
				throw timeoutException;
			}
			return senderThread.getResponse();
		}
		throw new SenderException("Could not perform executeRead() for queue [" + peek() + "]");
	}
}
