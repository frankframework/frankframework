package nl.nn.adapterframework.testtool.controller;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.testtool.*;

import java.util.*;

/**
 * This class is used to initialize and read from XsltProvider and Java Listeners.
 * @author Jaco de Groot, Murat Kaan Meral
 */
public class ListenerController {

	private MessageListener messageListener;
	private ScenarioTester scenarioTester;
	private ResultComparer resultComparer;

	public ListenerController(ScenarioTester scenarioTester) {
		this.scenarioTester = scenarioTester;
		messageListener = scenarioTester.getMessageListener();
		resultComparer = new ResultComparer(messageListener);
	}

	/**
	 * Initializes and adds the senders to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param javaListeners listeners to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public void initJavaListener(Map<String, Map<String, Object>> queues, List<String> javaListeners, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		messageListener.debugMessage(testName, "Initialize java listeners");
		Iterator<String> iterator = javaListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			String serviceName = (String)properties.get(name + ".serviceName");
			if (serviceName == null) {
				scenarioTester.closeQueues(queues, properties);
				queues = null;
				messageListener.errorMessage(testName, "Could not find property '" + name + ".serviceName'");
			} else {
				ListenerMessageHandler listenerMessageHandler = new ListenerMessageHandler();
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
				JavaListener javaListener = new JavaListener();
				javaListener.setName("Test Tool JavaListener");
				javaListener.setServiceName(serviceName);
				javaListener.setHandler(listenerMessageHandler);
				try {
					javaListener.open();
					Map<String, Object> javaListenerInfo = new HashMap<String, Object>();
					javaListenerInfo.put("javaListener", javaListener);
					javaListenerInfo.put("listenerMessageHandler", listenerMessageHandler);
					queues.put(name, javaListenerInfo);
					messageListener.debugMessage(testName, "Opened java listener '" + name + "'");
				} catch(ListenerException e) {
					scenarioTester.closeQueues(queues, properties);
					queues = null;
					messageListener.errorMessage(testName, "Could not open java listener '" + name + "': " + e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Initializes and adds the senders to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param xsltProviderListeners listeners to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public void initXsltProviderListener(Map<String, Map<String, Object>> queues, List<String> xsltProviderListeners, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		messageListener.debugMessage(testName, "Initialize xslt provider listeners");
		Iterator<String> iterator = xsltProviderListeners.iterator();
		while (queues != null && iterator.hasNext()) {
			String queueName = (String)iterator.next();
			String filename  = (String)properties.get(queueName + ".filename");
			if (filename == null) {
				scenarioTester.closeQueues(queues, properties);
				queues = null;
				messageListener.errorMessage(testName, "Could not find filename property for " + queueName);
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
						messageListener.debugMessage(testName, "XsltVersion set to '" + xsltVersion + "'");
					} catch(Exception e) {
					}
				}
				String xslt2String = (String)properties.get(queueName + ".xslt2");
				if (xslt2String != null) {
					try {
						boolean xslt2 = Boolean.valueOf(xslt2String).booleanValue();
						xsltProviderListener.setXslt2(xslt2);
						messageListener.debugMessage(testName, "Xslt2 set to '" + xslt2 + "'");
					} catch(Exception e) {
					}
				}
				String namespaceAwareString = (String)properties.get(queueName + ".namespaceAware");
				if (namespaceAwareString != null) {
					try {
						boolean namespaceAware = Boolean.valueOf(namespaceAwareString).booleanValue();
						xsltProviderListener.setNamespaceAware(namespaceAware);
						messageListener.debugMessage(testName, "Namespace aware set to '" + namespaceAware + "'");
					} catch(Exception e) {
					}
				}
				try {
					xsltProviderListener.init();
					Map<String, Object> xsltProviderListenerInfo = new HashMap<String, Object>();
					xsltProviderListenerInfo.put("xsltProviderListener", xsltProviderListener);
					queues.put(queueName, xsltProviderListenerInfo);
					messageListener.debugMessage(testName, "Opened xslt provider listener '" + queueName + "'");
				} catch(ListenerException e) {
					scenarioTester.closeQueues(queues, properties);
					queues = null;
					messageListener.errorMessage(testName, "Could not create xslt provider listener for '" + queueName + "': " + e.getMessage(), e);
				}
			}
		}

	}

	/**
	 * Closes Java Listeners that are in the queue and checks if there were any messages unread.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param properties properties defined by scenario file and global app constants.
	 * @return true if there were any remaining messages before closing.
	 */
	public boolean closeJavaListener(Map<String, Map<String, Object>> queues, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		boolean remainingMessagesFound = false;
		messageListener.debugMessage(testName, "Close java listeners");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.receivers.JavaListener".equals(properties.get(queueName + ".className"))) {
				Map<?, ?> javaListenerInfo = (Map<?, ?>)queues.get(queueName);
				JavaListener javaListener = (JavaListener)javaListenerInfo.get("javaListener");
				try {
					javaListener.close();
					messageListener.debugMessage(testName, "Closed java listener '" + queueName + "'");
				} catch(ListenerException e) {
					messageListener.errorMessage(testName, "Could not close java listener '" + queueName + "': " + e.getMessage(), e);
				}
				ListenerMessageHandler listenerMessageHandler = (ListenerMessageHandler)javaListenerInfo.get("listenerMessageHandler");
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

	/**
	 * Closes Xslt Provider Listeners that are in the queue and checks if there were any messages unread.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param properties properties defined by scenario file and global app constants.
	 * @return true if there were any remaining messages before closing.
	 */
	public boolean closeXsltProviderListener(Map<String, Map<String, Object>> queues, Properties properties) {
		String testName = properties.getProperty("scenario.description");
		boolean remainingMessagesFound = false;
		messageListener.debugMessage(testName, "Close xslt provider listeners");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.testtool.XsltProviderListener".equals(properties.get(queueName + ".className"))) {
				XsltProviderListener xsltProviderListener = (XsltProviderListener)((Map<?, ?>)queues.get(queueName)).get("xsltProviderListener");
				remainingMessagesFound = xsltProviderListenerCleanUp(testName, queues, queueName);
				messageListener.debugMessage(testName, "Closed xslt provider listener '" + queueName + "'");
			}
		}
		return remainingMessagesFound;
	}
	
	/**
	 * Checks if there were any messages unread for Xslt Provider Listener.
	 * @param queues Queue of steps to execute as well as the variables required to execute
	 * @param queueName name of the pipe to be used.
	 * @return true if there were any remaining messages before closing.
	 */
	private boolean xsltProviderListenerCleanUp(String testName, Map<String, Map<String, Object>> queues, String queueName) {
		boolean remainingMessagesFound = false;
		Map<?, ?> xsltProviderListenerInfo = (Map<?, ?>)queues.get(queueName);
		XsltProviderListener xsltProviderListener = (XsltProviderListener)xsltProviderListenerInfo.get("xsltProviderListener");
		String message = xsltProviderListener.getResult();
		if (message != null) {
			remainingMessagesFound = true;
			messageListener.wrongPipelineMessage(testName, "Found remaining message on '" + queueName + "'", message);
		}
		return remainingMessagesFound;
	}

	/**
	 * Sends the given data  to Xslt Provider Listener and compares the output to with the given content.
	 * @param step string that contains the whole step.
	 * @param stepDisplayName string that contains the pipe's display name.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param fileName name of the file that contains the expected result.
	 * @param fileContent Content of the file that contains expected result.
	 * @param properties properties defined by scenario file and global app constants.
	 * @return 0 if no problems, 1 if error has occurred, 2 if it has been autosaved.
	 */
	public int executeXsltProviderListenerWrite(String step, String stepDisplayName, Map<String, Map<String, Object>> queues, String queueName, String fileName, String fileContent, Properties properties, String  originalFilePath) {
		String testName = properties.getProperty("scenario.description");
		int result = TestTool.RESULT_ERROR;
		Map<?, ?> xsltProviderListenerInfo = (Map<?, ?>)queues.get(queueName);
		XsltProviderListener xsltProviderListener = (XsltProviderListener)xsltProviderListenerInfo.get("xsltProviderListener");
		String message = xsltProviderListener.getResult();
		if (message == null) {
			if ("".equals(fileName)) {
				result = TestTool.RESULT_OK;
			} else {
				messageListener.errorMessage(testName, "Could not read result (null returned)");
			}
		} else {
			result = resultComparer.compareResult(step, stepDisplayName, fileName, fileContent, message, properties, queueName, originalFilePath);
		}
		return result;
	}

	/**
	 * Reads the output of the pipe and compares it to the given expected output.
	 * @param stepDisplayName string that contains the pipe's display name.
	 * @param properties properties defined by scenario file and global app constants.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param fileContent string that contains the data to pass onto the xslt provider listener.
	 * @param xsltParameters parameters to pass onto the Xslt Provider Listener
	 * @return 1 if everything is ok, 0 if there has been an error.
	 */
	public int executeXsltProviderListenerRead(String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, String queueName, String fileContent, Map<String, Object> xsltParameters) {
		String testName = properties.getProperty("scenario.description");
		int result = TestTool.RESULT_ERROR;
		Map<?, ?> xsltProviderListenerInfo = (Map<?, ?>)queues.get(queueName);
		if (xsltProviderListenerInfo == null) {
			messageListener.errorMessage(testName, "No info found for xslt provider listener '" + queueName + "'");
		} else {
			XsltProviderListener xsltProviderListener = (XsltProviderListener)xsltProviderListenerInfo.get("xsltProviderListener");
			if (xsltProviderListener == null) {
				messageListener.errorMessage(testName, "XSLT provider listener not found for '" + queueName + "'");
			} else {
				try {
					xsltProviderListener.processRequest(fileContent, xsltParameters);
					result = TestTool.RESULT_OK;
				} catch(ListenerException e) {
					messageListener.errorMessage(testName, "Could not transform xml: " + e.getMessage(), e);
				}
				messageListener.debugPipelineMessage(testName, stepDisplayName, "Result:", fileContent);
			}
		}
		return result;	
	}
}

