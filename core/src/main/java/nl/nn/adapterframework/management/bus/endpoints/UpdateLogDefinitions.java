/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.management.bus.endpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.spi.StandardLevel;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.util.LogUtil;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.LOG_DEFINITIONS)
public class UpdateLogDefinitions {
	private Logger log = LogUtil.getLogger(this);

	private static final String FF_PACKAGE_PREFIX = "nl.nn.adapterframework";

	@ActionSelector(BusAction.GET)
	public Message<String> getLoggersAndDefinitions(Message<?> message) {
		String filter = BusMessageUtils.getHeader(message, "filter", null);
		LoggerContext logContext = LoggerContext.getContext(false);
		Map<String, Object> result = new HashMap<>();

		if(StringUtils.isEmpty(filter)) {
			result.put("definitions", getLogDefinitions(logContext));
			filter = FF_PACKAGE_PREFIX;
		}

		Map<String, StandardLevel> registeredLoggers = new TreeMap<>(); // A list with all Loggers that are logging to Log4j2
		for (Logger logger : logContext.getLoggers()) {
			String logName = logger.getName();
			String packageName = null;
			if(logName.contains(".")) {
				packageName = logName.substring(0, logName.lastIndexOf("."));
			} else {
				packageName = logName;
			}

			if(filter == null || packageName.startsWith(filter)) {
				StandardLevel newLevel = logger.getLevel().getStandardLevel();
				StandardLevel oldLevel = registeredLoggers.get(packageName);
				if(oldLevel != null && oldLevel.compareTo(newLevel) < 1) {
					continue;
				}
				registeredLoggers.put(packageName, newLevel);
			}
		}
		result.put("loggers", registeredLoggers);

		return ResponseMessage.ok(result);
	}

	public List<Object> getLogDefinitions(LoggerContext logContext) {
		List<Object> defaultLoggers = new ArrayList<>();
		Collection<LoggerConfig> loggerConfigs = logContext.getConfiguration().getLoggers().values();
		for(LoggerConfig config : loggerConfigs) {
			String name = config.getName();
			if(StringUtils.isNotEmpty(name) && name.contains(".") && !name.startsWith(LogUtil.MESSAGE_LOGGER+".")) {
				Map<String, Object> logger = new HashMap<>();
				logger.put("name", name);
				logger.put("level", config.getLevel().getStandardLevel());
				Set<String> appenders = config.getAppenders().keySet();
				if(!appenders.isEmpty()) {
					logger.put("appenders", config.getAppenders().keySet());
				}
				defaultLoggers.add(logger);
			}
		}
		return defaultLoggers;
	}

	@ActionSelector(BusAction.MANAGE)
	public Message<String> updateLogConfiguration(Message<?> message) {
		String loglevelStr = BusMessageUtils.getHeader(message, "level", null);
		Level level = Level.toLevel(loglevelStr, null);
		String logPackage = BusMessageUtils.getHeader(message, "logPackage", null);
		Boolean reconfigure = BusMessageUtils.getBooleanHeader(message, "reconfigure", null);

		if(reconfigure != null) {
			if(reconfigure) {
				LoggerContext logContext = LoggerContext.getContext(false);
				logContext.reconfigure();
				log2SecurityLog("reconfigured logdefinitions");
				return ResponseMessage.Builder.accepted();
			}

			return ResponseMessage.Builder.noContent();
		}

		if(StringUtils.isNotEmpty(logPackage) && level != null) {
			Configurator.setLevel(logPackage, level);
			log2SecurityLog("changed logdefinition ["+logPackage+"] to level ["+level.getStandardLevel().name()+"]");
			return ResponseMessage.Builder.accepted();
		}

		return ResponseMessage.Builder.badRequest();
	}

	private void log2SecurityLog(String logMessage) {
		log.warn(logMessage);
		LogUtil.getLogger("SEC").info(logMessage);
	}
}
