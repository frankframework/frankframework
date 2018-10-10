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
import nl.nn.testtool.TestTool;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.filter.Views;

import org.apache.log4j.helpers.OptionConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author Jaco de Groot
 */
public class DeploymentSpecificsBeanPostProcessor implements BeanPostProcessor {

	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof TestTool) {
			TestTool testTool = (TestTool)bean;
			// TODO appConstants.getResolvedProperty doen?
			String stage = System.getProperty("otap.stage");
			if ("ACC".equals(stage) || "PRD".equals(stage)) {
				testTool.setReportGeneratorEnabled(false);
			}
			AppConstants appConstants = AppConstants.getInstance();
			String testToolEnabled = appConstants.getProperty("testtool.enabled");
			if ("TRUE".equalsIgnoreCase(testToolEnabled)) {
				testTool.setReportGeneratorEnabled(true);
			}
		}
		if (bean instanceof nl.nn.testtool.storage.file.Storage) {
			// TODO appConstants via set methode door spring i.p.v. AppConstants.getInstance()?
			AppConstants appConstants = AppConstants.getInstance();
			String maxFileSize = appConstants.getResolvedProperty("ibistesttool.maxFileSize");
			if (maxFileSize != null) {
				nl.nn.testtool.storage.file.Storage loggingStorage = (nl.nn.testtool.storage.file.Storage)bean;
				long maximumFileSize = OptionConverter.toFileSize(maxFileSize, nl.nn.testtool.storage.file.Storage.DEFAULT_MAXIMUM_FILE_SIZE);
				loggingStorage.setMaximumFileSize(maximumFileSize);
			}
			String maxBackupIndex = appConstants.getResolvedProperty("ibistesttool.maxBackupIndex");
			if (maxBackupIndex != null) {
				nl.nn.testtool.storage.file.Storage loggingStorage = (nl.nn.testtool.storage.file.Storage)bean;
				int maximumBackupIndex = Integer.parseInt(maxBackupIndex);
				loggingStorage.setMaximumBackupIndex(maximumBackupIndex);
			}
		}
//		if (bean instanceof nl.nn.testtool.storage.diff.Storage) {
//			// TODO niet otap.stage maar een specifieke prop. gebruiken? op andere plekken in deze class ook?
//			String stage = System.getResolvedProperty("otap.stage");
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

	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

}
