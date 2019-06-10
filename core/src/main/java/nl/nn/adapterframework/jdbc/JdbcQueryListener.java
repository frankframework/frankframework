/*
   Copyright 2013 Nationale-Nederlanden

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

import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;



/** 
 * @since   4.7
 */
@IbisDescription(
	"Database Listener that operates on a table having at least a key and a status field. \n" 
)
public class JdbcQueryListener extends JdbcListener {

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
	}
	

	@IbisDoc({"primary key field of the table, used to identify messages", ""})
	public void setKeyField(String fieldname) {
		super.setKeyField(fieldname);
	}

	@IbisDoc({"(optional) field containing the message data", "<i>same as keyfield</i>"})
	public void setMessageField(String fieldname) {
		super.setMessageField(fieldname);
	}

	@IbisDoc({"query that returns a row to be processed. must contain a key field and optionally a message field", ""})
	public void setSelectQuery(String string) {
		super.setSelectQuery(string);
	}

	@IbisDoc({"sql statement to the status of a row to 'error'. must contain one parameter, that is set to the value of the key", "same as <code>updatestatustoprocessedquery</code>"})
	public void setUpdateStatusToErrorQuery(String string) {
		super.setUpdateStatusToErrorQuery(string);
	}

	@IbisDoc({"sql statement to the status of a row to 'processed'. must contain one parameter, that is set to the value of the key", ""})
	public void setUpdateStatusToProcessedQuery(String string) {
		super.setUpdateStatusToProcessedQuery(string);
	}


}
