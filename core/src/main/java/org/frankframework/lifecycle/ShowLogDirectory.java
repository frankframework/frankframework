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
package org.frankframework.lifecycle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.security.RolesAllowed;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.MessageDispatcher;
import org.frankframework.management.bus.endpoints.FileViewer;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.util.AppConstants;
import org.frankframework.util.FileUtils;
import org.frankframework.util.JsonDirectoryInfo;
import org.frankframework.util.SpringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.selector.MessageSelectorChain;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Logging should work even when the application failed to start which is why it's not wired through the {@link MessageDispatcher}.
 */
@IbisInitializer
public class ShowLogDirectory implements ApplicationContextAware {

	private final String defaultLogDirectory;
	private final String defaultLogWildcard = AppConstants.getInstance().getProperty("log.viewer.wildcard");
	private final boolean showDirectories = AppConstants.getInstance().getBoolean("log.viewer.showdirectories", true);
	private final int maxItems = AppConstants.getInstance().getInt("log.viewer.maxitems", 500);

	private @Setter @Autowired MessageChannel nullChannel;
	private @Setter ApplicationContext applicationContext;

	public ShowLogDirectory() {
		String logdir = AppConstants.getInstance().getProperty("log.dir");
		if(StringUtils.isEmpty(logdir)) {
			throw new IllegalStateException("unknown log directory, property [log.dir] has not been set!");
		}

		defaultLogDirectory = logdir.replace("\\\\", "\\");
	}

	/**
	 * This method is picked up by the IbisInitializer annotation and autowired via the SpringEnvironmentContext.
	 */
	@Bean
	public IntegrationFlow wireLogging() {
		return IntegrationFlow.from("frank-management-bus").handle(getHandler()).get();
	}

	public MessageHandler getHandler() {
		MessageSelectorChain selectors = new MessageSelectorChain();
		selectors.add(MessageDispatcher.headerSelector(BusTopic.LOGGING, BusTopic.TOPIC_HEADER_NAME));
		selectors.add(MessageDispatcher.headerSelector(BusAction.GET, BusAction.ACTION_HEADER_NAME));
		MessageFilter filter = new MessageFilter(selectors);
		filter.setDiscardChannel(nullChannel); // this prevents countless warnings in the log

		List<MessageHandler> handlers = new ArrayList<>();
		handlers.add(filter);

		MessageProcessor<?> mimp = new MethodInvokingMessageProcessor<>(this, "getLogDirectory");
		SpringUtils.autowireByType(applicationContext, mimp);
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(mimp);
		serviceActivator.setRequiresReply(true);

		handlers.add(serviceActivator);

		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setHandlers(handlers);
		chain.setComponentName("showLoggingComponent");
		return chain;
	}

	/**
	 * The actual action that is performed when calling the bus with the LOGGING topic.
	 */
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getLogDirectory(Message<?> message) {
		String directory = BusMessageUtils.getHeader(message, "directory", defaultLogDirectory);
		String wildcard = BusMessageUtils.getHeader(message, "wildcard", defaultLogWildcard);

		if(StringUtils.isNotEmpty(directory) && !FileUtils.readAllowed(FileViewer.permissionRules, directory, BusMessageUtils::hasAnyRole)) {
			throw new BusException("Access to path (" + directory + ") not allowed!", 403);
		}

		try {
			JsonDirectoryInfo info = new JsonDirectoryInfo(directory, wildcard, showDirectories, maxItems);
			return new JsonMessage(info.toJson());
		} catch (IOException e) {
			throw new BusException(e.getMessage(), 400);
		}
	}
}
