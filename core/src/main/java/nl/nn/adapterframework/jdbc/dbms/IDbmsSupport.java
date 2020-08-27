/*
   Copyright 2013, 2015, 2018, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.OutputStream;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.QueryExecutionContext;

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
	boolean mustInsertEmptyClobBeforeData();
	String emptyClobValue();
	String getUpdateClobQuery(String table, String clobField, String keyField);
	Object getClobUpdateHandle(ResultSet rs, int column) throws SQLException, JdbcException;
	Object getClobUpdateHandle(ResultSet rs, String column) throws SQLException, JdbcException;
	Writer getClobWriter(ResultSet rs, int column, Object clobUpdateHandle) throws SQLException, JdbcException;
	Writer getClobWriter(ResultSet rs, String column, Object clobUpdateHandle) throws SQLException, JdbcException;
	void updateClob(ResultSet rs, int column, Object clobUpdateHandle) throws SQLException, JdbcException;
	void updateClob(ResultSet rs, String column, Object clobUpdateHandle) throws SQLException, JdbcException;

	String getBlobFieldType();
	boolean mustInsertEmptyBlobBeforeData();
	String emptyBlobValue();
	String getUpdateBlobQuery(String table, String clobField, String keyField);
	Object getBlobUpdateHandle(ResultSet rs, int column) throws SQLException, JdbcException;
	Object getBlobUpdateHandle(ResultSet rs, String column) throws SQLException, JdbcException;
	OutputStream getBlobOutputStream(ResultSet rs, int column, Object blobUpdateHandle) throws SQLException, JdbcException;
	OutputStream getBlobOutputStream(ResultSet rs, String column, Object blobUpdateHandle) throws SQLException, JdbcException;
	void updateBlob(ResultSet rs, int column, Object blobUpdateHandle) throws SQLException, JdbcException;
	void updateBlob(ResultSet rs, String column, Object blobUpdateHandle) throws SQLException, JdbcException;

	String getTextFieldType();

	String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery) throws JdbcException;
	String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws JdbcException;
	String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery) throws JdbcException;
	String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws JdbcException;
	String getFirstRecordQuery(String tableName) throws JdbcException;

	/**
	 * Modify the provided selectQuery in such a way that the resulting query will not be blocked by locks, and will avoid placing locks itself as much as possible.
	 * Will always be executed together with {@link #prepareSessionForNonLockingRead(Connection)}.
	 * Preferably, the effective isolation level is READ_COMMITTED (commited rows of other transactions may be read), but if placing locks can be avoid by an isolation level similar to READ_UNCOMMITTED, that is allowed too.
	 * Should return the query unmodified if no special action is required.
	 * For an example, see {@link MsSqlServerDbmsSupport#prepareQueryTextForNonLockingRead(String)}
	 */
	String prepareQueryTextForNonLockingRead(String selectQuery) throws JdbcException;
	/**
	 * Modify the connection in such a way that it when select queries, prepared by {@link #prepareQueryTextForNonLockingRead(String)} or by {@link #prepareQueryTextForWorkQueuePeeking(int,String)}, 
	 * are executed, they will not be blocked by locks, and will avoid placing locks itself as much as possible.
	 * Preferably isolation level is READ_COMMITTED (commited rows of other transactions may be read), but if placing locks can be avoid by an isolation level similar to READ_UNCOMMITTED, that is allowed too.
	 * After the query is executed, jdbcSession.close() will be called, to return the connection to its normal state (which is expected to be REPEATABLE_READ).
	 * Should return null if no preparation of the connection is required.
	 * For an example, see {@link MySqlDbmsSupport#prepareSessionForNonLockingRead(Connection)}
	 */
	JdbcSession prepareSessionForNonLockingRead(Connection conn) throws JdbcException;

	String provideIndexHintAfterFirstKeyword(String tableName, String indexName);
	String provideFirstRowsHintAfterFirstKeyword(int rowCount);
	String provideTrailingFirstRowsHint(int rowCount);

	String getSchema(Connection conn) throws JdbcException;
	
	void convertQuery(QueryExecutionContext queryExecutionContext, String sqlDialectFrom) throws SQLException, JdbcException;
	
	boolean isTablePresent(Connection conn, String tableName) throws JdbcException;
	boolean isTablePresent(Connection conn, String schemaName, String tableName) throws JdbcException;
	boolean isColumnPresent(Connection conn, String tableName, String columnName) throws JdbcException;
	boolean isColumnPresent(Connection conn, String schemaName, String tableName, String columnName) throws JdbcException;
	boolean isIndexPresent(Connection conn, String schemaOwner, String tableName, String indexName);
	boolean isSequencePresent(Connection conn, String schemaOwner, String tableName, String sequenceName);
	boolean isIndexColumnPresent(Connection conn, String schemaOwner, String tableName, String indexName, String columnName);
	int getIndexColumnPosition(Connection conn, String schemaOwner, String tableName, String indexName, String columnName);
	boolean hasIndexOnColumn(Connection conn, String schemaOwner, String tableName, String columnName);
	boolean hasIndexOnColumns(Connection conn, String schemaOwner, String tableName, List<String> columns);
	String getSchemaOwner(Connection conn) throws SQLException, JdbcException;

	boolean isUniqueConstraintViolation(SQLException e);

	String getRowNumber(String order, String sort);
	String getRowNumberShortName();

	String getLength(String column);

	String getIbisStoreSummaryQuery();

	String getBooleanFieldType();
	String getBooleanValue(boolean value);
}
