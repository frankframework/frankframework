/*
   Copyright 2022-2024 WeAreFrank!

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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.IListener;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.ISender;
import org.frankframework.core.IWithParameters;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.http.WebServiceListener;
import org.frankframework.larva.FileListener;
import org.frankframework.larva.FileSender;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.ListenerMessage;
import org.frankframework.larva.ListenerMessageHandler;
import org.frankframework.larva.SenderThread;
import org.frankframework.larva.XsltProviderListener;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.Parameter;
import org.frankframework.senders.DelaySender;
import org.frankframework.stream.FileMessage;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlUtils;

@Log4j2
public class QueueWrapper extends HashMap<String, Object> implements Queue {
	private static final String CONVERT_MESSAGE_TO_EXCEPTION_KEY = "convertExceptionToMessage";
	private static final String PIPELINESESSION_KEY = "session";
	private static final String MESSAGE_HANDLER_KEY = "listenerMessageHandler";
	private static final String SENDER_THREAD_KEY = "defaultSenderThread";
	private final String queueKey;

	private QueueWrapper(IConfigurable configurable) {
		super();

		queueKey = StringUtil.lcFirst(configurable.getClass().getSimpleName());
		put(queueKey, configurable);

		if (configurable instanceof IPushingListener listener) {
			ListenerMessageHandler<?> handler = new ListenerMessageHandler<>();
			listener.setHandler(handler);
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

	public void setSenderThread(SenderThread senderThread) {
		put(SENDER_THREAD_KEY, senderThread);
	}

	public SenderThread getSenderThread() {
		return (SenderThread) get(SENDER_THREAD_KEY);
	}

	public void removeSenderThread() {
		remove(SENDER_THREAD_KEY);
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
		put(CONVERT_MESSAGE_TO_EXCEPTION_KEY, Boolean.valueOf(convertException));

		mapParameters(properties);

		return this;
	}

	private void mapParameters(Properties properties) {
		if(get() instanceof IWithParameters withParameters) {
			Map<String, IParameter> paramPropertiesMap = createParametersMapFromParamProperties(properties, getSession());
			paramPropertiesMap.values().forEach(withParameters::addParameter);
		}
	}

	public static Map<String, IParameter> createParametersMapFromParamProperties(Properties properties, PipeLineSession session) {
		final String _name = ".name";
		final String _param = "param";
		final String _type = ".type";
		Map<String, IParameter> result = new HashMap<>();
		boolean processed = false;
		int i = 1;
		while (!processed) {
			String name = properties.getProperty(_param + i + _name);
			if (name != null) {
				String type = properties.getProperty(_param + i + _type);
				String propertyValue = properties.getProperty(_param + i + ".value");
				Object value = propertyValue;

				if (value == null) {
					String filename = properties.getProperty(_param + i + ".valuefile.absolutepath");
					if (filename != null) {
						value = new FileMessage(new File(filename));
					} else {
						String inputStreamFilename = properties.getProperty(_param + i + ".valuefileinputstream.absolutepath");
						if (inputStreamFilename != null) {
							throw new IllegalStateException("valuefileinputstream is no longer supported use valuefile instead");
						}
					}
				}
				if ("node".equals(type)) {
					try {
						value = XmlUtils.buildNode(propertyValue, true);
					} catch (DomBuilderException e) {
						throw new IllegalStateException("Could not build node for parameter '" + name + "' with value: " + value, e);
					}
				} else if ("domdoc".equals(type)) {
					try {
						value = XmlUtils.buildDomDocument(propertyValue, true);
					} catch (DomBuilderException e) {
						throw new IllegalStateException("Could not build node for parameter '" + name + "' with value: " + value, e);
					}
				} else if ("list".equals(type)) {
					value = StringUtil.split(propertyValue);
				} else if ("map".equals(type)) {
					List<String> parts = StringUtil.split(propertyValue);
					Map<String, String> map = new LinkedHashMap<>();
					for (String part : parts) {
						String[] splitted = part.split("\\s*(=\\s*)+", 2);
						if (splitted.length == 2) {
							map.put(splitted[0], splitted[1]);
						} else {
							map.put(splitted[0], "");
						}
					}
					value = map;
				}
				String pattern = properties.getProperty(_param + i + ".pattern");
				if (value == null && pattern == null) {
					throw new IllegalStateException("Property '" + _param + i + " doesn't have a value or pattern");
				} else {
					try {
						Parameter parameter = new Parameter();
						parameter.setName(name);
						if (value != null && !(value instanceof String)) {
							parameter.setSessionKey(name);
							session.put(name, value);
						} else {
							parameter.setValue((String) value);
							parameter.setPattern(pattern);
						}
						parameter.configure();
						result.put(name, parameter);
					} catch (ConfigurationException e) {
						throw new IllegalStateException("Parameter '" + name + "' could not be configured");
					}
				}
				i++;
			} else {
				processed = true;
			}
		}
		return result;
	}


	public IConfigurable get() {
		return (IConfigurable) get(queueKey);
	}

	public static QueueWrapper create(IConfigurable configurable, Properties queueProperties, int defaultTimeout, String correlationId) { //TODO use correlationId
		QueueUtils.invokeSetters(configurable, queueProperties);
		return create(configurable).invokeSetters(defaultTimeout, queueProperties);
	}

	@Override
	public void configure() throws ConfigurationException {
		if(!(get() instanceof WebServiceListener)) {//requires a configuration as parent
			get().configure();
		}
	}

	@Override
	public void open() throws ConfigurationException {
		try {
			if(get() instanceof ISender) {
				((ISender) get()).start();
			}
			else if(get() instanceof IListener<?>) {
				((IListener<?>) get()).start();
			}
		} catch (LifecycleException e) {
			throw new ConfigurationException("error opening [" + get() + "]", e);
		}
	}

	public void close() throws Exception {
		IConfigurable configurable = get();
		if(configurable instanceof AutoCloseable autoCloseable) {
			autoCloseable.close();
		}
		else if(configurable instanceof ISender sender) {
			sender.stop();
		}
		else if(configurable instanceof IListener<?> listener) {
			listener.stop();
		}
		if (containsKey(PIPELINESESSION_KEY)) {
			getSession().close();
		}
	}

	@Override
	public int executeWrite(String stepDisplayName, String fileContent, String correlationId, Map<String, Object> parameters) throws TimeoutException, SenderException, ListenerException {
		if (get() instanceof FileSender fileSender) {
			fileSender.sendMessage(fileContent);
			return LarvaTool.RESULT_OK;
		}
		if (get() instanceof DelaySender delaySender) {
			try (PipeLineSession session = new PipeLineSession(); Message message = new Message(fileContent); ) {
				SenderResult senderResult = delaySender.sendMessage(message, session);
				CloseUtils.closeSilently(senderResult.getResult());
			}
			return LarvaTool.RESULT_OK;
		}
		if (get() instanceof XsltProviderListener xsltProviderListener) {
			xsltProviderListener.processRequest(fileContent, parameters);
			return LarvaTool.RESULT_OK;
		}
		if (get() instanceof ISender sender) {
			Boolean convertExceptionToMessage = (Boolean) get(CONVERT_MESSAGE_TO_EXCEPTION_KEY);
			PipeLineSession session = getSession();
			SenderThread senderThread = new SenderThread(sender, fileContent, session, convertExceptionToMessage, correlationId);
			senderThread.start();
			put(SENDER_THREAD_KEY, senderThread);
			setSenderThread(senderThread); // 'put' and 'set' do something similar
			return LarvaTool.RESULT_OK;
		}
		if (get() instanceof IListener<?>) {
			ListenerMessageHandler listenerMessageHandler = getMessageHandler();
			if (listenerMessageHandler == null) {
				throw new NoSuchElementException("No ListenerMessageHandler found");
			}
			PipeLineSession context;
			ListenerMessage requestListenerMessage = (ListenerMessage) get("listenerMessage");
			if (requestListenerMessage != null) {
				context = requestListenerMessage.getContext();
			} else {
				context = new PipeLineSession();
			}
			ListenerMessage listenerMessage = new ListenerMessage(fileContent, context);
			listenerMessageHandler.putResponseMessage(listenerMessage);
			return LarvaTool.RESULT_OK;
		}
		throw new SenderException("Could not perform executeWrite() for queue [" + get() + "]");
	}


	@Override
	public String executeRead(String step, String stepDisplayName, Properties properties, String fileName, String fileContent) throws SenderException, IOException, TimeoutException, ListenerException {
		if (get() instanceof FileSender fileSender) {
			return fileSender.getMessage();
		}
		if (get() instanceof FileListener fileListener) {
			return fileListener.getMessage();
		}
		if (get() instanceof XsltProviderListener xsltProviderListener) {
			return xsltProviderListener.getResult();
		}
		if (get() instanceof ISender) {
			SenderThread senderThread = (SenderThread) remove(SENDER_THREAD_KEY);
			removeSenderThread();
			if (senderThread == null) {
				throw new SenderException("No SenderThread found, no corresponding write request?");
			}
			SenderException senderException = senderThread.getSenderException();
			if (senderException != null) {
				throw senderException;
			}
			IOException ioException = senderThread.getIOException();
			if (ioException != null) {
				throw ioException;
			}
			TimeoutException timeoutException = senderThread.getTimeoutException();
			if (timeoutException != null) {
				throw timeoutException;
			}
			return senderThread.getResponse();
		}
		throw new SenderException("Could not perform executeRead() for queue [" + get() + "]");
	}
}
