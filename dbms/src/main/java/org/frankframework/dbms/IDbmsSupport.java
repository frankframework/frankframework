/*
   Copyright 2013, 2015, 2018, 2019 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLType;
import java.util.Date;
import java.util.List;

import jakarta.annotation.Nonnull;

/**
 * Interface to define DBMS specific SQL implementations.
 *
 * @author Gerrit van Brakel
 * @since
 */
public interface IDbmsSupport {

	Dbms getDbms();

	default String getDbmsName() {
		return getDbms().getKey();
	}

	boolean isParameterTypeMatchRequired();

	boolean hasSkipLockedFunctionality();

	/**
	 * SQL String returning current date and time of dbms.
	 */
	String getSysDate();

	String getDateAndOffset(String dateValue, int daysOffset);

	/**
	 * http://en.wikipedia.org/wiki/DUAL_table
	 */
	String getFromForTablelessSelect();

	String getAutoIncrementKeyFieldType();

	boolean autoIncrementKeyMustBeInserted();

	String autoIncrementInsertValue(String sequenceName);

	boolean autoIncrementUsesSequenceObject();

	String getTimestampFieldType();

	String getDatetimeLiteral(Date date);

	String getTimestampAsDate(String columnName);

	boolean isClobType(final ResultSetMetaData rsmeta, final int colNum) throws SQLException;

	Reader getClobReader(ResultSet rs, int column) throws SQLException, DbmsException;

	Reader getClobReader(ResultSet rs, String column) throws SQLException, DbmsException;

	Object getClobHandle(ResultSet rs, int column) throws SQLException, DbmsException;

	Writer getClobWriter(ResultSet rs, int column, Object clobHandle) throws SQLException, DbmsException;

	void updateClob(ResultSet rs, int column, Object clobHandle) throws SQLException, DbmsException;

	void updateClob(ResultSet rs, String column, Object clobHandle) throws SQLException, DbmsException;

	// CLOB insert/update methods, to support applying parameters for INSERT and UPDATE statements
	Object getClobHandle(PreparedStatement stmt, int column) throws SQLException, DbmsException;

	Writer getClobWriter(PreparedStatement stmt, int column, Object clobHandle) throws SQLException, DbmsException;

	void applyClobParameter(PreparedStatement stmt, int column, Object clobHandle) throws SQLException, DbmsException;


	String getBlobFieldType();

	boolean isBlobType(final ResultSetMetaData rsmeta, final int colNum) throws SQLException;

	InputStream getBlobInputStream(ResultSet rs, int column) throws SQLException, DbmsException;

	InputStream getBlobInputStream(ResultSet rs, String column) throws SQLException, DbmsException;

	Object getBlobHandle(ResultSet rs, int column) throws SQLException, DbmsException;

	OutputStream getBlobOutputStream(ResultSet rs, int column, Object blobHandle) throws SQLException, DbmsException;

	void updateBlob(ResultSet rs, int column, Object blobHandle) throws SQLException, DbmsException;

	void updateBlob(ResultSet rs, String column, Object blobHandle) throws SQLException, DbmsException;

	// BLOB insert/update methods, to support applying parameters for INSERT and UPDATE statements
	Object getBlobHandle(PreparedStatement stmt, int column) throws SQLException, DbmsException;

	OutputStream getBlobOutputStream(PreparedStatement stmt, int column, Object blobInsertHandle) throws SQLException, DbmsException;

	void applyBlobParameter(PreparedStatement stmt, int column, Object blobInsertHandle) throws SQLException, DbmsException;

	String getTextFieldType();

	String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery) throws DbmsException;

	String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws DbmsException;

	String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery) throws DbmsException;

	String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws DbmsException;

	/**
	 * Modify the provided selectQuery in such a way that the resulting query will not be blocked by locks, and will avoid placing locks itself as much as possible.
	 * Preferably, the effective isolation level is READ_COMMITTED (committed rows of other transactions may be read), but if placing locks can be avoided by an isolation level similar to READ_UNCOMMITTED, that is allowed too.
	 * Should return the query unmodified if no special action is required.
	 * For an example, see {@link MsSqlServerDbmsSupport#prepareQueryTextForNonLockingRead(String)}
	 */
	String prepareQueryTextForNonLockingRead(String selectQuery) throws DbmsException;

	String provideIndexHintAfterFirstKeyword(String tableName, String indexName);

	String provideFirstRowsHintAfterFirstKeyword(int rowCount);

	String provideTrailingFirstRowsHint(int rowCount);

	String getSchema(Connection conn) throws DbmsException;

	@Nonnull
	String convertQuery(@Nonnull String query, @Nonnull String sqlDialectFrom) throws SQLException, DbmsException;

	ResultSet getTableColumns(Connection conn, String tableName) throws DbmsException;

	ResultSet getTableColumns(Connection conn, String schemaName, String tableName) throws DbmsException;

	ResultSet getTableColumns(Connection conn, String schemaName, String tableName, String columnNamePattern) throws DbmsException;

	boolean isTablePresent(Connection conn, String tableName) throws DbmsException;

	boolean isTablePresent(Connection conn, String schemaName, String tableName) throws DbmsException;

	boolean isColumnPresent(Connection conn, String tableName, String columnName) throws DbmsException;

	boolean isColumnPresent(Connection conn, String schemaName, String tableName, String columnName) throws DbmsException;

	boolean isSequencePresent(Connection conn, String schemaOwner, String tableName, String sequenceName);

	boolean hasIndexOnColumn(Connection conn, String schemaOwner, String tableName, String columnName) throws DbmsException;

	boolean hasIndexOnColumns(Connection conn, String schemaOwner, String tableName, List<String> columns) throws DbmsException;

	boolean isConstraintViolation(SQLException e);

	String getLength(String column);

	String getBooleanValue(boolean value);

	/**
	 * DBMS Feature flag: is it possible to call a stored procedure that returns the results of a SELECT statement
	 * directly, without needing a REFCURSOR OUT parameter.
	 *
	 * @return true for database that can directly return SELECT results. Not supported for PostgreSQL and Oracle.
	 */
	default boolean isStoredProcedureResultSetSupported() {
		return true;
	}

	default SQLType getCursorSqlType() {
		return JDBCType.REF_CURSOR;
	}

	default boolean canFetchStatementParameterMetaData() {
		return true;
	}

	/**
	 * @param resultSetMetaData
	 * @param columnNumber
	 * @return whether the columnNumber metadata is defined as a RowVersion/Timestamp (MSSQL - specific, see {@link MsSqlServerDbmsSupport}).
	 * @throws SQLException
	 */
	default boolean isRowVersionTimestamp(ResultSetMetaData resultSetMetaData, int columnNumber) throws SQLException {
		return false;
	}
}
