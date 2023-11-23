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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.classloaders.DirectoryClassLoader;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jms.JMSFacade.DeliveryMode;
import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.jms.PullingJmsListener;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testtool.TestTool;
import nl.nn.adapterframework.util.EnumUtils;

public class QueueCreator {

	public static Map<String, Queue> openQueues(String scenarioDirectory, Properties properties, IbisContext ibisContext, Map<String, Object> writers, int parameterTimeout, String correlationId) {
		Map<String, Queue> queues = new HashMap<>();
		debugMessage("Get all queue names", writers);

		List<String> jmsSenders = new ArrayList<>();
		List<String> jmsListeners = new ArrayList<>();
		List<String> jdbcFixedQuerySenders = new ArrayList<>();

		List<String> manuallyCreatedQueues = new ArrayList<>();

		try {
			// Use DirectoryClassLoader to make it possible to retrieve resources (such as styleSheetName) relative to the scenarioDirectory.
			DirectoryClassLoader directoryClassLoader = new RelativePathDirectoryClassLoader();
			directoryClassLoader.setDirectory(scenarioDirectory);
			directoryClassLoader.setBasePath(".");
			directoryClassLoader.configure(null, "TestTool");

			Iterator iterator = properties.keySet().iterator();
			while (iterator.hasNext()) {
				String key = (String)iterator.next();
				int i = key.indexOf('.');
				if (i != -1) {
					int j = key.indexOf('.', i + 1);
					if (j != -1) {
						String queueName = key.substring(0, j);
						if(manuallyCreatedQueues.contains(queueName)) continue;
						manuallyCreatedQueues.add(queueName);

						debugMessage("queuename openqueue: " + queueName, writers);
						if ("nl.nn.adapterframework.jms.JmsSender".equals(properties.get(queueName + ".className")) && !jmsSenders.contains(queueName)) {
							debugMessage("Adding jmsSender queue: " + queueName, writers);
							jmsSenders.add(queueName);
						} else if ("nl.nn.adapterframework.jms.JmsListener".equals(properties.get(queueName + ".className")) && !jmsListeners.contains(queueName)) {
							debugMessage("Adding jmsListener queue: " + queueName, writers);
							jmsListeners.add(queueName);
						} else if ("nl.nn.adapterframework.jdbc.FixedQuerySender".equals(properties.get(queueName + ".className")) && !jdbcFixedQuerySenders.contains(queueName)) {
							debugMessage("Adding jdbcFixedQuerySender queue: " + queueName, writers);
							jdbcFixedQuerySenders.add(queueName);
						} else {
							String className = properties.getProperty(queueName+".className");
							if(StringUtils.isEmpty(className)) continue;
							Properties queueProperties = QueueUtils.getSubProperties(properties, queueName);

							//Deprecation warning
							if(queueProperties.containsValue("requestTimeOut") || queueProperties.containsValue("responseTimeOut")) {
								errorMessage("properties "+queueName+".requestTimeOut/"+queueName+".responseTimeOut have been replaced with "+queueName+".timeout", writers);
							}

							IConfigurable configurable = QueueUtils.createInstance(directoryClassLoader, className);
							Queue queue = QueueWrapper.create(configurable, queueProperties, parameterTimeout, correlationId);

							queue.configure();
							queue.open();
							queues.put(queueName, queue);
							debugMessage("Opened ["+className+"] '" + queueName + "'", writers);
						}
					}
				}
			}

			createJmsSenders(queues, jmsSenders, properties, writers, ibisContext, correlationId);
			createJmsListeners(queues, jmsListeners, properties, writers, ibisContext, correlationId, parameterTimeout);
			createFixedQuerySenders(queues, jdbcFixedQuerySenders, properties, writers, ibisContext, correlationId);
		} catch (Exception e) {
			closeQueues(queues, properties, writers, null);
			queues = null;
			errorMessage(e.getClass().getSimpleName() + ": "+e.getMessage(), e, writers);
		}

		return queues;
	}

	private static void createJmsSenders(Map<String, Queue> queues, List<String> jmsSenders, Properties properties, Map<String, Object> writers, IbisContext ibisContext, String correlationId) throws ConfigurationException {
		debugMessage("Initialize jms senders", writers);
		Iterator<String> iterator = jmsSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = iterator.next();
			String queue = (String)properties.get(queueName + ".queue");
			if (queue == null) {
				closeQueues(queues, properties, writers, correlationId);
				queues = null;
				errorMessage("Could not find property '" + queueName + ".queue'", writers);
			} else {
				JmsSender jmsSender = ibisContext.createBeanAutowireByName(JmsSender.class);
				jmsSender.setName("Test Tool JmsSender");
				jmsSender.setDestinationName(queue);
				jmsSender.setDestinationType(DestinationType.QUEUE);
				jmsSender.setAcknowledgeMode("auto");
				String jmsRealm = (String)properties.get(queueName + ".jmsRealm");
				if (jmsRealm!=null) {
					jmsSender.setJmsRealm(jmsRealm);
				} else {
					jmsSender.setJmsRealm("default");
				}
				String deliveryMode = properties.getProperty(queueName + ".deliveryMode");
				debugMessage("Property '" + queueName + ".deliveryMode': " + deliveryMode, writers);
				String persistent = properties.getProperty(queueName + ".persistent");
				debugMessage("Property '" + queueName + ".persistent': " + persistent, writers);
				String useCorrelationIdFrom = properties.getProperty(queueName + ".useCorrelationIdFrom");
				debugMessage("Property '" + queueName + ".useCorrelationIdFrom': " + useCorrelationIdFrom, writers);
				String replyToName = properties.getProperty(queueName + ".replyToName");
				debugMessage("Property '" + queueName + ".replyToName': " + replyToName, writers);
				if (deliveryMode != null) {
					debugMessage("Set deliveryMode to " + deliveryMode, writers);
					jmsSender.setDeliveryMode(EnumUtils.parse(DeliveryMode.class, deliveryMode));
				}
				if ("true".equals(persistent)) {
					debugMessage("Set persistent to true", writers);
					jmsSender.setPersistent(true);
				} else {
					debugMessage("Set persistent to false", writers);
					jmsSender.setPersistent(false);
				}
				if (replyToName != null) {
					debugMessage("Set replyToName to " + replyToName, writers);
					jmsSender.setReplyToName(replyToName);
				}
				Queue jmsSenderInfo = new JmsSenderQueue(jmsSender, useCorrelationIdFrom, properties.getProperty(queueName + ".jmsCorrelationId"));
				jmsSenderInfo.configure();
				//jmsSenderInfo.open(); // TODO: JmsSender was not opened here. Check if that should be done.
				queues.put(queueName, jmsSenderInfo);
				debugMessage("Opened jms sender '" + queueName + "'", writers);
			}
		}
	}

	private static void createJmsListeners(Map<String, Queue> queues, List<String> jmsListeners, Properties properties, Map<String, Object> writers, IbisContext ibisContext, String correlationId, int defaultTimeout) throws ConfigurationException {
		debugMessage("Initialize jms listeners", writers);
		Iterator<String> iterator = jmsListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = iterator.next();
			String queue = (String)properties.get(queueName + ".queue");
			String timeout = (String)properties.get(queueName + ".timeout");

			int nTimeout = defaultTimeout;
			if (timeout != null && timeout.length() > 0) {
				nTimeout = Integer.parseInt(timeout);
				debugMessage("Overriding default timeout setting of "+defaultTimeout+" with "+ nTimeout, writers);
			}

			if (queue == null) {
				closeQueues(queues, properties, writers, correlationId);
				queues = null;
				errorMessage("Could not find property '" + queueName + ".queue'", writers);
			} else {
				PullingJmsListener pullingJmsListener = ibisContext.createBeanAutowireByName(PullingJmsListener.class);
				pullingJmsListener.setName("Test Tool JmsListener");
				pullingJmsListener.setDestinationName(queue);
				pullingJmsListener.setDestinationType(DestinationType.QUEUE);
				pullingJmsListener.setAcknowledgeMode("auto");
				String jmsRealm = (String)properties.get(queueName + ".jmsRealm");
				if (jmsRealm!=null) {
					pullingJmsListener.setJmsRealm(jmsRealm);
				} else {
					pullingJmsListener.setJmsRealm("default");
				}
				// Call setJmsRealm twice as a workaround for a strange bug
				// where we get a java.lang.NullPointerException in a class of
				// the commons-beanutils.jar on the first call to setJmsRealm
				// after starting the Test Tool ear:
				// at org.apache.commons.beanutils.MappedPropertyDescriptor.internalFindMethod(MappedPropertyDescriptor.java(Compiled Code))
				// at org.apache.commons.beanutils.MappedPropertyDescriptor.internalFindMethod(MappedPropertyDescriptor.java:413)
				// ...
				// Looks like some sort of classloader problem where
				// internalFindMethod on another class is called (last line in
				// stacktrace has "Compiled Code" while other lines have
				// linenumbers).
				// Can be reproduced with for example:
				// - WebSphere Studio Application Developer (Windows) Version: 5.1.2
				// - Ibis4Juice build 20051104-1351
				// - y01\rr\getAgent1003\scenario01.properties
				pullingJmsListener.setTimeOut(nTimeout);
				String setForceMessageIdAsCorrelationId = (String)properties.get(queueName + ".setForceMessageIdAsCorrelationId");
				if ("true".equals(setForceMessageIdAsCorrelationId)) {
					pullingJmsListener.setForceMessageIdAsCorrelationId(true);
				}
				Queue jmsListenerInfo = new JmsListenerQueue(pullingJmsListener);
				jmsListenerInfo.configure();
				//jmsListenerInfo.open(); // TODO: jmsListener was not opened here. Check if that should be done.
				queues.put(queueName, jmsListenerInfo);
				debugMessage("Opened jms listener '" + queueName + "'", writers);
				if (TestTool.jmsCleanUp(queueName, pullingJmsListener, writers)) {
					errorMessage("Found one or more old messages on queue '" + queueName + "', you might want to run your tests with a higher 'wait before clean up' value", writers);
				}
			}
		}
	}

	private static void createFixedQuerySenders(Map<String, Queue> queues, List<String> jdbcFixedQuerySenders, Properties properties, Map<String, Object> writers, IbisContext ibisContext, String correlationId) {
		debugMessage("Initialize jdbc fixed query senders", writers);
		Iterator<String> iterator = jdbcFixedQuerySenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = iterator.next();

			Properties queueProperties = QueueUtils.getSubProperties(properties, name);
			boolean allFound = false;
			String preDelete = "";
			int preDeleteIndex = 1;
			String getBlobSmartString = (String)properties.get(name + ".getBlobSmart");
			if(StringUtils.isNotEmpty(getBlobSmartString)) {
				queueProperties.setProperty("blobSmartGet", getBlobSmartString);
			}

			Queue querySendersInfo = new QuerySenderQueue();
			while (!allFound && queues != null) {
				preDelete = (String)properties.get(name + ".preDel" + preDeleteIndex);
				if (preDelete != null) {
					FixedQuerySender deleteQuerySender = ibisContext.createBeanAutowireByName(FixedQuerySender.class);
					deleteQuerySender.setName("Test Tool pre delete query sender");

					try {
						QueueUtils.invokeSetters(deleteQuerySender, queueProperties);
						deleteQuerySender.setQueryType("delete");
						deleteQuerySender.setQuery("delete from " + preDelete);

						deleteQuerySender.configure();
						deleteQuerySender.open();
						deleteQuerySender.sendMessageOrThrow(TestTool.TESTTOOL_DUMMY_MESSAGE, null).close();
						deleteQuerySender.close();
					} catch(ConfigurationException e) {
						closeQueues(queues, properties, writers, correlationId);
						queues = null;
						errorMessage("Could not configure '" + name + "': " + e.getMessage(), e, writers);
					} catch(TimeoutException e) {
						closeQueues(queues, properties, writers, correlationId);
						queues = null;
						errorMessage("Time out on execute pre delete query for '" + name + "': " + e.getMessage(), e, writers);
					} catch(Exception e) {
						closeQueues(queues, properties, writers, correlationId);
						queues = null;
						errorMessage("Could not execute pre delete query for '" + name + "': " + e.getMessage(), e, writers);
					}
					preDeleteIndex++;
				} else {
					allFound = true;
				}
			}
			if (queues != null) {
				String prePostQuery = (String)properties.get(name + ".prePostQuery");
				if (prePostQuery != null) {
					FixedQuerySender prePostFixedQuerySender = ibisContext.createBeanAutowireByName(FixedQuerySender.class);
					prePostFixedQuerySender.setName("Test Tool query sender");
					try {
						QueueUtils.invokeSetters(prePostFixedQuerySender, queueProperties);
						prePostFixedQuerySender.setQuery(prePostQuery);
						prePostFixedQuerySender.setQueryType("select");
						prePostFixedQuerySender.configure();
					} catch(Exception e) {
						closeQueues(queues, properties, writers, correlationId);
						queues = null;
						errorMessage("Could not configure '" + name + "': " + e.getMessage(), e, writers);
					}
					if (queues != null) {
						try {
							prePostFixedQuerySender.open();
						} catch(SenderException e) {
							closeQueues(queues, properties, writers, correlationId);
							queues = null;
							errorMessage("Could not open (pre/post) '" + name + "': " + e.getMessage(), e, writers);
						}
					}
					if (queues != null) {
						try (PipeLineSession session = new PipeLineSession()) {
							session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
							Message message = prePostFixedQuerySender.sendMessageOrThrow(TestTool.TESTTOOL_DUMMY_MESSAGE, session);
							String result = message.asString();
							querySendersInfo.put("prePostQueryFixedQuerySender", prePostFixedQuerySender);
							querySendersInfo.put("prePostQueryResult", result);
							message.close();
						} catch(TimeoutException e) {
							closeQueues(queues, properties, writers, correlationId);
							queues = null;
							errorMessage("Time out on execute query for '" + name + "': " + e.getMessage(), e, writers);
						} catch(IOException | SenderException e) {
							closeQueues(queues, properties, writers, correlationId);
							queues = null;
							errorMessage("Could not execute query for '" + name + "': " + e.getMessage(), e, writers);
						}
					}
				}
			}
			if (queues != null) {
				String readQuery = (String)properties.get(name + ".readQuery");
				if (readQuery != null) {
					FixedQuerySender readQueryFixedQuerySender = ibisContext.createBeanAutowireByName(FixedQuerySender.class);
					readQueryFixedQuerySender.setName("Test Tool query sender");

					try {
						readQueryFixedQuerySender.setQueryType("select");
						QueueUtils.invokeSetters(readQueryFixedQuerySender, queueProperties);
						readQueryFixedQuerySender.setQuery(readQuery);
						readQueryFixedQuerySender.configure();
					} catch(Exception e) {
						closeQueues(queues, properties, writers, correlationId);
						queues = null;
						errorMessage("Could not configure '" + name + "': " + e.getMessage(), e, writers);
					}
					if (queues != null) {
						try {
							readQueryFixedQuerySender.open();
							querySendersInfo.put("readQueryQueryFixedQuerySender", readQueryFixedQuerySender);
						} catch(SenderException e) {
							closeQueues(queues, properties, writers, correlationId);
							queues = null;
							errorMessage("Could not open '" + name + "': " + e.getMessage(), e, writers);
						}
					}
				}
			}
			if (queues != null) {
				String waitBeforeRead = (String)properties.get(name + ".waitBeforeRead");
				if (waitBeforeRead != null) {
					try {
						querySendersInfo.put("readQueryWaitBeforeRead", new Integer(waitBeforeRead));
					} catch(NumberFormatException e) {
						errorMessage("Value of '" + name + ".waitBeforeRead' not a number: " + e.getMessage(), e, writers);
					}
				}
				queues.put(name, querySendersInfo);
				debugMessage("Opened jdbc connection '" + name + "'", writers);
			}
		}
	}



	private static void closeQueues(Map<String, Queue> queues, Properties properties, Map<String, Object> writers, String correlationId) {
		TestTool.closeQueues(queues, properties, writers, correlationId);
	}

	private static void debugMessage(String message, Map<String, Object> writers) {
		TestTool.debugMessage(message, writers);
	}

	private static void errorMessage(String message, Map<String, Object> writers) {
		TestTool.errorMessage(message, writers);
	}

	private static void errorMessage(String message, Exception e, Map<String, Object> writers) {
		TestTool.errorMessage(message, e, writers);
	}
}
