/*
Copyright 2017, 2020-2022 WeAreFrank!

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
package nl.nn.adapterframework.jdbc.migration;

import java.io.Writer;
import java.net.URL;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationMessageEvent;
import nl.nn.adapterframework.configuration.classloaders.ClassLoaderBase;
import nl.nn.adapterframework.core.IConfigurationAware;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.jdbc.IDataSourceFactory;
import nl.nn.adapterframework.dbms.JdbcException;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

/**
 * DatabaseMigration implementation for IAF.
 *
 * @author	Niels Meijer
 * @since	7.0-B4
 *
 */
public abstract class DatabaseMigratorBase implements IConfigurationAware, InitializingBean {

	protected Logger log = LogUtil.getLogger(this);
	private @Setter IDataSourceFactory dataSourceFactory = null;
	private @Getter Configuration configuration;
	private @Setter String defaultDatasourceName = JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
	private @Getter String name;
	private @Getter ClassLoader configurationClassLoader = null;
	private @Setter String datasourceName;

	@Override
	public void afterPropertiesSet() {
		if(dataSourceFactory == null) {
			throw new IllegalStateException("DataSourceFactory has not been autowired");
		}

		configurationClassLoader = configuration.getClassLoader();
		if(!(configurationClassLoader instanceof ClassLoaderBase)) { //Though this should technically never happen.. you never know!
			throw new IllegalStateException("unable to initialize database migrator");
		}
	}

	public String getDatasourceName() {
		if(datasourceName == null) {
			AppConstants appConstants = AppConstants.getInstance(configuration.getClassLoader());
			datasourceName = appConstants.getString("jdbc.migrator.datasource", appConstants.getString("jdbc.migrator.dataSource", JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME));
		}
		return datasourceName;
	}

	protected final URL getResource(String path) {
		return ((ClassLoaderBase) configurationClassLoader).getResource(path, false);
	}

	protected final DataSource lookupMigratorDatasource() throws SQLException {
		try {
			log.debug("looking up Datasource ["+getDatasourceName()+"] for JdbcMigrator ["+getName()+"]");
			return dataSourceFactory.getDataSource(getDatasourceName());
		} catch (NamingException e) {
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

}
