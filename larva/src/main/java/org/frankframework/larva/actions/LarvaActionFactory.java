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
package org.frankframework.larva.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;

import lombok.extern.log4j.Log4j2;

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
import org.frankframework.larva.Scenario;
import org.frankframework.larva.SenderThread;
import org.frankframework.larva.output.TestExecutionObserver;
import org.frankframework.stream.Message;
import org.frankframework.util.SpringUtils;

/**
 * This class is used to create and manage the lifecycle of Larva actions.
 */
@Log4j2
public class LarvaActionFactory {

	public static final String CLASS_NAME_PROPERTY_SUFFIX = ".className";
	private final int defaultTimeout;
	private final LarvaTool larvaTool;
	private final TestExecutionObserver testExecutionObserver;

	public LarvaActionFactory(LarvaTool larvaTool, TestExecutionObserver testExecutionObserver) {
		this.larvaTool = larvaTool;
		this.testExecutionObserver = testExecutionObserver;
		this.defaultTimeout = larvaTool.getLarvaConfig().getTimeout();
	}

	public Map<String, LarvaScenarioAction> createLarvaActions(Scenario scenario, ApplicationContext applicationContext, String correlationId) {
		Map<String, LarvaScenarioAction> larvaActions = new HashMap<>();
		debugMessage("Get all action names");

		try {
			Properties properties = scenario.getProperties();
			Set<String> actionNames = properties.keySet()
					.stream()
					.map(String.class::cast)
					.filter(key -> key.endsWith(CLASS_NAME_PROPERTY_SUFFIX))
					.map(key -> key.substring(0, key.lastIndexOf(".")))
					.collect(Collectors.toSet());

			for (String actionName : actionNames) {
				debugMessage("actionname openaction: " + actionName);
				String className = properties.getProperty(actionName + CLASS_NAME_PROPERTY_SUFFIX);
				if ("org.frankframework.jms.JmsListener".equals(className)) {
					className = "org.frankframework.jms.PullingJmsListener";
				}

				IConfigurable configurable = (IConfigurable) SpringUtils.createBean(applicationContext, className);
				log.debug("created FrankElement [{}]", configurable);

				Properties actionProperties = handleDeprecations(scenario, LarvaActionUtils.getSubProperties(properties, actionName), actionName);
				LarvaScenarioAction larvaScenarioAction = create(configurable, actionProperties, defaultTimeout, correlationId);
				SpringUtils.registerSingleton(applicationContext, actionName, larvaScenarioAction);
				larvaActions.put(actionName, larvaScenarioAction);
				debugMessage("Opened [" + className + "] '" + actionName + "'");
			}

		} catch (Exception e) {
			log.warn("Error occurred while creating Larva Scenario Actions", e);
			closeLarvaActions(larvaActions);
			larvaActions = null;
			errorMessage(e.getClass().getSimpleName() + ": "+e.getMessage(), e);
		}

		return larvaActions;
	}

	private static LarvaScenarioAction create(IConfigurable configurable, Properties actionProperties, int defaultTimeout, String correlationId) {
		final AbstractLarvaAction<?> larvaAction;
		if (configurable instanceof IPullingListener<?> pullingListener) {
			larvaAction = new PullingListenerAction(pullingListener);
		} else if (configurable instanceof IPushingListener<?> pushingListener) {
			larvaAction = new LarvaPushingListenerAction(pushingListener, defaultTimeout);
		} else if (configurable instanceof ISender sender) {
			larvaAction = new SenderAction(sender);
		} else {
			larvaAction = new LarvaAction(configurable);
		}

		larvaAction.invokeSetters(actionProperties);
		larvaAction.getSession().put(PipeLineSession.CORRELATION_ID_KEY, correlationId);

		return larvaAction;
	}

	private Properties handleDeprecations(Scenario scenario, Properties actionProperties, String keyBase) {
		if (actionProperties.containsKey("requestTimeOut") || actionProperties.containsKey("responseTimeOut")) {
			scenario.addWarning("Deprecation Warning: properties " + keyBase + ".requestTimeOut/" + keyBase + ".responseTimeOut have been replaced with " + keyBase + ".timeout");
		}
		if (actionProperties.containsKey("getBlobSmart")) {
			scenario.addWarning("Deprecation Warning: property " + keyBase + ".getBlobSmart has been replaced with " + keyBase + ".blobSmartGet");
			String blobSmart = ""+actionProperties.remove("getBlobSmart");
			actionProperties.setProperty("blobSmartGet", blobSmart);
		}
		if (actionProperties.containsKey("preDel1")) {
			scenario.addWarning("Removal Warning: property " + keyBase + ".preDel<index> has been removed without replacement");
		}
		if (actionProperties.containsKey("prePostQuery")) {
			scenario.addWarning("Removal Warning: property " + keyBase + ".prePostQuery has been removed without replacement");
		}

		return actionProperties;
	}

	public boolean closeLarvaActions(Map<String, LarvaScenarioAction> larvaActions) {
		boolean remainingMessagesFound = false;

		debugMessage("Close autoclosables");
		for (Map.Entry<String, LarvaScenarioAction> entry : larvaActions.entrySet()) {
			String actionName = entry.getKey();
			LarvaScenarioAction larvaScenarioAction = entry.getValue();
			if (larvaScenarioAction instanceof SenderAction senderAction && senderAction.getSenderThread() != null) {
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
					wrongPipelineMessage("Found remaining message on '" + actionName + "'", message);
				}
			}
			if (larvaScenarioAction instanceof LarvaPushingListenerAction listenerAction && listenerAction.getMessageHandler() != null) {
				ListenerMessageHandler<?> listenerMessageHandler = listenerAction.getMessageHandler();
				ListenerMessage listenerMessage = listenerMessageHandler.getRequestMessageOrNull();
				while (listenerMessage != null) {
					Message message = listenerMessage.getMessage();
					if (listenerMessage.getContext() != null) {
						listenerMessage.getContext().close();
					}
					wrongPipelineMessage("Found remaining request message on '" + actionName + "'", message);
					remainingMessagesFound = true;
					listenerMessage = listenerMessageHandler.getRequestMessageOrNull();
				}
				listenerMessage = listenerMessageHandler.getResponseMessageOrNull();
				while (listenerMessage != null) {
					Message message = listenerMessage.getMessage();
					if (listenerMessage.getContext() != null) {
						listenerMessage.getContext().close();
					}
					wrongPipelineMessage("Found remaining response message on '" + actionName + "'", message);
					remainingMessagesFound = true;
					listenerMessage = listenerMessageHandler.getResponseMessageOrNull();
				}
			}

			try {
				larvaScenarioAction.close();
				debugMessage("Closed action '" + actionName + "'");
			} catch(Exception e) {
				log.error("could not close '" + actionName + "'", e);
				errorMessage("Could not close '" + actionName + "': " + e.getMessage(), e);
			}
		}

		return remainingMessagesFound;
	}

	private void wrongPipelineMessage(String message, Message pipelineMessage) {
		String messageAsString = larvaTool.messageToString(pipelineMessage);
		testExecutionObserver.messageError(message, messageAsString != null ? messageAsString : "Unreadable message: [" + pipelineMessage + "]");
	}

	private void debugMessage(String message) {
		larvaTool.debugMessage(message);
	}

	private void errorMessage(String message, Exception e) {
		larvaTool.errorMessage(message, e);
	}
}
