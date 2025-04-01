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
package org.frankframework.jdbc;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import lombok.Getter;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.stream.Message;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlUtils;

/**
 * QuerySender that transforms the input message to a query.
 * <br/>
 * <pre>{@code
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
 *
 *  alter - sequenceName
 *        - startWith
 *
 *  sql   - type [0..1] one of {select;ddl;other}, other by default
 *        - query
 *
 * }</pre>
 * <br/>
 *
 * @ff.info Please note that the default value of {@code trimSpaces} is {@literal true}
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

	@Getter
	private class Column {
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
					throw new SenderException("got exception parsing value [" + value + "] to Integer", e);
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
					throw new SenderException("got exception parsing value [" + value + "] to Number using decimalSeparator [" + decimalSeparator + "] and groupingSeparator [" + groupingSeparator + "]", e);
				}
				if (value.indexOf('.') >= 0) {
					parameter = n.doubleValue();
				} else {
					parameter = n.intValue();
				}
			} else if (type.equalsIgnoreCase(TYPE_DATETIME)) {
				DateTimeFormatter formatter = DateFormatUtils.getDateTimeFormatterWithOptionalComponents(formatString);

				try {
					TemporalAccessor parsed = formatter.parse(value);
					parameter = Timestamp.from(Instant.from(parsed));
				} catch (DateTimeParseException e) {
					throw new SenderException("got exception parsing value [" + value + "] to Date using formatString [" + formatString + "]", e);
				}
			} else if (type.equalsIgnoreCase(TYPE_XMLDATETIME)) {
				java.util.Date nDate;
				try {
					nDate = XmlUtils.parseXmlDateTime(value);
				} catch (Exception e) {
					throw new SenderException("got exception parsing value [" + value + "] from xml dateTime to Date", e);
				}
				parameter = new Timestamp(nDate.getTime());
			} else if (type.equalsIgnoreCase(TYPE_BLOB)) {
				parameter = value.getBytes();
			} else {
				if (!type.equalsIgnoreCase(TYPE_FUNCTION)) {
					parameter = value;
				}
			}
		}

		private void fillQueryValue() {
			if (type.equalsIgnoreCase(TYPE_FUNCTION)) {
				queryValue = value;
			} else {
				queryValue = "?";
			}
		}

	}

	@Override
	public SenderResult sendMessage(Connection blockHandle, Message message, PipeLineSession session) throws SenderException, TimeoutException {
		Message result;
		try {
			Element queryElement = XmlUtils.buildElement(message.asString());
			String root = queryElement.getTagName();
			String tableName = XmlUtils.getChildTagAsString(queryElement, "tableName");
			Element columnsElement = XmlUtils.getFirstChildTag(queryElement, "columns");
			List<Column> columns;
			if (columnsElement != null) {
				columns = getColumns(columnsElement);
			} else {
				columns = Collections.emptyList();
			}
			String where = XmlUtils.getChildTagAsString(queryElement, "where");
			String order = XmlUtils.getChildTagAsString(queryElement, "order");

			if ("select".equalsIgnoreCase(root)) {
				result = selectQuery(blockHandle, tableName, columns, where, order).getResult();
			} else if ("insert".equalsIgnoreCase(root)) {
				result = insertQuery(blockHandle, tableName, columns);
			} else if ("delete".equalsIgnoreCase(root)) {
				result = deleteQuery(blockHandle, tableName, where);
			} else if ("update".equalsIgnoreCase(root)) {
				result = updateQuery(blockHandle, tableName, columns, where);
			} else if ("alter".equalsIgnoreCase(root)) {
				String sequenceName = XmlUtils.getChildTagAsString(queryElement, "sequenceName");
				int startWith = Integer.parseInt(XmlUtils.getChildTagAsString(queryElement, "startWith"));
				result = alterQuery(blockHandle, sequenceName, startWith);
			} else if ("sql".equalsIgnoreCase(root)) {
				String type = XmlUtils.getChildTagAsString(queryElement, "type");
				String query = XmlUtils.getChildTagAsString(queryElement, "query");
				result = sql(blockHandle, query, type);
			} else {
				throw new SenderException("unknown root element [" + root + "]");
			}
		} catch (DomBuilderException e) {
			throw new SenderException("got exception parsing [" + message + "]", e);
		} catch (JdbcException e) {
			throw new SenderException("got exception preparing [" + message + "]", e);
		} catch (IOException e) {
			throw new SenderException("got exception creating [" + message + "]", e);
		}

		return new SenderResult(result);
	}

	private SenderResult selectQuery(Connection connection, String tableName, List<Column> columns, String where, String order) throws SenderException, JdbcException {
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
			setBlobSmartGet(true);
			return executeSelectQuery(statement,null,null);
		} catch (SQLException e) {
			throw new SenderException("got exception executing a SELECT SQL command ["+ queryBuilder +"]", e);
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
			return executeUpdate(connection, query, columns);
		} catch (SenderException t) {
			throw new SenderException("got exception executing an INSERT SQL command [" + query + "]", t);
		}
	}

	private Message deleteQuery(Connection connection, String tableName, String where) throws SenderException, JdbcException {
		String query = "DELETE FROM " + tableName;
		if (where != null) {
			query = query + " WHERE " + where;
		}
		try {
			PreparedStatement statement = getStatement(connection, query, QueryType.OTHER);
			return executeOtherQuery(connection, statement, query, null, null, null, null, null);
		} catch (SQLException e) {
			throw new SenderException("got exception executing a DELETE SQL command [" + query + "]", e);
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
			return executeUpdate(connection, query, columns);
		} catch (SenderException t) {
			throw new SenderException("got exception executing an UPDATE SQL command [" + queryBuilder + "]", t);
		}
	}

	private Message sql(Connection connection, String query, String type) throws SenderException, JdbcException {
		try {
			PreparedStatement statement = getStatement(connection, query, QueryType.OTHER);
			setBlobSmartGet(true);
			if (StringUtils.isNotEmpty(type) && "select".equalsIgnoreCase(type)) {
				return executeSelectQuery(statement,null,null).getResult();
			} else if (StringUtils.isNotEmpty(type) && "ddl".equalsIgnoreCase(type)) {
				//TODO: Strip SQL comments, everything between -- and newline
				StringBuilder result = new StringBuilder();
				for (String q : StringUtil.split(query, ";")) {
					statement = getStatement(connection, q, QueryType.OTHER);
					if (q.trim().toLowerCase().startsWith("select")) {
						result.append(executeSelectQuery(statement,null,null).getResult().asString());
					} else {
						result.append(executeOtherQuery(connection, statement, q, null, null, null, null, null).asString());
					}
				}
				return new Message(result.toString());
			} else {
				return executeOtherQuery(connection, statement, query, null, null, null, null, null);
			}
		} catch (SQLException | IOException e) {
			throw new SenderException("got exception executing a SQL command ["+query+"]", e);
		}
	}

	private Message executeUpdate(Connection connection, String query, List<Column> columns) throws SenderException {
		try {
			PreparedStatement statement = getStatement(connection, query, QueryType.OTHER);
			applyParameters(statement, columns);
			return executeOtherQuery(connection, statement, query, null, null, null, null, null);
		} catch (Throwable t) {
			throw new SenderException(t);
		}
	}

	private Message alterQuery(Connection connection, String sequenceName, int startWith) throws SenderException {
		String callQuery = "declare" + " pragma autonomous_transaction;" + " ln_increment number;" + " ln_curr_val number;" + " ln_reset_increment number;" + " ln_reset_val number;" + "begin" + " select increment_by into ln_increment from user_sequences where sequence_name = '" + sequenceName + "';" + " select " + (startWith - 2) + " - " + sequenceName + ".nextval into ln_reset_increment from dual;" + " select " + sequenceName + ".nextval into ln_curr_val from dual;" + " EXECUTE IMMEDIATE 'alter sequence " + sequenceName + " increment by '|| ln_reset_increment ||' minvalue 0';" + " select " + sequenceName + ".nextval into ln_reset_val from dual;" + " EXECUTE IMMEDIATE 'alter sequence " + sequenceName + " increment by '|| ln_increment;" + "end;";
		log.debug("preparing procedure for query [{}]", ()-> callQuery);
		try (CallableStatement callableStatement = connection.prepareCall(callQuery)){
			callableStatement.setQueryTimeout(getTimeout());
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
					log.debug("parm [{}] is an Integer with value [{}]", var, column.getParameter());
					statement.setInt(var, Integer.parseInt(column.getParameter().toString()));
					var++;
				} else if (column.getParameter() instanceof Boolean) {
					log.debug("parm [{}] is an Boolean with value [{}]", var, column.getParameter());
					statement.setBoolean(var, Boolean.parseBoolean(column.getParameter().toString()));
					var++;
				} else if (column.getParameter() instanceof Double) {
					log.debug("parm [{}] is a Double with value [{}]", var, column.getParameter());
					statement.setDouble(var, Double.parseDouble(column.getParameter().toString()));
					var++;
				} else if (column.getParameter() instanceof Float) {
					log.debug("parm [{}] is a Float with value [{}]", var, column.getParameter());
					statement.setFloat(var, Float.parseFloat(column.getParameter().toString()));
					var++;
				} else if (column.getParameter() instanceof Timestamp) {
					log.debug("parm [{}] is a Timestamp with value [{}]", var, column.getParameter());
					statement.setTimestamp(var, (Timestamp) column.getParameter());
					var++;
				} else if (column.getParameter() instanceof byte[]) {
					log.debug("parm [{}] is a byte array with value [{}] = [{}]", var, column.getParameter(), new String((byte[]) column.getParameter()));
					statement.setBytes(var, (byte[]) column.getParameter());
					var++;
				} else {
					//if (column.getParameter() instanceof String)
					log.debug("parm [{}] is a String with value [{}]", var, column.getParameter());
					JdbcUtil.setParameter(statement, var, (String) column.getParameter(), getDbmsSupport().isParameterTypeMatchRequired());
					var++;
				}
			}
		}
	}

}
