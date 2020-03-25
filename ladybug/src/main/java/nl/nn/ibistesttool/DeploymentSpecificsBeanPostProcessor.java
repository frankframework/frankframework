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
			//TestTool testTool = (TestTool)bean;
			
			// Contract for testtool state:
			// - appconstants testtool.enabled stores global state
			// - when the state changes:
			//   appconstants testtool.enabled must be updated
			//   a DebuggerStatusChangedEvent must be fired to notify others
			// - to get notified of canges, components should listen to DebuggerStatusChangedEvents
			
			boolean testToolEnabled=true;
			AppConstants appConstants = AppConstants.getInstance();
			String testToolEnabledProperty=appConstants.getProperty("testtool.enabled");
			if (StringUtils.isNotEmpty(testToolEnabledProperty)) {
				testToolEnabled="true".equalsIgnoreCase(testToolEnabledProperty);
			} else {
				String stage = APP_CONSTANTS.getProperty("dtap.stage");
				if ("ACC".equals(stage) || "PRD".equals(stage)) {
					testToolEnabled=false;
				}
				appConstants.setProperty("testtool.enabled", testToolEnabled);
			}
			// enable/disable testtool via two switches, until one of the switches has become deprecated
			DebuggerStatusChangedEvent event = new DebuggerStatusChangedEvent(this, testToolEnabled);
			if (applicationEventPublisher != null) {
				applicationEventPublisher.publishEvent(event);
			}
		}
		if (bean instanceof nl.nn.testtool.storage.file.Storage) {
			// TODO appConstants via set methode door spring i.p.v. AppConstants.getInstance()?
			String maxFileSize = APP_CONSTANTS.getProperty("ibistesttool.maxFileSize");
			if (maxFileSize != null) {
				nl.nn.testtool.storage.file.Storage loggingStorage = (nl.nn.testtool.storage.file.Storage)bean;
				long maximumFileSize = OptionConverter.toFileSize(maxFileSize, nl.nn.testtool.storage.file.Storage.DEFAULT_MAXIMUM_FILE_SIZE);
				loggingStorage.setMaximumFileSize(maximumFileSize);
			}
			String maxBackupIndex = APP_CONSTANTS.getProperty("ibistesttool.maxBackupIndex");
			if (maxBackupIndex != null) {
				nl.nn.testtool.storage.file.Storage loggingStorage = (nl.nn.testtool.storage.file.Storage)bean;
				int maximumBackupIndex = Integer.parseInt(maxBackupIndex);
				loggingStorage.setMaximumBackupIndex(maximumBackupIndex);
			}
		}
//		if (bean instanceof nl.nn.testtool.storage.diff.Storage) {
//			// TODO niet dtap.stage maar een specifieke prop. gebruiken? op andere plekken in deze class ook?
//			String stage = System.getResolvedProperty("dtap.stage");
//			if ("LOC".equals(stage)) {
//				AppConstants appConstants = AppConstants.getInstance();
//				nl.nn.testtool.storage.diff.Storage runStorage = (nl.nn.testtool.storage.diff.Storage)bean;
//				runStorage.setReportsFilename(appConstants.getResolvedProperty("rootRealPath") + "../TestTool/reports.xml");
//werkt hier ook niet, deze ?singleton? bean wordt ook al aangemaakt voor tt servlet
//				System.out.println("xxxxxxxxxx" + appConstants.getResolvedProperty("rootRealPath") + "../TestTool/reports.xml");//TODO remove
//			}
//		}
		if (bean instanceof Views) {
			AppConstants appConstants = AppConstants.getInstance();
			String defaultView = appConstants.getResolvedProperty("ibistesttool.defaultView");
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
