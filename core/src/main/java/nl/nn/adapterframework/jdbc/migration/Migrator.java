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

import liquibase.exception.LiquibaseException;
import liquibase.exception.ValidationFailedException;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.jdbc.IDataSourceFactory;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Liquibase implementation for IAF. 
 * Please call close method explicitly to release the connection used by liquibase or instantiate this with try-with-resources.
 * 
 * @author	Niels Meijer
 * @since	7.0-B4
 *
 */
public class Migrator implements IConfigurable, AutoCloseable {

	private Logger log = LogUtil.getLogger(this);
	private @Setter IDataSourceFactory dataSourceFactory = null;
	private Configuration configuration;
	private @Setter String defaultDatasourceName = JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
	private @Getter @Setter String name;
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	private LiquibaseImpl instance;

	@Override
	public void configure() throws ConfigurationException {
		configure(configuration, null);
	}

	private DataSource getDatasource(String datasourceName) throws ConfigurationException {
		DataSource datasource;
		try {
			datasource = dataSourceFactory.getDataSource(datasourceName);
		} catch (NamingException e) {
			throw new ConfigurationException("Could not find Datasource ["+datasourceName+"]", e);
		}
		log.debug("looked up Datasource ["+datasourceName+"] for JdbcMigrator ["+getName()+"]");

		return new TransactionAwareDataSourceProxy(datasource);
	}

	private synchronized void configure(Configuration configuration, String changeLogFile) throws ConfigurationException {
		AppConstants appConstants = AppConstants.getInstance(configuration.getClassLoader());
		setName("JdbcMigrator for configuration ["+ configuration.getName() +"]");

		String datasourceName = appConstants.getString("jdbc.migrator.datasource", appConstants.getString("jdbc.migrator.dataSource", defaultDatasourceName));
		DataSource datasource = getDatasource(datasourceName);

		if(changeLogFile == null) {
			changeLogFile = appConstants.getString("liquibase.changeLogFile", "DatabaseChangelog.xml");
		}

		LiquibaseClassLoaderWrapper cl = new LiquibaseClassLoaderWrapper(configuration.getClassLoader());
		if(cl.getResource(changeLogFile) == null) {
			String msg = "unable to find database changelog file ["+changeLogFile+"]";
			msg += " classLoader ["+ClassUtils.nameOf(configuration.getClassLoader())+"]";
			log.debug(msg);
		}
		else {
			try {
				instance = new LiquibaseImpl(datasource, configuration, cl, changeLogFile);
			}
			catch (ValidationFailedException e) {
				ConfigurationWarnings.add(this, log, "liquibase validation failed: "+e.getMessage(), e);
			}
			catch (LiquibaseException e) {
				ConfigurationWarnings.add(this, log, "liquibase failed to initialize", e);
			}
			catch (Throwable e) {
				ConfigurationWarnings.add(this, log, "liquibase failed to initialize, error connecting to database ["+datasourceName+"]", e);
			}
		}
	}

	public void update() {
		if(this.instance != null) {
			try {
				instance.update();
			} catch (JdbcException e) {
				ConfigurationWarnings.add(configuration, log, e.getMessage(), e);
			}
		}
	}

	public Writer getUpdateSql(Writer writer) throws JdbcException {
		if(this.instance != null) {
			try {
				return instance.getUpdateScript(writer);
			} catch (Exception e) {
				throw new JdbcException("unable to generate database migration script", e);
			}
		}
		return writer;
	}

	@Override
	public void close() {
		if(this.instance != null) {
			try {
				instance.close();
			} catch (Exception e) {
				log.error("Failed to close the connection", e);
			}
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if(!(applicationContext instanceof Configuration)) {
			throw new IllegalStateException("context not instanceof configuration");
		}
		this.configuration = (Configuration) applicationContext;
	}

	@Override
	public ApplicationContext getApplicationContext() {
		return configuration;
	}
}
