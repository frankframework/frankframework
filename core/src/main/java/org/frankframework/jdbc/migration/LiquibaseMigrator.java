/*
   Copyright 2017-2022 WeAreFrank!

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
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.change.CheckSum;
import liquibase.changelog.ChangeLogHistoryService;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.RanChangeSet;
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

import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.classloaders.AbstractClassLoader;
import org.frankframework.core.Resource;
import org.frankframework.dbms.JdbcException;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

/**
 * LiquiBase implementation for IAF
 *
 * @author	Niels Meijer
 * @since	7.0-B4
 *
 */
public class LiquibaseMigrator extends AbstractDatabaseMigrator {

	protected Logger migrationLog = LogUtil.getLogger("liquibase.migrationLog");
	private Contexts contexts;
	private final LabelExpression labelExpression = new LabelExpression();

	@Override
	public Resource getChangeLog() {
		AppConstants appConstants = AppConstants.getInstance(getConfigurationClassLoader());
		String changeLogFile = appConstants.getString("liquibase.changeLogFile", "DatabaseChangelog.xml");

		ClassLoader classLoader = getConfigurationClassLoader();
		if (classLoader instanceof AbstractClassLoader base) {
			URL url = base.getResource(changeLogFile, false);
			if (url==null) {
				log.debug("database changelog file [{}] not found as local resource of classLoader [{}]", changeLogFile, classLoader);
				return null;
			}
		}

		Resource resource = Resource.getResource(this, changeLogFile);
		if(resource == null) {
			log.debug("unable to find database changelog file [{}] in classLoader [{}]", changeLogFile, classLoader);
			return null;
		}
		return resource;
	}

	private Liquibase createMigrator() throws SQLException, LiquibaseException {
		return createMigrator(getChangeLog());
	}

	private Liquibase createMigrator(Resource resource) throws SQLException, LiquibaseException {
		if(resource == null) {
			throw new LiquibaseException("no resource provided");
		}

		try (ResourceAccessor resourceAccessor = new LiquibaseResourceAccessor(resource)) {
			DatabaseConnection connection = getDatabaseConnection();

			return new Liquibase(resource.getSystemId(), resourceAccessor, connection);
		} catch (Exception e) {
			throw new LiquibaseException("unable to close ResourceAccessor", e);
		}
	}

	private DatabaseConnection getDatabaseConnection() throws SQLException {
		return new JdbcConnection(lookupMigratorDatasource().getConnection());
	}

	@Override
	public boolean validate() {
		try {
			if(hasMigrationScript()) {
				doValidate();
				return true;
			}
		}
		catch (ValidationFailedException e) {
			ConfigurationWarnings.add(this, log, "liquibase validation failed: "+e.getMessage(), e);
		}
		catch (LiquibaseException e) {
			ConfigurationWarnings.add(this, log, "liquibase failed to initialize", e);
		}
		catch (SQLException e) {
			ConfigurationWarnings.add(this, log, "liquibase failed to initialize, error connecting to database ["+getDatasourceName()+"]", e);
		}
		return false;
	}

	private void doValidate() throws LiquibaseException, SQLException {
		try (Liquibase liquibase = createMigrator()) {
			Database database = liquibase.getDatabase();

			LockService lockService = LockServiceFactory.getInstance().getLockService(database);
			lockService.waitForLock();

			List<RanChangeSet> alreadyExecutedChangeSets = database.getRanChangeSetList();
			for(RanChangeSet ranChangeSet : alreadyExecutedChangeSets) {
				CheckSum checkSum = ranChangeSet.getLastCheckSum();
				if(checkSum != null && checkSum.getVersion() < CheckSum.getCurrentVersion()) {
					migrationLog.warn("checksum [{}] for changeset [{}] is outdated and will be updated", checkSum, ranChangeSet);
				}
			}

			DatabaseChangeLog changeLog;
			try {
				changeLog = liquibase.getDatabaseChangeLog();
				ChangeLogHistoryService changeLogHistoryService = ChangeLogHistoryServiceFactory.getInstance().getChangeLogService(database);
				changeLogHistoryService.init();
				changeLogHistoryService.upgradeChecksums(changeLog, contexts, labelExpression); //Validate old checksums and update if required
				changeLogHistoryService.reset();

				changeLog.validate(database, contexts, labelExpression); //Validate the new (updated) checksums
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
	}

	@Override
	public void update() {
		List<String> changes = new ArrayList<>();
		try (Liquibase liquibase = createMigrator()) {
			// Don't show "UPDATE SUMMARY" in System.out
			liquibase.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG);
			List<ChangeSet> changeSets = liquibase.listUnrunChangeSets(contexts, labelExpression);
			for (ChangeSet changeSet : changeSets) {
				changes.add("LiquiBase applying change ["+changeSet.getId()+":"+changeSet.getAuthor()+"] description ["+changeSet.getDescription()+"]");
			}

			if(!changeSets.isEmpty()) {
				liquibase.update(contexts);

				ChangeSet lastChange = changeSets.get(changeSets.size()-1);
				String tag = lastChange.getId() + ":" + lastChange.getAuthor();
				liquibase.tag(tag);

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
			ConfigurationWarnings.add(this, log, errorMsg, e);
		}
	}

	@Override
	public void update(Writer writer) throws JdbcException {
		update(writer, getChangeLog());
	}

	@Override
	public void update(Writer writer, Resource resource) throws JdbcException {
		try (Liquibase migrator = createMigrator(resource)) {
			migrator.update(contexts, labelExpression, writer);
		} catch (Exception e) {
			throw new JdbcException("unable to generate database migration script", e);
		}
	}
}
