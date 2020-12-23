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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.doc.IbisDoc;

/**
 * Database Listener that operates on a table having at least a key and a status field.
 *
 * @since   4.7
 */
public class JdbcTableListener extends JdbcListener implements IProvidesMessageBrowsers<Object> {
	
	private String tableName;
	private String statusField;
	private String orderField;
	private String timestampField;
	private String selectCondition;
	
	private String statusValueAvailable;
	private String statusValueInProcess;
	private String statusValueProcessed;
	private String statusValueError;
	private String statusValueHold;

	private Set<ProcessState> knownProcessStates;
	private Map<ProcessState,Set<ProcessState>> targetProcessStates = new HashMap<>();

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getTableName())) {
			throw new ConfigurationException(getLogPrefix()+"must specify tableName");
		}
		if (StringUtils.isEmpty(getKeyField())) {
			throw new ConfigurationException(getLogPrefix()+"must specify keyField");
		}
		if (StringUtils.isEmpty(getStatusField())) {
			throw new ConfigurationException(getLogPrefix()+"must specify statusField");
		}
		if (StringUtils.isEmpty(getMessageField())) {
			log.info(getLogPrefix()+"has no messageField specified. Will use keyField as messageField, too");
		}
		if (StringUtils.isEmpty(getStatusValueError())) {
			throw new ConfigurationException(getLogPrefix()+"must specify statusValueError");
		}
		if (StringUtils.isEmpty(getStatusValueProcessed())) {
			throw new ConfigurationException(getLogPrefix()+"must specify statusValueProcessed");
		}
		setSelectQuery("SELECT "+getKeyField() + (StringUtils.isNotEmpty(getMessageField())?","+getMessageField():"")+
						" FROM "+getTableName()+
						" WHERE "+getStatusField()+
						(StringUtils.isNotEmpty(getStatusValueAvailable())?
						 "='"+getStatusValueAvailable()+"'":
						 " NOT IN ('"+getStatusValueError()+"','"+getStatusValueProcessed()+"')")+
						(StringUtils.isNotEmpty(getSelectCondition()) ? " AND ("+getSelectCondition()+")": "") +
						 (StringUtils.isNotEmpty(getOrderField())? " ORDER BY "+getOrderField():""));
		setUpdateStatusToProcessedQuery(getUpdateStatusQuery(getStatusValueProcessed(),null));
		setUpdateStatusToErrorQuery(getUpdateStatusQuery(getStatusValueError(),null)); 
		if (StringUtils.isNotEmpty(getStatusValueInProcess())) {
			setUpdateStatusToInProcessQuery(getUpdateStatusQuery(getStatusValueInProcess(),null)); 
			setRevertInProcessStatusQuery(getUpdateStatusQuery(getStatusValueAvailable(),null));
		}
		super.configure();
		if (StringUtils.isEmpty(getStatusValueInProcess()) && !getDbmsSupport().hasSkipLockedFunctionality()) {
			ConfigurationWarnings.add(this, log, "Database ["+getDbmsSupport().getDbmsName()+"] needs statusValueInProcess to run in multiple threads");
		}
		knownProcessStates = ProcessState.getMandatoryKnownStates();
		for (ProcessState state: ProcessState.values()) {
			if (StringUtils.isNotEmpty(getStatusValue(state))) {
				knownProcessStates.add(state);
			}
		}
		targetProcessStates = ProcessState.getTargetProcessStates(knownProcessStates);
	}

	protected String getUpdateStatusQuery(String fieldValue, String additionalSetClause) {
		return "UPDATE "+getTableName()+ 
				" SET "+getStatusField()+"='"+fieldValue+"'"+
				(StringUtils.isNotEmpty(getTimestampField())?","+getTimestampField()+"="+getDbmsSupport().getSysDate():"")+
				(StringUtils.isNotEmpty(additionalSetClause)?","+additionalSetClause:"")+
				" WHERE "+getKeyField()+"=?";
	}

	@Override
	public IMessageBrowser<Object> getMessageBrowser(ProcessState state) {
		String statusValue = getStatusValue(state);
		if (StringUtils.isEmpty(statusValue)) {
			return null;
		}
		return new JdbcTableMessageBrowser<Object>(this, statusValue, getStorageType(state));
	}


	@Override
	public Set<ProcessState> knownProcessStates() {
		return knownProcessStates;
	}

	@Override
	public Map<ProcessState,Set<ProcessState>> targetProcessStates() {
		return targetProcessStates;
	}

	@Override
	public void moveToProcessState(Object rawMessage, ProcessState toState, Map<String,Object> context) throws ListenerException {
		String query = getUpdateStatusQuery(getStatusValue(toState), null);
		String key=getIdFromRawMessage(rawMessage,context);
		if (isConnectionsArePooled()) {
			try (Connection conn = getConnection()) {
				execute(conn, query, key);
			} catch (JdbcException|SQLException e) {
				throw new ListenerException(e);
			}
		} else {
			synchronized (connection) {
				execute(connection, query, key);
			}
		}
	}

	public String getStatusValue(ProcessState state) {
		switch (state) {
		case AVAILABLE:
			return getStatusValueAvailable();
		case INPROCESS:
			return getStatusValueInProcess();
		case DONE:
			return getStatusValueProcessed();
		case ERROR:
			return getStatusValueError();
		case HOLD:
			return getStatusValueHold();
		default:
			throw new IllegalStateException("Unknown state ["+state+"]");
		}
	}
	
	public IMessageBrowser.StorageType getStorageType(ProcessState state) {
		switch (state) {
		case AVAILABLE:
		case INPROCESS:
		case DONE:
			return IMessageBrowser.StorageType.MESSAGELOG_RECEIVER;
		case ERROR:
		case HOLD:
			return IMessageBrowser.StorageType.ERRORSTORAGE;
		default:
			throw new IllegalStateException("Unknown state ["+state+"]");
		}
	}
	


	@Override
	public String getPhysicalDestinationName() {
		return super.getPhysicalDestinationName()+" "+getTableName();
	}



	@IbisDoc({"1", "Name of the table to be used", ""})
	public void setTableName(String string) {
		tableName = string;
	}
	public String getTableName() {
		return tableName;
	}

	@IbisDoc({"4", "Field containing the status of the message", ""})
	public void setStatusField(String fieldname) {
		statusField = fieldname;
	}
	public String getStatusField() {
		return statusField;
	}

	@IbisDoc({"5", "(optional) Field determining the order in which messages are processed", ""})
	public void setOrderField(String string) {
		orderField = string;
	}
	public String getOrderField() {
		return orderField;
	}

	@IbisDoc({"6", "(optional) Field used to store the date and time of the last change of the statusField", ""})
	public void setTimestampField(String fieldname) {
		timestampField = fieldname;
	}
	public String getTimestampField() {
		return timestampField;
	}

	@IbisDoc({"7", "(optional) Value of statusField indicating row is available to be processed. If not specified, any row not having any of the other status values is considered available.", ""})
	public void setStatusValueAvailable(String string) {
		statusValueAvailable = string;
	}
	public String getStatusValueAvailable() {
		return statusValueAvailable;
	}

	@IbisDoc({"8", "Value of statusField indicating the processing of the row resulted in an error", ""})
	public void setStatusValueError(String string) {
		statusValueError = string;
	}
	public String getStatusValueError() {
		return statusValueError;
	}

	@IbisDoc({"9", "Value of status field indicating row is processed ok", ""})
	public void setStatusValueProcessed(String string) {
		statusValueProcessed = string;
	}
	public String getStatusValueProcessed() {
		return statusValueProcessed;
	}

	@IbisDoc({"10", "Value of status field indicating is being processed. Can be left emtpy if database has SKIP LOCKED functionality", ""})
	public void setStatusValueInProcess(String string) {
		statusValueInProcess = string;
	}
	public String getStatusValueInProcess() {
		return statusValueInProcess;
	}

	@IbisDoc({"11", "Value of status field indicating message is on Hold, temporarily", ""})
	public void setStatusValueHold(String string) {
		statusValueHold = string;
	}
	public String getStatusValueHold() {
		return statusValueHold;
	}

	@IbisDoc({"12", "Additional condition for a row to belong to this TableListener", ""})
	public void setSelectCondition(String string) {
		selectCondition = string;
	}
	public String getSelectCondition() {
		return selectCondition;
	}

}
