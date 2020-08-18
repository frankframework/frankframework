/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.ibistesttool;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.webcontrol.api.DebuggerStatusChangedEvent;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.filter.Views;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.helpers.OptionConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * @author Jaco de Groot
 */
public class DeploymentSpecificsBeanPostProcessor implements BeanPostProcessor, ApplicationEventPublisherAware {
	private AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private ApplicationEventPublisher applicationEventPublisher;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof TestTool) {
			
			// Contract for testtool state:
			// - when the state changes a DebuggerStatusChangedEvent must be fired to notify others
			// - to get notified of canges, components should listen to DebuggerStatusChangedEvents
			// IbisDebuggerAdvice stores state in appconstants testtool.enabled for use by GUI
			
			boolean testToolEnabled=true;
			String testToolEnabledProperty=APP_CONSTANTS.getProperty("testtool.enabled");
			if (StringUtils.isNotEmpty(testToolEnabledProperty)) {
				testToolEnabled="true".equalsIgnoreCase(testToolEnabledProperty);
			} else {
				String stage = APP_CONSTANTS.getProperty("dtap.stage");
				if ("ACC".equals(stage) || "PRD".equals(stage)) {
					testToolEnabled=false;
				}
				APP_CONSTANTS.setProperty("testtool.enabled", testToolEnabled);
			}
			// notify other components of status of debugger
			DebuggerStatusChangedEvent event = new DebuggerStatusChangedEvent(this, testToolEnabled);
			if (applicationEventPublisher != null) {
				applicationEventPublisher.publishEvent(event);
			}
		}
		if (bean instanceof nl.nn.testtool.storage.file.Storage) {
			String maxFileSize = APP_CONSTANTS.getProperty("ibistesttool.maxFileSize");
			if (maxFileSize != null) {
				nl.nn.testtool.storage.file.Storage loggingStorage = (nl.nn.testtool.storage.file.Storage)bean;
				long maxFileSizeLong = OptionConverter.toFileSize(maxFileSize, nl.nn.testtool.storage.file.Storage.DEFAULT_MAXIMUM_FILE_SIZE);
				loggingStorage.setMaximumFileSize(maxFileSizeLong);
			}
			String maxBackupIndex = APP_CONSTANTS.getProperty("ibistesttool.maxBackupIndex");
			if (maxBackupIndex != null) {
				nl.nn.testtool.storage.file.Storage loggingStorage = (nl.nn.testtool.storage.file.Storage)bean;
				int maxBackupIndexInt = Integer.parseInt(maxBackupIndex);
				loggingStorage.setMaximumBackupIndex(maxBackupIndexInt);
			}
			String freeSpaceMinimum = APP_CONSTANTS.getProperty("ibistesttool.freeSpaceMinimum");
			if (freeSpaceMinimum != null) {
				nl.nn.testtool.storage.file.Storage loggingStorage = (nl.nn.testtool.storage.file.Storage)bean;
				long freeSpaceMinimumLong = OptionConverter.toFileSize(freeSpaceMinimum, -1);
				loggingStorage.setFreeSpaceMinimum(freeSpaceMinimumLong);
			}
		}
		if (bean instanceof Views) {
			String defaultView = APP_CONSTANTS.getResolvedProperty("ibistesttool.defaultView");
			if (defaultView != null) {
				Views views = (Views)bean;
				View view = views.setDefaultView(defaultView);
				if (view == null) {
					throw new BeanCreationException("Default view '" + defaultView + "' not found");
				}
			}
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
}
