/*
Copyright 2017 - 2021 WeAreFrank!

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
import java.io.InputStream;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.exception.LockException;
import liquibase.exception.ValidationFailedException;
import liquibase.executor.ExecutorService;
import liquibase.lockservice.LockService;
import liquibase.lockservice.LockServiceFactory;
import liquibase.resource.ResourceAccessor;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.AppConstants;

/**
 * LiquiBase implementation for IAF
 * 
 * @author	Niels Meijer
 * @since	7.0-B4
 *
 */
public class LiquibaseMigrator extends DatabaseMigratorBase {

	private Liquibase liquibase = null;
	private Contexts contexts;
	private LabelExpression labelExpression = new LabelExpression();

	public String getChangeLogFile() throws IOException {
		AppConstants appConstants = AppConstants.getInstance(getApplicationContext().getClassLoader());
		String changeLogFile = appConstants.getString("liquibase.changeLogFile", "DatabaseChangelog.xml");

		if(getResource(changeLogFile) == null) {
			String msg = "unable to find database changelog file [" + changeLogFile + "]";
			msg += " classLoader [" + getConfigurationClassLoader() + "]";
			throw new IOException(msg);
		}

		return changeLogFile;
	}

	@Override
	public void configure() throws ConfigurationException {
		configure(null, null);
	}

	public void configure(InputStream file, String filename) throws ConfigurationException {
		super.configure();

		try {
			String changeLogFile = getChangeLogFile();

			ResourceAccessor resourceAccessor;
			if(file == null) {
				resourceAccessor = new LiquibaseResourceAccessor(getConfigurationClassLoader());
			} else {
				resourceAccessor = new StreamResourceAccessor(file);
			}
			DatabaseConnection connection = getDatabaseConnection();

			this.liquibase = new Liquibase(changeLogFile, resourceAccessor, connection);
			validate();
		}
		catch (ValidationFailedException e) {
			ConfigurationWarnings.add(this, log, "liquibase validation failed: "+e.getMessage(), e);
		}
		catch (LiquibaseException e) {
			ConfigurationWarnings.add(this, log, "liquibase failed to initialize", e);
		}
		catch (IOException e) {
			log.debug(e.getMessage(), e); //this can only happen when jdbc.migrator.active=true but no migrator file is present.
		}
		catch (SQLException e) {
			ConfigurationWarnings.add(this, log, "liquibase failed to initialize, error connecting to database ["+getDatasourceName()+"]", e);
		}
	}

	private DatabaseConnection getDatabaseConnection() throws SQLException, ConfigurationException {
		DataSource datasource = lookupMigratorDatasource();
		return new JdbcConnection(datasource.getConnection());
	}

	private void validate() throws LiquibaseException {
		Database database = liquibase.getDatabase();

		LockService lockService = LockServiceFactory.getInstance().getLockService(database);
		lockService.waitForLock();

		DatabaseChangeLog changeLog;

		try {
			changeLog = liquibase.getDatabaseChangeLog();

			liquibase.checkLiquibaseTables(true, changeLog, contexts, labelExpression);
			ChangeLogHistoryServiceFactory.getInstance().getChangeLogService(database).generateDeploymentId();

			changeLog.validate(database, contexts, labelExpression);
		} finally {
			try {
				lockService.releaseLock();
			} catch (LockException e) {
				log.warn("unable to clean up Liquibase Lock", e);
			}

			LockServiceFactory.getInstance().resetAll();
			ChangeLogHistoryServiceFactory.getInstance().resetAll();
			Scope.getCurrentScope().getSingleton(ExecutorService.class).reset();
			liquibase.setChangeExecListener(null);
		}
	}

	@Override
	public void doUpdate() throws JdbcException {
		List<String> changes = new ArrayList<>();
		try {
			List<ChangeSet> changeSets = liquibase.listUnrunChangeSets(contexts, labelExpression);
			for (ChangeSet changeSet : changeSets) {
				changes.add("LiquiBase applying change ["+changeSet.getId()+":"+changeSet.getAuthor()+"] description ["+changeSet.getDescription()+"]");
			}

			if(!changeSets.isEmpty()) {
				liquibase.update(contexts);

				ChangeSet lastChange = changeSets.get(changeSets.size()-1);
				String tag = lastChange.getId() + ":" + lastChange.getAuthor();
				tag(tag);

				if(changes.size() > 1) {
					logConfigurationMessage("LiquiBase applied ["+changes.size()+"] change(s) and added tag ["+tag+"]");
				}
				else {
					for (String change : changes) {
						logConfigurationMessage(change + " tag ["+tag+"]");
					}
				}
			}
		}
		catch (Exception e) {
			String errorMsg = "Error running LiquiBase update. Failed to execute ["+changes.size()+"] change(s): ";
			errorMsg += e.getMessage();
			throw new JdbcException(errorMsg, e);
		}
	}

	public void rollback(String tagName) throws LiquibaseException {
		liquibase.rollback(tagName, contexts);
	}

	public void tag(String tagName) throws LiquibaseException {
		liquibase.tag(tagName);
	}

	@Override
	public void update(Writer writer) throws JdbcException {
		try {
			liquibase.update(contexts, labelExpression, writer);
		} catch (Exception e) {
			throw new JdbcException("unable to generate database migration script", e);
		}
	}

	@Override
	protected void doClose() throws LiquibaseException {
		if(liquibase != null) {
			liquibase.close();
		}
	}

	@Override
	public boolean isEnabled() {
		try {
			getChangeLogFile();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}