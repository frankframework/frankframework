/*
   Copyright 2013, 2015, 2018, 2019 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.StringUtil;


/**
 * @author Gerrit van Brakel
 */
public class GenericDbmsSupport implements IDbmsSupport {
	protected Logger log = LogManager.getLogger(this.getClass());

	protected static final String KEYWORD_SELECT = "select";
	protected static Map<String, ISqlTranslator> sqlTranslators = new HashMap<>();

	@Override
	public String getDbmsName() {
		return getDbms().getKey();
	}

	@Override
	public Dbms getDbms() {
		return Dbms.GENERIC;
	}

	@Override
	public boolean isParameterTypeMatchRequired() {
		return false;
	}

	@Override
	public boolean hasSkipLockedFunctionality() {
		return false;
	}

	@Override
	public String getSysDate() {
		return "NOW()";
	}

	@Override
	public String getDateAndOffset(String dateValue, int daysOffset) {
		return dateValue + " + " + daysOffset;
	}

	@Override
	public String getFromForTablelessSelect() {
		return "";
	}

	@Override
	public String getAutoIncrementKeyFieldType() {
		return "INT DEFAULT AUTOINCREMENT";
	}

	@Override
	public boolean autoIncrementKeyMustBeInserted() {
		return false;
	}

	@Override
	public String autoIncrementInsertValue(String sequenceName) {
		return null;
	}

	@Override
	public boolean autoIncrementUsesSequenceObject() {
		return false;
	}


	@Override
	public String getTimestampFieldType() {
		return "TIMESTAMP";
	}

	// method is used in JobDef.cleanupDatabase
	@Override
	public String getDatetimeLiteral(Date date) {
		return "TO_TIMESTAMP('" + getFormattedDate(date) + "', 'YYYY-MM-DD HH24:MI:SS')";
	}

	protected String getFormattedDate(Date date) {
		return DateFormatUtils.GENERIC_DATETIME_FORMATTER.format(date.toInstant());
	}

	@Override
	public String getTimestampAsDate(String columnName) {
		return "TO_CHAR(" + columnName + ",'YYYY-MM-DD')";
	}

	@Override
	public boolean isClobType(final ResultSetMetaData rsmeta, final int colNum) throws SQLException {
		switch (rsmeta.getColumnType(colNum)) {
			case Types.LONGVARCHAR:
			case Types.LONGNVARCHAR:
			case Types.CLOB:
				return true;
			default:
				return false;
		}
	}

	@Override
	public Object getClobHandle(ResultSet rs, int column) throws SQLException, DbmsException {
		return rs.getClob(column);
	}

	@Override
	public Writer getClobWriter(ResultSet rs, int column, Object clobHandle) throws SQLException {
		return ((Clob) clobHandle).setCharacterStream(1L);
	}

	@Override
	public void updateClob(ResultSet rs, int column, Object clobHandle) throws SQLException, DbmsException {
		rs.updateClob(column, (Clob) clobHandle);
	}

	@Override
	public void updateClob(ResultSet rs, String column, Object clobHandle) throws SQLException, DbmsException {
		rs.updateClob(column, (Clob) clobHandle);
	}

	@Override
	public Object getClobHandle(PreparedStatement stmt, int column) throws SQLException, DbmsException {
		return stmt.getConnection().createClob();
	}

	@Override
	public Writer getClobWriter(PreparedStatement stmt, int column, Object clobHandle) throws SQLException {
		return ((Clob) clobHandle).setCharacterStream(1L);
	}

	@Override
	public void applyClobParameter(PreparedStatement stmt, int column, Object clobHandle) throws SQLException {
		stmt.setClob(column, (Clob) clobHandle);
	}

	@Override
	public Reader getClobReader(ResultSet rs, int column) throws SQLException {
		Clob clob = rs.getClob(column);
		if (clob == null) {
			return null;
		}
		return clob.getCharacterStream();
	}

	@Override
	public Reader getClobReader(ResultSet rs, String column) throws SQLException {
		Clob clob = rs.getClob(column);
		if (clob == null) {
			return null;
		}
		return clob.getCharacterStream();
	}


	@Override
	public String getBlobFieldType() {
		return "BLOB";
	}

	@Override
	public boolean isBlobType(final ResultSetMetaData rsmeta, final int colNum) throws SQLException {
		switch (rsmeta.getColumnType(colNum)) {
			case Types.LONGVARBINARY:
			case Types.VARBINARY:
			case Types.BINARY:
			case Types.BLOB:
				return true;
			default:
				return false;
		}
	}

	@Override
	public Object getBlobHandle(ResultSet rs, int column) throws SQLException, DbmsException {
		return rs.getBlob(column);
	}

	protected OutputStream getBlobOutputStream(ResultSet rs, Object blobUpdateHandle) throws SQLException {
		return ((Blob) blobUpdateHandle).setBinaryStream(1L);
	}

	@Override
	public OutputStream getBlobOutputStream(ResultSet rs, int column, Object blobUpdateHandle) throws SQLException, DbmsException {
		return getBlobOutputStream(rs, blobUpdateHandle);
	}

	@Override
	public void updateBlob(ResultSet rs, int column, Object blobUpdateHandle) throws SQLException, DbmsException {
		rs.updateBlob(column, (Blob) blobUpdateHandle);
	}

	@Override
	public void updateBlob(ResultSet rs, String column, Object blobUpdateHandle) throws SQLException, DbmsException {
		rs.updateBlob(column, (Blob) blobUpdateHandle);
	}

	@Override
	public Object getBlobHandle(PreparedStatement stmt, int column) throws SQLException, DbmsException {
		return stmt.getConnection().createBlob();
	}

	@Override
	public OutputStream getBlobOutputStream(PreparedStatement stmt, int column, Object blobInsertHandle) throws SQLException {
		return ((Blob) blobInsertHandle).setBinaryStream(1L);
	}

	@Override
	public void applyBlobParameter(PreparedStatement stmt, int column, Object blobInsertHandle) throws SQLException {
		stmt.setBlob(column, (Blob) blobInsertHandle);
	}


	@Override
	public InputStream getBlobInputStream(ResultSet rs, int column) throws SQLException {
		Blob blob = rs.getBlob(column);
		if (blob == null) {
			return null;
		}
		return blob.getBinaryStream();
	}

	@Override
	public InputStream getBlobInputStream(ResultSet rs, String column) throws SQLException {
		Blob blob = rs.getBlob(column);
		if (blob == null) {
			return null;
		}
		return blob.getBinaryStream();
	}

	@Override
	public String getTextFieldType() {
		return "VARCHAR";
	}

	@Override
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery) throws DbmsException {
		return prepareQueryTextForWorkQueueReading(batchSize, selectQuery, -1);
	}

	@Override
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws DbmsException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new DbmsException("query [" + selectQuery + "] must start with keyword [" + KEYWORD_SELECT + "]");
		}
		log.warn("don't know how to perform prepareQueryTextForWorkQueueReading for this database type, doing a guess...");
		return selectQuery + " FOR UPDATE";
	}

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery) throws DbmsException {
		return prepareQueryTextForWorkQueuePeeking(batchSize, selectQuery, -1);
	}

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws DbmsException {
		return selectQuery;
	}

	@Override
	public String prepareQueryTextForNonLockingRead(String selectQuery) throws DbmsException {
		return selectQuery;
	}


	@Override
	public String provideIndexHintAfterFirstKeyword(String tableName, String indexName) {
		return "";
	}

	@Override
	public String provideFirstRowsHintAfterFirstKeyword(int rowCount) {
		return "";
	}

	@Override
	public String provideTrailingFirstRowsHint(int rowCount) {
		return "";
	}


	@Override
	public String getSchema(Connection conn) throws DbmsException {
		return null;
	}

	@Override
	public ResultSet getTableColumns(Connection conn, String tableName) throws DbmsException {
		return getTableColumns(conn, null, tableName);
	}

	@Override
	public ResultSet getTableColumns(Connection conn, String schemaName, String tableName) throws DbmsException {
		return getTableColumns(conn, schemaName, tableName, null);
	}

	@Override
	public ResultSet getTableColumns(Connection conn, String schemaName, String tableName, String columnNamePattern) throws DbmsException {
		try {
			return conn.getMetaData().getColumns(null, schemaName, tableName, columnNamePattern);
		} catch (SQLException e) {
			throw new DbmsException("exception retrieving columns for table [" + tableName + "]", e);
		}
	}

	@Override
	public boolean isTablePresent(Connection conn, String tableName) throws DbmsException {
		return isTablePresent(conn, null, tableName);
	}

	@Override
	public boolean isTablePresent(Connection conn, String schemaName, String tableName) throws DbmsException {
		try (ResultSet rs = conn.getMetaData().getTables(null, schemaName, tableName, null)) {
			return rs.next(); // rs.isAfterLast() does not work properly when rs.next() has not yet been called
		} catch (SQLException e) {
			throw new DbmsException("exception checking for existence of table [" + tableName + "]" + (schemaName == null ? "" : " with schema [" + schemaName + "]"), e);
		}
	}

	@Override
	public boolean isColumnPresent(Connection conn, String tableName, String columnName) throws DbmsException {
		return this.isColumnPresent(conn, null, tableName, columnName);
	}

	@Override
	public boolean isColumnPresent(Connection conn, String schemaName, String tableName, String columnName) throws DbmsException {
		try (ResultSet rs = conn.getMetaData().getColumns(null, schemaName, tableName, columnName)) {
			return rs.next(); // rs.isAfterLast() does not work properly when rs.next() has not yet been called
		} catch (SQLException e) {
			throw new DbmsException("exception checking for existence of column [" + columnName + "] in table [" + tableName + "]" + (schemaName == null ? "" : " with schema [" + schemaName + "]"), e);
		}
	}

	@Override
	public boolean hasIndexOnColumn(Connection conn, String schemaName, String tableName, String columnName) throws DbmsException {
		try (ResultSet rs = conn.getMetaData().getIndexInfo(null, schemaName, tableName, false, true)) {
			while (rs.next()) {
				if (tableName.equalsIgnoreCase(rs.getString("TABLE_NAME")) && columnName.equalsIgnoreCase(rs.getString("COLUMN_NAME")) && rs.getInt("ORDINAL_POSITION") == 1) {
					return true;
				}
			}
			return false;
		} catch (SQLException e) {
			throw new DbmsException("exception checking for existence of column [" + columnName + "] in table [" + tableName + "]" + (schemaName == null ? "" : " with schema [" + schemaName + "]"), e);
		}
	}

	protected boolean doHasIndexOnColumns(Connection conn, String schemaOwner, String tableName, List<String> columns, String indexTableName, String indexColumnTableName, String tableOwnerColumnName, String tableNameColumnName, String indexNameColumnName, String columnNameColumnName, String columPositionColumnName) {
		StringBuilder query = new StringBuilder("select count(*) from " + indexTableName + " ai");
		for (int i = 1; i <= columns.size(); i++) {
			query.append(", ").append(indexColumnTableName).append(" aic").append(i);
		}
		query.append(" where ai.").append(tableNameColumnName).append("='").append(tableName).append("'");
		if (tableOwnerColumnName != null) {
			query.append(" and ai.").append(tableOwnerColumnName).append("='").append(schemaOwner).append("'");
		}
		for (int i = 1; i <= columns.size(); i++) {
			query.append(" and ai.").append(indexNameColumnName).append("=aic").append(i).append(".").append(indexNameColumnName);
			query.append(" and aic").append(i).append(".").append(columnNameColumnName).append("='").append(columns.get(i - 1)).append("'");
			query.append(" and aic").append(i).append(".").append(columPositionColumnName).append("=").append(i);
		}
		try {
			return DbmsUtil.executeIntQuery(conn, query.toString()) >= 1;
		} catch (Exception e) {
			log.warn("could not determine presence of index columns on table [{}] using query [{}]", tableName, query, e);
			return false;
		}
	}

	/**
	 * Alternative implementation of isTablePresent(), that can be used by descender classes if the implementation via metadata does not work for that driver.
	 */
	protected boolean doIsTablePresent(Connection conn, String tablesTable, String schemaColumn, String tableNameColumn, String schemaName, String tableName) throws DbmsException {
		String query = "select count(*) from " + tablesTable + " where upper(" + tableNameColumn + ")=?";
		if (StringUtils.isNotEmpty(schemaName)) {
			if (StringUtils.isNotEmpty(schemaColumn)) {
				query += " and upper(" + schemaColumn + ")='" + schemaName.toUpperCase() + "'";
			} else {
				throw new DbmsException("no schemaColumn present in table [" + tablesTable + "] to test for presence of table [" + tableName + "] in schema [" + schemaName + "]");
			}
		}
		try {
			return DbmsUtil.executeIntQuery(conn, query, tableName.toUpperCase()) >= 1;
		} catch (Exception e) {
			log.warn("could not determine presence of table [{}]", tableName, e);
			return false;
		}
	}

	/**
	 * Alternative implementation of isColumnPresent(), that can be used by descender classes if the implementation via metadata does not work for that driver.
	 */
	protected boolean doIsColumnPresent(Connection conn, String columnsTable, String schemaColumn, String tableNameColumn, String columnNameColumn, String schemaName, String tableName, String columnName) throws DbmsException {
		String query = "select count(*) from " + columnsTable + " where upper(" + tableNameColumn + ")=? and upper(" + columnNameColumn + ")=?";
		if (StringUtils.isNotEmpty(schemaName)) {
			if (StringUtils.isNotEmpty(schemaColumn)) {
				query += " and upper(" + schemaColumn + ")='" + schemaName.toUpperCase() + "'";
			} else {
				throw new DbmsException("no schemaColumn present in table [" + columnsTable + "] to test for presence of column [" + columnName + "] of table [" + tableName + "] in schema [" + schemaName + "]");
			}
		}
		try {
			return DbmsUtil.executeIntQuery(conn, query, tableName.toUpperCase(), columnName.toUpperCase()) >= 1;
		} catch (Exception e) {
			log.warn("could not determine correct presence of column [{}] of table [{}]", columnName, tableName, e);
			return false;
		}
	}

	@Override
	public boolean isSequencePresent(Connection conn, String schemaOwner, String tableName, String sequenceName) {
		log.warn("could not determine presence of sequence [{}]", sequenceName);
		return true;
	}

	@Override
	public boolean hasIndexOnColumns(Connection conn, String schemaOwner, String tableName, List<String> columns) throws DbmsException {
		log.warn("could not determine presence of index columns on table [{}]", tableName);
		return true;
	}

	@Override
	public boolean isConstraintViolation(SQLException e) {
		String sqlState = e.getSQLState();
		return sqlState != null && sqlState.startsWith("23");
	}

	@Override
	public String getLength(String column) {
		return "LENGTH(" + column + ")";
	}

	@Override
	public String getBooleanValue(boolean value) {
		return ("" + value).toUpperCase();
	}

	protected ISqlTranslator createTranslator(String source, String target) throws DbmsException {
		return new SqlTranslator(source, target);
	}

	@Nullable
	protected ISqlTranslator getSqlTranslator(@Nonnull String sqlDialectFrom) throws DbmsException {
		String translatorKey = sqlDialectFrom + "->" + getDbmsName();
		if (!sqlTranslators.containsKey(translatorKey)) {
			try {
				ISqlTranslator translator = createTranslator(sqlDialectFrom, getDbmsName());
				if (!translator.canConvert(sqlDialectFrom, getDbmsName())) {
					sqlTranslators.put(translatorKey, null); // avoid trying to set up the translator again the next time
					return null;
				}
				sqlTranslators.put(translatorKey, translator);
				return translator;
			} catch (IllegalArgumentException e) {
				sqlTranslators.put(translatorKey, null); // avoid trying to set up the translator again the next time
				return null;
			} catch (Exception e) {
				throw new DbmsException("Could not translate sql query from " + sqlDialectFrom + " to " + getDbmsName(), e);
			}
		}
		return sqlTranslators.get(translatorKey);
	}

	@Override
	@Nonnull
	public String convertQuery(@Nonnull String query, @Nonnull String sqlDialectFrom) throws SQLException, DbmsException {
		if (!isQueryConversionRequired(sqlDialectFrom)) {
			return query;
		}
		ISqlTranslator translator = getSqlTranslator(sqlDialectFrom);
		if (translator == null) {
			warnConvertQuery(sqlDialectFrom);
			return query;
		}
		List<String> multipleQueries = splitQuery(query);
		StringBuilder convertedQueries = new StringBuilder();
		for (String singleQuery : multipleQueries) {
			String convertedQuery = translator.translate(singleQuery);
			if (convertedQuery != null) {
				if (!convertedQueries.isEmpty()) {
					convertedQueries.append("\n");
				}
				convertedQueries.append(convertedQuery);
			}
		}
		return convertedQueries.toString();
	}

	protected void warnConvertQuery(String sqlDialectFrom) {
		log.warn("don't know how to convert queries from [{}] to [{}]", sqlDialectFrom, getDbmsName());
	}

	protected boolean isQueryConversionRequired(String sqlDialectFrom) {
		return StringUtils.isNotEmpty(sqlDialectFrom) && StringUtils.isNotEmpty(getDbmsName()) && !sqlDialectFrom.equalsIgnoreCase(getDbmsName());
	}

	protected List<String> splitQuery(String query) {
		// A query can contain multiple queries separated by a semicolon
		List<String> splittedQueries = new ArrayList<>();
		if (!query.contains(";")) {
			splittedQueries.add(query);
		} else {
			int i = 0;
			int j = 0;
			while (j < query.length()) {
				if (query.charAt(j) == ';') {
					String line = query.substring(i, j + 1);
					// A semicolon between single quotes is ignored (number of single quotes in the query must be zero or an even number)
					int countApos = StringUtils.countMatches(line, "'");
					// A semicolon directly after 'END' is ignored when there is also a 'BEGIN' in the query
					int countBegin = StringUtil.countRegex(line.toUpperCase().replaceAll("\\s+", "  "), "\\sBEGIN\\s");
					int countEnd = StringUtil.countRegex(line.toUpperCase().replace(";", "; "), "\\sEND;");
					if ((countApos == 0 || (countApos & 1) == 0) && countBegin == countEnd) {
						splittedQueries.add(line.trim());
						i = j + 1;
					}
				}
				j++;
			}
			if (j > i)
				splittedQueries.add(query.substring(i, j).trim());
		}
		return splittedQueries;
	}
}
