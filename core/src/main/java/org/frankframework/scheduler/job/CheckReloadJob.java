/*
   Copyright 2021-2024 WeAreFrank!

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
package org.frankframework.scheduler.job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationUtils;
import org.frankframework.configuration.IbisManager;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.scheduler.AbstractJobDef;
import org.frankframework.util.AppConstants;
import org.frankframework.util.MessageKeeper.MessageKeeperLevel;
import org.frankframework.util.SpringUtils;

/**
 * Frank!Framework job which periodically looks in the {@code IBISCONFIG} table to see if a new {@link Configuration} should be loaded.
 * 
 * @ff.info This is a default job that can be controlled with the property {@literal checkReload.active} and {@literal checkReload.interval}.
 */
public class CheckReloadJob extends AbstractJobDef {
	private static final boolean CONFIG_AUTO_DB_CLASSLOADER = AppConstants.getInstance().getBoolean("configurations.database.autoLoad", false);
	private static final String DATABASE_CLASSLOADER = "DatabaseClassLoader";
	private boolean atLeastOneConfigurationHasDBClassLoader = CONFIG_AUTO_DB_CLASSLOADER;

	@Override
	public boolean beforeExecuteJob() {
		if(!atLeastOneConfigurationHasDBClassLoader) {
			IbisManager ibisManager = getIbisManager();
			for (Configuration configuration : ibisManager.getConfigurations()) {
				if(DATABASE_CLASSLOADER.equals(configuration.getClassLoaderType())) {
					atLeastOneConfigurationHasDBClassLoader =true;
					break;
				}
			}
		} else {
			getMessageKeeper().add("skipped job execution: autoload is disabled");
		}
		if(!atLeastOneConfigurationHasDBClassLoader) {
			getMessageKeeper().add("skipped job execution: no database configurations found");
		}
		return atLeastOneConfigurationHasDBClassLoader;
	}

	@Override
	public void execute() {
		IbisManager ibisManager = getIbisManager();
		if (ibisManager.getIbisContext().isLoadingConfigs()) {
			String msg = "skipping checkReload because one or more configurations are currently loading";
			getMessageKeeper().add(msg, MessageKeeperLevel.INFO);
			log.info(msg);
			return;
		}

		List<String> configNames = new ArrayList<>();
		List<String> configsToReload = new ArrayList<>();

		FixedQuerySender qs = SpringUtils.createBean(getApplicationContext());
		qs.setDatasourceName(getDataSource());
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		String booleanValueTrue = qs.getDbmsSupport().getBooleanValue(true);
		String selectQuery = "SELECT VERSION FROM IBISCONFIG WHERE NAME=? AND ACTIVECONFIG = "+booleanValueTrue+" and AUTORELOAD = "+booleanValueTrue;
		try {
			qs.configure();
			qs.start();
			try (Connection conn = qs.getConnection(); PreparedStatement stmt = conn.prepareStatement(selectQuery)) {
				for (Configuration configuration : ibisManager.getConfigurations()) {
					String configName = configuration.getName();
					configNames.add(configName);
					if (DATABASE_CLASSLOADER.equals(configuration.getClassLoaderType())) {
						stmt.setString(1, configName);
						try (ResultSet rs = stmt.executeQuery()) {
							if (rs.next()) {
								String ibisConfigVersion = rs.getString(1);
								String configVersion = configuration.getVersion(); // DatabaseClassLoader configurations always have a version
								if(StringUtils.isEmpty(configVersion) && configuration.getClassLoader() != null) { // If config hasn't loaded yet, don't skip it!
									log.warn("skipping autoreload for configuration [{}] unable to determine [configuration.version]", configName);
								}
								else if (!StringUtils.equalsIgnoreCase(ibisConfigVersion, configVersion)) {
									log.info("configuration [{}] with version [{}] will be reloaded with new version [{}]", configName, configVersion, ibisConfigVersion);
									configsToReload.add(configName);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			getMessageKeeper().add("error while executing query [" + selectQuery + "] (as part of scheduled job execution)", e);
			return;
		} finally {
			qs.stop();
		}

		if (!configsToReload.isEmpty()) {
			for (String configToReload : configsToReload) {
				ibisManager.getIbisContext().reload(configToReload);
			}
		}

		if (CONFIG_AUTO_DB_CLASSLOADER) {
			// load new (activated) configs
			List<String> dbConfigNames = null;
			try {
				dbConfigNames = ConfigurationUtils.retrieveConfigNamesFromDatabase(getApplicationContext());
			} catch (ConfigurationException e) {
				getMessageKeeper().add("error while retrieving configuration names from database", e);
			}
			if (dbConfigNames != null && !dbConfigNames.isEmpty()) {
				for (String currentDbConfigurationName : dbConfigNames) {
					if (!configNames.contains(currentDbConfigurationName)) {
						ibisManager.getIbisContext().load(currentDbConfigurationName);
					}
				}
				// unload old (deactivated) configurations
				for (String currentConfigurationName : configNames) {
					if (!dbConfigNames.contains(currentConfigurationName) && DATABASE_CLASSLOADER.equals(ibisManager.getConfiguration(currentConfigurationName).getClassLoaderType())) {
						ibisManager.getIbisContext().unload(currentConfigurationName);
					}
				}
			}
		}
	}

	protected String getDataSource() {
		return IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
	}
}
