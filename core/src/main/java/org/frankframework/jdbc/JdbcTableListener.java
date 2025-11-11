/*
   Copyright 2013 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IProvidesMessageBrowsers;
import org.frankframework.core.ITransactionalStorage;
import org.frankframework.core.ListenerException;
import org.frankframework.core.ProcessState;
import org.frankframework.dbms.JdbcException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.receivers.RawMessageWrapper;

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

	private final Map<ProcessState, String> statusValues = new EnumMap<>(ProcessState.class);

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
			log.info("{}has no messageField specified. Will use keyField as messageField, too", getLogPrefix());
		}
		if (StringUtils.isEmpty(getStatusValue(ProcessState.ERROR))) {
			throw new ConfigurationException("must specify statusValueError");
		}
		if (StringUtils.isEmpty(getStatusValue(ProcessState.DONE))) {
			throw new ConfigurationException("must specify statusValueProcessed");
		}
		if (StringUtils.isNotEmpty(selectCondition)) {
			verifySelectCondition();
		}
		setSelectQuery("SELECT " + getKeyField() +
								(StringUtils.isNotEmpty(getMessageIdField()) ? "," + getMessageIdField() : "") + (StringUtils.isNotEmpty(getCorrelationIdField()) ? "," + getCorrelationIdField() : "") +
								(StringUtils.isNotEmpty(getMessageField()) ? "," + getMessageField() : "") +
								(!getAdditionalFieldsList().isEmpty() ? "," + String.join(",", getAdditionalFieldsList()) : "") +
						" FROM " + getTableName() + (StringUtils.isNotBlank(tableAlias) ? " " + tableAlias.trim() : "") +
						" WHERE " + getStatusField() +
						(StringUtils.isNotEmpty(getStatusValue(ProcessState.AVAILABLE)) ?
						"='" + getStatusValue(ProcessState.AVAILABLE) + "'" :
						" NOT IN ('"+getStatusValue(ProcessState.ERROR) + "','" + getStatusValue(ProcessState.DONE) + (StringUtils.isNotEmpty(getStatusValue(ProcessState.HOLD)) ? "','" + getStatusValue(ProcessState.HOLD) : "") + "')") +
						(StringUtils.isNotEmpty(getSelectCondition()) ? " AND (" + getSelectCondition() + ")" : "") +
						(StringUtils.isNotEmpty(getOrderField()) ? " ORDER BY " + getOrderField() : ""));
		statusValues.forEach((state, value) -> setUpdateStatusQuery(state, "dummy query to register status value in JdbcListener")); // must have set updateStatusQueries before calling super.configure()
		super.configure();
		statusValues.forEach((state, value) -> setUpdateStatusQuery(state, createUpdateStatusQuery(value, null))); // set proper updateStatusQueries using createUpdateStatusQuery() after configure has been called();
		if (StringUtils.isEmpty(getStatusValue(ProcessState.INPROCESS)) && !getDbmsSupport().hasSkipLockedFunctionality()) {
			ConfigurationWarnings.add(this, log, "Database [" + getDbmsSupport().getDbmsName() + "] needs statusValueInProcess to run in multiple threads");
		}
	}

	@Override
	public void start() {
		super.start();
		// Validate this in start not configure, because a broken configuration could otherwise prevent the startup of the entire application.
		validateQueryFields();
	}

	private void validateQueryFields() throws LifecycleException {
		if (!isConnectionsArePooled()) {
			try {
				validateQueryFields(connection);
			} catch (SQLException e) {
				throw new LifecycleException("Error requesting database metadata", e);
			}
		} else {
			try (Connection conn = getConnection()) {
				validateQueryFields(conn);
			} catch (JdbcException | SQLException e) {
				throw new LifecycleException("Error requesting database metadata", e);
			}
		}
	}

	private void validateQueryFields(Connection conn) throws LifecycleException, SQLException {
		Set<String> columnNames = getTableColumnNames(conn, tableName);
		if (columnNames.isEmpty()) {
			columnNames = getTableColumnNames(conn, tableName.toUpperCase());
		}
		if (columnNames.isEmpty()) {
			throw new LifecycleException("Cannot find columns for table [%s]".formatted(tableName));
		}
		Map<String, String> configuredColumnNames = getConfiguredColumnNames();
		configuredColumnNames.values().removeAll(columnNames);
		if (!configuredColumnNames.isEmpty()) {
			throw new LifecycleException("The following columns cannot be found in the schema for table [%s]: [%s]".formatted(tableName, configuredColumnNames));
		}
	}

	@Nonnull
	private Set<String> getTableColumnNames(@Nonnull Connection conn, @Nonnull String table) throws SQLException {
		Set<String> columnNames = new HashSet<>();
		try (ResultSet columns = conn.getMetaData().getColumns(null, null, table, "%")) {
			while (columns.next()) {
				columnNames.add(columns.getString("COLUMN_NAME").toUpperCase());
			}
		}
		return columnNames;
	}

	private Map<String, String> getConfiguredColumnNames() {
		// Some of these fields might be NULL. Set.of() constructor doesn't allow NULL values.
		String[][] columnNames = {{"keyField", getKeyField()}, {"statusField", getStatusField()}, {"messageField", getMessageField()},
				{"messageIdField", getMessageIdField()}, {"correlationIdField", getCorrelationIdField()}, {"commentField", getCommentField()},
				{"statusField", getStatusField()}, {"timestampField", getTimestampField()}, {"orderField", getOrderField()}};

		// We may have additional fields that need to be added and checked, so need to add those.
		Map<String, String> configuredColumnNames = new HashMap<>();
		for (String[] columnDefinition : columnNames) {
			if (columnDefinition[1] != null) {
				configuredColumnNames.put(columnDefinition[0], columnDefinition[1].toUpperCase());
			}
		}
		int i = 0;
		for (String additionalField : getAdditionalFieldsList()) {
			configuredColumnNames.put("AdditionalField" + i++, additionalField.toUpperCase());
		}
		return configuredColumnNames;
	}

	private void verifySelectCondition() {
		verifyFieldNotInQuery(getCommentField(), selectCondition);
		verifyFieldNotInQuery(getTimestampField(), selectCondition);
	}

	protected void verifyFieldNotInQuery(String fieldName, String query) {
		if (StringUtils.isEmpty(fieldName)) return;

		String findFieldRE = "(^|\\W)(" + fieldName + ")(\\W|$)";
		Pattern pattern = Pattern.compile(findFieldRE);
		if (pattern.matcher(query).find()) {
			ConfigurationWarnings.add(this, log, "The query [" + query + "] may not reference the timestampField or commentField. Found: [" + fieldName + "].");
		}
	}

	protected String createUpdateStatusQuery(String fieldValue, String additionalSetClause) {
		return "UPDATE " + getTableName() +
				" SET " + getStatusField() + "='" + fieldValue + "'" +
				(StringUtils.isNotEmpty(getTimestampField()) ? "," + getTimestampField() + "=" + getDbmsSupport().getSysDate():"") +
				(StringUtils.isNotEmpty(getCommentField()) ? "," + getCommentField() + "=?" : "") +
				(StringUtils.isNotEmpty(additionalSetClause) ? "," + additionalSetClause : "") +
				" WHERE " + getStatusField() + "!='" + fieldValue + "' AND " + getKeyField() + "=?";
	}

	@Override
	protected RawMessageWrapper<M> changeProcessState(Connection connection, RawMessageWrapper<M> rawMessage, ProcessState toState, String reason) throws ListenerException {
		String query = getUpdateStatusQuery(toState);
		String key = getKeyFromRawMessage(rawMessage);
		List<String> parameters = new ArrayList<>();
		if (StringUtils.isNotEmpty(getCommentField()) && query.substring(query.indexOf('?') + 1).contains("?")) {
			if (getMaxCommentLength() >= 0 && reason != null && reason.length() > getMaxCommentLength()) {
				parameters.add(reason.substring(0, getMaxCommentLength()));
			} else {
				parameters.add(reason);
			}
		}
		parameters.add(key);
		return execute(connection, query, parameters) ? rawMessage : null;
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
	 * Field containing the status of the message.
	 * <b>NB: For optimal performance, an index should exist that starts with this field, followed by all fields that are used with a fixed value in the select condition, and end with the <code>orderField</code>.
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
	 * (optional) Field used to store the date and time of the last change of the <code>statusField</code>
	 */
	public void setTimestampField(String fieldname) {
		timestampField = fieldname;
	}

	/**
	 * (optional) Field used to store the reason of the last change of the <code>statusField</code>
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
	 * (optional) Value of <code>statusField</code> indicating row is available to be processed. If not specified, any row not having any of the other status values is considered available.
	 */
	public void setStatusValueAvailable(String string) {
		statusValues.put(ProcessState.AVAILABLE,string);
	}

	/**
	 * Value of <code>statusField</code> indicating the processing of the row resulted in an error
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
	 * Value of <code>statusField</code> indicating is being processed. Can be left emtpy if database has <code>SKIP LOCKED</code> functionality and the <code>transactionAttribute</code> of the <code>Receiver</code> can be (and is) set to <code>Required</code> or <code>RequiresNew</code>.
	 */
	public void setStatusValueInProcess(String string) {
		statusValues.put(ProcessState.INPROCESS, string);
	}

	/**
	 * Value of <code>statusField</code> indicating message is on Hold, temporarily
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
