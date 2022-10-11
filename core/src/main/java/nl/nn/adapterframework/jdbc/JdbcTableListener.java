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
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;

/**
 * Database Listener that operates on a table having at least a key and a status field.
 *
 * @since   4.7
 */
public class JdbcTableListener<M> extends JdbcListener<M> implements IProvidesMessageBrowsers<M> {

	private @Getter String tableName;
	private @Getter String tableAlias="t";
	private @Getter String statusField;
	private @Getter String orderField;
	private @Getter String timestampField;
	private @Getter String commentField;
	private @Getter String selectCondition;
	private @Getter int maxCommentLength=ITransactionalStorage.MAXCOMMENTLEN;

	private Map<ProcessState, String> statusValues = new HashMap<>();

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getTableName())) {
			throw new ConfigurationException("must specify tableName");
		}
		if (StringUtils.isEmpty(getKeyField())) {
			throw new ConfigurationException("must specify keyField");
		}
		if (StringUtils.isEmpty(getStatusField())) {
			throw new ConfigurationException("must specify statusField");
		}
		if (StringUtils.isEmpty(getMessageField())) {
			log.info(getLogPrefix()+"has no messageField specified. Will use keyField as messageField, too");
		}
		if (StringUtils.isEmpty(getStatusValue(ProcessState.ERROR))) {
			throw new ConfigurationException("must specify statusValueError");
		}
		if (StringUtils.isEmpty(getStatusValue(ProcessState.DONE))) {
			throw new ConfigurationException("must specify statusValueProcessed");
		}
		String alias = StringUtils.isNotBlank(getTableAlias())?getTableAlias().trim():"";
		setSelectQuery("SELECT "+getKeyField() +
								(StringUtils.isNotEmpty(getMessageIdField())?","+getMessageIdField():"")+(StringUtils.isNotEmpty(getCorrelationIdField())?","+getCorrelationIdField():"")+
								(StringUtils.isNotEmpty(getMessageField())?","+getMessageField():"") +
						" FROM "+getTableName() + (StringUtils.isNotBlank(tableAlias)?" "+tableAlias.trim():"") +
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
		String key=getKeyFromRawMessage(rawMessage);
		List<String> parameters = new ArrayList<>();
		if (StringUtils.isNotEmpty(getCommentField()) && query.substring(query.indexOf('?')+1).contains("?")) {
			if (getMaxCommentLength()>=0 && reason!=null && reason.length()>getMaxCommentLength()) {
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
		JdbcTableMessageBrowser<M> browser = new JdbcTableMessageBrowser<>(this, getStatusValue(state), getStorageType(state));
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
	public String getPhysicalDestinationName() {
		return super.getPhysicalDestinationName()+" "+getTableName();
	}



	/**
	 * Name of the table to be used
	 * @ff.mandatory
	 */
	public void setTableName(String string) {
		tableName = string;
	}

	/**
	 * Alias of the table, that can be used in selectCondition
	 * @ff.default t
	 */
	public void setTableAlias(String string) {
		tableAlias = string;
	}

	/**
	 * Field containing the status of the message
	 * @ff.mandatory
	 */
	public void setStatusField(String fieldname) {
		statusField = fieldname;
	}

	/**
	 * (optional) Comma separated list of fields determining the order in which messages are processed
	 */
	public void setOrderField(String string) {
		orderField = string;
	}

	/**
	 * (optional) Field used to store the date and time of the last change of the statusField
	 */
	public void setTimestampField(String fieldname) {
		timestampField = fieldname;
	}

	/**
	 * (optional) Field used to store the reason of the last change of the statusField
	 */
	public void setCommentField(String commentField) {
		this.commentField = commentField;
	}

	/**
	 * (optional) Maximum length of strings to be stored in commentField, or -1 for unlimited
	 * @ff.default 1000
	 */
	public void setMaxCommentLength(int maxCommentLength) {
		this.maxCommentLength = maxCommentLength;
	}

	/**
	 * (optional) Value of statusField indicating row is available to be processed. If not specified, any row not having any of the other status values is considered available.
	 */
	public void setStatusValueAvailable(String string) {
		statusValues.put(ProcessState.AVAILABLE,string);
	}

	/**
	 * Value of statusField indicating the processing of the row resulted in an error
	 * @ff.mandatory
	 */
	public void setStatusValueError(String string) {
		statusValues.put(ProcessState.ERROR, string);
	}

	/**
	 * Value of status field indicating row is processed OK
	 * @ff.mandatory
	 */
	public void setStatusValueProcessed(String string) {
		statusValues.put(ProcessState.DONE, string);
	}

	/**
	 * Value of status field indicating is being processed. Can be left emtpy if database has SKIP LOCKED functionality and the Receiver can be (and is) set to Required or RequiresNew.
	 */
	public void setStatusValueInProcess(String string) {
		statusValues.put(ProcessState.INPROCESS, string);
	}

	/**
	 * Value of status field indicating message is on Hold, temporarily
	 */
	public void setStatusValueHold(String string) {
		statusValues.put(ProcessState.HOLD, string);
	}

	/**
	 * Additional condition for a row to belong to this TableListener. Impacts all process states
	 */
	public void setSelectCondition(String string) {
		selectCondition = string;
	}

}
