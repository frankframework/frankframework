/*
Copyright 2016-2022 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.spi.StandardLevel;
import org.springframework.context.ApplicationEventPublisher;

import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Read and update logging configuration
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class UpdateLoggingConfig extends Base {

	private static final String FF_PACKAGE_PREFIX = "nl.nn.adapterframework";

	@GET
	@PermitAll
	@Path("/server/logging")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLogConfiguration() throws ApiException {

		Map<String, Object> logSettings = new HashMap<>(5);
		LoggerContext logContext = LoggerContext.getContext(false);
		Logger rootLogger = logContext.getRootLogger();

		logSettings.put("maxMessageLength", IbisMaskingLayout.getMaxLength());

		logSettings.put("errorLevels", Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR"));
		logSettings.put("loglevel", rootLogger.getLevel().toString());

		logSettings.put("logIntermediaryResults", AppConstants.getInstance().getBoolean("log.logIntermediaryResults", true));

		logSettings.put("enableDebugger", AppConstants.getInstance().getBoolean("testtool.enabled", true));

		return Response.status(Response.Status.CREATED).entity(logSettings).build();
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/server/logging")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateLogConfiguration(LinkedHashMap<String, Object> json) {

		Boolean logIntermediaryResults = null;
		int maxMessageLength = IbisMaskingLayout.getMaxLength();
		Boolean enableDebugger = null;
		StringBuilder msg = new StringBuilder();

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("loglevel")) {
				Level loglevel = Level.toLevel(""+value, null);
				LoggerContext logContext = LoggerContext.getContext(false);
				Logger rootLogger = logContext.getRootLogger();
				if(loglevel != null) {
					String changmsg = "LogLevel changed from [" + rootLogger.getLevel() + "] to [" + loglevel +"]";
					Configurator.setLevel(rootLogger.getName(), loglevel);
					msg.append(changmsg);
				}
			}
			else if(key.equalsIgnoreCase("logIntermediaryResults")) {
				logIntermediaryResults = Boolean.parseBoolean(""+value);
			}
			else if(key.equalsIgnoreCase("maxMessageLength")) {
				maxMessageLength = Integer.parseInt(""+value);
			}
			else if(key.equalsIgnoreCase("enableDebugger")) {
				enableDebugger = Boolean.parseBoolean(""+value);
			}
		}

		if(logIntermediaryResults != null) {
			boolean logIntermediary = AppConstants.getInstance().getBoolean("log.logIntermediaryResults", true);
			if(logIntermediary != logIntermediaryResults) {
				AppConstants.getInstance().put("log.logIntermediaryResults", "" + logIntermediaryResults);

				if(msg.length() > 0)
					msg.append(", logIntermediaryResults from [" + logIntermediary+ "] to [" + logIntermediaryResults + "]");
				else
					msg.append("logIntermediaryResults changed from [" + logIntermediary+ "] to [" + logIntermediaryResults + "]");
			}
		}

		if (maxMessageLength != IbisMaskingLayout.getMaxLength()) {
			if(msg.length() > 0)
				msg.append(", logMaxMessageLength from [" + IbisMaskingLayout.getMaxLength() + "] to [" + maxMessageLength + "]");
			else
				msg.append("logMaxMessageLength changed from [" + IbisMaskingLayout.getMaxLength() + "] to [" + maxMessageLength + "]");
			IbisMaskingLayout.setMaxLength(maxMessageLength);
		}

		if (enableDebugger != null) {
			boolean testtoolEnabled=AppConstants.getInstance().getBoolean("testtool.enabled", true);
			if (testtoolEnabled!=enableDebugger) {
				AppConstants.getInstance().put("testtool.enabled", "" + enableDebugger);
				DebuggerStatusChangedEvent event = new DebuggerStatusChangedEvent(this, enableDebugger);
				ApplicationEventPublisher applicationEventPublisher = getIbisManager().getApplicationEventPublisher();
				if (applicationEventPublisher!=null) {
					log.info("setting debugger enabled ["+enableDebugger+"]");
					if(msg.length() > 0)
						msg.append(", enableDebugger from [" + testtoolEnabled + "] to [" + enableDebugger + "]");
					else
						msg.append("enableDebugger changed from [" + testtoolEnabled + "] to [" + enableDebugger + "]");
					applicationEventPublisher.publishEvent(event);
				} else {
					log.warn("no applicationEventPublisher, cannot set debugger enabled to ["+enableDebugger+"]");
				}
			}
		}

		if(msg.length() > 0) {
			log.warn(msg.toString());
			LogUtil.getLogger("SEC").info(msg.toString());
		}

		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/server/logging/settings")
	@Relation("logging")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLogSettings(@QueryParam("filter") String filter) {

		LoggerContext logContext = LoggerContext.getContext(false);
		Map<String, Object> result = new HashMap<>();

		if(StringUtils.isEmpty(filter)) {
			List<Map<String, Object>> defaultLoggers = new ArrayList<>();
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
			Collections.sort(defaultLoggers, (a,b) -> a.get("name").toString().compareTo(b.get("name").toString()));
			result.put("definitions", defaultLoggers);

			filter = FF_PACKAGE_PREFIX;
		}

		Map<String, StandardLevel> registeredLoggers = new TreeMap<>(); // A list with all Loggers that are logging to Log4j2
		for (Logger log : logContext.getLoggers()) {
			String logName = log.getName();
			String packageName = null;
			if(logName.contains(".")) {
				packageName = logName.substring(0, logName.lastIndexOf("."));
			} else {
				packageName = logName;
			}

			if(filter == null || packageName.startsWith(filter)) {
				StandardLevel newLevel = log.getLevel().getStandardLevel();
				StandardLevel oldLevel = registeredLoggers.get(packageName);
				if(oldLevel != null && oldLevel.compareTo(newLevel) < 1) {
					continue;
				}
				registeredLoggers.put(packageName, newLevel);
			}
		}
		result.put("loggers", registeredLoggers);

		return Response.status(Response.Status.OK).entity(result).build();
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/server/logging/settings")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateLogger(LinkedHashMap<String, Object> json) {
		Level level = null;
		String logPackage = null;
		boolean reconfigure = false;

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("level")) {
				level = Level.toLevel(""+value, null);
			} else if(key.equalsIgnoreCase("logger")) {
				logPackage = (String) value;
			} else if(key.equalsIgnoreCase("reconfigure")) {
				reconfigure = Boolean.parseBoolean(""+value);
			}
		}

		if(reconfigure) {
			LoggerContext logContext = LoggerContext.getContext(false);
			logContext.reconfigure();
			log2SecurityLog("reconfigured logdefinitions");

			return Response.status(Response.Status.CREATED).build();
		}

		if(StringUtils.isNotEmpty(logPackage) && level != null) {
			Configurator.setLevel(logPackage, level);
			log2SecurityLog("changed logdefinition ["+logPackage+"] to level ["+level.getStandardLevel().name()+"]");
			return Response.status(Response.Status.ACCEPTED).build();
		}

		return Response.status(Response.Status.BAD_REQUEST).build();
	}

	private void log2SecurityLog(String logMessage) {
		log.warn(logMessage);
		LogUtil.getLogger("SEC").info(logMessage);
	}
}
