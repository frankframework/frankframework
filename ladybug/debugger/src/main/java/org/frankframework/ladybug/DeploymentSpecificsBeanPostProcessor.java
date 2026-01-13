/*
   Copyright 2018 Nationale-Nederlanden, 2024-2026 WeAreFrank!

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

import org.apache.logging.log4j.core.util.OptionConverter;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.wearefrank.ladybug.filter.View;
import org.wearefrank.ladybug.filter.Views;
import org.wearefrank.ladybug.storage.database.DatabaseStorage;

import liquibase.integration.spring.SpringLiquibase;

import org.frankframework.util.AppConstants;

/**
 * @author Jaco de Groot
 */
public class DeploymentSpecificsBeanPostProcessor implements BeanPostProcessor {
	private final AppConstants appConstants = AppConstants.getInstance();

	@Override
	public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		if (bean instanceof DatabaseStorage databaseStorage) {
			String maxStorageSize = appConstants.getProperty("ibistesttool.maxStorageSize");
			if (maxStorageSize != null) {
				long maxStorageSizeLong = OptionConverter.toFileSize(maxStorageSize, databaseStorage.getMaxStorageSize());
				databaseStorage.setMaxStorageSize(maxStorageSizeLong);
			}
			String maxStorageDays = appConstants.getProperty("ibistesttool.maxStorageDays");
			if (maxStorageDays != null) {
				long maxStorageDaysLong = OptionConverter.toFileSize(maxStorageDays, -1);
				databaseStorage.setMaxStorageDays(maxStorageDaysLong);
			}
		}

		if (bean instanceof org.wearefrank.ladybug.storage.file.Storage loggingStorage) {
			String maxFileSize = appConstants.getProperty("ibistesttool.maxFileSize");
			if (maxFileSize != null) {
				long maxFileSizeLong = OptionConverter.toFileSize(maxFileSize, org.wearefrank.ladybug.storage.file.Storage.DEFAULT_MAXIMUM_FILE_SIZE);
				loggingStorage.setMaximumFileSize(maxFileSizeLong);
			}

			String maxBackupIndex = appConstants.getProperty("ibistesttool.maxBackupIndex");
			if (maxBackupIndex != null) {
				int maxBackupIndexInt = Integer.parseInt(maxBackupIndex);
				loggingStorage.setMaximumBackupIndex(maxBackupIndexInt);
			}

			String freeSpaceMinimum = appConstants.getProperty("ibistesttool.freeSpaceMinimum");
			if (freeSpaceMinimum != null) {
				long freeSpaceMinimumLong = OptionConverter.toFileSize(freeSpaceMinimum, -1);
				loggingStorage.setFreeSpaceMinimum(freeSpaceMinimumLong);
			}
		}

		if (bean instanceof Views views) {
			String defaultView = appConstants.getProperty("ibistesttool.defaultView");
			if (defaultView != null) {
				View view = views.setDefaultView(defaultView);
				if (view == null) {
					throw new BeanCreationException("Default view '" + defaultView + "' not found");
				}
			}
		}

		if (bean instanceof SpringLiquibase springLiquibase) {
			boolean active = appConstants.getBoolean("ladybug.jdbc.migrator.active",
					appConstants.getBoolean("jdbc.migrator.active", false));
			springLiquibase.setShouldRun(active);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		return bean;
	}
}
