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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.spi.StandardLevel;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.EmptyMessage;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.util.LogUtil;

@Log4j2
@BusAware("frank-management-bus")
@TopicSelector(BusTopic.LOG_DEFINITIONS)
public class UpdateLogDefinitions {
	private static final String FF_PACKAGE_PREFIX = "org.frankframework";

	@ActionSelector(BusAction.GET)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
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
			String packageName;
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

		return new JsonMessage(result);
	}

	public List<LogDefinitionDAO> getLogDefinitions(LoggerContext logContext) {
		List<LogDefinitionDAO> defaultLoggers = new ArrayList<>();
		Collection<LoggerConfig> loggerConfigs = logContext.getConfiguration().getLoggers().values();
		for(LoggerConfig config : loggerConfigs) {
			String name = config.getName();
			if(StringUtils.isNotEmpty(name) && name.contains(".") && !name.startsWith(LogUtil.MESSAGE_LOGGER+".")) {
				LogDefinitionDAO def = new LogDefinitionDAO(name, config.getLevel());
				Set<String> appenders = config.getAppenders().keySet();
				if(!appenders.isEmpty()) {
					def.setAppenders(config.getAppenders().keySet());
				}
				defaultLoggers.add(def);
			}
		}

		defaultLoggers.sort(Comparator.comparing(LogDefinitionDAO::getName));
		return defaultLoggers;
	}

	public static class LogDefinitionDAO {
		private final @Getter String name;
		private final @Getter String level;
		private @Getter @Setter @JsonInclude(Include.NON_NULL) Set<String> appenders;

		public LogDefinitionDAO(String name, Level level) {
			this.name = name;
			this.level = level.getStandardLevel().name();
		}
	}

	@SuppressWarnings("java:S4792") // Changing the logger level is not a security-sensitive operation, because roles are checked
	@ActionSelector(BusAction.MANAGE)
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> updateLogConfiguration(Message<?> message) {
		String logLevelStr = BusMessageUtils.getHeader(message, "level", null);
		Level level = Level.toLevel(logLevelStr, null);
		String logPackage = BusMessageUtils.getHeader(message, "logPackage", null);
		Boolean reconfigure = BusMessageUtils.getBooleanHeader(message, "reconfigure", null);

		if(reconfigure != null) {
			if(reconfigure) {
				LoggerContext logContext = LoggerContext.getContext(false);
				logContext.reconfigure();
				log2SecurityLog("reconfigured logdefinitions");
				return EmptyMessage.accepted();
			}

			return EmptyMessage.noContent();
		}

		if(StringUtils.isNotEmpty(logPackage) && level != null) {
			Configurator.setLevel(logPackage, level);
			log2SecurityLog("changed logdefinition ["+logPackage+"] to level ["+level.getStandardLevel().name()+"]");
			return EmptyMessage.accepted();
		}
		throw new BusException("neither [reconfigure], [logPackage] or [level] provided");
	}

	@ActionSelector(BusAction.UPLOAD)
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<?> createLogConfiguration(Message<?> message) {
		String logLevelStr = BusMessageUtils.getHeader(message, "level", null);
		Level level = Level.toLevel(logLevelStr, null);
		String logPackage = BusMessageUtils.getHeader(message, "logPackage", null);

		if(StringUtils.isNotEmpty(logPackage) && level != null) {
			LoggerContext logContext = LoggerContext.getContext(false);
			Configuration logConfiguration = logContext.getConfiguration();

			logConfiguration.addLogger(logPackage, new LoggerConfig(logPackage, level, false));
			return EmptyMessage.accepted();
		}
		throw new BusException("neither [logPackage] or [level] provided");
	}

	private void log2SecurityLog(String logMessage) {
		log.warn(logMessage);
		LogUtil.getLogger("SEC").info(logMessage);
	}
}
