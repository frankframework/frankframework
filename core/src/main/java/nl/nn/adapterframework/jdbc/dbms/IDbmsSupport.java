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
package nl.nn.adapterframework.jdbc.dbms;

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

import javax.annotation.Nonnull;

import nl.nn.adapterframework.jdbc.JdbcException;

/**
 * Interface to define DBMS specific SQL implementations.
 *
 * @author  Gerrit van Brakel
 * @since
 */
public interface IDbmsSupport {

	/**
	 * Numeric value defining database type, defined in {@link DbmsSupportFactory}.
	 */
	Dbms getDbms();
	String getDbmsName();

	boolean isParameterTypeMatchRequired();
	boolean hasSkipLockedFunctionality();
	/**
	 * SQL String returning current date and time of dbms.
	 */
	String getSysDate();
	String getDateAndOffset(String dateValue, int daysOffset);

	String getNumericKeyFieldType();

	/**
	 * http://en.wikipedia.org/wiki/DUAL_table
	 */
	String getFromForTablelessSelect();

	String getAutoIncrementKeyFieldType();
	boolean autoIncrementKeyMustBeInserted();
	String autoIncrementInsertValue(String sequenceName);
	boolean autoIncrementUsesSequenceObject();
	String getInsertedAutoIncrementValueQuery(String sequenceName);

	String getTimestampFieldType();
	String getDatetimeLiteral(Date date);
	String getTimestampAsDate(String columnName);

	String getClobFieldType();
	boolean isClobType(final ResultSetMetaData rsmeta, final int colNum) throws SQLException;
	boolean mustInsertEmptyClobBeforeData();
	String emptyClobValue();
	Reader getClobReader(ResultSet rs, int column) throws SQLException, JdbcException;
	Reader getClobReader(ResultSet rs, String column) throws SQLException, JdbcException;

	// CLOB update methods, to support updating ResultSets using SELECT ... FOR UPDATE statements
	String getUpdateClobQuery(String table, String clobField, String keyField);
	Object getClobHandle(ResultSet rs, int column) throws SQLException, JdbcException;
	Object getClobHandle(ResultSet rs, String column) throws SQLException, JdbcException;
	Writer getClobWriter(ResultSet rs, int column, Object clobHandle) throws SQLException, JdbcException;
	Writer getClobWriter(ResultSet rs, String column, Object clobHandle) throws SQLException, JdbcException;
	void updateClob(ResultSet rs, int column, Object clobHandle) throws SQLException, JdbcException;
	void updateClob(ResultSet rs, String column, Object clobHandle) throws SQLException, JdbcException;

	// CLOB insert/update methods, to support applying parameters for INSERT and UPDATE statements
	Object getClobHandle(PreparedStatement stmt, int column) throws SQLException, JdbcException;
	Writer getClobWriter(PreparedStatement stmt, int column, Object clobHandle) throws SQLException, JdbcException;
	void applyClobParameter(PreparedStatement stmt, int column, Object clobHandle) throws SQLException, JdbcException;


	String getBlobFieldType();
	boolean isBlobType(final ResultSetMetaData rsmeta, final int colNum) throws SQLException;
	boolean mustInsertEmptyBlobBeforeData();
	String emptyBlobValue();
	InputStream getBlobInputStream(ResultSet rs, int column) throws SQLException, JdbcException;
	InputStream getBlobInputStream(ResultSet rs, String column) throws SQLException, JdbcException;

	// BLOB update methods, to support updating ResultSets using SELECT ... FOR UPDATE statements
	String getUpdateBlobQuery(String table, String clobField, String keyField);
	Object getBlobHandle(ResultSet rs, int column) throws SQLException, JdbcException;
	Object getBlobHandle(ResultSet rs, String column) throws SQLException, JdbcException;
	OutputStream getBlobOutputStream(ResultSet rs, int column, Object blobHandle) throws SQLException, JdbcException;
	OutputStream getBlobOutputStream(ResultSet rs, String column, Object blobHandle) throws SQLException, JdbcException;
	void updateBlob(ResultSet rs, int column, Object blobHandle) throws SQLException, JdbcException;
	void updateBlob(ResultSet rs, String column, Object blobHandle) throws SQLException, JdbcException;

	// BLOB insert/update methods, to support applying parameters for INSERT and UPDATE statements
	Object getBlobHandle(PreparedStatement stmt, int column) throws SQLException, JdbcException;
	OutputStream getBlobOutputStream(PreparedStatement stmt, int column, Object blobInsertHandle) throws SQLException, JdbcException;
	void applyBlobParameter(PreparedStatement stmt, int column, Object blobInsertHandle) throws SQLException, JdbcException;

	String getTextFieldType();

	String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery) throws JdbcException;
	String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws JdbcException;
	String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery) throws JdbcException;
	String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws JdbcException;
	String getFirstRecordQuery(String tableName) throws JdbcException;

	/**
	 * Modify the provided selectQuery in such a way that the resulting query will not be blocked by locks, and will avoid placing locks itself as much as possible.
	 * Preferably, the effective isolation level is READ_COMMITTED (committed rows of other transactions may be read), but if placing locks can be avoided by an isolation level similar to READ_UNCOMMITTED, that is allowed too.
	 * Should return the query unmodified if no special action is required.
	 * For an example, see {@link MsSqlServerDbmsSupport#prepareQueryTextForNonLockingRead(String)}
	 */
	String prepareQueryTextForNonLockingRead(String selectQuery) throws JdbcException;

	String provideIndexHintAfterFirstKeyword(String tableName, String indexName);
	String provideFirstRowsHintAfterFirstKeyword(int rowCount);
	String provideTrailingFirstRowsHint(int rowCount);

	String getSchema(Connection conn) throws JdbcException;

	@Nonnull
	String convertQuery(@Nonnull String query, @Nonnull String sqlDialectFrom) throws SQLException, JdbcException;

	ResultSet getTableColumns(Connection conn, String tableName) throws JdbcException;
	ResultSet getTableColumns(Connection conn, String schemaName, String tableName) throws JdbcException;
	ResultSet getTableColumns(Connection conn, String schemaName, String tableName, String columnNamePattern) throws JdbcException;
	boolean isTablePresent(Connection conn, String tableName) throws JdbcException;
	boolean isTablePresent(Connection conn, String schemaName, String tableName) throws JdbcException;
	boolean isColumnPresent(Connection conn, String tableName, String columnName) throws JdbcException;
	boolean isColumnPresent(Connection conn, String schemaName, String tableName, String columnName) throws JdbcException;
	boolean isIndexPresent(Connection conn, String schemaOwner, String tableName, String indexName);
	boolean isSequencePresent(Connection conn, String schemaOwner, String tableName, String sequenceName);
	boolean hasIndexOnColumn(Connection conn, String schemaOwner, String tableName, String columnName) throws JdbcException;
	boolean hasIndexOnColumns(Connection conn, String schemaOwner, String tableName, List<String> columns) throws JdbcException;
	String getSchemaOwner(Connection conn) throws SQLException, JdbcException;

	boolean isConstraintViolation(SQLException e);

	String getRowNumber(String order, String sort);
	String getRowNumberShortName();

	String getLength(String column);

	String getIbisStoreSummaryQuery();

	String getBooleanFieldType();
	String getBooleanValue(boolean value);
	String getCleanUpIbisstoreQuery(String tableName, String keyField, String typeField, String expiryDateField, int maxRows);

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
}
