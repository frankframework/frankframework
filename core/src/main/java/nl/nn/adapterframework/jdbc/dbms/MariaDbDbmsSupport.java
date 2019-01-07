/*
   Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.jdbc.dbms;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;

//@formatter:off
/**
 * Support for MariaDB.
 * 
 * <p><b>convertOracleQuery:</b>
 * <table border="1">
 * <tr><th>from (Oracle)</th><th>sequenceExists</th><th>resultQuery</th><th>to (MariaDB)</th></tr>
 * <tr><td colspan="1" rowspan="2"><code>SELECT <i>sequence_name</i>.NEXTVAL FROM DUAL</code></td><td>true</td><td>&nbsp;</td><td><code>SELECT NEXTVAL(<i>sequence_name</i>) AS NEXTVAL</code></td></tr>
 * <tr><td>false</td><td>&nbsp;</td><td><code>SELECT '<i>auto_increment_value</i>' AS NEXTVAL<br><code>ALTER TABLE <i>table_name</i> AUTO_INCREMENT=(<i>auto_increment_value</i> + 1)</code></code></td></tr>
 * <tr><td colspan="1" rowspan="3"><code>SELECT <i>sequence_name</i>.CURRVAL FROM DUAL</code></td><td>true</td><td>&nbsp;</td><td><code>SELECT LASTVAL(<i>sequence_name</i>) AS CURRVAL</code></td></tr>
 * <tr><td colspan="1" rowspan="2">false</td><td>true</td><td><code>SELECT LAST_INSERT_ID() AS CURRVAL</code></td></tr>
 * <tr><td>false</td><td><code>SELECT '(<i>auto_increment_value</i> - 1)' AS CURRVAL</code></td></tr>
 * <tr><td colspan="1" rowspan="2"><code>INSERT INTO <i>table_name</i> (<i>column1</i>, <i>column2</i>, <i>column3</i>, ...) VALUES (<i>sequence_name</i>.NEXTVAL, <i>value2</i>, <i>value3</i>, ...)</code></td><td>true</td><td>&nbsp;</td><td><code>INSERT INTO <i>table_name</i> (<i>column1</i>, <i>column2</i>, <i>column3</i>, ...) VALUES ((SELECT NEXTVAL(<i>sequence_name</i>), <i>value2</i>, <i>value3</i>, ...)</code></td></tr>
 * <tr><td>false</td><td>&nbsp;</td><td><code>INSERT INTO <i>table_name</i> (<i>column1</i>, <i>column2</i>, <i>column3</i>, ...) VALUES (0, <i>value2</i>, <i>value3</i>, ...)</code></td></tr>
 * <tr><td><code>INSERT INTO <i>table_name</i> (<i>column1</i>, <i>column2</i>, <i>column3</i>, <i>column4</i>, <i>column5</i>, ...) VALUES (EMPTY_BLOB(), SYSDATE, SYSTIMESTAMP, <i>value4</i>, <i>value5</i>, ...)</code></td><td>&nbsp;</td><td>&nbsp;</td><td><code>INSERT INTO <i>table_name</i> (<i>column1</i>, <i>column2</i>, <i>column3</i>, <i>column4</i>, <i>column5</i>, ...) VALUES ('', SYSDATE(), SYSDATE(), <i>value4</i>, <i>value5</i>, ...)</code></td></tr>
 * <tr><td><code>SELECT <i>column</i> FROM <i>table_name</i> WHERE <i>condition</i> FOR UPDATE</code><br>(where <i>column</i> is BLOB or CLOB)</td><td>&nbsp;</td><td>&nbsp;</td><td><code>UPDATE <i>table_name</i> SET <i>column</i>=? WHERE <i>condition</i></code></td></tr>
 * </table>
 * </p>
 * 
 * @author Peter Leeuwenburgh
 */
//@formatter:on

public class MariaDbDbmsSupport extends GenericDbmsSupport {

	@Override
	public int getDatabaseType() {
		return DbmsSupportFactory.DBMS_MARIADB;
	}

	@Override
	public String getDbmsName() {
		return "MariaDB";
	}

	@Override
	public String getSchema(Connection conn) throws JdbcException {
		return JdbcUtil.executeStringQuery(conn, "SELECT DATABASE()");
	}

	@Override
	public String getIbisStoreSummaryQuery() {
		String messageDateConverter = "date_format(MESSAGEDATE,'%Y-%m-%d')";
		return "select type, slotid, " + messageDateConverter
				+ " msgdate, count(*) msgcount from ibisstore group by slotid, type, "
				+ messageDateConverter + " order by type, slotid, "
				+ messageDateConverter;
	}

	@Override
	public String convertOracleQuery(Connection connection, String query,
			boolean resultQuery, boolean updateable)
			throws JdbcException, SQLException {
		String convertedQuery = null;
		String newQuery = query.replaceAll("([,\\(\\)])", " $1 ");
		String[] split = newQuery.trim().split("\\s+");
		if (split.length == 4 && "SELECT".equalsIgnoreCase(split[0])
				&& split[1].contains(".") && "FROM".equalsIgnoreCase(split[2])
				&& "DUAL".equalsIgnoreCase(split[3])) {
			convertedQuery = convertOracleQuerySelectFromDual(connection, split,
					resultQuery);
		} else if (split.length > 2 && "INSERT".equalsIgnoreCase(split[0])
				&& "INTO".equalsIgnoreCase(split[1])) {
			convertedQuery = convertOracleQueryInsertInto(connection, split);
		} else if (updateable && "SELECT".equalsIgnoreCase(split[0])
				&& split.length > 5
				&& "FOR".equalsIgnoreCase(split[split.length - 2])
				&& "UPDATE".equalsIgnoreCase(split[split.length - 1])) {
			convertedQuery = convertOracleQuerySelectForUpdate(split);
		}
		if (convertedQuery != null && !convertedQuery.equals(query)) {
			if (log.isDebugEnabled()) {
				log.debug("converted oracle query [" + query + "] to ["
						+ convertedQuery + "]");
			}
			return convertedQuery;
		}
		if (log.isDebugEnabled()) {
			log.debug("oracle query [" + query + "] not converted");
		}
		return query;
	}

	private String convertOracleQuerySelectFromDual(Connection connection,
			String[] split, boolean resultQuery) throws JdbcException {
		String[] seqSplit = split[1].trim().split("\\.");
		if (seqSplit.length == 2 && "NEXTVAL".equalsIgnoreCase(seqSplit[1])) {
			return convertOracleQuerySelectNextValFromDual(connection, split,
					seqSplit);
		} else {
			if (seqSplit.length == 2
					&& "CURRVAL".equalsIgnoreCase(seqSplit[1])) {
				return convertOracleQuerySelectCurrValFromDual(connection,
						split, seqSplit, resultQuery);
			}
		}
		return null;
	}

	private String convertOracleQuerySelectNextValFromDual(
			Connection connection, String[] split, String[] seqSplit)
			throws JdbcException {
		if (sequenceExists(connection, seqSplit[0])) {
			return split[0] + " " + seqSplit[1] + "(" + seqSplit[0] + ") AS "
					+ seqSplit[1];
		} else {
			String tableName = retrieveTableName(connection, seqSplit[0]);
			int autoIncrement = retrieveAutoIncrement(connection, tableName);
			alterAutoIncrement(connection, tableName, (autoIncrement + 1));
			return split[0] + " '" + autoIncrement + "' AS " + seqSplit[1];
		}
	}

	private String convertOracleQuerySelectCurrValFromDual(
			Connection connection, String[] split, String[] seqSplit,
			boolean resultQuery) throws JdbcException {
		if (sequenceExists(connection, seqSplit[0])) {
			return split[0] + " LASTVAL(" + seqSplit[0] + ") AS " + seqSplit[1];
		} else {
			if (resultQuery) {
				return split[0] + " LAST_INSERT_ID() AS " + seqSplit[1];
			} else {
				String tableName = retrieveTableName(connection, seqSplit[0]);
				int autoIncrement = retrieveAutoIncrement(connection,
						tableName);
				return split[0] + " '" + (autoIncrement - 1) + "' AS "
						+ seqSplit[1];
			}
		}
	}

	private String convertOracleQueryInsertInto(Connection connection,
			String[] split) {
		boolean changed = false;
		StringBuilder sb = new StringBuilder("");
		for (int i = 0; i < split.length; i++) {
			if (i > 0 && !"(".equals(split[i - 1]) && !",".equals(split[i])
					&& !")".equals(split[i])) {
				sb.append(" ");
			}
			if (split[i].contains(".")) {
				String[] seqSplit = split[i].trim().split("\\.");
				if (seqSplit.length == 2
						&& "NEXTVAL".equalsIgnoreCase(seqSplit[1])) {
					if (sequenceExists(connection, seqSplit[0])) {
						sb.append("(SELECT " + seqSplit[1] + "(" + seqSplit[0]
								+ "))");
					} else {
						sb.append("0");
					}
					changed = true;
				} else {
					sb.append(split[i]);
				}
			} else if (("EMPTY_BLOB".equalsIgnoreCase(split[i])
					|| "EMPTY_CLOB".equalsIgnoreCase(split[i]))
					&& split.length > (i + 2) && "(".equals(split[i + 1])
					&& ")".equals(split[i + 2])) {
				sb.append("''");
				i = i + 2;
				changed = true;
			} else if ("SYSDATE".equalsIgnoreCase(split[i])
					|| "SYSTIMESTAMP".equalsIgnoreCase(split[i])) {
				sb.append("SYSDATE()");
				changed = true;
			} else {
				sb.append(split[i]);
			}
		}
		if (changed) {
			return sb.toString();
		}
		return null;
	}

	private String convertOracleQuerySelectForUpdate(String[] split) {
		StringBuilder sb = new StringBuilder(
				"UPDATE " + split[3] + " SET " + split[1] + "=?");
		for (int i = 4; i < split.length - 2; i++) {
			sb.append(" ");
			sb.append(split[i]);
		}
		return sb.toString();
	}

	private String retrieveTableName(Connection connection, String sequenceName)
			throws JdbcException {
		String tableName = sequenceName;
		if (tableName.startsWith("SEQ_")) {
			tableName = StringUtils.substringAfter(tableName, "SEQ_");
		}
		if (tableName.endsWith("_SEQ")) {
			tableName = StringUtils.substringBeforeLast(tableName, "_SEQ");
		}
		if (tableName.contains("_SEQ_")) {
			tableName = StringUtils.replace(tableName, "_SEQ_", "_T_");
		}
		if (isTablePresent(connection, tableName)) {
			return tableName;
		} else {
			throw new JdbcException(
					"could not find corresponding table for sequence ["
							+ sequenceName + "]");
		}
	}

	private int retrieveAutoIncrement(Connection connection, String tableName)
			throws JdbcException {
		String query = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='"
				+ getSchema(connection) + "' AND TABLE_NAME='" + tableName
				+ "' AND EXTRA LIKE '%auto_increment%'";
		int countAutoIncrement = JdbcUtil.executeIntQuery(connection, query);
		if (countAutoIncrement == 0) {
			throw new JdbcException(
					"could not find auto_increment column for table ["
							+ tableName + "]");
		} else if (countAutoIncrement > 1) {
			throw new JdbcException(
					"could not have multiple auto_increment columns for table ["
							+ tableName + "]");
		}

		query = "SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='"
				+ getSchema(connection) + "' AND TABLE_NAME='" + tableName
				+ "'";
		int autoIncrement = JdbcUtil.executeIntQuery(connection, query);
		if (autoIncrement < 1) {
			return 1;
		}
		return autoIncrement;
	}

	private boolean sequenceExists(Connection connection, String sequenceName) {
		String query = "SELECT LASTVAL(" + sequenceName + ")";
		try {
			String lastVal = JdbcUtil.executeStringQuery(connection, query);
			return true;
		} catch (JdbcException e) {
			if (log.isDebugEnabled())
				log.debug("exception checking for existence of [" + sequenceName
						+ "] using query [" + query + "]: " + e.getMessage());
			return false;
		}
	}

	@Override
	public int alterSequence(Connection connection, String sequenceName,
			int startWith) throws JdbcException {
		if (sequenceExists(connection, sequenceName)) {
			String query = "ALTER SEQUENCE " + sequenceName + " RESTART "
					+ startWith;
			return JdbcUtil.executeIntQuery(connection, query);
		} else {
			String tableName = retrieveTableName(connection, sequenceName);
			return alterAutoIncrement(connection, tableName, startWith);
		}
	}

	public int alterAutoIncrement(Connection connection, String tableName,
			int startWith) throws JdbcException {
		String query = "ALTER TABLE " + tableName + " AUTO_INCREMENT="
				+ startWith;
		return JdbcUtil.executeIntQuery(connection, query);
	}
}
