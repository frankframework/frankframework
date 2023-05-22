/*
   Copyright 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.lifecycle;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.JsonResponseMessage;
import nl.nn.adapterframework.management.bus.MessageDispatcher;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Dir2Map;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.webcontrol.FileViewerServlet;

/**
 * Logging should work even when the application failed to start which is why it's not wired through the {@link MessageDispatcher}.
 */
@IbisInitializer
public class ShowLogDirectory {

	private String defaultLogDirectory = AppConstants.getInstance().getResolvedProperty("logging.path").replace("\\\\", "\\");
	private String defaultLogWildcard = AppConstants.getInstance().getProperty("logging.wildcard");
	private boolean showDirectories = AppConstants.getInstance().getBoolean("logging.showdirectories", false);
	private int maxItems = AppConstants.getInstance().getInt("logging.items.max", 500);

	/**
	 * This method is picked op by the IbisInitializer annotation and autowired via the SpringEnvironmentContext.
	 */
	@Bean
	public IntegrationFlow wireLogging() {
		return IntegrationFlows.from("frank-management-bus")
				.filter(MessageDispatcher.headerSelector(BusTopic.LOGGING, BusTopic.TOPIC_HEADER_NAME))
				.filter(MessageDispatcher.headerSelector(BusAction.GET, BusAction.ACTION_HEADER_NAME))
				.handle(getHandler()).get();
	}

	public MessageHandler getHandler() {
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(this, "getLogDirectory");
		serviceActivator.setRequiresReply(true);
		return serviceActivator;
	}

	/**
	 * The actual action that is performed when calling the bus with the LOGGING topic.
	 */
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getLogDirectory(Message<?> message) {
		String directory = BusMessageUtils.getHeader(message, "directory", defaultLogDirectory);
		boolean sizeFormat = BusMessageUtils.getBooleanHeader(message, "sizeFormat", true);
		String wildcard = BusMessageUtils.getHeader(message, "wildcard", defaultLogWildcard);

		if(StringUtils.isNotEmpty(directory) && !FileUtils.readAllowed(FileViewerServlet.permissionRules, directory, BusMessageUtils::hasAnyRole)) {
			throw new BusException("Access to path (" + directory + ") not allowed!");
		}

		Map<String, Object> returnMap = new HashMap<>();
		Dir2Map dir = new Dir2Map(directory, sizeFormat, wildcard, showDirectories, maxItems);

		returnMap.put("list", dir.getList());
		returnMap.put("count", dir.size());
		returnMap.put("directory", dir.getDirectory());
		returnMap.put("sizeFormat", sizeFormat);
		returnMap.put("wildcard", wildcard);

		return new JsonResponseMessage(returnMap);
	}
}
