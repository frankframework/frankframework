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
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.TestConfig;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.senders.FrankSender;
import org.frankframework.stream.Message;

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

		List<String> jdbcFixedQuerySenders = new ArrayList<>();

		try {
			// Use DirectoryClassLoader to make it possible to retrieve resources (such as styleSheetName) relative to the scenarioDirectory.
			DirectoryClassLoader directoryClassLoader = new RelativePathDirectoryClassLoader();
			directoryClassLoader.setDirectory(scenarioDirectory);
			directoryClassLoader.setBasePath(".");
			directoryClassLoader.configure(null, "LarvaTool");

			Set<String> queueNames = properties.keySet()
					.stream()
					.map(String.class::cast)
					.filter(key -> key.endsWith(CLASS_NAME_PROPERTY_SUFFIX))
					.map(key -> key.substring(0, key.lastIndexOf(".")))
					.collect(Collectors.toSet());

			for (String queueName : queueNames) {
				debugMessage("queuename openqueue: " + queueName);
				String className = properties.getProperty(queueName + CLASS_NAME_PROPERTY_SUFFIX);
				if ("org.frankframework.jms.JmsListener".equals(className)) {
					className = "org.frankframework.jms.PullingJmsListener";
				}

				if ("org.frankframework.jdbc.FixedQuerySender".equals(className) && !jdbcFixedQuerySenders.contains(queueName)) {
					debugMessage("Adding jdbcFixedQuerySender queue: " + queueName);
					jdbcFixedQuerySenders.add(queueName);
				} else {
					Properties queueProperties = QueueUtils.getSubProperties(properties, queueName);

					// Deprecation warning
					if (queueProperties.containsValue("requestTimeOut") || queueProperties.containsValue("responseTimeOut")) {
						errorMessage("properties " + queueName + ".requestTimeOut/" + queueName + ".responseTimeOut have been replaced with " + queueName + ".timeout");
					}

					IConfigurable configurable = QueueUtils.createInstance(ibisContext, directoryClassLoader, className);
					log.debug("created FrankElement [{}]", configurable);
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

			createFixedQuerySenders(queues, jdbcFixedQuerySenders, properties, ibisContext, correlationId);
		} catch (Exception e) {
			log.warn("Error occurred while creating queues", e);
			closeQueues(queues, properties, null);
			queues = null;
			errorMessage(e.getClass().getSimpleName() + ": "+e.getMessage(), e);
		}

		return queues;
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
