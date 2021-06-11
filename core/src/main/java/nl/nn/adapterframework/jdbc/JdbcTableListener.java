/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.doc.IbisDoc;

/**
 * Database Listener that operates on a table having at least a key and a status field.
 *
 * @since   4.7
 */
public class JdbcTableListener<M> extends JdbcListener<M> implements IProvidesMessageBrowsers<M> {
	
	private String tableName;
	private String tableAlias="t";
	private String statusField;
	private String orderField;
	private String timestampField;
	private String commentField;
	private String selectCondition;
	private int maxCommentLength=ITransactionalStorage.MAXCOMMENTLEN;
	
	private Map<ProcessState, String> statusValues = new HashMap<>();

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
		if (StringUtils.isEmpty(getStatusValue(ProcessState.ERROR))) {
			throw new ConfigurationException(getLogPrefix()+"must specify statusValueError");
		}
		if (StringUtils.isEmpty(getStatusValue(ProcessState.DONE))) {
			throw new ConfigurationException(getLogPrefix()+"must specify statusValueProcessed");
		}
		String alias = StringUtils.isNotBlank(getTableAlias())?getTableAlias().trim():"";
		setSelectQuery("SELECT "+getKeyField() + (StringUtils.isNotEmpty(getMessageField())?","+getMessageField():"")+
						" FROM "+getTableName()+" "+ alias +
						" WHERE "+getStatusField()+
						(StringUtils.isNotEmpty(getStatusValue(ProcessState.AVAILABLE))?
						 "='"+getStatusValue(ProcessState.AVAILABLE)+"'":
						 " NOT IN ('"+getStatusValue(ProcessState.ERROR)+"','"+getStatusValue(ProcessState.DONE)+(StringUtils.isNotEmpty(getStatusValue(ProcessState.HOLD))?"','"+getStatusValue(ProcessState.HOLD):"")+"')")+
						(StringUtils.isNotEmpty(getSelectCondition()) ? " AND ("+getSelectCondition()+")": "") +
						(StringUtils.isNotEmpty(getOrderField())? " ORDER BY "+getOrderField():""));
		statusValues.forEach((state, value) -> setUpdateStatusQuery(state, "dummy query to register status value in JdbcListener")); // must have set updateStatusQueries before calling super.configure()
		super.configure();
		statusValues.forEach((state, value) -> setUpdateStatusQuery(state, createUpdateStatusQuery(value, null))); // set proper updateStatusQueries using createUpdateStatusQuery() after configure has been called();
		if (StringUtils.isEmpty(getStatusValue(ProcessState.INPROCESS)) && !getDbmsSupport().hasSkipLockedFunctionality()) {
			ConfigurationWarnings.add(this, log, "Database ["+getDbmsSupport().getDbmsName()+"] needs statusValueInProcess to run in multiple threads");
		}
		
	}

	protected String createUpdateStatusQuery(String fieldValue, String additionalSetClause) {
		return "UPDATE "+getTableName()+ 
				" SET "+getStatusField()+"='"+fieldValue+"'"+
				(StringUtils.isNotEmpty(getTimestampField())?","+getTimestampField()+"="+getDbmsSupport().getSysDate():"")+
				(StringUtils.isNotEmpty(getCommentField())?","+getCommentField()+"=?":"")+
				(StringUtils.isNotEmpty(additionalSetClause)?","+additionalSetClause:"")+
				" WHERE "+getStatusField()+"!='"+fieldValue+"' AND "+getKeyField()+"=?";
	}

	@Override
	protected M changeProcessState(Connection connection, M rawMessage, ProcessState toState, String reason) throws ListenerException {
		String query = getUpdateStatusQuery(toState);
		String key=getIdFromRawMessage(rawMessage, null);
		List<String> parameters = new ArrayList<>();
		if (StringUtils.isNotEmpty(getCommentField()) && query.substring(query.indexOf('?')+1).contains("?")) {
			if (getMaxCommentLength()>=0 && reason.length()>getMaxCommentLength()) {
				parameters.add(reason.substring(0, getMaxCommentLength()));
			} else {
				parameters.add(reason);
			}
		}
		parameters.add(key);
		return execute(connection, query, parameters.toArray(new String[parameters.size()])) ? rawMessage : null;
	}

	
	@Override
	public IMessageBrowser<M> getMessageBrowser(ProcessState state) {
		if (!knownProcessStates().contains(state)) {
			return null;
		}
		JdbcTableMessageBrowser<M> browser = new JdbcTableMessageBrowser<M>(this, getStatusValue(state), getStorageType(state));
		if (StringUtils.isNotEmpty(getCommentField())) {
			browser.setCommentField(commentField);
		}
		return browser;
	}


	public String getStatusValue(ProcessState state) {
		return statusValues.get(state);
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
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessageOrWrapper, Map<String,Object> context) throws ListenerException {
		// skip moving message to DONE or ERROR, as this is now performed by Receiver calling changeProcessState()
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

	@IbisDoc({"2", "Alias of the table, that can be used in selectCondition", "t"})
	public void setTableAlias(String string) {
		tableAlias = string;
	}
	public String getTableAlias() {
		return tableAlias;
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

	@IbisDoc({"6", "(optional) Field used to store the reason of the last change of the statusField", ""})
	public void setCommentField(String commentField) {
		this.commentField = commentField;
	}
	public String getCommentField() {
		return commentField;
	}

	@IbisDoc({"6", "(optional) Maximum length of strings to be stored in commentField, or -1 for unlimited", ITransactionalStorage.MAXCOMMENTLEN+""})
	public void setMaxCommentLength(int maxCommentLength) {
		this.maxCommentLength = maxCommentLength;
	}
	public int getMaxCommentLength() {
		return maxCommentLength;
	}

	@IbisDoc({"7", "(optional) Value of statusField indicating row is available to be processed. If not specified, any row not having any of the other status values is considered available.", ""})
	public void setStatusValueAvailable(String string) {
		statusValues.put(ProcessState.AVAILABLE,string);
	}

	@IbisDoc({"8", "Value of statusField indicating the processing of the row resulted in an error", ""})
	public void setStatusValueError(String string) {
		statusValues.put(ProcessState.ERROR, string);
	}

	@IbisDoc({"9", "Value of status field indicating row is processed ok", ""})
	public void setStatusValueProcessed(String string) {
		statusValues.put(ProcessState.DONE, string);
	}

	@IbisDoc({"10", "Value of status field indicating is being processed. Can be left emtpy if database has SKIP LOCKED functionality and the Receiver can be (and is) set to Required or RequiresNew.", ""})
	public void setStatusValueInProcess(String string) {
		statusValues.put(ProcessState.INPROCESS, string);
	}

	@IbisDoc({"11", "Value of status field indicating message is on Hold, temporarily", ""})
	public void setStatusValueHold(String string) {
		statusValues.put(ProcessState.HOLD, string);
	}

	@IbisDoc({"12", "Additional condition for a row to belong to this TableListener", ""})
	public void setSelectCondition(String string) {
		selectCondition = string;
	}
	public String getSelectCondition() {
		return selectCondition;
	}

}
