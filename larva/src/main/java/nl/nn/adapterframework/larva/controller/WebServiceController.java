/*
   Copyright 2019-2020 Integration Partners

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
package nl.nn.adapterframework.larva.controller;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.http.WebServiceSender;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.larva.*;

import java.util.*;

/**
 * This class is used to initialize and execute Web Service Senders and Listeners.
 * @author Jaco de Groot, Murat Kaan Meral
 *
 */
public class WebServiceController {

	private MessageListener messageListener;
	private ScenarioTester scenarioTester;
	private ResultComparer resultComparer;

	public WebServiceController(ScenarioTester scenarioTester) {
		this.scenarioTester = scenarioTester;
		messageListener = scenarioTester.getMessageListener();
		resultComparer = new ResultComparer(messageListener);
	}

	/**
	 * Initializes the senders specified by webServiceSenders and adds it to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param webServiceSenders List of web service senders to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public void initSenders(Map<String, Map<String, Object>> queues, List<String> webServiceSenders, Properties properties, String testName) {
		messageListener.debugMessage(testName, "Initialize web service senders");
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
				scenarioTester.closeQueues(queues, properties, testName);
				queues = null;
				messageListener.errorMessage(testName, "Could not find url property for " + name);
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
					messageListener.errorMessage(testName, "Could not configure '" + name + "': " + e.getMessage(), e);
					scenarioTester.closeQueues(queues, properties, testName);
					queues = null;
				}
				if (queues != null) {
					try {
						webServiceSender.open();
					} catch (SenderException e) {
						scenarioTester.closeQueues(queues, properties, testName);
						queues = null;
						messageListener.errorMessage(testName, "Could not open '" + name + "': " + e.getMessage(), e);
					}
					if (queues != null) {
						Map<String, Object> webServiceSenderInfo = new HashMap<String, Object>();
						webServiceSenderInfo.put("webServiceSender", webServiceSender);
						webServiceSenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
						queues.put(name, webServiceSenderInfo);
						messageListener.debugMessage(testName, "Opened web service sender '" + name + "'");
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
	public void initListeners(Map<String, Map<String, Object>> queues, List<String> webServiceListeners, Properties properties, long globalTimeout, String testName) {
		messageListener.debugMessage(testName, "Initialize web service listeners");
		Iterator<String> iterator = webServiceListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			String serviceNamespaceURI = (String)properties.get(name + ".serviceNamespaceURI");

			if (serviceNamespaceURI == null) {
				scenarioTester.closeQueues(queues, properties, testName);
				queues = null;
				messageListener.errorMessage(testName, "Could not find property '" + name + ".serviceNamespaceURI'");
			} else {
				ListenerMessageHandler listenerMessageHandler = new ListenerMessageHandler();
				listenerMessageHandler.setRequestTimeOut(globalTimeout);
				listenerMessageHandler.setResponseTimeOut(globalTimeout);
				try {
					long requestTimeOut = Long.parseLong((String)properties.get(name + ".requestTimeOut"));
					listenerMessageHandler.setRequestTimeOut(requestTimeOut);
					messageListener.debugMessage(testName, "Request time out set to '" + requestTimeOut + "'");
				} catch(Exception e) {
				}
				try {
					long responseTimeOut = Long.parseLong((String)properties.get(name + ".responseTimeOut"));
					listenerMessageHandler.setResponseTimeOut(responseTimeOut);
					messageListener.debugMessage(testName, "Response time out set to '" + responseTimeOut + "'");
				} catch(Exception e) {
				}
				WebServiceListener webServiceListener = new WebServiceListener();
				webServiceListener.setName("Test Tool WebServiceListener");
				webServiceListener.setServiceNamespaceURI(serviceNamespaceURI);
				webServiceListener.setHandler(listenerMessageHandler);
				try {
					webServiceListener.open();
				} catch (ListenerException e) {
					scenarioTester.closeQueues(queues, properties, testName);
					queues = null;
					messageListener.errorMessage(testName, "Could not open web service listener '" + name + "': " + e.getMessage(), e);
				}
				Map<String, Object> webServiceListenerInfo = new HashMap<String, Object>();
				webServiceListenerInfo.put("webServiceListener", webServiceListener);
				webServiceListenerInfo.put("listenerMessageHandler", listenerMessageHandler);
				queues.put(name, webServiceListenerInfo);
				ServiceDispatcher serviceDispatcher = ServiceDispatcher.getInstance();
				try {
					serviceDispatcher.registerServiceClient(serviceNamespaceURI, webServiceListener);
					messageListener.debugMessage(testName, "Opened web service listener '" + name + "'");
				} catch(ListenerException e) {
					scenarioTester.closeQueues(queues, properties, testName);
					queues = null;
					messageListener.errorMessage(testName, "Could not open web service listener '" + name + "': " + e.getMessage(), e);
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
	public int write(String testName, String stepDisplayName, Map<String, Map<String, Object>> queues, String queueName, String fileContent) {
		int result = TestTool.RESULT_ERROR;

		Map<?, ?> listenerInfo = (Map<?, ?>)queues.get(queueName);
		ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler)listenerInfo.get("listenerMessageHandler");
		if (listenerMessageHandler == null) {
			messageListener.errorMessage(testName, "No ListenerMessageHandler found");
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
			messageListener.debugPipelineMessage(testName, stepDisplayName, "Successfully put message on '" + queueName + "':", fileContent);
			messageListener.debugMessage(testName, "Successfully put message on '" + queueName + "'");
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
	public int read(String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, String queueName, String fileName, String fileContent, String  originalFilePath, String testName) {
		int result = TestTool.RESULT_ERROR;
		Map listenerInfo = (Map)queues.get(queueName);
		ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler)listenerInfo.get("listenerMessageHandler");
		if (listenerMessageHandler == null) {
			messageListener.errorMessage(testName, "No ListenerMessageHandler found");
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
					messageListener.errorMessage(testName, "Could not read listenerMessageHandler message (null returned)");
				}
			} else {
				if ("".equals(fileName)) {
					messageListener.debugPipelineMessage(testName, stepDisplayName, "Unexpected message read from '" + queueName + "':", message);
				} else {
					result = resultComparer.compareResult(step, stepDisplayName, fileName, fileContent, message, properties, queueName, originalFilePath, testName);
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
	public void closeSender(Map<String, Map<String, Object>> queues, Properties properties, String testName) {
		messageListener.debugMessage(testName, "Close web service senders");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.http.WebServiceSender".equals(properties.get(queueName + ".className"))) {
				WebServiceSender webServiceSender = (WebServiceSender)((Map<?, ?>)queues.get(queueName)).get("webServiceSender");
				Map<?, ?> webServiceSenderInfo = (Map<?, ?>)queues.get(queueName);
				SenderThread senderThread = (SenderThread)webServiceSenderInfo.remove("webServiceSenderThread");
				if (senderThread != null) {
					messageListener.debugMessage(testName, "Found remaining SenderThread");
					SenderException senderException = senderThread.getSenderException();
					if (senderException != null) {
						messageListener.errorMessage(testName, "Found remaining SenderException: " + senderException.getMessage(), senderException);
					}
					TimeOutException timeOutException = senderThread.getTimeOutException();
					if (timeOutException != null) {
						messageListener.errorMessage(testName, "Found remaining TimeOutException: " + timeOutException.getMessage(), timeOutException);
					}
					String message = senderThread.getResponse();
					if (message != null) {
						messageListener.wrongPipelineMessage(testName, "Found remaining message on '" + queueName + "'", message);
					}
				}
				try {
					webServiceSender.close();
				} catch (SenderException e) {
					//Ignore
				}
				messageListener.debugMessage(testName, "Closed webservice sender '" + queueName + "'");
			}
		}
	}

	/**
	 * Closes the web service senders that are in the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param properties properties defined by scenario file and global app constants.
	 * @return true if there are still messages remaining.
	 */
	public boolean closeListener(Map<String, Map<String, Object>> queues, Properties properties, String testName) {
		boolean remainingMessagesFound = false;
		messageListener.debugMessage(testName, "Close web service listeners");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.http.WebServiceListener".equals(properties.get(queueName + ".className"))) {
				Map<?, ?> webServiceListenerInfo = (Map<?, ?>)queues.get(queueName);
				WebServiceListener webServiceListener = (WebServiceListener)webServiceListenerInfo.get("webServiceListener");
				webServiceListener.close();
				messageListener.debugMessage(testName, "Closed web service listener '" + queueName + "'");
				ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler)webServiceListenerInfo.get("listenerMessageHandler");
				if (listenerMessageHandler != null) {
					ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessage(0);
					while (listenerMessage != null) {
						String message = listenerMessage.getMessage();
						messageListener.wrongPipelineMessage(testName, "Found remaining request message on '" + queueName + "'", message);
						remainingMessagesFound = true;
						listenerMessage = listenerMessageHandler.getRequestMessage(0);
					}
					listenerMessage = listenerMessageHandler.getResponseMessage(0);
					while (listenerMessage != null) {
						String message = listenerMessage.getMessage();
						messageListener.wrongPipelineMessage(testName, "Found remaining response message on '" + queueName + "'", message);
						remainingMessagesFound = true;
						listenerMessage = listenerMessageHandler.getResponseMessage(0);
					}
				}
			}
		}
		return remainingMessagesFound;
	}
}
