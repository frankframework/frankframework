/*
Copyright 2017, 2020-2024 WeAreFrank!

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
package org.frankframework.jdbc.migration;

import java.io.Writer;
import java.net.URL;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationMessageEvent;
import org.frankframework.configuration.classloaders.AbstractClassLoader;
import org.frankframework.core.IConfigurationAware;
import org.frankframework.core.Resource;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

/**
 * DatabaseMigration implementation for IAF.
 *
 * @author	Niels Meijer
 * @since	7.0-B4
 *
 */
public abstract class AbstractDatabaseMigrator implements ConfigurableLifecycle, IConfigurationAware, InitializingBean {

	protected Logger log = LogUtil.getLogger(this);
	private @Setter IDataSourceFactory dataSourceFactory = null;
	private @Getter Configuration configuration;
	private @Setter String defaultDatasourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
	private @Getter String name;
	private @Getter ClassLoader configurationClassLoader = null;
	private @Setter String datasourceName;

	@Override
	public void afterPropertiesSet() {
		if(dataSourceFactory == null) {
			throw new IllegalStateException("DataSourceFactory has not been autowired");
		}

		configurationClassLoader = configuration.getClassLoader();
		if(!(configurationClassLoader instanceof AbstractClassLoader)) { //Though this should technically never happen.. you never know!
			throw new IllegalStateException("unable to initialize database migrator");
		}
	}

	public String getDatasourceName() {
		if(datasourceName == null) {
			AppConstants appConstants = AppConstants.getInstance(configuration.getClassLoader());
			datasourceName = appConstants.getString("jdbc.migrator.datasource", appConstants.getString("jdbc.migrator.dataSource", IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME));
		}
		return datasourceName;
	}

	protected final URL getResource(String path) {
		return ((AbstractClassLoader) configurationClassLoader).getResource(path, false);
	}

	protected final DataSource lookupMigratorDatasource() throws SQLException {
		try {
			log.debug("looking up Datasource [{}] for JdbcMigrator [{}]", getDatasourceName(), getName());
			return dataSourceFactory.getDataSource(getDatasourceName());
		} catch (IllegalStateException e) {
			throw new SQLException("cannot connect to datasource ["+getDatasourceName()+"]", e);
		}
	}

	/**
	 * Validate the current already executed ChangeSets against the migration script
	 */
	public abstract boolean validate();

	/**
	 * Run the migration script against the database.
	 */
	public abstract void update() throws JdbcException;

	/**
	 * Run the migration script and write the output to the {@link Writer}.
	 */
	public abstract void update(Writer writer) throws JdbcException;

	/**
	 * Run the provided migration script (against the local database) and write the output to the {@link Writer}.
	 */
	public abstract void update(Writer writer, Resource resource) throws JdbcException;

	/**
	 * Check whether the configuration contains liquibase script that can be translated into sql statements in the classpath
	 */
	public boolean hasMigrationScript() {
		return getChangeLog() != null;
	}

	public abstract Resource getChangeLog();

	protected final void logConfigurationMessage(String message) {
		configuration.publishEvent(new ConfigurationMessageEvent(this, message));
	}

	@Override
	public final void setApplicationContext(ApplicationContext applicationContext) {
		if(!(applicationContext instanceof Configuration)) {
			throw new IllegalStateException("context not instanceof configuration");
		}
		this.configuration = (Configuration) applicationContext;
	}

	@Override //Can't lombok because the field name is configuration
	public final ApplicationContext getApplicationContext() {
		return configuration;
	}

	public boolean isEnabled() {
		return AppConstants.getInstance(configuration.getClassLoader()).getBoolean("jdbc.migrator.active", false);
	}

	@Override
	public int getPhase() {
		return Integer.MIN_VALUE; // Starts first
	}

	@Override
	public void start() {
		//Do nothing
	}

	@Override
	public void stop() {
		//Do nothing
	}

	@Override
	public boolean isRunning() {
		return false;
	}

	@Override
	public void configure() throws ConfigurationException {
		if(isEnabled()) {
			try {
				if(validate()) {
					update();
				}
			} catch (Exception e) {
				configuration.log("unable to run JDBC migration", e);
			}
		}
	}
}
