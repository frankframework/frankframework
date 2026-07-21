/*
   Copyright 2024-2026 WeAreFrank!

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
package org.frankframework.ladybug;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;

import lombok.extern.log4j.Log4j2;

import org.frankframework.management.bus.DebuggerStatusChangedEvent;
import org.frankframework.util.AppConstants;

/**
 * Used to determine whether the test tool is activated by using the {@literal testtool.enabled} appConstants property.
 * <br/>
 * Will throw a {@see DebuggerStatusChangedEvent} after the value changes.
 *
 * @see LadybugDebugger
 * @see IbisDebuggerAdvice
 */
@Log4j2
public class DebuggerActive implements ApplicationEventPublisherAware, ApplicationListener<DebuggerStatusChangedEvent>, InitializingBean, BooleanSupplier {
	private static final AtomicBoolean IS_LADYBUG_ACTIVE = new AtomicBoolean(false);
	private static final String TESTTOOL_ENABLED_PROPERTY = "testtool.enabled";

	private ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void afterPropertiesSet() {
		// Contract for testtool state:
		// - when the state changes a DebuggerStatusChangedEvent must be fired to notify others
		// - to get notified of changes, components should listen to DebuggerStatusChangedEvents

		boolean testToolEnabled = inferTestToolState();

		// update the state
		setState(testToolEnabled);

		// notify other components of status of debugger
		DebuggerStatusChangedEvent event = new DebuggerStatusChangedEvent(this, testToolEnabled);
		if (applicationEventPublisher != null) {
			log.debug("sending DebuggerStatusChangedEvent [{}]", event);
			applicationEventPublisher.publishEvent(event);
		}
	}

	private boolean inferTestToolState() {
		final AppConstants appConstants = AppConstants.getInstance();
		String testToolEnabledProperty = appConstants.getProperty(TESTTOOL_ENABLED_PROPERTY);

		if (StringUtils.isNotEmpty(testToolEnabledProperty)) {
			return "true".equalsIgnoreCase(testToolEnabledProperty) || "!false".equalsIgnoreCase(testToolEnabledProperty);
		}

		// Property has not been set, so determine it based on the dtap.stage.
		String stage = appConstants.getProperty("dtap.stage");
		return !("ACC".equals(stage) || "PRD".equals(stage));
	}

	// Store the state in AppConstants testtool.enabled for use by GUI/Larva
	private void setState(boolean testToolEnabled) {
		// Always store the new updated value in the AppConstants
		AppConstants.setGlobalProperty(TESTTOOL_ENABLED_PROPERTY, testToolEnabled);

		IS_LADYBUG_ACTIVE.set(testToolEnabled);
	}

	@Override
	public void onApplicationEvent(@NonNull DebuggerStatusChangedEvent event) {
		if (event.getSource() != this) {
			log.debug("received DebuggerStatusChangedEvent [{}]", event);
			setState(event.isEnabled());
		}
	}

	@Override
	public boolean getAsBoolean() {
		return IS_LADYBUG_ACTIVE.get();
	}
}
