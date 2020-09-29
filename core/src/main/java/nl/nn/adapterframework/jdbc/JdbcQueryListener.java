/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.jdbc;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.doc.IbisDoc;

/**

/**
 * Database Listener that operates on a table having at least a key and a status field.
 *
 * @since   4.7
 */
public class JdbcQueryListener extends JdbcListener {

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getSelectQuery())) {
			throw new ConfigurationException("selectQuery must be specified");
		}
		if (StringUtils.isEmpty(getUpdateStatusToProcessedQuery())) {
			throw new ConfigurationException("updateStatusToProcessedQuery must be specified");
		}
		if (StringUtils.isEmpty(getKeyField())) {
			throw new ConfigurationException("keyField must be specified");
		}
		if (StringUtils.isEmpty(getUpdateStatusToErrorQuery())) {
			log.info(getLogPrefix()+"has no updateStatusToErrorQuery specified, will use updateStatusToProcessedQuery instead");
			setUpdateStatusToErrorQuery(getUpdateStatusToProcessedQuery());
		}
		super.configure();
		if (StringUtils.isEmpty(getUpdateStatusToInProcessQuery()) && !getDbmsSupport().hasSkipLockedFunctionality()) {
			ConfigurationWarnings.add(this, log, "Database ["+getDbmsSupport().getDbmsName()+"] needs updateStatusToInProcessQuery to run in multiple threads");
		}
	}
	

	@Override
	@IbisDoc({"1", "Primary key field of the table, used to identify messages", ""})
	public void setKeyField(String fieldname) {
		super.setKeyField(fieldname);
	}

	@Override
	@IbisDoc({"2", "(Optional) field containing the message data", "<i>same as keyField</i>"})
	public void setMessageField(String fieldname) {
		super.setMessageField(fieldname);
	}

	@Override
	@IbisDoc({"3", "Query that returns a row to be processed. Must contain a key field and optionally a message field", ""})
	public void setSelectQuery(String string) {
		super.setSelectQuery(string);
	}

	@Override
	@IbisDoc({"SQL statement to set the status of a row to 'processed'. Must contain one parameter, that is set to the value of the key", ""})
	public void setUpdateStatusToProcessedQuery(String string) {
		super.setUpdateStatusToProcessedQuery(string);
	}

	@Override
	@IbisDoc({"SQL statement to set the status of a row to 'error'. Must contain one parameter, that is set to the value of the key", "same as <code>updateStatusToProcessedQuery</code>"})
	public void setUpdateStatusToErrorQuery(String string) {
		super.setUpdateStatusToErrorQuery(string);
	}

	@Override
	@IbisDoc({"SQL statement to set the status of a row to 'in process'. Must contain one parameter, that is set to the value of the key. Can be left emtpy if database has SKIP LOCKED functionality", ""})
	public void setUpdateStatusToInProcessQuery(String string) {
		super.setUpdateStatusToInProcessQuery(string);
	}

	@Override
	@IbisDoc({"SQL statement to set the status of a row to 'available'. Must contain one parameter, that is set to the value of the key. Only use in rollbacks, when updateStatusToInProcessQuery is specified", ""})
	public void setRevertInProcessStatusQuery(String string) {
		super.setRevertInProcessStatusQuery(string);
	}


}
