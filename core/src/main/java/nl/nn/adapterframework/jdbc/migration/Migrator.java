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

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.lang3.StringUtils;

import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.ValidationFailedException;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.classloaders.ClassLoaderBase;
import nl.nn.adapterframework.jdbc.JdbcFacade;
import nl.nn.adapterframework.util.AppConstants;

/**
 * LiquiBase implementation for IAF. 
 * Please call close method explicitly to release the connection used by liquibase or instantiate this with try-with-resources.
 * 
 * @author	Niels Meijer
 * @since	7.0-B4
 *
 */
public class Migrator extends JdbcFacade implements AutoCloseable {

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
		AppConstants appConstants = AppConstants.getInstance(configuration.getClassLoader());
		setName("JdbcMigrator for configuration["+ configuration.getName() +"]");
		if(StringUtils.isEmpty(getDatasourceName())) {	
			setDatasourceName(appConstants.getString("jdbc.migrator.dataSource", null));
		}
		super.configure();


		if(changeLogFile == null)
			changeLogFile = appConstants.getString("liquibase.changeLogFile", "DatabaseChangelog.xml");

		ClassLoader classLoader = configuration.getClassLoader();
		if(!(classLoader instanceof ClassLoaderBase)) { //Though this should technically never happen.. you never know!
			ConfigurationWarnings.add(configuration, log, "unable to initialize database migrator");
			return;
		}

		try {
			instance = new LiquibaseImpl(ibisContext, getDatasource(), configuration, changeLogFile);
		}
		catch (ValidationFailedException e) {
			ConfigurationWarnings.add(configuration, log, "liquibase validation failed: "+e.getMessage(), e);
		}
		catch (LiquibaseException e) {
			ConfigurationWarnings.add(configuration, log, "liquibase failed to initialize", e);
		}
		catch (IOException e) {
			log.debug(e.getMessage(), e); //this can only happen when jdbc.migrator.active=true but no migrator file is present.
		}
		catch (Throwable e) {
			ConfigurationWarnings.add(configuration, log, "liquibase failed to initialize, error connecting to database ["+getDatasourceName()+"]", e);
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

	@Override
	public void close() {
		try {
			if(this.instance != null) {
				try {
					instance.close();
				} catch (DatabaseException e) {
					log.error("Failed to close the connection", e);
				}
			}
		} finally {
			super.close();
		}
	}
}
