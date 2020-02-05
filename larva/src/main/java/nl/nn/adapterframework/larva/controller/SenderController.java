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
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.classloaders.DirectoryClassLoader;
import nl.nn.adapterframework.core.*;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.http.IbisWebServiceSender;
import nl.nn.adapterframework.larva.*;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.DelaySender;
import nl.nn.adapterframework.senders.IbisJavaSender;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Class to initialize, execute and close senders that are used by Larva test tool.
 * @author Jaco de Groot, Murat Kaan Meral
 *
 */
public class SenderController {
	//@Autowired
	static IbisContext ibisContext;

	private MessageListener messageListener;
	private ScenarioTester scenarioTester;
	private ResultComparer resultComparer;

	public SenderController(ScenarioTester scenarioTester) {
		this.scenarioTester = scenarioTester;
		messageListener = scenarioTester.getMessageListener();
		resultComparer = new ResultComparer(messageListener);
		ibisContext = TestTool.getIbisContext();
	}

	/**
	 * Initializes the senders specified by ibisWebServiceSenders and adds it to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param ibisWebServiceSenders List of ibis web service senders to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public void initIbisWebSender(Map<String, Map<String, Object>> queues, List<String> ibisWebServiceSenders, Properties properties, String testName) {
		messageListener.debugMessage(testName, "Initialize ibis web service senders");
		Iterator<String> iterator = ibisWebServiceSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
	
			String ibisHost = (String)properties.get(name + ".ibisHost");
			String ibisInstance = (String)properties.get(name + ".ibisInstance");
			String serviceName = (String)properties.get(name + ".serviceName");
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));

			if (ibisHost == null) {
				scenarioTester.closeQueues(queues, properties, testName);
				queues = null;
				messageListener.errorMessage(testName, "Could not find ibisHost property for " + name);
			} else if (ibisInstance == null) {
				scenarioTester.closeQueues(queues, properties, testName);
				queues = null;
				messageListener.errorMessage(testName, "Could not find ibisInstance property for " + name);
			} else if (serviceName == null) {
				scenarioTester.closeQueues(queues, properties, testName);
				queues = null;
				messageListener.errorMessage(testName, "Could not find serviceName property for " + name);
			} else {
				IbisWebServiceSender ibisWebServiceSender = new IbisWebServiceSender();
				ibisWebServiceSender.setName("Test Tool IbisWebServiceSender");
				ibisWebServiceSender.setIbisHost(ibisHost);
				ibisWebServiceSender.setIbisInstance(ibisInstance);
				ibisWebServiceSender.setServiceName(serviceName);
				try {
					ibisWebServiceSender.configure();
				} catch(ConfigurationException e) {
					messageListener.errorMessage(testName, "Could not configure '" + name + "': " + e.getMessage(), e);
					scenarioTester.closeQueues(queues, properties, testName);
					queues = null;
				}
				try {
					ibisWebServiceSender.open();
				} catch (SenderException e) {
					scenarioTester.closeQueues(queues, properties, testName);
					queues = null;
					messageListener.errorMessage(testName, "Could not open '" + name + "': " + e.getMessage(), e);
				}
				if (queues != null) {
					Map<String, Object> ibisWebServiceSenderInfo = new HashMap<String, Object>();
					ibisWebServiceSenderInfo.put("ibisWebServiceSender", ibisWebServiceSender);
					ibisWebServiceSenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
					queues.put(name, ibisWebServiceSenderInfo);
					messageListener.debugMessage(testName, "Opened ibis web service sender '" + name + "'");
				}
			}
		}
	}

	/**
	 * 
	 * Initializes the senders specified by HTTP senders and adds it to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param httpSenders List of HTTP senders to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 * @param scenarioDirectory directory to load neccessary classes.
	 */
	public void initHttpSender(Map<String, Map<String, Object>> queues, List<String> httpSenders, Properties properties, String scenarioDirectory, String testName) {
		messageListener.debugMessage(testName, "Initialize http senders");
		Iterator<String> iterator = httpSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));
			String url = (String)properties.get(name + ".url");
			String userName = (String)properties.get(name + ".userName");
			String password = (String)properties.get(name + ".password");
			String headerParams = (String)properties.get(name + ".headersParams");
			String xhtmlString = (String)properties.get(name + ".xhtml");
			String methodtype = (String)properties.get(name + ".methodType");
			String paramsInUrlString = (String)properties.get(name + ".paramsInUrl");
			String inputMessageParam = (String)properties.get(name + ".inputMessageParam");
			String multipartString = (String)properties.get(name + ".multipart");
 			String styleSheetName = (String)properties.get(name + ".styleSheetName");
			if (url == null) {
				scenarioTester.closeQueues(queues, properties, testName);
				queues = null;
				messageListener.errorMessage(testName, "Could not find url property for " + name);
			} else {
				HttpSender httpSender = null;
				ParameterResolutionContext parameterResolutionContext = null;
				ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
				try {
					// Use directoryClassLoader to make it possible to specify
					// styleSheetName relative to the scenarioDirectory.
					DirectoryClassLoader directoryClassLoader = new DirectoryClassLoader(originalClassLoader);
					directoryClassLoader.setDirectory(scenarioDirectory);
					directoryClassLoader.setBasePath(".");
					directoryClassLoader.configure(ibisContext, "dummy");
					Thread.currentThread().setContextClassLoader(directoryClassLoader);
					httpSender = new HttpSender();
					httpSender.setName("Test Tool HttpSender");
					httpSender.setUrl(url);
					httpSender.setUserName(userName);
					httpSender.setPassword(password);
					httpSender.setHeadersParams(headerParams);
					if (StringUtils.isNotEmpty(xhtmlString)) {
						httpSender.setXhtml(Boolean.valueOf(xhtmlString).booleanValue());
					}
					if (StringUtils.isNotEmpty(methodtype)) {
						httpSender.setMethodType(methodtype);
					}
					if (StringUtils.isNotEmpty(paramsInUrlString)) {
						httpSender.setParamsInUrl(Boolean.valueOf(paramsInUrlString).booleanValue());
					}
					if (StringUtils.isNotEmpty(inputMessageParam)) {
						httpSender.setInputMessageParam(inputMessageParam);
					}
					if (StringUtils.isNotEmpty(multipartString)) {
						httpSender.setMultipart(Boolean.valueOf(multipartString).booleanValue());
					}
					if (StringUtils.isNotEmpty(styleSheetName)) {
						httpSender.setStyleSheetName(styleSheetName);
					}
					parameterResolutionContext = new ParameterResolutionContext();
					parameterResolutionContext.setSession(new PipeLineSessionBase());
					Map<String, Object> paramPropertiesMap = TestPreparer.createParametersMapFromParamProperties(properties, name, true, parameterResolutionContext);
					Iterator<String> parameterNameIterator = paramPropertiesMap.keySet().iterator();
					while (parameterNameIterator.hasNext()) {
						String parameterName = (String)parameterNameIterator.next();
						Parameter parameter = (Parameter)paramPropertiesMap.get(parameterName);
						httpSender.addParameter(parameter);
					}
					httpSender.configure();
				} catch(ConfigurationException e) {
					messageListener.errorMessage(testName, "Could not configure '" + name + "': " + e.getMessage(), e);
					scenarioTester.closeQueues(queues, properties, testName);
					queues = null;
				} finally {
					if (originalClassLoader != null) {
						Thread.currentThread().setContextClassLoader(originalClassLoader);
					}
				}
				if (queues != null) {
					try {
						httpSender.open();
					} catch (SenderException e) {
						scenarioTester.closeQueues(queues, properties, testName);
						queues = null;
						messageListener.errorMessage(testName, "Could not open '" + name + "': " + e.getMessage(), e);
					}
					if (queues != null) {
						Map<String, Object> httpSenderInfo = new HashMap<String, Object>();
						httpSenderInfo.put("httpSender", httpSender);
						httpSenderInfo.put("parameterResolutionContext", parameterResolutionContext);
						httpSenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
						queues.put(name, httpSenderInfo);
						messageListener.debugMessage(testName, "Opened http sender '" + name + "'");
					}
				}
			}
		}

	}

	/**
	 * Initializes the senders specified by ibisJavaSenders and adds it to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param ibisJavaSenders List of ibis java senders to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public void initIbisJavaSender(Map<String, Map<String, Object>> queues, List<String> ibisJavaSenders, Properties properties, String testName) {
		messageListener.debugMessage(testName, "Initialize ibis java senders");
		Iterator<String> iterator = ibisJavaSenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			String serviceName = (String)properties.get(name + ".serviceName");
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));
			if (serviceName == null) {
				scenarioTester.closeQueues(queues, properties, testName);
				queues = null;
				messageListener.errorMessage(testName, "Could not find serviceName property for " + name);
			} else {
				IbisJavaSender ibisJavaSender = new IbisJavaSender();
				ibisJavaSender.setName("Test Tool IbisJavaSender");
				ibisJavaSender.setServiceName(serviceName);
				ParameterResolutionContext parameterResolutionContext = new ParameterResolutionContext();
				parameterResolutionContext.setSession(new PipeLineSessionBase());
				Map<String, Object> paramPropertiesMap = TestPreparer.createParametersMapFromParamProperties(properties, name, true, parameterResolutionContext);
				Iterator<String> parameterNameIterator = paramPropertiesMap.keySet().iterator();
				while (parameterNameIterator.hasNext()) {
					String parameterName = (String)parameterNameIterator.next();
					Parameter parameter = (Parameter)paramPropertiesMap.get(parameterName);
					ibisJavaSender.addParameter(parameter);
				}
				try {
					ibisJavaSender.configure();
				} catch(ConfigurationException e) {
					messageListener.errorMessage(testName, "Could not configure '" + name + "': " + e.getMessage(), e);
					scenarioTester.closeQueues(queues, properties, testName);
					queues = null;
				}
				if (queues != null) {
					try {
						ibisJavaSender.open();
					} catch (SenderException e) {
						scenarioTester.closeQueues(queues, properties, testName);
						queues = null;
						messageListener.errorMessage(testName, "Could not open '" + name + "': " + e.getMessage(), e);
					}
					if (queues != null) {
						Map<String, Object> ibisJavaSenderInfo = new HashMap<String, Object>();
						ibisJavaSenderInfo.put("ibisJavaSender", ibisJavaSender);
						ibisJavaSenderInfo.put("parameterResolutionContext", parameterResolutionContext);
						ibisJavaSenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
						queues.put(name, ibisJavaSenderInfo);
						messageListener.debugMessage(testName, "Opened ibis java sender '" + name + "'");
					}
				}
			}
		}
	}

	/**
	 * Initializes the senders specified by delaySenders and adds it to the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param delaySenders List of delay senders to be initialized.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public void initDelaySender(Map<String, Map<String, Object>> queues, List<String> delaySenders, Properties properties, String testName) {

		messageListener.debugMessage(testName, "Initialize delay senders");
		Iterator<String> iterator = delaySenders.iterator();
		while (queues != null && iterator.hasNext()) {
			String name = (String)iterator.next();
			Boolean convertExceptionToMessage = new Boolean((String)properties.get(name + ".convertExceptionToMessage"));
			String delayTime = (String)properties.get(name + ".delayTime");
			DelaySender delaySender = new DelaySender();
			if (delayTime!=null) {
				delaySender.setDelayTime(Long.parseLong(delayTime));
			}
			delaySender.setName("Test Tool DelaySender");
			Map<String, Object> delaySenderInfo = new HashMap<String, Object>();
			delaySenderInfo.put("delaySender", delaySender);
			delaySenderInfo.put("convertExceptionToMessage", convertExceptionToMessage);
			queues.put(name, delaySenderInfo);
			messageListener.debugMessage(testName, "Opened delay sender '" + name + "'");
		}

	}

	/**
	 * Closes Ibis Web Senders that are still on the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public void closeIbisWebSender(Map<String, Map<String, Object>> queues, Properties properties, String testName) {
		messageListener.debugMessage(testName, "Close ibis webservice senders");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.http.IbisWebServiceSender".equals(properties.get(queueName + ".className"))) {
				IbisWebServiceSender ibisWebServiceSender = (IbisWebServiceSender)((Map<?, ?>)queues.get(queueName)).get("ibisWebServiceSender");
				Map<?, ?> ibisWebServiceSenderInfo = (Map<?, ?>)queues.get(queueName);
				SenderThread senderThread = (SenderThread)ibisWebServiceSenderInfo.remove("ibisWebServiceSenderThread");
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
					ibisWebServiceSender.close();
					messageListener.debugMessage(testName, "Closed ibis webservice sender '" + queueName + "'");
				} catch(SenderException e) {
					messageListener.errorMessage(testName, "Could not close '" + queueName + "': " + e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Closes Ibis Java Senders that are still on the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public void closeIbisJavaSender(Map<String, Map<String, Object>> queues, Properties properties, String testName) {
		messageListener.debugMessage(testName, "Close ibis java senders");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.senders.IbisJavaSender".equals(properties.get(queueName + ".className"))) {
				IbisJavaSender ibisJavaSender = (IbisJavaSender)((Map<?, ?>)queues.get(queueName)).get("ibisJavaSender");

				Map<?, ?> ibisJavaSenderInfo = (Map<?, ?>)queues.get(queueName);
				SenderThread ibisJavaSenderThread = (SenderThread)ibisJavaSenderInfo.remove("ibisJavaSenderThread");
				if (ibisJavaSenderThread != null) {
					messageListener.debugMessage(testName, "Found remaining SenderThread");
					SenderException senderException = ibisJavaSenderThread.getSenderException();
					if (senderException != null) {
						messageListener.errorMessage(testName, "Found remaining SenderException: " + senderException.getMessage(), senderException);
					}
					TimeOutException timeOutException = ibisJavaSenderThread.getTimeOutException();
					if (timeOutException != null) {
						messageListener.errorMessage(testName, "Found remaining TimeOutException: " + timeOutException.getMessage(), timeOutException);
					}
					String message = ibisJavaSenderThread.getResponse();
					if (message != null) {
						messageListener.wrongPipelineMessage(testName, "Found remaining message on '" + queueName + "'", message);
					}
				}

				try {
				ibisJavaSender.close();
					messageListener.debugMessage(testName, "Closed ibis java sender '" + queueName + "'");
				} catch(SenderException e) {
					messageListener.errorMessage(testName, "Could not close '" + queueName + "': " + e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Closes Delay Senders that are still on the queue.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param properties properties defined by scenario file and global app constants.
	 */
	public void closeDelaySender(Map<String, Map<String, Object>> queues, Properties properties, String testName) {
		messageListener.debugMessage(testName, "Close delay senders");
		Iterator iterator = queues.keySet().iterator();
		while (iterator.hasNext()) {
			String queueName = (String)iterator.next();
			if ("nl.nn.adapterframework.senders.DelaySender".equals(properties.get(queueName + ".className"))) {
				DelaySender delaySender = (DelaySender)((Map<?, ?>)queues.get(queueName)).get("delaySender");
				try {
					delaySender.close();
					messageListener.debugMessage(testName, "Closed delay sender '" + queueName + "'");
				} catch(SenderException e) {
					messageListener.errorMessage(testName, "Could not close delay sender '" + queueName + "': " + e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Gets response from a sender and then compares the response to the expected output.
	 * @param step string that contains the whole step.
	 * @param stepDisplayName string that contains the pipe's display name.
	 * @param properties properties defined by scenario file and global app constants.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param senderType type of sender that will be used for reading.
	 * @param fileName name of the file that contains the expected result.
	 * @param fileContent Content of the file that contains expected result.
	 * @return 1 if no problems, 0 if error has occurred, 2 if it has been autosaved.
	 */
	public int executeSenderRead(String step, String stepDisplayName, Properties properties, Map<String, Map<String, Object>> queues, String queueName, String senderType, String fileName, String fileContent, String originalFilePath, String testName) {
		int result = TestTool.RESULT_ERROR;
	
		Map<?, ?> senderInfo = (Map<?, ?>)queues.get(queueName);
		SenderThread senderThread = (SenderThread)senderInfo.remove(senderType + "SenderThread");
		if (senderThread == null) {
			messageListener.errorMessage(testName, "No SenderThread found, no " + senderType + "Sender.write request?");
		} else {
			SenderException senderException = senderThread.getSenderException();
			if (senderException == null) {
				TimeOutException timeOutException = senderThread.getTimeOutException();
				if (timeOutException == null) {
					String message = senderThread.getResponse();
					if (message == null) {
						if ("".equals(fileName)) {
							result = TestTool.RESULT_OK;
						} else {
							messageListener.errorMessage(testName, "Could not read " + senderType + "Sender message (null returned)");
						}
					} else {
						if ("".equals(fileName)) {
							messageListener.debugPipelineMessage(testName, stepDisplayName, "Unexpected message read from '" + queueName + "':", message);
						} else {
							result = resultComparer.compareResult(step, stepDisplayName, fileName, fileContent, message, properties, queueName, originalFilePath, testName);
						}
					}
				} else {
					messageListener.errorMessage(testName, "Could not read " + senderType + "Sender message (TimeOutException): " + timeOutException.getMessage(), timeOutException);
				}
			} else {
				messageListener.errorMessage(testName, "Could not read " + senderType + "Sender message (SenderException): " + senderException.getMessage(), senderException);
			}
		}
		
		return result;
	}

	/**
	 * Starts writing data to sender with given type on a new thread.
	 * @param stepDisplayName string that contains the pipe's display name.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param senderType type of sender that will be used for writing.
	 * @param fileContent string that contains the content to be written.
	 * @return 1 if started successfully, 1 if there has been an error.
	 */
	public int executeSenderWrite(String testName, String stepDisplayName, Map<String, Map<String, Object>> queues, String queueName, String senderType, String fileContent) {
		int result = TestTool.RESULT_ERROR;
		Map senderInfo = (Map)queues.get(queueName);
		if(senderInfo == null) {
			messageListener.debugMessage(testName, "Sender information is null.");
		}
		ISender sender = (ISender)senderInfo.get(senderType + "Sender");
		Boolean convertExceptionToMessage = (Boolean)senderInfo.get("convertExceptionToMessage");
		ParameterResolutionContext parameterResolutionContext = (ParameterResolutionContext)senderInfo.get("parameterResolutionContext");
		SenderThread senderThread;
		if (parameterResolutionContext == null) {
			senderThread = new SenderThread(sender, fileContent, convertExceptionToMessage.booleanValue());
		} else {
			senderThread = new SenderThread((ISenderWithParameters)sender, fileContent, parameterResolutionContext, convertExceptionToMessage.booleanValue());
		}
		senderThread.start();
		senderInfo.put(senderType + "SenderThread", senderThread);
		messageListener.debugPipelineMessage(testName, stepDisplayName, "Successfully started thread writing to '" + queueName + "':", fileContent);
		messageListener.debugMessage(testName, "Successfully started thread writing to '" + queueName + "'");
		result = TestTool.RESULT_OK;
		return result;
	}

	/**
	 * 
	 * @param stepDisplayName string that contains the pipe's display name.
	 * @param queues Queue of steps to execute as well as the variables required to execute.
	 * @param queueName name of the pipe to be used.
	 * @param fileContent string that contains the content to be written.
	 * @return 1 if everything was written successfully, 0 if there has been an error.
	 */
	public int executeDelaySenderWrite(String testName, String stepDisplayName, Map<String, Map<String, Object>> queues, String queueName, String fileContent) {
		int result = TestTool.RESULT_ERROR;
		Map<?, ?> delaySenderInfo = (Map<?, ?>)queues.get(queueName);
		DelaySender delaySender = (DelaySender)delaySenderInfo.get("delaySender");
		try {
			delaySender.sendMessage(null, fileContent);
			messageListener.debugPipelineMessage(testName, stepDisplayName, "Successfully written to '" + queueName + "':", fileContent);
			result = TestTool.RESULT_OK;
		} catch(Exception e) {
			messageListener.errorMessage(testName, "Exception writing to file: " + e.getMessage(), e);
		}
		return result;
	}

}
