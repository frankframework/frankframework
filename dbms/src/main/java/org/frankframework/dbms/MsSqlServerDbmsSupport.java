/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.dbms;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.util.DateFormatUtils;


/**
 * @author Gerrit van Brakel
 */
public class MsSqlServerDbmsSupport extends GenericDbmsSupport {

	private static final int CLOB_SIZE_THRESHOLD = 10_000_000; // larger than this is considered a CLOB, smaller a string

	@Override
	public Dbms getDbms() {
		return Dbms.MSSQL;
	}

	@Override
	public boolean hasSkipLockedFunctionality() {
		return true;
	}

	@Override
	public String getSysDate() {
		return "CURRENT_TIMESTAMP";
	}

	@Override
	public String getDateAndOffset(String dateValue, int daysOffset) {
		return "DATEADD(day, " + daysOffset + "," + dateValue + ")";
	}

	@Override
	public String getAutoIncrementKeyFieldType() {
		return "INT IDENTITY";
	}

	@Override
	public String getTimestampFieldType() {
		return "DATETIME";
	}

	@Override
	public String getDatetimeLiteral(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat(DateFormatUtils.FORMAT_DATETIME_GENERIC);
		String formattedDate = formatter.format(date);
		return "CONVERT(datetime, '" + formattedDate + "', 120)";
	}

	@Override
	public String getTimestampAsDate(String columnName) {
		return "CONVERT(VARCHAR(10), " + columnName + ", 120)";
	}

	@Override
	public String getBlobFieldType() {
		return "VARBINARY(MAX)";
	}

	@Override
	public boolean isClobType(final ResultSetMetaData rsmeta, final int colNum) throws SQLException {
		return (rsmeta.getColumnType(colNum) == Types.VARCHAR || rsmeta.getColumnType(colNum) == Types.NVARCHAR) && rsmeta.getPrecision(colNum) > CLOB_SIZE_THRESHOLD;
	}

	@Override
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws DbmsException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new DbmsException("query [" + selectQuery + "] must start with keyword [" + KEYWORD_SELECT + "]");
		}
		// see http://www.mssqltips.com/tip.asp?tip=1257
		String result = selectQuery.substring(0, KEYWORD_SELECT.length()) + (batchSize > 0 ? " TOP " + batchSize : "") + selectQuery.substring(KEYWORD_SELECT.length());
		int wherePos = result.toLowerCase().indexOf("where");
		if (wherePos < 0) {
			result += " WITH (updlock,readpast)";
		} else {
			result = result.substring(0, wherePos) + " WITH (updlock,readpast) " + result.substring(wherePos);
		}
		return result;
	}

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws DbmsException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new DbmsException("query [" + selectQuery + "] must start with keyword [" + KEYWORD_SELECT + "]");
		}
		// see http://www.mssqltips.com/tip.asp?tip=1257
		String result = selectQuery.substring(0, KEYWORD_SELECT.length()) + (batchSize > 0 ? " TOP " + batchSize : "") + selectQuery.substring(KEYWORD_SELECT.length());
		int wherePos = result.toLowerCase().indexOf("where");
		if (wherePos < 0) {
			result += " WITH (readpast)";
		} else {
			result = result.substring(0, wherePos) + " WITH (readpast) " + result.substring(wherePos);
		}
		return result;
	}

	@Override
	public String prepareQueryTextForNonLockingRead(String selectQuery) throws DbmsException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new DbmsException("query [" + selectQuery + "] must start with keyword [" + KEYWORD_SELECT + "]");
		}
		String result = selectQuery;
		int wherePos = result.toLowerCase().indexOf("where");
		if (wherePos < 0) {
			result += " WITH (nolock)";
		} else {
			result = result.substring(0, wherePos) + " WITH (nolock) " + result.substring(wherePos);
		}
		return result;
	}

	@Override
	public String provideTrailingFirstRowsHint(int rowCount) {
		return " OPTION (FAST " + rowCount + ")";
	}

	@Override
	public String getSchema(Connection conn) throws DbmsException {
		return DbmsUtil.executeStringQuery(conn, "SELECT SCHEMA_NAME()");
	}

	@Override
	public String getLength(String column) {
		return "LEN(" + column + ")";
	}

	@Override
	public boolean isSequencePresent(Connection conn, String schemaOwner, String tableName, String sequenceName) {
		String query = "select objectproperty(object_id('" + tableName + "'), 'TableHasIdentity')";
		try {
			return DbmsUtil.executeIntQuery(conn, query) >= 1;
		} catch (Exception e) {
			log.warn("could not determine presence of identity on table [{}]", tableName, e);
			return false;
		}
	}

	@Override
	public boolean hasIndexOnColumns(Connection conn, String schemaOwner, String tableName, List<String> columns) {
		StringBuilder query = new StringBuilder("select count(*) from sys.indexes si");
		for (int i = 1; i <= columns.size(); i++) {
			query.append(", sys.index_columns sic").append(i);
		}
		query.append(" where si.object_id = object_id('").append(tableName).append("')");
		for (int i = 1; i <= columns.size(); i++) {
			query.append(" and si.object_id=sic").append(i).append(".object_id");
			query.append(" and si.index_id=sic").append(i).append(".index_id");
			query.append(" and col_name(sic").append(i).append(".object_id, sic").append(i).append(".column_id)='").append(columns.get(i - 1)).append("'");
			query.append(" and sic").append(i).append(".index_column_id=").append(i);
		}
		try {
			return DbmsUtil.executeIntQuery(conn, query.toString()) >= 1;
		} catch (Exception e) {
			log.warn("could not determine presence of index columns on table [{}] using query [{}]", tableName, query, e);
			return false;
		}
	}

	@Override
	public String getBooleanValue(boolean value) {
		return value ? "1" : "0";
	}

	@Override
	public boolean isRowVersionTimestamp(ResultSetMetaData resultSetMetaData, int columnNumber) throws SQLException {
		// MSSQL timestamp is a binary type, containing the rowversion as a binary number.
		return resultSetMetaData.getColumnTypeName(columnNumber).equals("timestamp");
	}
}
