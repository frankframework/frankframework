/*
Copyright 2017, 2020, 2021 Integration Partners B.V.

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

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.jdbc.JdbcFacade;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;

import java.io.Writer;

import org.apache.commons.lang3.StringUtils;

import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.exception.ValidationFailedException;

/**
 * LiquiBase implementation for IAF
 * 
 * @author	Niels Meijer
 * @since	7.0-B4
 *
 */
public class Migrator extends JdbcFacade {

	private IbisContext ibisContext;
	private LiquibaseImpl instance;

	public Migrator() {
	}

	@Override
	public void configure() throws ConfigurationException {
		throw new IllegalStateException("No configuration is specified!");
	}

	public void configure(Configuration configuration) throws ConfigurationException {
		configure(configuration, null);
	}

	public synchronized void configure(Configuration configuration, String changeLogFile) throws ConfigurationException {
		setName("JdbcMigrator for configuration["+ configuration.getName() +"]");
		super.configure();

		AppConstants appConstants = AppConstants.getInstance(configuration.getClassLoader());

		if(changeLogFile == null)
			changeLogFile = appConstants.getString("liquibase.changeLogFile", "DatabaseChangelog.xml");

		LiquibaseClassLoaderWrapper cl = new LiquibaseClassLoaderWrapper(configuration.getClassLoader());
		if(cl.getResource(changeLogFile) == null) {
			String msg = "unable to find database changelog file ["+changeLogFile+"]";
			msg += " classLoader ["+ClassUtils.nameOf(configuration.getClassLoader())+"]";
			log.debug(msg);
		}
		else {
			if(StringUtils.isEmpty(getDatasourceName())) {
				String dataSource = appConstants.getString("jdbc.migrator.dataSource", appConstants.getResolvedProperty("jdbc.datasource.default"));
				setDatasourceName(dataSource);
			}

			try {
				JdbcConnection connection = new JdbcConnection(getConnection());
				instance = new LiquibaseImpl(ibisContext, connection, configuration, changeLogFile);
			}
			catch (ValidationFailedException e) {
				ConfigurationWarnings.add(configuration, log, "liquibase validation failed", e);
			}
			catch (LiquibaseException e) {
				ConfigurationWarnings.add(configuration, log, "liquibase failed to initialize", e);
			}
			catch (Throwable e) {
				ConfigurationWarnings.add(configuration, log, "liquibase failed to initialize, error connecting to database ["+getDatasourceName()+"]", e);
			}
		}
	}

	public void setIbisContext(IbisContext ibisContext) {
		this.ibisContext = ibisContext;
	}

	public void update() {
		if(this.instance != null)
			instance.update();
	}

	public Writer getUpdateSql(Writer writer) throws LiquibaseException {
		if(this.instance != null)
			return instance.getUpdateScript(writer);
		return writer;
	}
}
