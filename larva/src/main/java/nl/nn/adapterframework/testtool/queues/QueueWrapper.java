/*
   Copyright 2022-2023 WeAreFrank!

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import lombok.extern.log4j.Log4j2;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.DelaySender;
import nl.nn.adapterframework.stream.FileMessage;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testtool.FileListener;
import nl.nn.adapterframework.testtool.FileSender;
import nl.nn.adapterframework.testtool.HttpServletResponseMock;
import nl.nn.adapterframework.testtool.ListenerMessage;
import nl.nn.adapterframework.testtool.ListenerMessageHandler;
import nl.nn.adapterframework.testtool.SenderThread;
import nl.nn.adapterframework.testtool.TestTool;
import nl.nn.adapterframework.testtool.XsltProviderListener;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.StringUtil;
import nl.nn.adapterframework.util.XmlUtils;

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
		if(get() instanceof IWithParameters) {
			Map<String, Object> paramPropertiesMap = createParametersMapFromParamProperties(properties, true, getSession());
			Iterator<String> parameterNameIterator = paramPropertiesMap.keySet().iterator();
			while (parameterNameIterator.hasNext()) {
				String parameterName = parameterNameIterator.next();
				Parameter parameter = (Parameter)paramPropertiesMap.get(parameterName);
				((IWithParameters) get()).addParameter(parameter);
			}
		}
	}

	public static Map<String, Object> createParametersMapFromParamProperties(Properties properties, boolean createParameterObjects, PipeLineSession session) {
		final String _name = ".name";
		final String _param = "param";
		final String _type = ".type";
		Map<String, Object> result = new HashMap<>();
		boolean processed = false;
		int i = 1;
		while (!processed) {
			String name = properties.getProperty(_param + i + _name);
			if (name != null) {
				Object value;
				String type = properties.getProperty(_param + i + _type);
				if ("httpResponse".equals(type)) {
					String outputFile;
					String filename = properties.getProperty(_param + i + ".filename");
					if (filename != null) {
						outputFile = properties.getProperty(_param + i + ".filename.absolutepath");
					} else {
						outputFile = properties.getProperty(_param + i + ".outputfile");
					}
					HttpServletResponseMock httpServletResponseMock = new HttpServletResponseMock();
					httpServletResponseMock.setOutputFile(outputFile);
					value = httpServletResponseMock;
				}
				else {
					value = properties.getProperty(_param + i + ".value");
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
				}
				if ("node".equals(type)) {
					try {
						value = XmlUtils.buildNode(Message.asString(value), true);
					} catch (DomBuilderException | IOException e) {
						throw new IllegalStateException("Could not build node for parameter '" + name + "' with value: " + value, e);
					}
				} else if ("domdoc".equals(type)) {
					try {
						value = XmlUtils.buildDomDocument(Message.asString(value), true);
					} catch (DomBuilderException | IOException e) {
						throw new IllegalStateException("Could not build node for parameter '" + name + "' with value: " + value, e);
					}
				} else if ("list".equals(type)) {
					try {
						List<String> parts = new ArrayList<>(Arrays.asList(Message.asString(value).split("\\s*(,\\s*)+")));
						List<String> list = new LinkedList<>();
						for (String part : parts) {
							list.add(part);
						}
						value = list;
					} catch (IOException e) {
						throw new IllegalStateException("Could not build a list for parameter '" + name + "' with value: " + value, e);
					}
				} else if ("map".equals(type)) {
					try {
						List<String> parts = new ArrayList<>(Arrays.asList(Message.asString(value).split("\\s*(,\\s*)+")));
						Map<String, String> map = new LinkedHashMap<>();
						for (String part : parts) {
							String[] splitted = part.split("\\s*(=\\s*)+", 2);
							if (splitted.length==2) {
								map.put(splitted[0], splitted[1]);
							} else {
								map.put(splitted[0], "");
							}
						}
						value = map;
					} catch (IOException e) {
						throw new IllegalStateException("Could not build a map for parameter '" + name + "' with value: " + value, e);
					}
				}
				if (createParameterObjects) {
					String  pattern = properties.getProperty(_param + i + ".pattern");
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
								parameter.setValue((String)value);
								parameter.setPattern(pattern);
							}
							parameter.configure();
							result.put(name, parameter);
//							debugMessage("Add param with name '" + name + "', value '" + value + "' and pattern '" + pattern + "' for property '" + property + "'", writers);
						} catch (ConfigurationException e) {
							throw new IllegalStateException("Parameter '" + name + "' could not be configured");
						}
					}
				} else {
					if (value == null) {
						throw new IllegalStateException("Property '" + _param + i + ".value' or '" + _param + i + ".valuefile' not found while property '" + _param + i + ".name' exist");
					} else {
						result.put(name, value);
//						debugMessage("Add param with name '" + name + "' and value '" + value + "' for property '" + property + "'", writers);
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
				((ISender) get()).open();
			}
			else if(get() instanceof IListener<?>) {
				((IListener<?>) get()).open();
			}
		} catch (SenderException | ListenerException e) {
			throw new ConfigurationException("error opening ["+get()+"]", e);
		}
	}

	public void close() throws Exception {
		if(get() instanceof AutoCloseable) {
			((AutoCloseable) get()).close();
		}
		else if(get() instanceof ISender) {
			((ISender) get()).close();
		}
		else if(get() instanceof IListener<?>) {
			((IListener<?>) get()).close();
		}
	}

	@Override
	public int executeWrite(String stepDisplayName, String fileContent, String correlationId, Map<String, Object> parameters) throws TimeoutException, SenderException, ListenerException {
		if(get() instanceof FileSender) {
			FileSender fileSender = (FileSender)get();
			fileSender.sendMessage(fileContent);
			return TestTool.RESULT_OK;
		}
		if (get() instanceof DelaySender) {
			DelaySender delaySender = (DelaySender) get();
			SenderResult senderResult = delaySender.sendMessage(new Message(fileContent), null);
			try {
				senderResult.getResult().close();
			} catch (IOException e) {
				log.warn("Could not close senderResult for queue {}", get(), e);
			}
			return TestTool.RESULT_OK;
		}
		if(get() instanceof XsltProviderListener) {
			XsltProviderListener xsltProviderListener = (XsltProviderListener)get();
			xsltProviderListener.processRequest(fileContent, parameters);
			return TestTool.RESULT_OK;
		}
		if(get() instanceof ISender) {
			ISender sender = (ISender)get();
			Boolean convertExceptionToMessage = (Boolean)get(CONVERT_MESSAGE_TO_EXCEPTION_KEY);
			PipeLineSession session = getSession();
			SenderThread senderThread = new SenderThread(sender, fileContent, session, convertExceptionToMessage.booleanValue(), correlationId);
			senderThread.start();
			put(SENDER_THREAD_KEY, senderThread);
			setSenderThread(senderThread); // 'put' and 'set' do something similar
			return TestTool.RESULT_OK;
		}
		if(get() instanceof IListener<?>) {
			ListenerMessageHandler listenerMessageHandler = getMessageHandler();
			if (listenerMessageHandler == null) {
				throw new NoSuchElementException("No ListenerMessageHandler found");
			}
			Map<?, ?> context = new HashMap<>();
			ListenerMessage requestListenerMessage = (ListenerMessage)get("listenerMessage");
			if (requestListenerMessage != null) {
				context = requestListenerMessage.getContext();
			}
			ListenerMessage listenerMessage = new ListenerMessage(fileContent, context);
			listenerMessageHandler.putResponseMessage(listenerMessage);
			return TestTool.RESULT_OK;
		}
		throw new SenderException("Could not perform executeWrite() for queue ["+get()+"]");
	}


	@Override
	public String executeRead(String step, String stepDisplayName, Properties properties, String fileName, String fileContent) throws SenderException, IOException, TimeoutException, ListenerException {
		if(get() instanceof FileSender) {
			FileSender fileSender = (FileSender)get();
			return fileSender.getMessage();
		}
		if(get() instanceof FileListener) {
			FileListener fileListener = (FileListener)get();
			return fileListener.getMessage();
		}
		if(get() instanceof XsltProviderListener) {
			XsltProviderListener xsltProviderListener = (XsltProviderListener)get();
			return xsltProviderListener.getResult();
		}
		if(get() instanceof ISender) {
			SenderThread senderThread = (SenderThread)remove(SENDER_THREAD_KEY);
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
			TimeoutException timeOutException = senderThread.getTimeOutException();
			if (timeOutException != null) {
				throw timeOutException;
			}
			return senderThread.getResponse();
		}
		throw new SenderException("Could not perform executeRead() for queue ["+get()+"]");
	}
}
