/*
Copyright 2017, 2020 Integration Partners B.V.

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.jdbc.JdbcFacade;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
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
		configure(null, null, null);
	}

	public void configure(String configurationName) throws ConfigurationException {
		configure(configurationName, null, null);
	}

	public void configure(String configurationName, ClassLoader classLoader) throws ConfigurationException {
		configure(configurationName, classLoader, null);
	}

	public synchronized void configure(String configurationName, ClassLoader classLoader, String changeLogFile) throws ConfigurationException {
		setName("JdbcMigrator for configuration["+configurationName+"]");

		AppConstants appConstants = AppConstants.getInstance(classLoader);

		if(changeLogFile == null)
			changeLogFile = appConstants.getString("liquibase.changeLogFile", "DatabaseChangelog.xml");

		LiquibaseClassLoaderWrapper cl = new LiquibaseClassLoaderWrapper(classLoader);
		if(cl.getResource(changeLogFile) == null) {
			String msg = "unable to find database changelog file ["+changeLogFile+"]";
			if(configurationName != null)
				msg += " classLoader ["+ClassUtils.nameOf(classLoader)+"]";

			log.debug(msg);
		}
		else {
			String dataSource = appConstants.getString("jdbc.migrator.dataSource", appConstants.getResolvedProperty("jdbc.datasource.default"));
			setDatasourceName(dataSource);

			try {
				JdbcConnection connection = new JdbcConnection(getConnection());
				instance = new LiquibaseImpl(ibisContext, cl, connection, configurationName, changeLogFile);
			}
			catch (ValidationFailedException e) {
				throw new ConfigurationException("liquibase validation failed", e);
			}
			catch (LiquibaseException e) {
				throw new ConfigurationException("liquibase failed to initialize", e);
			}
			catch (Throwable e) {
				throw new ConfigurationException("liquibase failed to initialize, error connecting to database ["+dataSource+"]", e);
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
}
