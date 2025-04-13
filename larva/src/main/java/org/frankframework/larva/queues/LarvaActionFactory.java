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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.IbisContext;
import org.frankframework.configuration.classloaders.DirectoryClassLoader;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.IPullingListener;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.ListenerMessage;
import org.frankframework.larva.ListenerMessageHandler;
import org.frankframework.larva.SenderThread;
import org.frankframework.senders.FrankSender;
import org.frankframework.stream.Message;

/**
 * This class is used to create and manage the lifecycle of Larva queues.
 */
@Log4j2
public class LarvaActionFactory {

	public static final String CLASS_NAME_PROPERTY_SUFFIX = ".className";
	private final int defaultTimeout;
	private final LarvaTool testTool;

	public LarvaActionFactory(LarvaTool testTool) {
		this.testTool = testTool;
		this.defaultTimeout = testTool.getConfig().getTimeout();
	}

	public Map<String, LarvaScenarioAction> createLarvaActions(String scenarioDirectory, Properties properties, IbisContext ibisContext, String correlationId) {
		Map<String, LarvaScenarioAction> queues = new HashMap<>();
		debugMessage("Get all queue names");

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

				IConfigurable configurable = LarvaActionUtils.createInstance(ibisContext, directoryClassLoader, className);
				log.debug("created FrankElement [{}]", configurable);
				if (configurable instanceof FrankSender frankSender) {
					frankSender.setIbisManager(ibisContext.getIbisManager());
				}

				Properties queueProperties = handleDeprecations(LarvaActionUtils.getSubProperties(properties, queueName), queueName);
				LarvaScenarioAction queue = create(configurable, queueProperties, defaultTimeout, correlationId);
				queues.put(queueName, queue);
				debugMessage("Opened [" + className + "] '" + queueName + "'");
			}

		} catch (Exception e) {
			log.warn("Error occurred while creating queues", e);
			closeLarvaActions(queues);
			queues = null;
			errorMessage(e.getClass().getSimpleName() + ": "+e.getMessage(), e);
		}

		return queues;
	}

	private static LarvaScenarioAction create(IConfigurable configurable, Properties queueProperties, int defaultTimeout, String correlationId) throws ConfigurationException {
		final AbstractLarvaAction<?> queue;
		if (configurable instanceof IPullingListener pullingListener) {
			queue = new PullingListenerAction(pullingListener);
		} else if (configurable instanceof IPushingListener pushingListener) {
			queue = new LarvaPushingListenerAction(pushingListener);
		} else if (configurable instanceof ISender sender) {
			queue = new SenderAction(sender);
		} else {
			queue = new LarvaAction(configurable);
		}

		queue.invokeSetters(defaultTimeout, queueProperties);
		queue.getSession().put(PipeLineSession.CORRELATION_ID_KEY, correlationId);

		queue.configure();
		queue.start();

		return queue;
	}

	private Properties handleDeprecations(Properties queueProperties, String keyBase) {
		if (queueProperties.containsKey("requestTimeOut") || queueProperties.containsKey("responseTimeOut")) {
			warningMessage("Deprecation Warning: properties " + keyBase + ".requestTimeOut/" + keyBase + ".responseTimeOut have been replaced with " + keyBase + ".timeout");
		}
		if (queueProperties.containsKey("getBlobSmart")) {
			warningMessage("Deprecation Warning: property " + keyBase + ".getBlobSmart has been replaced with " + keyBase + ".blobSmartGet");
			String blobSmart = ""+queueProperties.remove("getBlobSmart");
			queueProperties.setProperty("blobSmartGet", blobSmart);
		}
		if (queueProperties.containsKey("preDel1")) {
			warningMessage("Removal Warning: property " + keyBase + ".preDel<index> has been removed without replacement");
		}
		if (queueProperties.containsKey("prePostQuery")) {
			warningMessage("Removal Warning: property " + keyBase + ".prePostQuery has been removed without replacement");
		}

		return queueProperties;
	}

	public boolean closeLarvaActions(Map<String, LarvaScenarioAction> queues) {
		boolean remainingMessagesFound = false;

		debugMessage("Close autoclosables");
		for (Map.Entry<String, LarvaScenarioAction> entry : queues.entrySet()) {
			String queueName = entry.getKey();
			LarvaScenarioAction queue = entry.getValue();
			if (queue instanceof SenderAction senderAction && senderAction.getSenderThread() != null) {
				SenderThread senderThread = senderAction.getSenderThread();
				debugMessage("Found remaining SenderThread");
				SenderException senderException = senderThread.getSenderException();
				if (senderException != null) {
					errorMessage("Found remaining SenderException: " + senderException.getMessage(), senderException);
				}
				TimeoutException timeoutException = senderThread.getTimeoutException();
				if (timeoutException != null) {
					errorMessage("Found remaining TimeOutException: " + timeoutException.getMessage(), timeoutException);
				}
				Message message = senderThread.getResponse();
				if (message != null) {
					wrongPipelineMessage("Found remaining message on '" + queueName + "'", message);
				}
			}
			if (queue instanceof LarvaPushingListenerAction listenerAction && listenerAction.getMessageHandler() != null) {
				ListenerMessageHandler<?> listenerMessageHandler = listenerAction.getMessageHandler();
				ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessage();
				while (listenerMessage != null) {
					Message message = listenerMessage.getMessage();
					if (listenerMessage.getContext() != null) {
						listenerMessage.getContext().close();
					}
					wrongPipelineMessage("Found remaining request message on '" + queueName + "'", message);
					remainingMessagesFound = true;
					listenerMessage = listenerMessageHandler.getRequestMessage();
				}
				listenerMessage = listenerMessageHandler.getResponseMessage();
				while (listenerMessage != null) {
					Message message = listenerMessage.getMessage();
					if (listenerMessage.getContext() != null) {
						listenerMessage.getContext().close();
					}
					wrongPipelineMessage("Found remaining response message on '" + queueName + "'", message);
					remainingMessagesFound = true;
					listenerMessage = listenerMessageHandler.getResponseMessage();
				}
			}

			try {
				queue.close();
				debugMessage("Closed queue '" + queueName + "'");
			} catch(Exception e) {
				log.error("could not close '" + queueName + "'", e);
				errorMessage("Could not close '" + queueName + "': " + e.getMessage(), e);
			}
		}

		return remainingMessagesFound;
	}

	private void wrongPipelineMessage(String message, Message pipelineMessage) {
		testTool.wrongPipelineMessage(message, pipelineMessage);
	}

	private void debugMessage(String message) {
		testTool.debugMessage(message);
	}

	private void warningMessage(String message) {
		testTool.warningMessage(message);
	}

	private void errorMessage(String message, Exception e) {
		testTool.errorMessage(message, e);
	}
}
