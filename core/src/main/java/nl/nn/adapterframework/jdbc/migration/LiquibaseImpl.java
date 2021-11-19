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
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.logging.LogFactory;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.LogUtil;

/**
 * LiquiBase implementation for IAF
 * 
 * @author	Niels Meijer
 * @since	7.0-B4
 *
 */
public class LiquibaseImpl {

	private Liquibase liquibase = null;
	private Contexts contexts;
	private LabelExpression labelExpression = new LabelExpression();
	private IbisContext ibisContext = null;
	private Configuration configuration = null;
	protected Logger log = LogUtil.getLogger(this);

	public LiquibaseImpl(IbisContext ibisContext, DataSource datasource, Configuration configuration, String changeLogFile) throws LiquibaseException, SQLException, IOException {
		this.ibisContext = ibisContext;
		this.configuration = configuration;

		LiquibaseResourceAccessor resourceAccessor = new LiquibaseResourceAccessor(configuration.getClassLoader());
		if(resourceAccessor.getResource(changeLogFile) == null) {
			String msg = "unable to find database changelog file [" + changeLogFile + "]";
			msg += " classLoader [" + configuration.getClassLoader() + "]";
			throw new IOException(msg);
		}

		JdbcConnection connection = new JdbcConnection(datasource.getConnection());
		// Override liquibase internal logger
		LogFactory.setInstance(new LogFactory() {

			@Override
			public liquibase.logging.Logger getLog(String name) {
				liquibase.logging.Logger log = new LiquibaseLogger(name);
				return log;
			}
		});

		this.liquibase = new Liquibase(changeLogFile, resourceAccessor, connection);
		this.liquibase.validate();
	}

	private void log(String message) {
		if(ibisContext != null) 
			ibisContext.log(configuration.getName(), null, message);
	}

	public void update() {
		List<String> changes = new ArrayList<String>();
		try {
			List<ChangeSet> changeSets = liquibase.listUnrunChangeSets(contexts, labelExpression);
			for (ChangeSet changeSet : changeSets) {
				changes.add("LiquiBase applying change ["+changeSet.getId()+":"+changeSet.getAuthor()+"] description ["+changeSet.getDescription()+"]");
			}

			if(changeSets.size() > 0) {
				liquibase.update(contexts);

				ChangeSet lastChange = changeSets.get(changeSets.size()-1);
				String tag = lastChange.getId() + ":" + lastChange.getAuthor();
				tag(tag);

				if(changes.size() > 1) {
					log("LiquiBase applied ["+changes.size()+"] change(s) and added tag ["+tag+"]");
				}
				else {
					for (String change : changes) {
						log(change + " tag ["+tag+"]");
					}
				}
			}
		}
		catch (Exception e) {
			String errorMsg = "Error running LiquiBase update. Failed to execute ["+changes.size()+"] change(s): ";
			errorMsg += e.getMessage();
			ConfigurationWarnings.add(configuration, log, errorMsg, e);
		}
	}

	public void rollback(String tagName) throws LiquibaseException {
		if(liquibase != null) {
			liquibase.rollback(tagName, contexts);
		}
	}

	public void tag(String tagName) throws LiquibaseException {
		if(liquibase != null) {
			liquibase.tag(tagName);
		}
	}

	public Writer getUpdateScript(Writer writer) throws LiquibaseException {
		if(liquibase != null) {
			liquibase.update(contexts, labelExpression, writer);
		}
		return writer;
	}

	public void close() throws DatabaseException {
		if(liquibase != null) {
			Database db = liquibase.getDatabase();
			if(db != null) {
				db.close();
			}
		}
	}
}
