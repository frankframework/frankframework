package nl.nn.adapterframework.testtool;

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
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.http.WebServiceSender;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.jms.PullingJmsListener;
import nl.nn.adapterframework.jms.JMSFacade.DeliveryMode;
import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.senders.DelaySender;
import nl.nn.adapterframework.senders.IbisJavaSender;
import nl.nn.adapterframework.testtool.queues.IQueue;
import nl.nn.adapterframework.testtool.queues.QueueUtils;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.EnumUtils;

public class QueueCreator {

	public static Map<String, Map<String, Object>> openQueues(String scenarioDirectory, Properties properties, IbisContext ibisContext, AppConstants appConstants, Map<String, Object> writers, int parameterTimeout, String correlationId) {
		Map<String, Map<String, Object>> queues = new HashMap<String, Map<String, Object>>();
		debugMessage("Get all queue names", writers);
		List<String> jmsSenders = new ArrayList<String>();
		List<String> jmsListeners = new ArrayList<String>();
		List<String> jdbcFixedQuerySenders = new ArrayList<String>();
		List<String> ibisWebServiceSenders = new ArrayList<String>();
		List<String> webServiceSenders = new ArrayList<String>();
		List<String> webServiceListeners = new ArrayList<String>();
		List<String> httpSenders = new ArrayList<String>();
		List<String> ibisJavaSenders = new ArrayList<String>();
		List<String> delaySenders = new ArrayList<String>();
		List<String> javaListeners = new ArrayList<String>();
		List<String> fileSenders = new ArrayList<String>();
		List<String> fileListeners = new ArrayList<String>();
		List<String> xsltProviderListeners = new ArrayList<String>();

		Iterator iterator = properties.keySet().iterator();
		while (iterator.hasNext()) {
			String key = (String)iterator.next();
			int i = key.indexOf('.');
			if (i != -1) {
				int j = key.indexOf('.', i + 1);
				if (j != -1) {
					String queueName = key.substring(0, j);
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
					} else if ("nl.nn.adapterframework.http.IbisWebServiceSender".equals(properties.get(queueName + ".className")) && !ibisWebServiceSenders.contains(queueName)) {
						debugMessage("Adding ibisWebServiceSender queue: " + queueName, writers);
						ibisWebServiceSenders.add(queueName);
					} else if ("nl.nn.adapterframework.http.WebServiceSender".equals(properties.get(queueName + ".className")) && !webServiceSenders.contains(queueName)) {
						debugMessage("Adding webServiceSender queue: " + queueName, writers);
						webServiceSenders.add(queueName);
					} else if ("nl.nn.adapterframework.http.WebServiceListener".equals(properties.get(queueName + ".className")) && !webServiceListeners.contains(queueName)) {
						debugMessage("Adding webServiceListener queue: " + queueName, writers);
						webServiceListeners.add(queueName);
					} else if ("nl.nn.adapterframework.http.HttpSender".equals(properties.get(queueName + ".className")) && !httpSenders.contains(queueName)) {
						debugMessage("Adding httpSender queue: " + queueName, writers);
						httpSenders.add(queueName);
					} else if ("nl.nn.adapterframework.senders.IbisJavaSender".equals(properties.get(queueName + ".className")) && !ibisJavaSenders.contains(queueName)) {
						debugMessage("Adding ibisJavaSender queue: " + queueName, writers);
						ibisJavaSenders.add(queueName);
					} else if ("nl.nn.adapterframework.senders.DelaySender".equals(properties.get(queueName + ".className")) && !delaySenders.contains(queueName)) {
						debugMessage("Adding delaySender queue: " + queueName, writers);
						delaySenders.add(queueName);
					} else if ("nl.nn.adapterframework.receivers.JavaListener".equals(properties.get(queueName + ".className")) && !javaListeners.contains(queueName)) {
						debugMessage("Adding javaListener queue: " + queueName, writers);
						javaListeners.add(queueName);
					} else if ("nl.nn.adapterframework.testtool.FileSender".equals(properties.get(queueName + ".className")) && !fileSenders.contains(queueName)) {
						debugMessage("Adding fileSender queue: " + queueName, writers);
						fileSenders.add(queueName);
					} else if ("nl.nn.adapterframework.testtool.FileListener".equals(properties.get(queueName + ".className")) && !fileListeners.contains(queueName)) {
						debugMessage("Adding fileListener queue: " + queueName, writers);
						fileListeners.add(queueName);
					} else if ("nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className")) && !xsltProviderListeners.contains(queueName)) {
						debugMessage("Adding xsltProviderListeners queue: " + queueName, writers);
						xsltProviderListeners.add(queueName);
					}
				}
			}
		}

		debugMessage("Initialize jms senders", writers);
		iterator = jmsSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String queue = (String)properties.get(queueName + ".queue");
			if (queue == null) {
				closeQueues(queues, properties, writers, correlationId);
				queues = null;
				errorMessage("Could not find property '" + queueName + ".queue'", writers);
			} else {
				JmsSender jmsSender = (JmsSender)ibisContext.createBeanAutowireByName(JmsSender.class);
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
				try {
					jmsSender.configure();
				} catch (ConfigurationException e) {
					throw new RuntimeException(e);
				}
				Map<String, Object> jmsSenderInfo = new HashMap<String, Object>();
				jmsSenderInfo.put("jmsSender", jmsSender);
				jmsSenderInfo.put("useCorrelationIdFrom", useCorrelationIdFrom);
				String jmsCorrelationId = properties.getProperty(queueName + ".jmsCorrelationId");
				if (jmsCorrelationId!=null) {
					jmsSenderInfo.put("jmsCorrelationId", jmsCorrelationId);
					debugMessage("Property '" + queueName + ".jmsCorrelationId': " + jmsCorrelationId, writers);
				}
				queues.put(queueName, jmsSenderInfo);
				debugMessage("Opened jms sender '" + queueName + "'", writers);
			}
		}

		debugMessage("Initialize jms listeners", writers);
		iterator = jmsListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String queue = (String)properties.get(queueName + ".queue");
			String timeout = (String)properties.get(queueName + ".timeout");

			int nTimeout = parameterTimeout;
			if (timeout != null && timeout.length() > 0) {
				nTimeout = Integer.parseInt(timeout);
				debugMessage("Overriding default timeout setting of "+parameterTimeout+" with "+ nTimeout, writers);
			}

			if (queue == null) {
				closeQueues(queues, properties, writers, correlationId);
				queues = null;
				errorMessage("Could not find property '" + queueName + ".queue'", writers);
			} else {
				PullingJmsListener pullingJmsListener = (PullingJmsListener)ibisContext.createBeanAutowireByName(PullingJmsListener.class);
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
				try {
					pullingJmsListener.configure();
				} catch (ConfigurationException e) {
					throw new RuntimeException(e);
				}
				Map<String, Object> jmsListenerInfo = new HashMap<String, Object>();
				jmsListenerInfo.put("jmsListener", pullingJmsListener);
				queues.put(queueName, jmsListenerInfo);
				debugMessage("Opened jms listener '" + queueName + "'", writers);
				if (jmsCleanUp(queueName, pullingJmsListener, writers)) {
					errorMessage("Found one or more old messages on queue '" + queueName + "', you might want to run your tests with a higher 'wait before clean up' value", writers);
				}
			}
		}

		debugMessage("Initialize jdbc fixed query senders", writers);
		iterator = jdbcFixedQuerySenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();

			Properties queueProperties = QueueUtils.getSubProperties(properties, name);
			boolean allFound = false;
			String preDelete = "";
			int preDeleteIndex = 1;
			String getBlobSmartString = (String)properties.get(name + ".getBlobSmart");
			if(StringUtils.isNotEmpty(getBlobSmartString)) {
				queueProperties.setProperty("blobSmartGet", getBlobSmartString);
			}

			Map<String, Object> querySendersInfo = new HashMap<String, Object>();
			while (!allFound && queues != null) {
				preDelete = (String)properties.get(name + ".preDel" + preDeleteIndex);
				if (preDelete != null) {
					FixedQuerySender deleteQuerySender = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
					deleteQuerySender.setName("Test Tool pre delete query sender");

					try {
						QueueUtils.invokeSetters(deleteQuerySender, queueProperties);
						deleteQuerySender.setQueryType("delete");
						deleteQuerySender.setQuery("delete from " + preDelete);

						deleteQuerySender.configure();
						deleteQuerySender.open();
						deleteQuerySender.sendMessage(TestTool.TESTTOOL_DUMMY_MESSAGE, null);
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
					FixedQuerySender prePostFixedQuerySender = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
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
						try {
							PipeLineSession session = new PipeLineSession();
							session.put(PipeLineSession.businessCorrelationIdKey, correlationId);
							String result = prePostFixedQuerySender.sendMessage(TestTool.TESTTOOL_DUMMY_MESSAGE, session).asString();
							querySendersInfo.put("prePostQueryFixedQuerySender", prePostFixedQuerySender);
							querySendersInfo.put("prePostQueryResult", result);
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
					FixedQuerySender readQueryFixedQuerySender = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
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

		debugMessage("Initialize web service senders", writers);
		iterator = webServiceSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));

			try {
				WebServiceSender webServiceSender = QueueUtils.createSender(WebServiceSender.class);
				Properties queueProperties = QueueUtils.getSubProperties(properties, name);
				QueueUtils.invokeSetters(webServiceSender, queueProperties);
				webServiceSender.configure();
				webServiceSender.open();

				Map<String, Object> webServiceSenderInfo = new HashMap<>();
				webServiceSenderInfo.put("webServiceSender", webServiceSender);
				webServiceSenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
				queues.put(name, webServiceSenderInfo);
				debugMessage("Opened web service sender '" + name + "'", writers);
			} catch (Exception e) {
				closeQueues(queues, properties, writers, correlationId);
				queues = null;
				errorMessage("ERROR on ["+name+"]: "+e.getMessage(), writers);
			}
		}

		debugMessage("Initialize web service listeners", writers);
		iterator = webServiceListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();

			//Deprecation warning
			if(properties.contains(name + ".requestTimeOut") || properties.contains(name + ".responseTimeOut")) {
				errorMessage("properties "+name+".requestTimeOut/"+name+".responseTimeOut have been replaced with "+name+".timeout", writers);
			}

			try {
				WebServiceListener webServiceListener = QueueUtils.createListener(WebServiceListener.class);
				ListenerMessageHandler handler = new ListenerMessageHandler<>();
				handler.setTimeout(parameterTimeout);
				Properties queueProperties = QueueUtils.getSubProperties(properties, name);
				QueueUtils.invokeSetters(handler, queueProperties); //timeout settings
				QueueUtils.invokeSetters(webServiceListener, queueProperties);
				webServiceListener.setHandler(handler);
//				webServiceListener.configure();//TODO why was configure never called?
				webServiceListener.open();

				Map<String, Object> webServiceListenerInfo = new HashMap<>();
				webServiceListenerInfo.put("webServiceListener", webServiceListener);
				webServiceListenerInfo.put("listenerMessageHandler", handler);
				queues.put(name, webServiceListenerInfo);
			} catch (Exception e) {
				closeQueues(queues, properties, writers, correlationId);
				queues = null;
				errorMessage("ERROR on ["+name+"]: "+e.getMessage(), writers);
			}
		}

		debugMessage("Initialize http senders", writers);
		iterator = httpSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

			try {
				// Use directoryClassLoader to make it possible to specify
				// styleSheetName relative to the scenarioDirectory.
				//TODO create larva classloader without basepath
				DirectoryClassLoader directoryClassLoader = new DirectoryClassLoader(originalClassLoader);
				directoryClassLoader.setDirectory(scenarioDirectory);
				directoryClassLoader.setBasePath(".");
				directoryClassLoader.configure(null, "dummy");
				Thread.currentThread().setContextClassLoader(directoryClassLoader);

				// Create the actual sender
				HttpSender httpSender = QueueUtils.createSender(HttpSender.class);
				Properties queueProperties = QueueUtils.getSubProperties(properties, name);
				QueueUtils.invokeSetters(httpSender, queueProperties);

				PipeLineSession session = new PipeLineSession();
				Map<String, Object> paramPropertiesMap = createParametersMapFromParamProperties(properties, name, writers, true, session);
				Iterator<String> parameterNameIterator = paramPropertiesMap.keySet().iterator();
				while (parameterNameIterator.hasNext()) {
					String parameterName = parameterNameIterator.next();
					Parameter parameter = (Parameter)paramPropertiesMap.get(parameterName);
					httpSender.addParameter(parameter);
				}

				httpSender.configure();
				httpSender.open();

				Map<String, Object> httpSenderInfo = new HashMap<>();
				httpSenderInfo.put("httpSender", httpSender);
				httpSenderInfo.put("session", session);
				Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));
				httpSenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
				queues.put(name, httpSenderInfo);
				debugMessage("Opened http sender '" + name + "'", writers);
			} catch (Exception e) {
				errorMessage("ERROR on ["+name+"]: "+e.getMessage(), writers);
				closeQueues(queues, properties, writers, correlationId);
				queues = null;
			} finally {
				if (originalClassLoader != null) {
					Thread.currentThread().setContextClassLoader(originalClassLoader);
				}
			}
		}

		debugMessage("Initialize ibis java senders", writers);
		iterator = ibisJavaSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			String serviceName = (String)properties.get(name + ".serviceName");
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));
			if (serviceName == null) {
				closeQueues(queues, properties, writers, correlationId);
				queues = null;
				errorMessage("Could not find serviceName property for " + name, writers);
			} else {
				IbisJavaSender ibisJavaSender = QueueUtils.createSender(IbisJavaSender.class);
				ibisJavaSender.setServiceName(serviceName);
				PipeLineSession session = new PipeLineSession();
				Map<String, Object> paramPropertiesMap = createParametersMapFromParamProperties(properties, name, writers, true, session);
				Iterator<String> parameterNameIterator = paramPropertiesMap.keySet().iterator();
				while (parameterNameIterator.hasNext()) {
					String parameterName = parameterNameIterator.next();
					Parameter parameter = (Parameter)paramPropertiesMap.get(parameterName);
					ibisJavaSender.addParameter(parameter);
				}
				try {
					ibisJavaSender.configure();
				} catch(ConfigurationException e) {
					errorMessage("Could not configure '" + name + "': " + e.getMessage(), e, writers);
					closeQueues(queues, properties, writers, correlationId);
					queues = null;
				}
				if (queues != null) {
					try {
						ibisJavaSender.open();
					} catch (SenderException e) {
						closeQueues(queues, properties, writers, correlationId);
						queues = null;
						errorMessage("Could not open '" + name + "': " + e.getMessage(), e, writers);
					}
					if (queues != null) {
						Map<String, Object> ibisJavaSenderInfo = new HashMap<String, Object>();
						ibisJavaSenderInfo.put("ibisJavaSender", ibisJavaSender);
						ibisJavaSenderInfo.put("session", session);
						ibisJavaSenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
						queues.put(name, ibisJavaSenderInfo);
						debugMessage("Opened ibis java sender '" + name + "'", writers);
					}
				}
			}
		}

		debugMessage("Initialize delay senders", writers);
		iterator = delaySenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));
			try {
				DelaySender delaySender = QueueUtils.createSender(DelaySender.class);

				Map<String, Object> delaySenderInfo = new HashMap<String, Object>();
				delaySenderInfo.put("delaySender", delaySender);
				delaySenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
				queues.put(name, delaySenderInfo);
				debugMessage("Opened delay sender '" + name + "'", writers);
			} catch (Exception e) {
				errorMessage("ERROR on ["+name+"]: "+e.getMessage(), writers);
			}
		}

		debugMessage("Initialize java listeners", writers);
		iterator = javaListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();

			//Deprecation warning
			if(properties.contains(name + ".requestTimeOut") || properties.contains(name + ".responseTimeOut")) {
				errorMessage("properties "+name+".requestTimeOut/"+name+".responseTimeOut have been replaced with "+name+".timeout", writers);
			}

			try {
				JavaListener javaListener = QueueUtils.createListener(JavaListener.class);
				ListenerMessageHandler handler = new ListenerMessageHandler<>();
				handler.setTimeout(parameterTimeout);
				Properties queueProperties = QueueUtils.getSubProperties(properties, name);
				QueueUtils.invokeSetters(handler, queueProperties); //timeout settings
				QueueUtils.invokeSetters(javaListener, queueProperties);
				javaListener.setHandler(handler);
				javaListener.configure();
				javaListener.open();

				Map<String, Object> javaListenerInfo = new HashMap<>();
				javaListenerInfo.put("javaListener", javaListener);
				javaListenerInfo.put("listenerMessageHandler", handler);
				queues.put(name, javaListenerInfo);
				debugMessage("Opened java listener '" + name + "'", writers);
			} catch (Exception e) {
				errorMessage("ERROR on ["+name+"]: "+e.getMessage(), writers);
			}
		}

		debugMessage("Initialize file senders", writers);
		iterator = fileSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();

			try {
				IQueue fileSender = QueueUtils.createQueue(FileSender.class);
				Properties queueProperties = QueueUtils.getSubProperties(properties, queueName);
				QueueUtils.invokeSetters(fileSender, queueProperties);
				fileSender.configure();

				Map<String, Object> fileSenderInfo = new HashMap<>();
				fileSenderInfo.put("fileSender", fileSender);
				queues.put(queueName, fileSenderInfo);
				debugMessage("Opened file sender '" + queueName + "'", writers);
			} catch (Exception e) {
				errorMessage("ERROR on ["+queueName+"]: "+e.getMessage(), writers);
			}
		}

		createFileListeners(queues, fileListeners, properties, writers);

		debugMessage("Initialize xslt provider listeners", writers);
		iterator = xsltProviderListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String filename  = (String)properties.get(queueName + ".filename");
			if (filename == null) {
				closeQueues(queues, properties, writers, correlationId);
				queues = null;
				errorMessage("Could not find filename property for " + queueName, writers);
			} else {
				Boolean fromClasspath = new Boolean((String)properties.get(queueName + ".fromClasspath"));
				if (!fromClasspath) {
					filename = (String)properties.get(queueName + ".filename.absolutepath");
				}
				XsltProviderListener xsltProviderListener = new XsltProviderListener();
				xsltProviderListener.setFromClasspath(fromClasspath);
				xsltProviderListener.setFilename(filename);
				String xsltVersionString = (String)properties.get(queueName + ".xsltVersion");
				if (xsltVersionString != null) {
					try {
						int xsltVersion = Integer.valueOf(xsltVersionString).intValue();
						xsltProviderListener.setXsltVersion(xsltVersion);
						debugMessage("XsltVersion set to '" + xsltVersion + "'", writers);
					} catch(Exception e) {
					}
				}
				String xslt2String = (String)properties.get(queueName + ".xslt2");
				if (xslt2String != null) {
					try {
						boolean xslt2 = Boolean.valueOf(xslt2String).booleanValue();
						xsltProviderListener.setXslt2(xslt2);
						debugMessage("Xslt2 set to '" + xslt2 + "'", writers);
					} catch(Exception e) {
					}
				}
				String namespaceAwareString = (String)properties.get(queueName + ".namespaceAware");
				if (namespaceAwareString != null) {
					try {
						boolean namespaceAware = Boolean.valueOf(namespaceAwareString).booleanValue();
						xsltProviderListener.setNamespaceAware(namespaceAware);
						debugMessage("Namespace aware set to '" + namespaceAware + "'", writers);
					} catch(Exception e) {
					}
				}
				try {
					xsltProviderListener.init();
					Map<String, Object> xsltProviderListenerInfo = new HashMap<String, Object>();
					xsltProviderListenerInfo.put("xsltProviderListener", xsltProviderListener);
					queues.put(queueName, xsltProviderListenerInfo);
					debugMessage("Opened xslt provider listener '" + queueName + "'", writers);
				} catch(ListenerException e) {
					closeQueues(queues, properties, writers, correlationId);
					queues = null;
					errorMessage("Could not create xslt provider listener for '" + queueName + "': " + e.getMessage(), e, writers);
				}
			}
		}

		return queues;
	}

	private static void createFileListeners(Map<String, Map<String, Object>> queues, List<String> fileListeners, Properties properties, Map<String, Object> writers) {
		debugMessage("Initialize file listeners", writers);
		Iterator<String> iterator = fileListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = iterator.next();

			try {
				IQueue fileListener = QueueUtils.createQueue(FileListener.class);
				Properties queueProperties = QueueUtils.getSubProperties(properties, queueName);
				QueueUtils.invokeSetters(fileListener, queueProperties);
				fileListener.configure();

				Map<String, Object> fileListenerInfo = new HashMap<String, Object>();
				fileListenerInfo.put("fileListener", fileListener);
				queues.put(queueName, fileListenerInfo);
				debugMessage("Opened file listener '" + queueName + "'", writers);
				if (fileListenerCleanUp(queueName, (FileListener) fileListener, writers)) {
					errorMessage("Found old messages on '" + queueName + "'", writers);
				}
			} catch (Exception e) {
				errorMessage("ERROR on ["+queueName+"]: "+e.getMessage(), writers);
			}
		}
	}

	private static boolean fileListenerCleanUp(String queueName, FileListener fileListener, Map<String, Object> writers) {
		return TestTool.fileListenerCleanUp(queueName, fileListener, writers);
	}

	private static boolean jmsCleanUp(String queueName, PullingJmsListener pullingJmsListener, Map<String, Object> writers) {
		return TestTool.jmsCleanUp(queueName, pullingJmsListener, writers);
	}

	private static Map<String, Object> createParametersMapFromParamProperties(Properties properties, String property, Map<String, Object> writers, boolean createParameterObjects, PipeLineSession session) {
		return TestTool.createParametersMapFromParamProperties(properties, property, writers, createParameterObjects, session);
	}

	private static void closeQueues(Map<String, Map<String, Object>> queues, Properties properties, Map<String, Object> writers, String correlationId) {
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
