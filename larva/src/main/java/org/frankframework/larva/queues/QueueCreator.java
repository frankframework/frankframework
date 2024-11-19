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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.IbisContext;
import org.frankframework.configuration.classloaders.DirectoryClassLoader;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.jdbc.AbstractJdbcQuerySender;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jms.JMSFacade;
import org.frankframework.jms.JMSFacade.DeliveryMode;
import org.frankframework.jms.JMSFacade.DestinationType;
import org.frankframework.jms.JmsSender;
import org.frankframework.jms.PullingJmsListener;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.TestConfig;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.senders.FrankSender;
import org.frankframework.stream.Message;
import org.frankframework.util.EnumUtils;

@Log4j2
public class QueueCreator {

	public static final String CLASS_NAME_PROPERTY_SUFFIX = ".className";
	private final TestConfig config;
	private final LarvaTool testTool;

	public QueueCreator(TestConfig config, LarvaTool testTool) {
		this.config = config;
		this.testTool = testTool;
	}

	public Map<String, Queue> openQueues(String scenarioDirectory, Properties properties, IbisContext ibisContext, String correlationId) {
		Map<String, Queue> queues = new HashMap<>();
		debugMessage("Get all queue names");

		List<String> jmsSenders = new ArrayList<>();
		List<String> jmsListeners = new ArrayList<>();
		List<String> jdbcFixedQuerySenders = new ArrayList<>();

		try {
			// Use DirectoryClassLoader to make it possible to retrieve resources (such as styleSheetName) relative to the scenarioDirectory.
			DirectoryClassLoader directoryClassLoader = new RelativePathDirectoryClassLoader();
			directoryClassLoader.setDirectory(scenarioDirectory);
			directoryClassLoader.setBasePath(".");
			directoryClassLoader.configure(null, "LarvaTool");

			Set<String> queueNames = properties.keySet()
					.stream()
					.map(o -> (String)o)
					.filter(key -> key.endsWith(CLASS_NAME_PROPERTY_SUFFIX))
					.map(key -> key.substring(0, key.lastIndexOf(".")))
					.collect(Collectors.toSet());

			for (String queueName : queueNames) {
				debugMessage("queuename openqueue: " + queueName);
				String className = properties.getProperty(queueName + CLASS_NAME_PROPERTY_SUFFIX);
				if ("org.frankframework.jms.JmsSender".equals(className) && !jmsSenders.contains(queueName)) {
					debugMessage("Adding jmsSender queue: " + queueName);
					jmsSenders.add(queueName);
				} else if ("org.frankframework.jms.JmsListener".equals(className) && !jmsListeners.contains(queueName)) {
					debugMessage("Adding jmsListener queue: " + queueName);
					jmsListeners.add(queueName);
				} else if ("org.frankframework.jdbc.FixedQuerySender".equals(className) && !jdbcFixedQuerySenders.contains(queueName)) {
					debugMessage("Adding jdbcFixedQuerySender queue: " + queueName);
					jdbcFixedQuerySenders.add(queueName);
				} else {
					Properties queueProperties = QueueUtils.getSubProperties(properties, queueName);

					//Deprecation warning
					if (queueProperties.containsValue("requestTimeOut") || queueProperties.containsValue("responseTimeOut")) {
						errorMessage("properties " + queueName + ".requestTimeOut/" + queueName + ".responseTimeOut have been replaced with " + queueName + ".timeout");
					}

					IConfigurable configurable = QueueUtils.createInstance(directoryClassLoader, className);
					if (configurable instanceof FrankSender frankSender) {
						frankSender.setIbisManager(ibisContext.getIbisManager());
					}
					Queue queue = QueueWrapper.create(configurable, queueProperties, config.getTimeout(), correlationId);

					queue.configure();
					queue.open();
					queues.put(queueName, queue);
					debugMessage("Opened [" + className + "] '" + queueName + "'");
				}
			}

			createJmsSenders(queues, jmsSenders, properties, ibisContext, correlationId);
			createJmsListeners(queues, jmsListeners, properties, ibisContext, correlationId, config.getTimeout());
			createFixedQuerySenders(queues, jdbcFixedQuerySenders, properties, ibisContext, correlationId);
		} catch (Exception e) {
			closeQueues(queues, properties, null);
			queues = null;
			errorMessage(e.getClass().getSimpleName() + ": "+e.getMessage(), e);
		}

		return queues;
	}

	private void createJmsSenders(Map<String, Queue> queues, List<String> jmsSenders, Properties properties, IbisContext ibisContext, String correlationId) throws ConfigurationException {
		debugMessage("Initialize jms senders");
		Iterator<String> iterator = jmsSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = iterator.next();
			String queue = (String)properties.get(queueName + ".queue");
			if (queue == null) {
				closeQueues(queues, properties, correlationId);
				queues = null;
				errorMessage("Could not find property '" + queueName + ".queue'");
			} else {
				JmsSender jmsSender = ibisContext.createBeanAutowireByName(JmsSender.class);
				jmsSender.setName("Test Tool JmsSender");
				jmsSender.setDestinationName(queue);
				jmsSender.setDestinationType(DestinationType.QUEUE);
				jmsSender.setAcknowledgeMode(JMSFacade.AcknowledgeMode.AUTO_ACKNOWLEDGE);
				String jmsRealm = (String)properties.get(queueName + ".jmsRealm");
				if (jmsRealm!=null) {
					jmsSender.setJmsRealm(jmsRealm);
				} else {
					jmsSender.setJmsRealm("default");
				}
				String deliveryMode = properties.getProperty(queueName + ".deliveryMode");
				debugMessage("Property '" + queueName + ".deliveryMode': " + deliveryMode);
				String persistent = properties.getProperty(queueName + ".persistent");
				debugMessage("Property '" + queueName + ".persistent': " + persistent);
				String useCorrelationIdFrom = properties.getProperty(queueName + ".useCorrelationIdFrom");
				debugMessage("Property '" + queueName + ".useCorrelationIdFrom': " + useCorrelationIdFrom);
				String replyToName = properties.getProperty(queueName + ".replyToName");
				debugMessage("Property '" + queueName + ".replyToName': " + replyToName);
				if (deliveryMode != null) {
					debugMessage("Set deliveryMode to " + deliveryMode);
					jmsSender.setDeliveryMode(EnumUtils.parse(DeliveryMode.class, deliveryMode));
				}
				if ("true".equals(persistent)) {
					debugMessage("Set persistent to true");
					jmsSender.setPersistent(true);
				} else {
					debugMessage("Set persistent to false");
					jmsSender.setPersistent(false);
				}
				if (replyToName != null) {
					debugMessage("Set replyToName to " + replyToName);
					jmsSender.setReplyToName(replyToName);
				}
				Queue jmsSenderInfo = new JmsSenderQueue(jmsSender, useCorrelationIdFrom, properties.getProperty(queueName + ".jmsCorrelationId"));
				jmsSenderInfo.configure();
				//jmsSenderInfo.open(); // TODO: JmsSender was not opened here. Check if that should be done.
				queues.put(queueName, jmsSenderInfo);
				debugMessage("Opened jms sender '" + queueName + "'");
			}
		}
	}

	private void createJmsListeners(Map<String, Queue> queues, List<String> jmsListeners, Properties properties, IbisContext ibisContext, String correlationId, int defaultTimeout) throws ConfigurationException {
		debugMessage("Initialize jms listeners");
		Iterator<String> iterator = jmsListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = iterator.next();
			String queue = (String)properties.get(queueName + ".queue");
			String timeout = (String)properties.get(queueName + ".timeout");

			int nTimeout = defaultTimeout;
			if (timeout != null && !timeout.isEmpty()) {
				nTimeout = Integer.parseInt(timeout);
				debugMessage("Overriding default timeout setting of "+defaultTimeout+" with "+ nTimeout);
			}

			if (queue == null) {
				closeQueues(queues, properties, correlationId);
				queues = null;
				errorMessage("Could not find property '" + queueName + ".queue'");
			} else {
				PullingJmsListener pullingJmsListener = ibisContext.createBeanAutowireByName(PullingJmsListener.class);
				pullingJmsListener.setName("Test Tool JmsListener");
				pullingJmsListener.setDestinationName(queue);
				pullingJmsListener.setDestinationType(DestinationType.QUEUE);
				pullingJmsListener.setAcknowledgeMode(JMSFacade.AcknowledgeMode.AUTO_ACKNOWLEDGE);
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
				pullingJmsListener.setTimeout(nTimeout);
				String setForceMessageIdAsCorrelationId = (String)properties.get(queueName + ".setForceMessageIdAsCorrelationId");
				if ("true".equals(setForceMessageIdAsCorrelationId)) {
					pullingJmsListener.setForceMessageIdAsCorrelationId(true);
				}
				Queue jmsListenerInfo = new JmsListenerQueue(pullingJmsListener);
				jmsListenerInfo.configure();
				//jmsListenerInfo.open(); // TODO: jmsListener was not opened here. Check if that should be done.
				queues.put(queueName, jmsListenerInfo);
				debugMessage("Opened jms listener '" + queueName + "'");
				if (testTool.jmsCleanUp(queueName, pullingJmsListener)) {
					errorMessage("Found one or more old messages on queue '" + queueName + "', you might want to run your tests with a higher 'wait before clean up' value");
				}
			}
		}
	}

	private void createFixedQuerySenders(Map<String, Queue> queues, List<String> jdbcFixedQuerySenders, Properties properties, IbisContext ibisContext, String correlationId) {
		debugMessage("Initialize jdbc fixed query senders");
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
						deleteQuerySender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
						deleteQuerySender.setQuery("delete from " + preDelete);

						deleteQuerySender.configure();
						deleteQuerySender.start();
						try (PipeLineSession session = new PipeLineSession()){
							deleteQuerySender.sendMessageOrThrow(LarvaTool.getQueryFromSender(deleteQuerySender), session).close();
						} finally {
							deleteQuerySender.stop();
						}
					} catch(ConfigurationException e) {
						closeQueues(queues, properties, correlationId);
						queues = null;
						errorMessage("Could not configure '" + name + "': " + e.getMessage(), e);
					} catch(TimeoutException e) {
						closeQueues(queues, properties, correlationId);
						queues = null;
						errorMessage("Time out on execute pre delete query for '" + name + "': " + e.getMessage(), e);
					} catch(Exception e) {
						closeQueues(queues, properties, correlationId);
						queues = null;
						errorMessage("Could not execute pre delete query for '" + name + "': " + e.getMessage(), e);
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
						prePostFixedQuerySender.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);
						prePostFixedQuerySender.configure();
					} catch(Exception e) {
						closeQueues(queues, properties, correlationId);
						queues = null;
						errorMessage("Could not configure '" + name + "': " + e.getMessage(), e);
					}
					if (queues != null) {
						try {
							prePostFixedQuerySender.start();
						} catch(LifecycleException e) {
							closeQueues(queues, properties, correlationId);
							queues = null;
							errorMessage("Could not open (pre/post) '" + name + "': " + e.getMessage(), e);
						}
					}
					if (queues != null) {
						try (PipeLineSession session = new PipeLineSession()) {
							session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
							String result;
							try (Message message = prePostFixedQuerySender.sendMessageOrThrow(LarvaTool.getQueryFromSender(prePostFixedQuerySender), session)) {
								result = message.asString();
							}
							querySendersInfo.put("prePostQueryFixedQuerySender", prePostFixedQuerySender);
							querySendersInfo.put("prePostQueryResult", result);
						} catch(TimeoutException e) {
							closeQueues(queues, properties, correlationId);
							queues = null;
							errorMessage("Time out on execute query for '" + name + "': " + e.getMessage(), e);
						} catch(IOException | SenderException e) {
							closeQueues(queues, properties, correlationId);
							queues = null;
							errorMessage("Could not execute query for '" + name + "': " + e.getMessage(), e);
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
						readQueryFixedQuerySender.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);
						QueueUtils.invokeSetters(readQueryFixedQuerySender, queueProperties);
						readQueryFixedQuerySender.setQuery(readQuery);
						readQueryFixedQuerySender.configure();
					} catch(Exception e) {
						closeQueues(queues, properties, correlationId);
						queues = null;
						errorMessage("Could not configure '" + name + "': " + e.getMessage(), e);
					}
					if (queues != null) {
						try {
							readQueryFixedQuerySender.start();
							querySendersInfo.put("readQueryQueryFixedQuerySender", readQueryFixedQuerySender);
						} catch(LifecycleException e) {
							closeQueues(queues, properties, correlationId);
							queues = null;
							errorMessage("Could not open '" + name + "': " + e.getMessage(), e);
						}
					}
				}
			}
			if (queues != null) {
				String waitBeforeRead = (String)properties.get(name + ".waitBeforeRead");
				if (waitBeforeRead != null) {
					try {
						querySendersInfo.put("readQueryWaitBeforeRead", Integer.valueOf(waitBeforeRead));
					} catch(NumberFormatException e) {
						errorMessage("Value of '" + name + ".waitBeforeRead' not a number: " + e.getMessage(), e);
					}
				}
				queues.put(name, querySendersInfo);
				debugMessage("Opened jdbc connection '" + name + "'");
			}
		}
	}

	private void closeQueues(Map<String, Queue> queues, Properties properties, String correlationId) {
		testTool.closeQueues(queues, properties, correlationId);
	}

	private void debugMessage(String message) {
		testTool.debugMessage(message);
	}

	private void errorMessage(String message) {
		testTool.errorMessage(message);
	}

	private void errorMessage(String message, Exception e) {
		testTool.errorMessage(message, e);
	}
}
