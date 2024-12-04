/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.ibistesttool;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.DebuggerStatusChangedEvent;
import org.frankframework.util.AppConstants;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * Used to determine whether the test tool is activated by using the {@literal testtool.enabled} appConstants property.
 * <br/>
 * Will throw a {@see DebuggerStatusChangedEvent} after the value changes.
 *
 * @see Debugger
 * @see IbisDebuggerAdvice
 */
public class DebuggerActive implements ApplicationEventPublisherAware, InitializingBean {
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();

	private ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void afterPropertiesSet() {
		// Contract for testtool state:
		// - when the state changes a DebuggerStatusChangedEvent must be fired to notify others
		// - to get notified of canges, components should listen to DebuggerStatusChangedEvents
		// IbisDebuggerAdvice stores state in appconstants testtool.enabled for use by GUI

		boolean testToolEnabled = true;
		String testToolEnabledProperty = APP_CONSTANTS.getProperty("testtool.enabled");

		if (StringUtils.isNotEmpty(testToolEnabledProperty)) {
			testToolEnabled = "true".equalsIgnoreCase(testToolEnabledProperty);
		} else {
			String stage = APP_CONSTANTS.getProperty("dtap.stage");
			if ("ACC".equals(stage) || "PRD".equals(stage)) {
				testToolEnabled = false;
			}
			APP_CONSTANTS.setProperty("testtool.enabled", testToolEnabled);
		}

		// notify other components of status of debugger
		DebuggerStatusChangedEvent event = new DebuggerStatusChangedEvent(this, testToolEnabled);
		if (applicationEventPublisher != null) {
			applicationEventPublisher.publishEvent(event);
		}
	}
}
