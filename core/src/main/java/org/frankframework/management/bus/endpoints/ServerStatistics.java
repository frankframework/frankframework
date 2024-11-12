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
package org.frankframework.management.bus.endpoints;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.ServletContext;
import org.apache.logging.log4j.Logger;
import org.frankframework.configuration.ApplicationWarnings;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.Adapter;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.ProcessState;
import org.frankframework.lifecycle.MessageEventListener;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.receivers.Receiver;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageKeeper;
import org.frankframework.util.Misc;
import org.frankframework.util.ProcessMetrics;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.web.context.WebApplicationContext;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.APPLICATION)
public class ServerStatistics extends BusEndpointBase {
	private static final Logger log = LogUtil.getLogger(Misc.class);

	private static final int MAX_MESSAGE_SIZE = AppConstants.getInstance().getInt("adapter.message.max.size", 0);
	private static final boolean showCountErrorStore = AppConstants.getInstance().getBoolean("errorStore.count.show", true);

	@ActionSelector(BusAction.GET)
	@PermitAll
	public Message<String> getServerInformation(Message<?> message) {
		Map<String, Object> returnMap = new HashMap<>();

		AppConstants appConstants = AppConstants.getInstance();
		Map<String, Object> framework = new HashMap<>(2);
		framework.put("name", "FF!");
		framework.put("version", appConstants.getProperty("application.version"));
		returnMap.put("framework", framework);

		Map<String, Object> instance = new HashMap<>(2);
		instance.put("version", appConstants.getProperty("instance.version"));
		instance.put("name", appConstants.getProperty("instance.name"));
		returnMap.put("instance", instance);

		String dtapStage = appConstants.getProperty("dtap.stage");
		returnMap.put("dtap.stage", dtapStage);
		String dtapSide = appConstants.getProperty("dtap.side");
		returnMap.put("dtap.side", dtapSide);

		String upn = BusMessageUtils.getUserPrincipalName();
		if(upn != null && !"anonymousUser".equals(upn)) {
			returnMap.put("userName", upn);
		}

		returnMap.put("applicationServer", getApplicationServer());
		returnMap.put("javaVersion", System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") + ")");
		Map<String, Object> fileSystem = new HashMap<>(2);
		fileSystem.put("totalSpace", getFileSystemTotalSpace());
		fileSystem.put("freeSpace", getFileSystemFreeSpace());
		returnMap.put("fileSystem", fileSystem);
		returnMap.put("processMetrics", ProcessMetrics.toMap());
		returnMap.put("machineName" , Misc.getHostname());
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		returnMap.put("serverTime", zonedDateTime.toInstant().toEpochMilli());
		returnMap.put("serverTimezone", zonedDateTime.getZone().getId());
		returnMap.put("serverTimeISO", zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		try {
			returnMap.put("uptime", getApplicationContext().getStartupDate());
		} catch (Exception e) {
			LogUtil.getLogger(this).info("unable to determine application uptime", e);
		}

		return new JsonMessage(returnMap);
	}

	static Long getFileSystemTotalSpace() {
		try {
			File systemDir = getSystemDir();
			return systemDir.getTotalSpace();
		} catch ( SecurityException e ) {
			log.debug("Caught Exception",e);
			return null;
		}
	}

	static Long getFileSystemFreeSpace() {
		try {
			File systemDir = getSystemDir();
			return systemDir.getFreeSpace();
		} catch ( SecurityException e ) {
			log.debug("Caught Exception",e);
			return null;
		}
	}

	private static File getSystemDir() {
		String dirName = AppConstants.getInstance().getProperty("log.dir");
		return new File(dirName);
	}

	@ActionSelector(BusAction.WARNINGS)
	@PermitAll
	public Message<String> getApplicationWarnings() {
		Map<String, Object> returnMap = new HashMap<>();
		MessageEventListener eventListener = getBean("MessageEventListener", MessageEventListener.class);

		long totalErrorStoreCount = 0;
		if(!showCountErrorStore)
			totalErrorStoreCount = -1;

		for (Configuration configuration : getIbisManager().getConfigurations()) {
			Map<String, Object> configurationsMap = new HashMap<>();

			//Configuration specific exceptions
			if (configuration.getConfigurationException()!=null) {
				String message = configuration.getConfigurationException().getMessage();
				configurationsMap.put("exception", message);
			}

			if (configuration.isActive()) {
				//ErrorStore count
				if (showCountErrorStore) {
					long esr = 0;
					for (Adapter adapter : configuration.getRegisteredAdapters()) {
						for (Receiver<?> receiver: adapter.getReceivers()) {
							IMessageBrowser<?> errorStorage = receiver.getMessageBrowser(ProcessState.ERROR);
							if (errorStorage != null) {
								try {
									esr += errorStorage.getMessageCount();
								} catch (Exception e) {
									//error("error occurred on getting number of errorlog records for adapter ["+adapter.getName()+"]",e);
									log.warn("Assuming there are no errorlog records for adapter [{}]", adapter.getName());
								}
							}
						}
					}
					totalErrorStoreCount += esr;
					configurationsMap.put("errorStoreCount", esr);
				}

				//Configuration specific warnings
				ConfigurationWarnings configWarns = configuration.getConfigurationWarnings();
				if(configWarns != null && configWarns.size() > 0) {
					configurationsMap.put("warnings", configWarns.getWarnings());
				}

				//Configuration specific messages
				MessageKeeper messageKeeper = eventListener.getMessageKeeper(configuration.getName());
				if(messageKeeper != null) {
					List<Object> messages = mapMessageKeeperMessages(messageKeeper);
					if(!messages.isEmpty()) {
						configurationsMap.put("messages", messages);
					}
				}
			}

			returnMap.put(configuration.getName(), configurationsMap);
		}

		//Total ErrorStore Count
		returnMap.put("totalErrorStoreCount", totalErrorStoreCount);

		//Global warnings
		ApplicationWarnings globalConfigWarnings = getBean("applicationWarnings", ApplicationWarnings.class);
		if (globalConfigWarnings.size()>0) {
			List<Object> warnings = new ArrayList<>();
			for (int j=0; j<globalConfigWarnings.size(); j++) {
				warnings.add(globalConfigWarnings.get(j));
			}
			returnMap.put("warnings", warnings);
		}

		//Global messages
		MessageKeeper messageKeeper = eventListener.getMessageKeeper();
		List<Object> messages = mapMessageKeeperMessages(messageKeeper);
		if(!messages.isEmpty()) {
			returnMap.put("messages", messages);
		}

		return new JsonMessage(returnMap);
	}

	private List<Object> mapMessageKeeperMessages(MessageKeeper messageKeeper) {
		List<Object> messages = new ArrayList<>();
		for (int t = 0; t < messageKeeper.size(); t++) {
			Map<String, Object> configurationMessage = new HashMap<>();
			String msg = messageKeeper.getMessage(t).getMessageText();
			if (MAX_MESSAGE_SIZE > 0 && msg.length() > MAX_MESSAGE_SIZE) {
				msg = msg.substring(0, MAX_MESSAGE_SIZE) + "...(" + (msg.length() - MAX_MESSAGE_SIZE) + " characters more)";
			}
			configurationMessage.put("message", msg);
			Instant date = messageKeeper.getMessage(t).getMessageDate();
			configurationMessage.put("date", DateFormatUtils.format(date, DateFormatUtils.FULL_GENERIC_FORMATTER));
			String level = messageKeeper.getMessage(t).getMessageLevel();
			configurationMessage.put("level", level);
			messages.add(configurationMessage);
		}
		return messages;
	}

	private String getApplicationServer() {
		return getApplicationServer(getApplicationContext());
	}

	private String getApplicationServer(ApplicationContext ac) {
		if(ac instanceof WebApplicationContext context) {
			ServletContext sc = context.getServletContext();
			if(sc != null) {
				return sc.getServerInfo();
			}
		}
		if(ac.getParent() != null) {
			return getApplicationServer(ac.getParent());
		}
		return "unknown";
	}
}
