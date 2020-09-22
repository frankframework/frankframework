/*
Copyright 2020 WeAreFrank!

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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.codec.Charsets;
import org.apache.commons.lang.StringUtils;
//import org.postgresql.largeobject.LargeObject;
//import org.postgresql.largeobject.LargeObjectManager;

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

	@Override
	public Reader getClobReader(ResultSet rs, int column) throws SQLException, JdbcException {
		return rs.getCharacterStream(column);
	}
	@Override
	public Reader getClobReader(ResultSet rs, String column) throws SQLException, JdbcException {
		return rs.getCharacterStream(column);
	}

	@Override
	public String getClobFieldType() {
		return "TEXT";
	}
	
//	private LargeObjectManager getLargeObjectManager(ResultSet rs) throws SQLException {
//		return rs.getStatement().getConnection().unwrap(org.postgresql.PGConnection.class).getLargeObjectAPI();
//	}
	
	private Object createLob(ResultSet rs) throws SQLException {
		if (useLargeObjectFeature) {
			throw new IllegalStateException("Handling BLOBs and CLOBs as LargeObjects not available");
//			LargeObjectManager lobj = getLargeObjectManager(rs);
//			// Create a new large object
//			long oid = lobj.createLO(LargeObjectManager.READ | LargeObjectManager.WRITE);
//			return oid;
		}
		return new ByteArrayOutputStream();
	}

	private OutputStream openLobOutputStream(ResultSet rs, Object blobUpdateHandle) throws SQLException {
//		if (useLargeObjectFeature) {
//			LargeObjectManager lobj = getLargeObjectManager(rs);
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
	
	@Override
	public Object getClobUpdateHandle(ResultSet rs, int column) throws SQLException, JdbcException {
		return createLob(rs);
	}
	@Override
	public Object getClobUpdateHandle(ResultSet rs, String column) throws SQLException, JdbcException {
		return createLob(rs);
	}

	@Override
	public String getBlobFieldType() {
		return "BYTEA";
	}
	
	@Override
	public Object getBlobUpdateHandle(ResultSet rs, int column) throws SQLException, JdbcException {
		return createLob(rs);
	}
	@Override
	public Object getBlobUpdateHandle(ResultSet rs, String column) throws SQLException, JdbcException {
		return createLob(rs);
	}


	@Override
	public OutputStream getBlobOutputStream(ResultSet rs, int column, Object blobUpdateHandle) throws SQLException, JdbcException {
		return openLobOutputStream(rs, blobUpdateHandle);
	}
	@Override
	public OutputStream getBlobOutputStream(ResultSet rs, String column, Object blobUpdateHandle) throws SQLException, JdbcException {
		return openLobOutputStream(rs, blobUpdateHandle);
	}

	@Override
	public Writer getClobWriter(ResultSet rs, int column, Object clobUpdateHandle) throws SQLException, JdbcException {
		try {
			Writer out = new OutputStreamWriter(openLobOutputStream(rs, clobUpdateHandle), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			return out;
		} catch (UnsupportedEncodingException e) {
			throw new JdbcException(e);
		}
	}
	@Override
	public Writer getClobWriter(ResultSet rs, String column, Object clobUpdateHandle) throws SQLException, JdbcException {
		try {
			Writer out = new OutputStreamWriter(openLobOutputStream(rs, clobUpdateHandle), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			return out;
		} catch (UnsupportedEncodingException e) {
			throw new JdbcException(e);
		}
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
	public void updateBlob(ResultSet rs, int column, Object blobUpdateHandle) throws SQLException, JdbcException {
		updateLob(rs, column, blobUpdateHandle, true);
	}
	@Override
	public void updateBlob(ResultSet rs, String column, Object blobUpdateHandle) throws SQLException, JdbcException {
		updateLob(rs, column, blobUpdateHandle, true);
	}

	@Override
	public void updateClob(ResultSet rs, int column, Object clobUpdateHandle) throws SQLException, JdbcException {
		updateLob(rs, column, clobUpdateHandle, false);
	}
	@Override
	public void updateClob(ResultSet rs, String column, Object clobUpdateHandle) throws SQLException, JdbcException {
		updateLob(rs, column, clobUpdateHandle, false);
	}

	@Override
	public boolean isTablePresent(Connection conn, String tableName) throws JdbcException {
		return doIsTablePresent(conn, "pg_catalog.pg_tables", "schemaname", "tablename", "public", tableName);
	}

	@Override
	public boolean isColumnPresent(Connection conn, String tableName, String columnName) throws JdbcException {
		return doIsColumnPresent(conn, "information_schema.columns", "TABLE_SCHEMA", "TABLE_NAME", "COLUMN_NAME", "public", tableName, columnName);
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
