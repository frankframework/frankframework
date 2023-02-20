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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.DebuggerStatusChangedEvent;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.LOG_CONFIGURATION)
public class UpdateLogSettings extends BusEndpointBase {
	private static final String LOG_INTERMEDIARY_RESULTS_PROPERTY = "log.logIntermediaryResults";
	private static final String TESTTOOL_ENABLED_PROPERTY = "testtool.enabled";

	@ActionSelector(BusAction.GET)
	public Message<String> getLogConfiguration(Message<?> message) {
		Map<String, Object> logSettings = new HashMap<>(3);
		LoggerContext logContext = LoggerContext.getContext(false);
		org.apache.logging.log4j.core.Logger rootLogger = logContext.getRootLogger();

		logSettings.put("maxMessageLength", IbisMaskingLayout.getMaxLength());

		List<String> errorLevels = new ArrayList<>(Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR"));
		logSettings.put("errorLevels", errorLevels);
		logSettings.put("loglevel", rootLogger.getLevel().toString());

		logSettings.put("logIntermediaryResults", AppConstants.getInstance().getBoolean(LOG_INTERMEDIARY_RESULTS_PROPERTY, true));

		logSettings.put("enableDebugger", AppConstants.getInstance().getBoolean(TESTTOOL_ENABLED_PROPERTY, true));

		return ResponseMessage.ok(logSettings);
	}

	@ActionSelector(BusAction.MANAGE)
	public void updateLogConfiguration(Message<?> message) {
		String loglevelStr = BusMessageUtils.getHeader(message, "logLevel", null);
		Level loglevel = Level.toLevel(loglevelStr, null);
		Boolean logIntermediaryResults = BusMessageUtils.getBooleanHeader(message, "logIntermediaryResults", null);
		Boolean enableDebugger = BusMessageUtils.getBooleanHeader(message, "enableDebugger", null);
		Integer maxMessageLength = BusMessageUtils.getIntHeader(message, "maxMessageLength", null);

		updateLogConfiguration(loglevel, logIntermediaryResults, maxMessageLength, enableDebugger);
	}

	private void updateLogConfiguration(Level loglevel, Boolean logIntermediaryResults, Integer maxMessageLength, Boolean enableDebugger) {
		StringBuilder msg = new StringBuilder();

		if(loglevel != null) {
			LoggerContext logContext = LoggerContext.getContext(false);
			org.apache.logging.log4j.core.Logger rootLogger = logContext.getRootLogger();
			if(rootLogger.getLevel() != loglevel) {
				msg.append("LogLevel changed from [" + rootLogger.getLevel() + "] to [" + loglevel +"]");
				Configurator.setLevel(rootLogger.getName(), loglevel);
			}
		}

		boolean logIntermediary = AppConstants.getInstance().getBoolean(LOG_INTERMEDIARY_RESULTS_PROPERTY, true);
		if(logIntermediaryResults != null && logIntermediary != logIntermediaryResults) {
			AppConstants.getInstance().put(LOG_INTERMEDIARY_RESULTS_PROPERTY, "" + logIntermediaryResults);

			if(msg.length() > 0)
				msg.append(", logIntermediaryResults from [" + logIntermediary+ "] to [" + logIntermediaryResults + "]");
			else
				msg.append("logIntermediaryResults changed from [" + logIntermediary+ "] to [" + logIntermediaryResults + "]");
		}

		if (maxMessageLength != null && maxMessageLength != IbisMaskingLayout.getMaxLength()) {
			if(msg.length() > 0)
				msg.append(", logMaxMessageLength from [" + IbisMaskingLayout.getMaxLength() + "] to [" + maxMessageLength + "]");
			else
				msg.append("logMaxMessageLength changed from [" + IbisMaskingLayout.getMaxLength() + "] to [" + maxMessageLength + "]");
			IbisMaskingLayout.setMaxLength(maxMessageLength);
		}

		if (enableDebugger != null) {
			boolean testtoolEnabled=AppConstants.getInstance().getBoolean(TESTTOOL_ENABLED_PROPERTY, true);
			if (testtoolEnabled!=enableDebugger) {
				AppConstants.getInstance().put(TESTTOOL_ENABLED_PROPERTY, "" + enableDebugger);
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
			log.warn(msg::toString);
			LogUtil.getLogger("SEC").info(msg::toString);
		}
	}
}
