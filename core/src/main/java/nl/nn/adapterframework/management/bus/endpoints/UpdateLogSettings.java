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

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.springframework.messaging.Message;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.util.AppConstants;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.LOGGING)
public class UpdateLogSettings {
	private @Getter @Setter IbisManager ibisManager;

	@ActionSelector(BusAction.STATUS)
	public Message<String> getLogConfiguration(Message<?> message) {
		Map<String, Object> logSettings = new HashMap<>(3);
		LoggerContext logContext = LoggerContext.getContext(false);
		Logger rootLogger = logContext.getRootLogger();

		logSettings.put("maxMessageLength", IbisMaskingLayout.getMaxLength());

		List<String> errorLevels = new ArrayList<>(Arrays.asList("DEBUG", "INFO", "WARN", "ERROR"));
		logSettings.put("errorLevels", errorLevels);
		logSettings.put("loglevel", rootLogger.getLevel().toString());

		logSettings.put("logIntermediaryResults", AppConstants.getInstance().getBoolean("log.logIntermediaryResults", true));

		logSettings.put("enableDebugger", AppConstants.getInstance().getBoolean("testtool.enabled", true));

		return ResponseMessage.ok(logSettings);
	}
}
