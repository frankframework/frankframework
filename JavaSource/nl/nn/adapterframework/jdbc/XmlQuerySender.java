/*
 * $Log: XmlQuerySender.java,v $
 * Revision 1.4  2011-03-16 16:42:40  L190409
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 * Revision 1.3  2010/03/03 08:55:17  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed '(EOFException) Unexpected end of ZLIB input stream'
 *
 * Revision 1.2  2010/02/01 08:32:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * setConfigWarning
 *
 * Revision 1.1  2010/01/28 09:48:00  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

/**
 * QuerySender that transforms the input message to a query.
 * <br/><code><pre>
 *  select
 *  delete
 *  insert
 *  update - tableName
 *         - columns [0..1] - column [1..n] - name
 *                                          - value [0..1]
 *                                          - type [0..1] one of {string;function;number;datetime;blob;clob}, string by default
 *                                          - decimalSeparator [0..1] only applicable for type=number
 *                                          - groupingSeparator [0..1] only applicable for type=number
 *                                          - formatString [0..1] only applicable for type=datetime, yyyy-MM-dd HH:mm:ss.SSS by default 
 *         - where [0..1]
 *         - order [0..1]
 * <br/>
 *  alter - sequenceName
 *        - startWith
 * <br/>
 * </pre></code><br/>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.XmlQuerySender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceNameXA(String) datasourceNameXA}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>username used to connect to datasource</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>password used to connect to datasource</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setConnectionsArePooled(boolean) connectionsArePooled}</td><td>when true, it is assumed that an connectionpooling mechanism is present. Before a message is sent, a new connection is obtained, that is closed after the message is sent. When transacted is true, connectionsArePooled is true, too</td><td>true</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNullValue(String) nullValue}</td><td>value used in result as contents of fields that contain no value (SQL-NULL)</td><td><i>empty string</></td></tr>
 * <tr><td>{@link #setTrimSpaces(boolean) trimSpaces}</td><td>remove trailing blanks from all values.</td><td>true</td></tr>
 * <tr><td>{@link #setBlobCharset(String) blobCharset}</td><td>charset used to read and write BLOBs</td><td>UTF-8</td></tr>
 * <tr><td>{@link #setBlobsCompressed(boolean) blobsCompressed}</td><td>controls whether blobdata is stored compressed in the database</td><td>true</td></tr>
 * <tr><td>{@link #setBlobSmartGet(boolean) blobSmartGet}</td><td>controls automatically whether blobdata is stored compressed and/or serialized in the database</td><td>false</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>the number of seconds the driver will wait for a Statement object to execute. If the limit is exceeded, a TimeOutException is thrown. 0 means no timeout</td><td>0</td></tr>
 * <tr><td>{@link #setIncludeFieldDefinition(boolean) includeFieldDefinition}</td><td>when <code>true</code>, the result contains besides the returned rows also a header with information about the fetched fields</td><td>true</td></tr>
 * </table>
 * </p>
 * 
 * @version Id
 * @author  Peter Leeuwenburgh
 */
public class XmlQuerySender extends DirectQuerySender {
	public static final String version = "$RCSfile: XmlQuerySender.java,v $ $Revision: 1.4 $ $Date: 2011-03-16 16:42:40 $";

	public static final String TYPE_STRING = "string";
	public static final String TYPE_NUMBER = "number";
	public static final String TYPE_BLOB = "blob";
	public static final String TYPE_CLOB = "clob";
	public static final String TYPE_FUNCTION = "function";
	public static final String TYPE_DATETIME = "datetime";
	public static final String TYPE_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

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
			if (type.equalsIgnoreCase(TYPE_NUMBER)) {
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
					parameter = new Float(n.floatValue());
				} else {
					parameter = new Integer(n.intValue());
				}
			} else {
				if (type.equalsIgnoreCase(TYPE_DATETIME)) {
					DateFormat df = new SimpleDateFormat(formatString);
					java.util.Date nDate;
					try {
						nDate = df.parse(value);
					} catch (ParseException e) {
						throw new SenderException(getLogPrefix() + "got exception parsing value [" + value + "] to Date using formatString [" + formatString + "]", e);
					}
					parameter = new java.sql.Timestamp(nDate.getTime());
				} else {
					if (type.equalsIgnoreCase(TYPE_BLOB) || type.equalsIgnoreCase(TYPE_CLOB) || type.equalsIgnoreCase(TYPE_FUNCTION)) {
						//skip
					} else {
						// type.equalsIgnoreCase("string")
						parameter = new String(value);
					}
				}
			}
		}

		private void fillQueryValue() {
			queryValue = "?";
			if (type.equalsIgnoreCase(TYPE_BLOB)) {
				queryValue = "EMPTY_BLOB()";
			} else {
				if (type.equalsIgnoreCase(TYPE_CLOB)) {
					queryValue = "EMPTY_CLOB()";
				} else {
					if (type.equalsIgnoreCase(TYPE_FUNCTION)) {
						queryValue = value;
					}
				}
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

	protected String sendMessage(Connection connection, String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		Element queryElement;
		String tableName = null;
		Vector columns = null;
		String where = null;
		String order = null;
		String result = null;
		try {
			queryElement = XmlUtils.buildElement(message);
			String root = queryElement.getTagName();
			tableName = XmlUtils.getChildTagAsString(queryElement, "tableName");
			Element columnsElement = XmlUtils.getFirstChildTag(queryElement, "columns");
			if (columnsElement != null) {
				columns = getColumns(columnsElement);
			}
			where = XmlUtils.getChildTagAsString(queryElement, "where");
			order = XmlUtils.getChildTagAsString(queryElement, "order");

			if (root.equalsIgnoreCase("select")) {
				result = selectQuery(connection, correlationID, tableName, columns, where, order);
			} else {
				if (root.equalsIgnoreCase("insert")) {
					result = insertQuery(connection, correlationID, prc, tableName, columns);
				} else {
					if (root.equalsIgnoreCase("delete")) {
						result = deleteQuery(connection, correlationID, tableName, where);
					} else {
						if (root.equalsIgnoreCase("update")) {
							result = updateQuery(connection, correlationID, tableName, columns, where);
						} else {
							if (root.equalsIgnoreCase("alter")) {
								String sequenceName = XmlUtils.getChildTagAsString(queryElement, "sequenceName");
								int startWith = Integer.parseInt(XmlUtils.getChildTagAsString(queryElement, "startWith"));
								result = alterQuery(connection, sequenceName, startWith);
							} else {
								throw new SenderException(getLogPrefix() + "unknown root element [" + root + "]");
							}
						}
					}
				}
			}
		} catch (DomBuilderException e) {
			throw new SenderException(getLogPrefix() + "got exception parsing [" + message + "]", e);
		} catch (JdbcException e) {
			throw new SenderException(getLogPrefix() + "got exception preparing [" + message + "]", e);
		}

		return result;
	}

	private String selectQuery(Connection connection, String correlationID, String tableName, Vector columns, String where, String order) throws SenderException, JdbcException {
		try {
			String query = "SELECT ";
			if (columns != null) {
				Iterator iter = columns.iterator();
				boolean firstColumn = true;
				while (iter.hasNext()) {
					Column column = (Column) iter.next();
					if (firstColumn) {
						query = query + column.getName();
						firstColumn = false;
					} else {
						query = query + "," + column.getName();
					}
				}
			} else {
				query = query + "*";
			}
			query = query + " FROM " + tableName;
			if (where != null) {
				query = query + " WHERE " + where;
			}
			if (order != null) {
				query = query + " ORDER BY " + order;
			}
			PreparedStatement statement = getStatement(connection, correlationID, query);
			statement.setQueryTimeout(getTimeout());
			setBlobSmartGet(true);
			return executeSelectQuery(statement);
		} catch (SQLException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command", e);
		}
	}

	private String insertQuery(Connection connection, String correlationID, ParameterResolutionContext prc, String tableName, Vector columns) throws SenderException {
		try {
			String query = "INSERT INTO " + tableName + " (";
			Iterator iter = columns.iterator();
			String queryColumns = null;
			String queryValues = null;
			while (iter.hasNext()) {
				Column column = (Column) iter.next();
				if (queryColumns == null) {
					queryColumns = column.getName();
				} else {
					queryColumns = queryColumns + "," + column.getName();
				}
				if (queryValues == null) {
					queryValues = column.getQueryValue();
				} else {
					queryValues = queryValues + "," + column.getQueryValue();
				}
			}
			query = query + queryColumns + ") VALUES (" + queryValues + ")";
			return executeUpdate(connection, correlationID, tableName, query, columns);
		} catch (SenderException t) {
			throw new SenderException(getLogPrefix() + "got exception executing an INSERT SQL command", t);
		}
	}

	private String deleteQuery(Connection connection, String correlationID, String tableName, String where) throws SenderException, JdbcException {
		try {
			String query = "DELETE FROM " + tableName;
			if (where != null) {
				query = query + " WHERE " + where;
			}
			PreparedStatement statement = getStatement(connection, correlationID, query);
			statement.setQueryTimeout(getTimeout());
			return executeOtherQuery(connection, correlationID, statement, query, null, null);
		} catch (SQLException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a DELETE SQL command", e);
		}
	}

	private String updateQuery(Connection connection, String correlationID, String tableName, Vector columns, String where) throws SenderException {
		try {
			String query = "UPDATE " + tableName + " SET ";
			Iterator iter = columns.iterator();
			String querySet = null;
			while (iter.hasNext()) {
				Column column = (Column) iter.next();
				if (querySet == null) {
					querySet = column.getName();
				} else {
					querySet = querySet + "," + column.getName();
				}
				querySet = querySet + "=" + column.getQueryValue();
			}
			query = query + querySet;
			if (where != null) {
				query = query + " WHERE " + where;
			}
			return executeUpdate(connection, correlationID, tableName, query, columns);
		} catch (SenderException t) {
			throw new SenderException(getLogPrefix() + "got exception executing an UPDATE SQL command", t);
		}
	}

	private String executeUpdate(Connection connection, String correlationID, String tableName, String query, Vector columns) throws SenderException {
		try {
			if (existLob(columns)) {
				CallableStatement callableStatement = getCallWithRowIdReturned(connection, correlationID, query);
				applyParameters(callableStatement, columns);
				int ri = 1 + countParameters(columns);
				callableStatement.registerOutParameter(ri, Types.VARCHAR);
				callableStatement.setQueryTimeout(getTimeout());
				int numRowsAffected = callableStatement.executeUpdate();
				String rowId = callableStatement.getString(ri);
				log.debug(getLogPrefix() + "returning ROWID [" + rowId + "]");

				Iterator iter = columns.iterator();
				while (iter.hasNext()) {
					Column column = (Column) iter.next();
					if (column.getType().equalsIgnoreCase(TYPE_BLOB) || column.getType().equalsIgnoreCase(TYPE_CLOB)) {
						query = "SELECT " + column.getName() + " FROM " + tableName + " WHERE ROWID=?" + " FOR UPDATE";
						PreparedStatement statement = getStatement(connection, correlationID, query);
						statement.setString(1, rowId);
						statement.setQueryTimeout(getTimeout());
						if (column.getType().equalsIgnoreCase(TYPE_BLOB)) {
							executeUpdateBlobQuery(statement, column.getValue());
						} else {
							executeUpdateClobQuery(statement, column.getValue());
						}
					}
				}
				return "<result><rowsupdated>" + numRowsAffected + "</rowsupdated></result>";
			} else {
				PreparedStatement statement = getStatement(connection, correlationID, query);
				applyParameters(statement, columns);
				statement.setQueryTimeout(getTimeout());
				return executeOtherQuery(connection, correlationID, statement, query, null, null);
			}
		} catch (Throwable t) {
			throw new SenderException(t);
		}
	}

	private boolean existLob(Vector columns) {
		Iterator iter = columns.iterator();
		while (iter.hasNext()) {
			Column column = (Column) iter.next();
			if (column.getType().equalsIgnoreCase(TYPE_BLOB) || column.getType().equalsIgnoreCase(TYPE_CLOB)) {
				return true;
			}
		}
		return false;
	}

	private int countParameters(Vector columns) {
		int parameterCount = 0;
		Iterator iter = columns.iterator();
		while (iter.hasNext()) {
			Column column = (Column) iter.next();
			if (column.getParameter() != null) {
				parameterCount++;
			}
		}
		return parameterCount;
	}

	private String alterQuery(Connection connection, String sequenceName, int startWith) throws SenderException {
		try {
			String callQuery = "declare" + " pragma autonomous_transaction;" + " ln_increment number;" + " ln_curr_val number;" + " ln_reset_increment number;" + " ln_reset_val number;" + "begin" + " select increment_by into ln_increment from user_sequences where sequence_name = '" + sequenceName + "';" + " select " + (startWith - 2) + " - " + sequenceName + ".nextval into ln_reset_increment from dual;" + " select " + sequenceName + ".nextval into ln_curr_val from dual;" + " EXECUTE IMMEDIATE 'alter sequence " + sequenceName + " increment by '|| ln_reset_increment ||' minvalue 0';" + " select " + sequenceName + ".nextval into ln_reset_val from dual;" + " EXECUTE IMMEDIATE 'alter sequence " + sequenceName + " increment by '|| ln_increment;" + "end;";
			log.debug(getLogPrefix() + "preparing procedure for query [" + callQuery + "]");
			CallableStatement callableStatement = connection.prepareCall(callQuery);
			int numRowsAffected = callableStatement.executeUpdate();
			return "<result><rowsupdated>" + numRowsAffected + "</rowsupdated></result>";
		} catch (SQLException e) {
			throw new SenderException(e);
		}
	}

	private Vector getColumns(Element columnsElement) throws SenderException {
		Collection columnElements = XmlUtils.getChildTags(columnsElement, "column");
		Iterator iter = columnElements.iterator();
		if (iter.hasNext()) {
			Vector columns = new Vector();
			while (iter.hasNext()) {
				Element columnElement = (Element) iter.next();
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
		} else {
			return null;
		}
	}

	private void applyParameters(PreparedStatement statement, Vector columns) throws SQLException {
		Iterator iter = columns.iterator();
		int var = 1;
		while (iter.hasNext()) {
			Column column = (Column) iter.next();
			if (column.getParameter() != null) {
				if (column.getParameter() instanceof String) {
					log.debug("parm [" + var + "] is a String with value [" + column.getParameter().toString() + "]");
					statement.setString(var, (String) column.getParameter());
					var++;
				}
				if (column.getParameter() instanceof Integer) {
					log.debug("parm [" + var + "] is an Integer with value [" + column.getParameter().toString() + "]");
					statement.setInt(var, Integer.parseInt(column.getParameter().toString()));
					var++;
				}
				if (column.getParameter() instanceof Float) {
					log.debug("parm [" + var + "] is a Float with value [" + column.getParameter().toString() + "]");
					statement.setFloat(var, Float.parseFloat(column.getParameter().toString()));
					var++;
				}
				if (column.getParameter() instanceof Timestamp) {
					log.debug("parm [" + var + "] is a Timestamp with value [" + column.getParameter().toString() + "]");
					statement.setTimestamp(var, (Timestamp) column.getParameter());
					var++;
				}
			}
		}
	}

	public void configure(ParameterList parameterList) throws ConfigurationException {
		super.configure(parameterList);
		ConfigurationWarnings cw = ConfigurationWarnings.getInstance();
		cw.add("The XmlSender is not released for production. The configuration options for this pipe will change in a non-backward compatible way");
	}

}