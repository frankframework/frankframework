/*
   Copyright 2013, 2017 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import nl.nn.adapterframework.dbms.JdbcException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.StringUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * QuerySender that transforms the input message to a query.
 * <br/><code><pre>
 *  select
 *  delete
 *  insert
 *  update - tableName
 *         - columns [0..1] - column [1..n] - name
 *                                          - value [0..1]
 *                                          - type [0..1] one of {string;function;number;datetime;blob;clob;xmldatetime}, string by default
 *                                          - decimalSeparator [0..1] only applicable for type=number
 *                                          - groupingSeparator [0..1] only applicable for type=number
 *                                          - formatString [0..1] only applicable for type=datetime, yyyy-MM-dd HH:mm:ss.SSS by default
 *         - where [0..1]
 *         - order [0..1]
 * <br/>
 *  alter - sequenceName
 *        - startWith
 * <br/>
 *  sql   - type [0..1] one of {select;ddl;other}, other by default
 *        - query
 * <br/>
 * </pre></code><br/>
 *
 * @author  Peter Leeuwenburgh
 */
public class XmlQuerySender extends DirectQuerySender {

	public static final String TYPE_STRING = "string";
	public static final String TYPE_NUMBER = "number";
	public static final String TYPE_INTEGER = "integer";
	public static final String TYPE_BLOB = "blob";
	public static final String TYPE_CLOB = "clob";
	public static final String TYPE_BOOLEAN = "boolean";
	public static final String TYPE_FUNCTION = "function";
	public static final String TYPE_DATETIME = "datetime";
	public static final String TYPE_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String TYPE_XMLDATETIME = "xmldatetime";

	public class Column {
		private String name = null;
		private String value = null;
		private String type = TYPE_STRING;
		private String decimalSeparator = null;
		private String groupingSeparator = null;
		private String formatString = TYPE_DATETIME_PATTERN;
		private Object parameter = null;
		private String queryValue = null;

		public Column(String name, String value, String type, String decimalSeparator, String groupingSeparator, String formatString) throws SenderException {
			if (name != null) {
				this.name = name;
			}
			if (value != null) {
				this.value = value;
			}
			if (type != null) {
				this.type = type;
			}
			if (decimalSeparator != null) {
				this.decimalSeparator = decimalSeparator;
			}
			if (groupingSeparator != null) {
				this.groupingSeparator = groupingSeparator;
			}
			if (formatString != null) {
				this.formatString = formatString;
			}

			if (value != null) {
				fillParameter();
			}

			fillQueryValue();
		}

		private void fillParameter() throws SenderException {
			if (type.equalsIgnoreCase(TYPE_INTEGER)) {
				DecimalFormat df = new DecimalFormat();
				Number n;
				try {
					n = df.parse(value);
				} catch (ParseException e) {
					throw new SenderException(getLogPrefix() + "got exception parsing value [" + value + "] to Integer", e);
				}
				parameter = n.intValue();
			} else if (type.equalsIgnoreCase(TYPE_BOOLEAN)) {
				parameter = Boolean.valueOf(value);
			} else if (type.equalsIgnoreCase(TYPE_NUMBER)) {
				DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
				if (StringUtils.isNotEmpty(decimalSeparator)) {
					decimalFormatSymbols.setDecimalSeparator(decimalSeparator.charAt(0));
				}
				if (StringUtils.isNotEmpty(groupingSeparator)) {
					decimalFormatSymbols.setGroupingSeparator(groupingSeparator.charAt(0));
				}
				DecimalFormat df = new DecimalFormat();
				df.setDecimalFormatSymbols(decimalFormatSymbols);
				Number n;
				try {
					n = df.parse(value);
				} catch (ParseException e) {
					throw new SenderException(getLogPrefix() + "got exception parsing value [" + value + "] to Number using decimalSeparator [" + decimalSeparator + "] and groupingSeparator [" + groupingSeparator + "]", e);
				}
				if (value.indexOf('.') >= 0) {
					parameter = n.doubleValue();
				} else {
					parameter = n.intValue();
				}
			} else if (type.equalsIgnoreCase(TYPE_DATETIME)) {
				DateTimeFormatter df = DateTimeFormatter.ofPattern(formatString);
				parameter = new Timestamp(df.parse(value, Instant::from).toEpochMilli());
			} else if (type.equalsIgnoreCase(TYPE_XMLDATETIME)) {
				java.util.Date nDate;
				try {
					nDate = DateUtils.parseXmlDateTime(value);
				} catch (Exception e) {
					throw new SenderException(getLogPrefix() + "got exception parsing value [" + value + "] from xml dateTime to Date", e);
				}
				parameter = new Timestamp(nDate.getTime());
			} else if (type.equalsIgnoreCase(TYPE_BLOB)) {
				if (!getDbmsSupport().mustInsertEmptyBlobBeforeData()) {
					parameter = value.getBytes();
				}
			} else {
				if (!(type.equalsIgnoreCase(TYPE_CLOB) && getDbmsSupport().mustInsertEmptyClobBeforeData()) && !type.equalsIgnoreCase(TYPE_FUNCTION)) {
					parameter = value;
				}
			}
		}

		private void fillQueryValue() {
			if (type.equalsIgnoreCase(TYPE_BLOB) && getDbmsSupport().mustInsertEmptyBlobBeforeData()) {
				queryValue = getDbmsSupport().emptyBlobValue();
			} else if (type.equalsIgnoreCase(TYPE_CLOB) && getDbmsSupport().mustInsertEmptyClobBeforeData()) {
				queryValue = getDbmsSupport().emptyClobValue();
			} else if (type.equalsIgnoreCase(TYPE_FUNCTION)) {
				queryValue = value;
			} else {
				queryValue = "?";
			}
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

		public String getType() {
			return type;
		}

		public String getDecimalSeparator() {
			return decimalSeparator;
		}

		public String getGroupingSeparator() {
			return groupingSeparator;
		}

		public String getFormatString() {
			return formatString;
		}

		public Object getParameter() {
			return parameter;
		}

		public String getQueryValue() {
			return queryValue;
		}
	}

	@Override
	protected PipeRunResult sendMessageOnConnection(Connection connection, Message message, PipeLineSession session, IForwardTarget next) throws SenderException, TimeoutException {
		Element queryElement;
		String tableName;
		List<Column> columns;
		String where;
		String order;
		PipeRunResult result;
		try {
			queryElement = XmlUtils.buildElement(message.asString());
			String root = queryElement.getTagName();
			tableName = XmlUtils.getChildTagAsString(queryElement, "tableName");
			Element columnsElement = XmlUtils.getFirstChildTag(queryElement, "columns");
			if (columnsElement != null) {
				columns = getColumns(columnsElement);
			} else {
				columns = Collections.emptyList();
			}
			where = XmlUtils.getChildTagAsString(queryElement, "where");
			order = XmlUtils.getChildTagAsString(queryElement, "order");

			if (root.equalsIgnoreCase("select")) {
				result = selectQuery(connection, tableName, columns, where, order, session, next);
			} else {
				if (root.equalsIgnoreCase("insert")) {
					result = new PipeRunResult(null, insertQuery(connection, tableName, columns));
				} else {
					if (root.equalsIgnoreCase("delete")) {
						result = new PipeRunResult(null, deleteQuery(connection, tableName, where));
					} else {
						if (root.equalsIgnoreCase("update")) {
							result = new PipeRunResult(null, updateQuery(connection, tableName, columns, where));
						} else {
							if (root.equalsIgnoreCase("alter")) {
								String sequenceName = XmlUtils.getChildTagAsString(queryElement, "sequenceName");
								int startWith = Integer.parseInt(XmlUtils.getChildTagAsString(queryElement, "startWith"));
								result = new PipeRunResult(null, alterQuery(connection, sequenceName, startWith));
							} else {
								if (root.equalsIgnoreCase("sql")) {
									String type = XmlUtils.getChildTagAsString(queryElement, "type");
									String query = XmlUtils.getChildTagAsString(queryElement, "query");
									result = new PipeRunResult(null, sql(connection, query, type));
								} else {
								throw new SenderException(getLogPrefix() + "unknown root element [" + root + "]");
							}
						}
					}
				}
			}
			}
		} catch (DomBuilderException e) {
			throw new SenderException(getLogPrefix() + "got exception parsing [" + message + "]", e);
		} catch (JdbcException e) {
			throw new SenderException(getLogPrefix() + "got exception preparing [" + message + "]", e);
		} catch (IOException e) {
			throw new SenderException(getLogPrefix() + "got exception creating [" + message + "]", e);
		}

		return result;
	}

	private PipeRunResult selectQuery(Connection connection, String tableName, List<Column> columns, String where, String order, PipeLineSession session, IForwardTarget next) throws SenderException, JdbcException {
		StringBuilder queryBuilder = new StringBuilder("SELECT ");
		if (columns != null && !columns.isEmpty()) {
			String columnSelection = columns.stream()
					.map(Column::getName)
					.collect(Collectors.joining(","));
			queryBuilder.append(columnSelection);
		} else {
			queryBuilder.append("*");
		}
		queryBuilder.append(" FROM ").append(tableName);
		if (where != null) {
			queryBuilder.append(" WHERE ").append(where);
		}
		if (order != null) {
			queryBuilder.append(" ORDER BY ").append(order);
		}
		try {
			String query = queryBuilder.toString();
			PreparedStatement statement = getStatement(connection, query, QueryType.SELECT);
			statement.setQueryTimeout(getTimeout());
			setBlobSmartGet(true);
			return executeSelectQuery(statement,null,null, session, next);
		} catch (SQLException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command ["+ queryBuilder +"]", e);
		}
	}

	private Message insertQuery(Connection connection, String tableName, List<Column> columns) throws SenderException {
		String queryColumns = columns.stream()
				.map(Column::getName)
				.collect(Collectors.joining(","));
		String queryValues = columns.stream()
				.map(Column::getQueryValue)
				.collect(Collectors.joining(","));

		String query ="INSERT INTO " + tableName + " (" + queryColumns + ") VALUES (" + queryValues + ")";

		try {
			return executeUpdate(connection, tableName, query, columns);
		} catch (SenderException t) {
			throw new SenderException(getLogPrefix() + "got exception executing an INSERT SQL command [" + query + "]", t);
		}
	}

	private Message deleteQuery(Connection connection, String tableName, String where) throws SenderException, JdbcException {
		String query = "DELETE FROM " + tableName;
		if (where != null) {
			query = query + " WHERE " + where;
		}
		try {
			PreparedStatement statement = getStatement(connection, query, QueryType.OTHER);
			statement.setQueryTimeout(getTimeout());
			return executeOtherQuery(connection, statement, query, null, null, null, null, null);
		} catch (SQLException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a DELETE SQL command [" + query + "]", e);
		}
	}

	private Message updateQuery(Connection connection, String tableName, List<Column> columns, String where) throws SenderException {
		StringBuilder queryBuilder = new StringBuilder("UPDATE " + tableName + " SET ");
		String querySet = columns.stream()
				.map(column -> column.getName() + "=" + column.getQueryValue())
				.collect(Collectors.joining(","));

		queryBuilder.append(querySet);
		if (where != null) {
			queryBuilder.append(" WHERE ").append(where);
		}
		try {
			String query = queryBuilder.toString();
			return executeUpdate(connection, tableName, query, columns);
		} catch (SenderException t) {
			throw new SenderException(getLogPrefix() + "got exception executing an UPDATE SQL command [" + queryBuilder + "]", t);
		}
	}

	private Message sql(Connection connection, String query, String type) throws SenderException, JdbcException {
		try {
			PreparedStatement statement = getStatement(connection, query, QueryType.OTHER);
			statement.setQueryTimeout(getTimeout());
			setBlobSmartGet(true);
			if (StringUtils.isNotEmpty(type) && type.equalsIgnoreCase("select")) {
				return executeSelectQuery(statement,null,null, null, null).getResult();
			} else if (StringUtils.isNotEmpty(type) && type.equalsIgnoreCase("ddl")) {
				//TODO: Strip SQL comments, everything between -- and newline
				StringBuilder result = new StringBuilder();
				for (String q : StringUtil.split(query, ";")) {
					statement = getStatement(connection, q, QueryType.OTHER);
					if (q.trim().toLowerCase().startsWith("select")) {
						result.append(executeSelectQuery(statement,null,null, null, null).getResult().asString());
					} else {
						result.append(executeOtherQuery(connection, statement, q, null, null, null, null, null).asString());
					}
				}
				return new Message(result.toString());
			} else {
				return executeOtherQuery(connection, statement, query, null, null, null, null, null);
			}
		} catch (SQLException | IOException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SQL command ["+query+"]", e);
		}
	}

	private Message executeUpdate(Connection connection, String tableName, String query, List<Column> columns) throws SenderException {
		try {
			if ((existBlob(columns) && getDbmsSupport().mustInsertEmptyBlobBeforeData()) || (existClob(columns) && getDbmsSupport().mustInsertEmptyClobBeforeData())) {
				CallableStatement callableStatement = getCallWithRowIdReturned(connection, query);
				applyParameters(callableStatement, columns);
				int ri = 1 + countParameters(columns);
				callableStatement.registerOutParameter(ri, Types.VARCHAR);
				callableStatement.setQueryTimeout(getTimeout());
				int numRowsAffected = callableStatement.executeUpdate();
				String rowId = callableStatement.getString(ri);
				log.debug(getLogPrefix() + "returning ROWID [" + rowId + "]");

				for (Column column : columns) {
					if (column.getType().equalsIgnoreCase(TYPE_BLOB) || column.getType().equalsIgnoreCase(TYPE_CLOB)) {
						query = "SELECT " + column.getName() + " FROM " + tableName + " WHERE ROWID=?" + " FOR UPDATE";
						QueryType queryType;
						if (column.getType().equalsIgnoreCase(TYPE_BLOB)) {
							queryType = QueryType.UPDATEBLOB;
						} else {
							queryType = QueryType.UPDATECLOB;
						}
						PreparedStatement statement = getStatement(connection, query, queryType);
						statement.setString(1, rowId);
						statement.setQueryTimeout(getTimeout());
						if (column.getType().equalsIgnoreCase(TYPE_BLOB)) {
							executeUpdateBlobQuery(statement, new Message(column.getValue()));
						} else {
							executeUpdateClobQuery(statement, new Message(column.getValue()));
						}
					}
				}
				return new Message("<result><rowsupdated>" + numRowsAffected + "</rowsupdated></result>");
			}
			PreparedStatement statement = getStatement(connection, query, QueryType.OTHER);
			applyParameters(statement, columns);
			statement.setQueryTimeout(getTimeout());
			return executeOtherQuery(connection, statement, query, null, null, null, null, null);
		} catch (Throwable t) {
			throw new SenderException(t);
		}
	}

	private boolean existBlob(List<Column> columns) {
		for (Column column : columns) {
			if (column.getType().equalsIgnoreCase(TYPE_BLOB)) {
				return true;
			}
		}
		return false;
	}

	private boolean existClob(List<Column> columns) {
		for (Column column : columns) {
			if (column.getType().equalsIgnoreCase(TYPE_CLOB)) {
				return true;
			}
		}
		return false;
	}

	private int countParameters(List<Column> columns) {
		int parameterCount = 0;
		for (Column column : columns) {
			if (column.getParameter() != null) {
				parameterCount++;
			}
		}
		return parameterCount;
	}

	private Message alterQuery(Connection connection, String sequenceName, int startWith) throws SenderException {
		try {
			String callQuery = "declare" + " pragma autonomous_transaction;" + " ln_increment number;" + " ln_curr_val number;" + " ln_reset_increment number;" + " ln_reset_val number;" + "begin" + " select increment_by into ln_increment from user_sequences where sequence_name = '" + sequenceName + "';" + " select " + (startWith - 2) + " - " + sequenceName + ".nextval into ln_reset_increment from dual;" + " select " + sequenceName + ".nextval into ln_curr_val from dual;" + " EXECUTE IMMEDIATE 'alter sequence " + sequenceName + " increment by '|| ln_reset_increment ||' minvalue 0';" + " select " + sequenceName + ".nextval into ln_reset_val from dual;" + " EXECUTE IMMEDIATE 'alter sequence " + sequenceName + " increment by '|| ln_increment;" + "end;";
			log.debug(getLogPrefix() + "preparing procedure for query [" + callQuery + "]");
			CallableStatement callableStatement = connection.prepareCall(callQuery);
			int numRowsAffected = callableStatement.executeUpdate();
			return new Message("<result><rowsupdated>" + numRowsAffected + "</rowsupdated></result>");
		} catch (SQLException e) {
			throw new SenderException(e);
		}
	}

	private List<Column> getColumns(Element columnsElement) throws SenderException {
		Collection<Node> columnElements = XmlUtils.getChildTags(columnsElement, "column");
		if (columnElements.isEmpty()) {
			return Collections.emptyList();
		}
		List<Column> columns = new ArrayList<>();
		for (Node element : columnElements) {
			Element columnElement = (Element) element;
			Element nameElement = XmlUtils.getFirstChildTag(columnElement, "name");
			String name = null;
			if (nameElement != null) {
				name = XmlUtils.getStringValue(nameElement);
			}
			Element valueElement = XmlUtils.getFirstChildTag(columnElement, "value");
			String value = null;
			if (valueElement != null) {
				value = XmlUtils.getStringValue(valueElement);
			}
			Element typeElement = XmlUtils.getFirstChildTag(columnElement, "type");
			String type = null;
			if (typeElement != null) {
				type = XmlUtils.getStringValue(typeElement);
			}
			Element decimalSeparatorElement = XmlUtils.getFirstChildTag(columnElement, "decimalSeparator");
			String decimalSeparator = null;
			if (decimalSeparatorElement != null) {
				decimalSeparator = XmlUtils.getStringValue(decimalSeparatorElement);
			}
			Element groupingSeparatorElement = XmlUtils.getFirstChildTag(columnElement, "groupingSeparator");
			String groupingSeparator = null;
			if (groupingSeparatorElement != null) {
				groupingSeparator = XmlUtils.getStringValue(groupingSeparatorElement);
			}
			Element formatStringElement = XmlUtils.getFirstChildTag(columnElement, "formatString");
			String formatString = null;
			if (formatStringElement != null) {
				formatString = XmlUtils.getStringValue(formatStringElement);
			}
			Column column = new Column(name, value, type, decimalSeparator, groupingSeparator, formatString);
			columns.add(column);
		}
		return columns;
	}

	private void applyParameters(PreparedStatement statement, List<Column> columns) throws SQLException {
		int var = 1;
		for (Column column : columns) {
			if (column.getParameter() != null) {
				if (column.getParameter() instanceof Integer) {
					log.debug("parm [" + var + "] is an Integer with value [" + column.getParameter().toString() + "]");
					statement.setInt(var, Integer.parseInt(column.getParameter().toString()));
					var++;
				} else if (column.getParameter() instanceof Boolean) {
					log.debug("parm [" + var + "] is an Boolean with value [" + column.getParameter().toString() + "]");
					statement.setBoolean(var, Boolean.parseBoolean(column.getParameter().toString()));
					var++;
				} else if (column.getParameter() instanceof Double) {
					log.debug("parm [" + var + "] is a Double with value [" + column.getParameter().toString() + "]");
					statement.setDouble(var, Double.parseDouble(column.getParameter().toString()));
					var++;
				} else if (column.getParameter() instanceof Float) {
					log.debug("parm [" + var + "] is a Float with value [" + column.getParameter().toString() + "]");
					statement.setFloat(var, Float.parseFloat(column.getParameter().toString()));
					var++;
				} else if (column.getParameter() instanceof Timestamp) {
					log.debug("parm [" + var + "] is a Timestamp with value [" + column.getParameter().toString() + "]");
					statement.setTimestamp(var, (Timestamp) column.getParameter());
					var++;
				} else if (column.getParameter() instanceof byte[]) {
					log.debug("parm [" + var + "] is a byte array with value [" + column.getParameter().toString() + "] = [" + new String((byte[]) column.getParameter()) + "]");
					statement.setBytes(var, (byte[]) column.getParameter());
					var++;
				} else {
					//if (column.getParameter() instanceof String)
					log.debug("parm [" + var + "] is a String with value [" + column.getParameter().toString() + "]");
					JdbcUtil.setParameter(statement, var, (String) column.getParameter(), getDbmsSupport().isParameterTypeMatchRequired());
					var++;
				}
			}
		}
	}

}
