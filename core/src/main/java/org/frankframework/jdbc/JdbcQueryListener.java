/*
   Copyright 2013 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package org.frankframework.jdbc;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.ProcessState;

/**
 * Database Listener that operates on a table having at least a key and a status field.
 *
 * @since   4.7
 */
@Deprecated(forRemoval = true, since = "7.6.0")
@ConfigurationWarning("Please replace with JdbcTableListener for ease of configuration and improved manageability")
public class JdbcQueryListener extends JdbcListener {

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getSelectQuery())) {
			throw new ConfigurationException("selectQuery must be specified");
		}
		if (!knownProcessStates().contains(ProcessState.DONE)) {
			throw new ConfigurationException("updateStatusToProcessedQuery must be specified");
		}
		if (StringUtils.isEmpty(getKeyField())) {
			throw new ConfigurationException("keyField must be specified");
		}
		if (!knownProcessStates().contains(ProcessState.ERROR)) {
			log.info("{}has no updateStatusToErrorQuery specified, will use updateStatusToProcessedQuery instead", getLogPrefix());
			setUpdateStatusQuery(ProcessState.ERROR,getUpdateStatusQuery(ProcessState.DONE));
		}
		super.configure();
		if (!knownProcessStates().contains(ProcessState.INPROCESS) && !getDbmsSupport().hasSkipLockedFunctionality()) {
			ConfigurationWarnings.add(this, log, "Database ["+getDbmsSupport().getDbmsName()+"] needs updateStatusToInProcessQuery to run in multiple threads");
		}
	}

	/**
	 * Query that returns a row to be processed. Must contain a key field and optionally a message field.
	 * @ff.mandatory
	 */
	@Override
	public void setSelectQuery(String string) {
		super.setSelectQuery(string);
	}

	/**
	 * SQL statement to set the status of a row to 'processed'. Must contain one parameter, that is set to the value of the key
	 * @ff.mandatory
	 */
	public void setUpdateStatusToProcessedQuery(String query) {
		setUpdateStatusQuery(ProcessState.DONE, query);
	}

	/**
	 * SQL statement to set the status of a row to 'error'. Must contain one parameter, that is set to the value of the key
	 * @ff.default same as <code>updateStatusToProcessedQuery</code>
	 */
	public void setUpdateStatusToErrorQuery(String query) {
		setUpdateStatusQuery(ProcessState.ERROR, query);
	}

	/**
	 * SQL statement to set the status of a row to 'in process'. Must contain one parameter, that is set to the value of the key.
	 * Can be left emtpy if database has SKIP LOCKED functionality and the Receiver can be (and is) set to Required or RequiresNew.
	 */
	public void setUpdateStatusToInProcessQuery(String query) {
		setUpdateStatusQuery(ProcessState.INPROCESS, query);
	}

	/**
	 * SQL statement to set the status of a row to 'available'. Must contain one parameter, that is set to the value of the key.
	 * Only used in rollbacks, when updateStatusToInProcessQuery is specified
	 */
	public void setRevertInProcessStatusQuery(String query) {
		setUpdateStatusQuery(ProcessState.AVAILABLE, query);
	}

}
