/*
Copyright 2020, 2021 WeAreFrank!

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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.StringUtils;
//import org.postgresql.largeobject.LargeObject;
//import org.postgresql.largeobject.LargeObjectManager;

import lombok.SneakyThrows;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.StreamUtil;

/**
* Support for PostgreSQL.
* 
* Limitations:
*   PostgreSQL blobs and clobs are handled via byte arrays that are kept in memory. The maximum size of blobs and clobs is therefor limited by memory size.
*/
public class PostgresqlDbmsSupport extends GenericDbmsSupport {

	private final boolean useLargeObjectFeature=false;
	
	@Override
	public Dbms getDbms() {
		return Dbms.POSTGRESQL;
	}

	@Override
	public boolean isParameterTypeMatchRequired() {
		return true;
	}

	@Override
	public boolean hasSkipLockedFunctionality() {
		return true;
	}


	@Override
	public String getDatetimeLiteral(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formattedDate = formatter.format(date);
		return "timestamp '" + formattedDate + "'";
	}

	@Override
	public String getTimestampAsDate(String columnName) {
		return "DATE("+columnName+")";
	}

	@Override
	public String getDateAndOffset(String dateValue, int daysOffset) {
		return "DATE ("+dateValue+ ") + " + daysOffset ;
	}


	
//	private LargeObjectManager getLargeObjectManager(Statement stmt) throws SQLException {
//		return stmt.getConnection().unwrap(org.postgresql.PGConnection.class).getLargeObjectAPI();
//	}
	
	private Object createLob(Statement stmt) throws SQLException {
		if (useLargeObjectFeature) {
			throw new IllegalStateException("Handling BLOBs and CLOBs as LargeObjects not available");
//			LargeObjectManager lobj = getLargeObjectManager(stmt);
//			// Create a new large object
//			long oid = lobj.createLO(LargeObjectManager.READ | LargeObjectManager.WRITE);
//			return oid;
		}
		return new ByteArrayOutputStream();
	}

	private OutputStream openLobOutputStream(Statement stmt, Object blobUpdateHandle) throws SQLException {
//		if (useLargeObjectFeature) {
//			LargeObjectManager lobj = getLargeObjectManager(stmt);
//			long oid = (long)blobUpdateHandle;
//			LargeObject obj = lobj.open(oid, LargeObjectManager.WRITE);
//			return obj.getOutputStream();
//		}
		return (ByteArrayOutputStream)blobUpdateHandle;
	}
	
	private void updateLob(ResultSet rs, int column, Object blobUpdateHandle, boolean binary) throws SQLException {
		if (useLargeObjectFeature) {
			rs.updateLong(column, (long)blobUpdateHandle);
			return;
		}
		if (binary) {
			rs.updateBytes(column, (((ByteArrayOutputStream)blobUpdateHandle).toByteArray()));
		} else {
			rs.updateString(column, new String(((ByteArrayOutputStream)blobUpdateHandle).toByteArray(),Charsets.UTF_8));
		}
	}
	private void updateLob(ResultSet rs, String column, Object blobUpdateHandle, boolean binary) throws SQLException {
		if (useLargeObjectFeature) {
			rs.updateLong(column, (long)blobUpdateHandle);
			return;
		}
		if (binary) {
			rs.updateBytes(column, (((ByteArrayOutputStream)blobUpdateHandle).toByteArray()));
		} else {
			rs.updateString(column, new String(((ByteArrayOutputStream)blobUpdateHandle).toByteArray(),Charsets.UTF_8));
		}
	}
	private void updateLob(PreparedStatement stmt, int column, Object blobUpdateHandle, boolean binary) throws SQLException {
		if (useLargeObjectFeature) {
			stmt.setLong(column, (long)blobUpdateHandle);
			return;
		}
		if (binary) {
			stmt.setBytes(column, (((ByteArrayOutputStream)blobUpdateHandle).toByteArray()));
		} else {
			stmt.setString(column, new String(((ByteArrayOutputStream)blobUpdateHandle).toByteArray(),Charsets.UTF_8));
		}
	}


	@Override
	public String getClobFieldType() {
		return "TEXT";
	}
	@Override
	public boolean isClobType(final ResultSetMetaData rsmeta, final int colNum) throws SQLException {
		return rsmeta.getColumnType(colNum)==Types.VARCHAR && "text".equals(rsmeta.getColumnTypeName(colNum));
	}
	
	@Override
	public Reader getClobReader(ResultSet rs, int column) throws SQLException, JdbcException {
		return rs.getCharacterStream(column);
	}
	@Override
	public Reader getClobReader(ResultSet rs, String column) throws SQLException, JdbcException {
		return rs.getCharacterStream(column);
	}

	@Override
	public Object getClobHandle(ResultSet rs, int column) throws SQLException, JdbcException {
		return createLob(rs.getStatement());
	}
	@Override
	public Object getClobHandle(ResultSet rs, String column) throws SQLException, JdbcException {
		return createLob(rs.getStatement());
	}
	@Override
	public Object getClobHandle(PreparedStatement stmt, int column) throws SQLException, JdbcException {
		return createLob(stmt);
	}

	@Override
	@SneakyThrows(UnsupportedEncodingException.class)
	public Writer getClobWriter(ResultSet rs, int column, Object clobHandle) throws SQLException, JdbcException {
		return new OutputStreamWriter(openLobOutputStream(rs.getStatement(), clobHandle), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}
	@Override
	@SneakyThrows(UnsupportedEncodingException.class)
	public Writer getClobWriter(ResultSet rs, String column, Object clobHandle) throws SQLException, JdbcException {
		return new OutputStreamWriter(openLobOutputStream(rs.getStatement(), clobHandle), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}
	@Override
	@SneakyThrows(UnsupportedEncodingException.class)
	public Writer getClobWriter(PreparedStatement stmt, int column, Object clobHandle) throws SQLException, JdbcException {
		return new OutputStreamWriter(openLobOutputStream(stmt, clobHandle), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}

	@Override
	public void updateClob(ResultSet rs, int column, Object clobHandle) throws SQLException, JdbcException {
		updateLob(rs, column, clobHandle, false);
	}
	@Override
	public void updateClob(ResultSet rs, String column, Object clobHandle) throws SQLException, JdbcException {
		updateLob(rs, column, clobHandle, false);
	}
	@Override
	public void applyClobParameter(PreparedStatement stmt, int column, Object clobHandle) throws SQLException, JdbcException {
		updateLob(stmt, column, clobHandle, false);
	}


	@Override
	public String getBlobFieldType() {
		return "BYTEA";
	}
	@Override
	public InputStream getBlobInputStream(ResultSet rs, int column) throws SQLException, JdbcException {
		return rs.getBinaryStream(column);
	}
	@Override
	public InputStream getBlobInputStream(ResultSet rs, String column) throws SQLException, JdbcException{
		return rs.getBinaryStream(column);
	}
	
	@Override
	public Object getBlobHandle(ResultSet rs, int column) throws SQLException, JdbcException {
		return createLob(rs.getStatement());
	}
	@Override
	public Object getBlobHandle(ResultSet rs, String column) throws SQLException, JdbcException {
		return createLob(rs.getStatement());
	}
	@Override
	public Object getBlobHandle(PreparedStatement stmt, int column) throws SQLException, JdbcException {
		return createLob(stmt);
	}

	@Override
	public OutputStream getBlobOutputStream(ResultSet rs, int column, Object blobHandle) throws SQLException, JdbcException {
		return openLobOutputStream(rs.getStatement(), blobHandle);
	}
	@Override
	public OutputStream getBlobOutputStream(ResultSet rs, String column, Object blobHandle) throws SQLException, JdbcException {
		return openLobOutputStream(rs.getStatement(), blobHandle);
	}
	@Override
	public OutputStream getBlobOutputStream(PreparedStatement stmt, int column, Object blobHandle) throws SQLException, JdbcException {
		return openLobOutputStream(stmt, blobHandle);
	}

	@Override
	public void updateBlob(ResultSet rs, int column, Object blobHandle) throws SQLException, JdbcException {
		updateLob(rs, column, blobHandle, true);
	}
	@Override
	public void updateBlob(ResultSet rs, String column, Object blobHandle) throws SQLException, JdbcException {
		updateLob(rs, column, blobHandle, true);
	}
	@Override
	public void applyBlobParameter(PreparedStatement stmt, int column, Object blobHandle) throws SQLException, JdbcException {
		updateLob(stmt, column, blobHandle, true);
	}

	
	@Override
	public boolean isTablePresent(Connection conn, String tableName) throws JdbcException {
		return doIsTablePresent(conn, "pg_catalog.pg_tables", "schemaname", "tablename", "public", tableName);
	}

	@Override
	public boolean isColumnPresent(Connection conn, String schemaName, String tableName, String columnName) throws JdbcException {
		return doIsColumnPresent(conn, "information_schema.columns", "TABLE_SCHEMA", "TABLE_NAME", "COLUMN_NAME", schemaName!=null?schemaName:"public", tableName, columnName);
	}

	@Override
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		if (wait < 0) {
			return selectQuery+(batchSize>0?" LIMIT "+batchSize:"")+" FOR UPDATE SKIP LOCKED";
		} else {
			throw new IllegalArgumentException(getDbms()+" does not support setting lock wait timeout in query");
		}
	}

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		if (wait < 0) {
			return selectQuery+(batchSize>0?" LIMIT "+batchSize:"")+" FOR SHARE SKIP LOCKED"; // take shared lock, to be able to use 'skip locked'
		} else {
			throw new IllegalArgumentException(getDbms()+" does not support setting lock wait timeout in query");
		}
	}

	// commented out prepareSessionForNonLockingRead(), see https://dev.mysql.com/doc/refman/8.0/en/innodb-consistent-read.html
//	@Override
//	public JdbcSession prepareSessionForNonLockingRead(Connection conn) throws JdbcException {
//		JdbcUtil.executeStatement(conn, "SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
//		JdbcUtil.executeStatement(conn, "START TRANSACTION");
//		return new JdbcSession() {
//
//			@Override
//			public void close() throws Exception {
//				JdbcUtil.executeStatement(conn, "COMMIT");
//			}
//			
//		};
//	}


	public int alterAutoIncrement(Connection connection, String tableName, int startWith) throws JdbcException {
		String query = "ALTER TABLE " + tableName + " AUTO_INCREMENT=" + startWith;
		return JdbcUtil.executeIntQuery(connection, query);
	}

	@Override
	public String getInsertedAutoIncrementValueQuery(String sequenceName) {
		return "SELECT LAST_INSERT_ID()";
	}


	// DDL related methods, have become more or less obsolete (and untested) with the introduction of Liquibase for table definitions
	@Override
	public String getAutoIncrementKeyFieldType() {
		return "INT AUTO_INCREMENT";
	}

}
