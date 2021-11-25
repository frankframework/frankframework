/*
Copyright 2017, 2020, 2021 WeAreFrank!

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

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationMessageEvent;
import nl.nn.adapterframework.configuration.classloaders.ClassLoaderBase;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.jdbc.IDataSourceFactory;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Liquibase implementation for IAF. 
 * Please call close method explicitly to release the connection used by liquibase or instantiate this with try-with-resources.
 * 
 * @author	Niels Meijer
 * @since	7.0-B4
 *
 */
public abstract class Migrator implements IConfigurable, AutoCloseable {

	protected Logger log = LogUtil.getLogger(this);
	private @Setter IDataSourceFactory dataSourceFactory = null;
	private Configuration configuration;
	private @Setter String defaultDatasourceName = JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
	private @Getter @Setter String name;
	private @Getter ClassLoader configurationClassLoader = null;
	private @Getter String datasourceName;

	@Override
	public void configure() throws ConfigurationException {
		setName("JdbcMigrator for configuration ["+ configuration.getName() +"]");

		AppConstants appConstants = AppConstants.getInstance(configuration.getClassLoader());
		datasourceName = appConstants.getString("jdbc.migrator.datasource", appConstants.getString("jdbc.migrator.dataSource", JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME));

		configurationClassLoader = configuration.getClassLoader();
		if(!(configurationClassLoader instanceof ClassLoaderBase)) { //Though this should technically never happen.. you never know!
			throw new ConfigurationException("unable to initialize database migrator");
		}
	}

//	protected final URL getResource(String path) {
//		return ((ClassLoaderBase) configurationClassLoader).getResource(path, false);
//	}

	protected final DataSource lookupMigratorDatasource() throws ConfigurationException {
		DataSource datasource;
		try {
			datasource = dataSourceFactory.getDataSource(datasourceName);
		} catch (NamingException e) {
			throw new ConfigurationException("Could not find Datasource ["+datasourceName+"]", e);
		}
		log.debug("looked up Datasource ["+datasourceName+"] for JdbcMigrator ["+getName()+"]");

		return new TransactionAwareDataSourceProxy(datasource);
	}

	/**
	 * Run the migration script against the database.
	 */
	public abstract void update() throws JdbcException;
//		if(this.instance != null) {
//			try {
//				instance.update();
//			} catch (JdbcException e) {
//				ConfigurationWarnings.add(configuration, log, e.getMessage(), e);
//			}
//		}

	/**
	 * Run the migration script and write the output to the {@link Writer}.
	 */
	public abstract void update(Writer writer) throws JdbcException;

	@Override
	public final void close() {
		try {
			doClose();
		} catch (Exception e) {
			log.error("failed to close the connection", e);
		}
	}

	protected final void logConfigurationMessage(String message) {
		configuration.publishEvent(new ConfigurationMessageEvent(this, message));
	}

	protected abstract void doClose() throws Exception;

	public boolean hasLiquibaseScript(Configuration config) {
		AppConstants appConstants = AppConstants.getInstance(config.getClassLoader());
		String changeLogFile = appConstants.getString("liquibase.changeLogFile", "DatabaseChangelog.xml");
		LiquibaseResourceAccessor resourceAccessor = new LiquibaseResourceAccessor(config.getClassLoader());
		if(resourceAccessor.getResource(changeLogFile) == null) {
			log.debug("No liquibase script ["+changeLogFile+"] found in the classpath of configuration ["+config.getName()+"]");
			return false;
		}
		return true;
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

}
