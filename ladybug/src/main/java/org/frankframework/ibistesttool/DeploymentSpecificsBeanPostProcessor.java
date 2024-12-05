/*
   Copyright 2018 Nationale-Nederlanden, 2024 WeAreFrank!

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

import liquibase.integration.spring.SpringLiquibase;
import org.apache.logging.log4j.core.util.OptionConverter;
import org.frankframework.util.AppConstants;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import nl.nn.testtool.filter.View;
import nl.nn.testtool.filter.Views;
import nl.nn.testtool.storage.database.DatabaseStorage;

/**
 * @author Jaco de Groot
 */
public class DeploymentSpecificsBeanPostProcessor implements BeanPostProcessor {
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof DatabaseStorage databaseStorage) {
			String maxStorageSize = APP_CONSTANTS.getProperty("ibistesttool.maxStorageSize");
			if (maxStorageSize != null) {
				long maxStorageSizeLong = OptionConverter.toFileSize(maxStorageSize, databaseStorage.getMaxStorageSize());
				databaseStorage.setMaxStorageSize(maxStorageSizeLong);
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
		if (bean instanceof Views views) {
			String defaultView = APP_CONSTANTS.getProperty("ibistesttool.defaultView");
			if (defaultView != null) {
				View view = views.setDefaultView(defaultView);
				if (view == null) {
					throw new BeanCreationException("Default view '" + defaultView + "' not found");
				}
			}
		}
		if (bean instanceof SpringLiquibase springLiquibase) {
			boolean active = APP_CONSTANTS.getBoolean("ladybug.jdbc.migrator.active",
					APP_CONSTANTS.getBoolean("jdbc.migrator.active", false));
			springLiquibase.setShouldRun(active);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
