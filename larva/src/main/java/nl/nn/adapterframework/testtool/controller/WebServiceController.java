package nl.nn.adapterframework.testtool.controller;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.http.IbisWebServiceSender;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.http.WebServiceSender;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.testtool.ListenerMessage;
import nl.nn.adapterframework.testtool.ListenerMessageHandler;
import nl.nn.adapterframework.testtool.MessageListener;
import nl.nn.adapterframework.testtool.ResultComparer;
import nl.nn.adapterframework.testtool.ScenarioTester;
import nl.nn.adapterframework.testtool.SenderThread;
import nl.nn.adapterframework.testtool.TestTool;

/**
 * This class is used to initialize and execute Web Service Senders and Listeners.
 * @author Jaco de Groot, Murat Kaan Meral
 *
 */
public class WebServiceController {

	/**
	 * Initializes the senders specified by webServiceSenders and adds it to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param webServiceSenders List of web service senders to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public static void initSenders(Map<String, Map<String, Object>> queues, List<String> webServiceSenders, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		MessageListener.debugMessage(testName, "Initialize web service senders");
		Iterator<String> iterator = webServiceSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));
			String url = (String)properties.get(name + ".url");
			String userName = (String)properties.get(name + ".userName");
			String password = (String)properties.get(name + ".password");
			String soap = (String)properties.get(name + ".soap");
			String allowSelfSignedCertificates = (String)properties.get(name + ".allowSelfSignedCertificates");
			if (url == null) {
				ScenarioTester.closeQueues(queues, properties);
				queues = null;
				MessageListener.errorMessage(testName, "Could not find url property for " + name);
			} else {
				WebServiceSender webServiceSender = new WebServiceSender();
				webServiceSender.setName("Test Tool WebServiceSender");
				webServiceSender.setUrl(url);
				webServiceSender.setUserName(userName);
				webServiceSender.setPassword(password);
				if (soap != null) {
					webServiceSender.setSoap(new Boolean(soap));
				}
				if (allowSelfSignedCertificates != null) {
					webServiceSender.setAllowSelfSignedCertificates(new Boolean(allowSelfSignedCertificates));
				}
				String serviceNamespaceURI = (String)properties.get(name + ".serviceNamespaceURI");
				if (serviceNamespaceURI != null) {
					webServiceSender.setServiceNamespaceURI(serviceNamespaceURI);
				}
				String serviceNamespace = (String)properties.get(name + ".serviceNamespace");
				if (serviceNamespace != null) {
					webServiceSender.setServiceNamespace(serviceNamespace);
				}
				try {
					webServiceSender.configure();
				} catch(ConfigurationException e) {
					MessageListener.errorMessage(testName, "Could not configure '" + name + "': " + e.getMessage(), e);
					ScenarioTester.closeQueues(queues, properties);
					queues = null;
				}
				if (queues != null) {
					try {
						webServiceSender.open();
					} catch (SenderException e) {
						ScenarioTester.closeQueues(queues, properties);
						queues = null;
						MessageListener.errorMessage(testName, "Could not open '" + name + "': " + e.getMessage(), e);
					}
					if (queues != null) {
						Map<String, Object> webServiceSenderInfo = new HashMap<String, Object>();
						webServiceSenderInfo.put("webServiceSender", webServiceSender);
						webServiceSenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
						queues.put(name, webServiceSenderInfo);
						MessageListener.debugMessage(testName, "Opened web service sender '" + name + "'");
					}
				}
			}
		}

	}

	/**
	 * Initializes the listeners specified by webServiceListeners and adds it to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param webServiceListeners List of web service listeners to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 * @param globalTimeout timeout value for listening to messages.
	 */
	public static void initListeners(Map<String, Map<String, Object>> queues, List<String> webServiceListeners, Properties properties, long globalTimeout) {
		String testName = properties.getProperty("scenario.description");
		MessageListener.debugMessage(testName, "Initialize web service listeners");
		Iterator<String> iterator = webServiceListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			String serviceNamespaceURI = (String)properties.get(name + ".serviceNamespaceURI");

			if (serviceNamespaceURI == null) {
				ScenarioTester.closeQueues(queues, properties);
				queues = null;
				MessageListener.errorMessage(testName, "Could not find property '" + name + ".serviceNamespaceURI'");
			} else {
				ListenerMessageHandler listenerMessageHandler = new ListenerMessageHandler();
				listenerMessageHandler.setRequestTimeOut(globalTimeout);
				listenerMessageHandler.setResponseTimeOut(globalTimeout);
				try {
					long requestTimeOut = Long.parseLong((String)properties.get(name + ".requestTimeOut"));
					listenerMessageHandler.setRequestTimeOut(requestTimeOut);
					MessageListener.debugMessage(testName, "Request time out set to '" + requestTimeOut + "'");
				} catch(Exception e) {
				}
				try {
					long responseTimeOut = Long.parseLong((String)properties.get(name + ".responseTimeOut"));
					listenerMessageHandler.setResponseTimeOut(responseTimeOut);
					MessageListener.debugMessage(testName, "Response time out set to '" + responseTimeOut + "'");
				} catch(Exception e) {
				}
				WebServiceListener webServiceListener = new WebServiceListener();
				webServiceListener.setName("Test Tool WebServiceListener");
				webServiceListener.setServiceNamespaceURI(serviceNamespaceURI);
				webServiceListener.setHandler(listenerMessageHandler);
				try {
					webServiceListener.open();
				} catch (ListenerException e) {
					ScenarioTester.closeQueues(queues, properties);
					queues = null;
					MessageListener.errorMessage(testName, "Could not open web service listener '" + name + "': " + e.getMessage(), e);
				}
				Map<String, Object> webServiceListenerInfo = new HashMap<String, Object>();
				webServiceListenerInfo.put("webServiceListener", webServiceListener);
				webServiceListenerInfo.put("listenerMessageHandler", listenerMessageHandler);
				queues.put(name, webServiceListenerInfo);
				ServiceDispatcher serviceDispatcher = ServiceDispatcher.getInstance();
				try {
					serviceDispatcher.registerServiceClient(serviceNamespaceURI, webServiceListener);
					MessageListener.debugMessage(testName, "Opened web service listener '" + name + "'");
				} catch(ListenerException e) {
					ScenarioTester.closeQueues(queues, properties);
					queues = null;
					MessageListener.errorMessage(testName, "Could not open web service listener '" + name + "': " + e.getMessage(), e);
				}
			}
		}
	}
	
	/**
	 * Writes message to the pipe.
	 * @param stepDisplayName to be displayed, used for debugging.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param fileContent The message to send.
	 * @return positive integer if no problems, 0 if there has been an error.
	 */
	public static int write(String testName, String stepDisplayName, Map<String, Map<String, Object>> queues, String queueName, String fileContent) {
		int result = TestTool.RESULT_ERROR;

		Map<?, ?> listenerInfo = (Map<?, ?>)queues.get(queueName);
		ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler)listenerInfo.get("listenerMessageHandler");
		if (listenerMessageHandler == null) {
			MessageListener.errorMessage(testName, "No ListenerMessageHandler found");
		} else {
			String correlationId = null;
			Map<?, ?> context = new HashMap<Object, Object>();
			ListenerMessage requestListenerMessage = (ListenerMessage)listenerInfo.get("listenerMessage");
			if (requestListenerMessage != null) {
				correlationId = requestListenerMessage.getCorrelationId();
				context = requestListenerMessage.getContext();
			}
			ListenerMessage listenerMessage = new ListenerMessage(correlationId, fileContent, context);
			listenerMessageHandler.putResponseMessage(listenerMessage);
			MessageListener.debugPipelineMessage(testName, stepDisplayName, "Successfully put message on '" + queueName + "':", fileContent);
			MessageListener.debugMessage(testName, "Successfully put message on '" + queueName + "'");
			result = TestTool.RESULT_OK;
		}

		return result;
	}
	
	/**
	 * Reads message from the pipe and then compares it to the content of the expected result file.
	 * @param step string that contains the whole step.
	 * @param stepDisplayName string that contains the pipe's display name.
	 * @param properties properties defined by scenario file and global app constants.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param fileName name of the file that contains the expected result.
	 * @param fileContent Content of the file that contains expected result.
	 * @return 0 if no problems, 1 if error has occurred, 2 if it has been autosaved.
	 */
	public static int read(String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, String queueName, String fileName, String fileContent) {
		int result = TestTool.RESULT_ERROR;
		String testName = properties.getProperty("scenario.description");
		Map listenerInfo = (Map)queues.get(queueName);
		ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler)listenerInfo.get("listenerMessageHandler");
		if (listenerMessageHandler == null) {
			MessageListener.errorMessage(testName, "No ListenerMessageHandler found");
		} else {
			String message = null;
			ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessage();
			if (listenerMessage != null) {
				message = listenerMessage.getMessage();
				listenerInfo.put("listenerMessage", listenerMessage);
			}
			if (message == null) {
				if ("".equals(fileName)) {
					result = TestTool.RESULT_OK;
				} else {
					MessageListener.errorMessage(testName, "Could not read listenerMessageHandler message (null returned)");
				}
			} else {
				if ("".equals(fileName)) {
					MessageListener.debugPipelineMessage(testName, stepDisplayName, "Unexpected message read from '" + queueName + "':", message);
				} else {
					result = ResultComparer.compareResult(step, stepDisplayName, fileName, fileContent, message, properties, queueName);
					if (result!=TestTool.RESULT_OK) {
						// Send a clean up reply because there is probably a
						// thread waiting for a reply
						String correlationId = null;
						Map<?, ?> context = new HashMap<Object, Object>();
						listenerMessage = new ListenerMessage(correlationId, TestTool.TESTTOOL_CLEAN_UP_REPLY, context);
						listenerMessageHandler.putResponseMessage(listenerMessage);
					}
				}
			}
		}
		
		return result;
	}

	/**
	 * Closes the web service senders that are in the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public static void closeSender(Map<String, Map<String, Object>> queues, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		MessageListener.debugMessage(testName, "Close web service senders");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.http.WebServiceSender".equals(properties.get(queueName + ".className"))) {
				WebServiceSender webServiceSender = (WebServiceSender)((Map<?, ?>)queues.get(queueName)).get("webServiceSender");
				Map<?, ?> webServiceSenderInfo = (Map<?, ?>)queues.get(queueName);
				SenderThread senderThread = (SenderThread)webServiceSenderInfo.remove("webServiceSenderThread");
				if (senderThread != null) {
					MessageListener.debugMessage(testName, "Found remaining SenderThread");
					SenderException senderException = senderThread.getSenderException();
					if (senderException != null) {
						MessageListener.errorMessage(testName, "Found remaining SenderException: " + senderException.getMessage(), senderException);
					}
					TimeOutException timeOutException = senderThread.getTimeOutException();
					if (timeOutException != null) {
						MessageListener.errorMessage(testName, "Found remaining TimeOutException: " + timeOutException.getMessage(), timeOutException);
					}
					String message = senderThread.getResponse();
					if (message != null) {
						MessageListener.wrongPipelineMessage(testName, "Found remaining message on '" + queueName + "'", message);
					}
				}
				try {
					webServiceSender.close();
				} catch (SenderException e) {
					//Ignore
				}
				MessageListener.debugMessage(testName, "Closed webservice sender '" + queueName + "'");
			}
		}
	}

	/**
	 * Closes the web service senders that are in the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param properties properties defined by scenario file and global app constants.
	 * @return true if there are still messages remaining.
	 */
	public static boolean closeListener(Map<String, Map<String, Object>> queues, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		boolean remainingMessagesFound = false;
		MessageListener.debugMessage(testName, "Close web service listeners");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.http.WebServiceListener".equals(properties.get(queueName + ".className"))) {
				Map<?, ?> webServiceListenerInfo = (Map<?, ?>)queues.get(queueName);
				WebServiceListener webServiceListener = (WebServiceListener)webServiceListenerInfo.get("webServiceListener");
				webServiceListener.close();
				MessageListener.debugMessage(testName, "Closed web service listener '" + queueName + "'");
				ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler)webServiceListenerInfo.get("listenerMessageHandler");
				if (listenerMessageHandler != null) {
					ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessage(0);
					while (listenerMessage != null) {
						String message = listenerMessage.getMessage();
						MessageListener.wrongPipelineMessage(testName, "Found remaining request message on '" + queueName + "'", message);
						remainingMessagesFound = true;
						listenerMessage = listenerMessageHandler.getRequestMessage(0);
					}
					listenerMessage = listenerMessageHandler.getResponseMessage(0);
					while (listenerMessage != null) {
						String message = listenerMessage.getMessage();
						MessageListener.wrongPipelineMessage(testName, "Found remaining response message on '" + queueName + "'", message);
						remainingMessagesFound = true;
						listenerMessage = listenerMessageHandler.getResponseMessage(0);
					}
				}
			}
		}
		return remainingMessagesFound;
	}
}
