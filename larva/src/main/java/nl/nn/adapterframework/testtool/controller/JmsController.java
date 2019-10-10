/**
 * 
 */
package nl.nn.adapterframework.testtool.controller;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.Message;

import org.springframework.beans.factory.annotation.Autowired;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.jms.PullingJmsListener;
import nl.nn.adapterframework.testtool.MessageListener;
import nl.nn.adapterframework.testtool.ResultComparer;
import nl.nn.adapterframework.testtool.ScenarioTester;
import nl.nn.adapterframework.testtool.TestTool;

/**
 * This class is used to initialize and execute JMS Senders and Listeners.
 * @author Jaco de Groot, Murat Kaan Meral
 *
 */
public class JmsController {
	@Autowired
	static IbisContext ibisContext;

	/**
	 * Initializes senders and adds them to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param jmsSenders List of JMS senders to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public static void initSenders(Map<String, Map<String, Object>> queues, List<String> jmsSenders, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		MessageListener.debugMessage(testName, "Initialize jms senders");
		Iterator<String> iterator = jmsSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String queue = (String)properties.get(queueName + ".queue");
			if (queue == null) {
				ScenarioTester.closeQueues(queues, properties);
				queues = null;
				MessageListener.errorMessage(testName, "Could not find property '" + queueName + ".queue'");
			} else {
				JmsSender jmsSender = (JmsSender)ibisContext.createBeanAutowireByName(JmsSender.class);
				jmsSender.setName("Test Tool JmsSender");
				jmsSender.setDestinationName(queue);
				jmsSender.setDestinationType("QUEUE");
				jmsSender.setAcknowledgeMode("auto");
				String jmsRealm = (String)properties.get(queueName + ".jmsRealm");
				if (jmsRealm!=null) {
					jmsSender.setJmsRealm(jmsRealm);
				} else {
					jmsSender.setJmsRealm("default");
				}
				String deliveryMode = properties.getProperty(queueName + ".deliveryMode");
				MessageListener.debugMessage(testName, "Property '" + queueName + ".deliveryMode': " + deliveryMode);
				String persistent = properties.getProperty(queueName + ".persistent");
				MessageListener.debugMessage(testName, "Property '" + queueName + ".persistent': " + persistent);
				String useCorrelationIdFrom = properties.getProperty(queueName + ".useCorrelationIdFrom");
				MessageListener.debugMessage(testName, "Property '" + queueName + ".useCorrelationIdFrom': " + useCorrelationIdFrom);
				String replyToName = properties.getProperty(queueName + ".replyToName");
				MessageListener.debugMessage(testName, "Property '" + queueName + ".replyToName': " + replyToName);
				if (deliveryMode != null) {
					MessageListener.debugMessage(testName, "Set deliveryMode to " + deliveryMode);
					jmsSender.setDeliveryMode(deliveryMode);
				}
				if ("true".equals(persistent)) {
					MessageListener.debugMessage(testName, "Set persistent to true");
					jmsSender.setPersistent(true);
				} else {
					MessageListener.debugMessage(testName, "Set persistent to false");
					jmsSender.setPersistent(false);
				}
				if (replyToName != null) {
					MessageListener.debugMessage(testName, "Set replyToName to " + replyToName);
					jmsSender.setReplyToName(replyToName);
				}
				Map<String, Object> jmsSenderInfo = new HashMap<String, Object>();
				jmsSenderInfo.put("jmsSender", jmsSender);
				jmsSenderInfo.put("useCorrelationIdFrom", useCorrelationIdFrom);
				String correlationId = properties.getProperty(queueName + ".jmsCorrelationId");
				if (correlationId!=null) {
					jmsSenderInfo.put("jmsCorrelationId", correlationId);
					MessageListener.debugMessage(testName, "Property '" + queueName + ".jmsCorrelationId': " + correlationId);
				}
				queues.put(queueName, jmsSenderInfo);
				MessageListener.debugMessage(testName, "Opened jms sender '" + queueName + "'");
			}
		}
	}
	
	/**
	 * Initializes listeners and adds them to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param jmsListeners List of JMS listeners to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 * @param globalTimeout timeout to set for JMS listeners.
	 */
	public static void initListeners(Map<String, Map<String, Object>> queues, List<String> jmsListeners, Properties properties, long globalTimeout) {
		String testName = properties.getProperty("scenario.description");
		MessageListener.debugMessage(testName, "Initialize jms listeners");
		Iterator<String> iterator = jmsListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String queue = (String)properties.get(queueName + ".queue");
			String timeout = (String)properties.get(queueName + ".timeout");

			int nTimeout = (int) globalTimeout;
			if (timeout != null && timeout.length() > 0) {
				nTimeout = Integer.parseInt(timeout);
				MessageListener.debugMessage(testName, "Overriding default timeout setting of "+globalTimeout+" with "+ nTimeout);
			}

			if (queue == null) {
				ScenarioTester.closeQueues(queues, properties);
				queues = null;
				MessageListener.errorMessage(testName, "Could not find property '" + queueName + ".queue'");
			} else {
				PullingJmsListener pullingJmsListener = (PullingJmsListener)ibisContext.createBeanAutowireByName(PullingJmsListener.class);
				pullingJmsListener.setName("Test Tool JmsListener");
				pullingJmsListener.setDestinationName(queue);
				pullingJmsListener.setDestinationType("QUEUE");
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
				Map<String, Object> jmsListenerInfo = new HashMap<String, Object>();
				jmsListenerInfo.put("jmsListener", pullingJmsListener);
				queues.put(queueName, jmsListenerInfo);
				MessageListener.debugMessage(testName, "Opened jms listener '" + queueName + "'");
				if (cleanup(testName, queueName, pullingJmsListener)) {
					MessageListener.errorMessage(testName, "Found one or more old messages on queue '" + queueName + "', you might want to run your tests with a higher 'wait before clean up' value");
				}
			}
		}
	}

	/**
	 * Closes JMS senders.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public static void closeSenders(Map<String, Map<String, Object>> queues, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		MessageListener.debugMessage(testName, "Close jms senders");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.jms.JmsSender".equals(properties.get(queueName + ".className"))) {
				JmsSender jmsSender = (JmsSender)((Map<?, ?>)queues.get(queueName)).get("jmsSender");
				jmsSender.close();
				MessageListener.debugMessage(testName, "Closed jms sender '" + queueName + "'");
			}
		}
	}
	
	/**
	 * Closes JMS listeners.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public static boolean closeListeners(Map<String, Map<String, Object>> queues, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		boolean remainingMessagesFound = false;
		MessageListener.debugMessage(testName, "Close jms listeners");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.jms.JmsListener".equals(properties.get(queueName + ".className"))) {
				PullingJmsListener pullingJmsListener = (PullingJmsListener)((Map<?, ?>)queues.get(queueName)).get("jmsListener");
				if (cleanup(testName, queueName, pullingJmsListener)) {
					remainingMessagesFound =  true;
				}
				pullingJmsListener.close();
				MessageListener.debugMessage(testName, "Closed jms listener '" + queueName + "'");
			}
		}
		return remainingMessagesFound;
	}
	
	/**
	 * Checks and cleans any remaining message for the JMS listener.
	 * @param queueName name of the pipe to be used.
	 * @param pullingJmsListener Jms Listener that requires cleaning.
	 * @return True if there are any messages remaining.
	 */
	public static boolean cleanup(String testName, String queueName, PullingJmsListener pullingJmsListener) {
		boolean remainingMessagesFound = false;
		MessageListener.debugMessage(testName, "Check for remaining messages on '" + queueName + "'");
		long oldTimeOut = pullingJmsListener.getTimeOut();
		pullingJmsListener.setTimeOut(10);
		boolean empty = false;
		while (!empty) {
			Message rawMessage = null;
			String message = null;
			Map<String, Object> threadContext = null;
			try {
				threadContext = pullingJmsListener.openThread();
				rawMessage = pullingJmsListener.getRawMessage(threadContext);
				if (rawMessage != null) {
					message = pullingJmsListener.getStringFromRawMessage(rawMessage, threadContext);
					remainingMessagesFound = true;
					if (message == null) {
						MessageListener.errorMessage(testName, "Could not translate raw message from jms queue '" + queueName + "'");
					} else {
						MessageListener.wrongPipelineMessage(testName, "Found remaining message on '" + queueName + "'", message);
					}
				}
			} catch(ListenerException e) {
				MessageListener.errorMessage(testName, "ListenerException on jms clean up '" + queueName + "': " + e.getMessage(), e);
			} finally {
				if (threadContext != null) {
					try {
						pullingJmsListener.closeThread(threadContext);
					} catch(ListenerException e) {
						MessageListener.errorMessage(testName, "Could not close thread on jms listener '" + queueName + "': " + e.getMessage(), e);
					}
				}
			}
			if (rawMessage == null) {
				empty = true;
			}
		}
		pullingJmsListener.setTimeOut((int) oldTimeOut);
		return remainingMessagesFound;
	}

	/**
	 * Sends the fileContent to given jms sender.
	 * @param stepDisplayName string that contains the pipe's display name.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param fileContent Content that will be send to the pipe.
	 * @return 1 if everything is ok, 0 if there has been an error.
	 */
	public static int write(String testName, String stepDisplayName, Map<String, Map<String, Object>> queues, String queueName, String fileContent) {
		int result = TestTool.RESULT_ERROR;
		
		Map<?, ?> jmsSenderInfo = (Map<?, ?>)queues.get(queueName);
		JmsSender jmsSender = (JmsSender)jmsSenderInfo.get("jmsSender");
		try {
			String correlationId = null;
			String useCorrelationIdFrom = (String)jmsSenderInfo.get("useCorrelationIdFrom");
			if (useCorrelationIdFrom != null) {
				Map<?, ?> listenerInfo = (Map<?, ?>)queues.get(useCorrelationIdFrom);
				if (listenerInfo == null) {
					MessageListener.errorMessage(testName, "Could not find listener '" + useCorrelationIdFrom + "' to use correlation id from");
				} else {
					correlationId = (String)listenerInfo.get("correlationId");
					if (correlationId == null) {
						MessageListener.errorMessage(testName, "Could not find correlation id from listener '" + useCorrelationIdFrom + "'");
					}
				}
			}
			if (correlationId == null) {
				correlationId = (String)jmsSenderInfo.get("jmsCorrelationId");
			}
			if (correlationId == null) {
				correlationId = TestTool.TESTTOOL_CORRELATIONID;
			}
			jmsSender.sendMessage(correlationId, fileContent);
			MessageListener.debugPipelineMessage(testName, stepDisplayName, "Successfully written to '" + queueName + "':", fileContent);
			result = TestTool.RESULT_OK;
		} catch(TimeOutException e) {
			MessageListener.errorMessage(testName, "Time out sending jms message to '" + queueName + "': " + e.getMessage(), e);
		} catch(SenderException e) {
			MessageListener.errorMessage(testName, "Could not send jms message to '" + queueName + "': " + e.getMessage(), e);
		}
		
		return result;
	}

	/**
	 * Reads from the pipe and compares the output to the expected string.
	 * @param step string that contains the whole step.
	 * @param stepDisplayName string that contains the pipe's display name.
	 * @param properties properties defined by scenario file and global app constants.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param fileName file that contains the expedted output.
	 * @param fileContent expected output.
	 * @return 1 if everything is ok, 0 if there has been an error, 2 if autosaved.
	 */
	public static int read(String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, String queueName, String fileName, String fileContent) {
		int result = TestTool.RESULT_ERROR;
		String testName = properties.getProperty("scenario.description");

		Map jmsListenerInfo = (Map)queues.get(queueName);
		PullingJmsListener pullingJmsListener = (PullingJmsListener)jmsListenerInfo.get("jmsListener");
		Map threadContext = null;
		String message = null;
		try {
			threadContext = pullingJmsListener.openThread();
			Message rawMessage = pullingJmsListener.getRawMessage(threadContext);
			if (rawMessage != null) {
				message = pullingJmsListener.getStringFromRawMessage(rawMessage, threadContext);
				String correlationId = pullingJmsListener.getIdFromRawMessage(rawMessage, threadContext);
				jmsListenerInfo.put("correlationId", correlationId);
			}
		} catch(ListenerException e) {
			if (!"".equals(fileName)) {
				MessageListener.errorMessage(testName, "Could not read jms message from '" + queueName + "': " + e.getMessage(), e);
			}
		} finally {
			if (threadContext != null) {
				try {
					pullingJmsListener.closeThread(threadContext);
				} catch(ListenerException e) {
					MessageListener.errorMessage(testName, "Could not close thread on jms listener '" + queueName + "': " + e.getMessage(), e);
				}
			}
		}
		
		if (message == null) {
			if ("".equals(fileName)) {
				result = TestTool.RESULT_OK;
			} else {
				MessageListener.errorMessage(testName, "Could not read jms message (null returned)");
			}
		} else {
			result = ResultComparer.compareResult(step, stepDisplayName, fileName, fileContent, message, properties, queueName);
		}

		return result;	
	}

}
