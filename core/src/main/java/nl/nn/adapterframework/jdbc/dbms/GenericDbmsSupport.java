/*
   Copyright 2013, 2015, 2018, 2019 Nationale-Nederlanden

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
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since  
 */
public class GenericDbmsSupport implements IDbmsSupport {
	protected Logger log = LogUtil.getLogger(this.getClass());

	protected final static String KEYWORD_SELECT="select";

	public String getDbmsName() {
		return "generic";
	}

	public int getDatabaseType() {
		return DbmsSupportFactory.DBMS_GENERIC;
	}

	public String getSysDate() {
		return "NOW()";
	}

	public String getNumericKeyFieldType() {
		return "INT";
	}

	public String getFromForTablelessSelect() {
		return "";
	}

	public String getAutoIncrementKeyFieldType() {
		return "INT DEFAULT AUTOINCREMENT";
	}
	
	public boolean autoIncrementKeyMustBeInserted() {
		return false;
	}

	public String autoIncrementInsertValue(String sequenceName) {
		return null;
	}

	public boolean autoIncrementUsesSequenceObject() {
		return false;
	}
	
	public String getInsertedAutoIncrementValueQuery(String sequenceName) {
		return null;
	}

	public String getTimestampFieldType() {
		return "TIMESTAMP";
	}

	public String getClobFieldType() {
		return "LONG BINARY";
	}
	public boolean mustInsertEmptyClobBeforeData() {
		return false;
	}
	public String getUpdateClobQuery(String table, String clobField, String keyField) {
		return null;
	}
	public String emptyClobValue() {
		return null;
	}

	public Object getClobUpdateHandle(ResultSet rs, int column) throws SQLException, JdbcException {
		Clob clob = rs.getClob(column);
		if (clob==null) {
			throw new JdbcException("no clob found in column ["+column+"]");
		}
		return clob;
	}
	public Object getClobUpdateHandle(ResultSet rs, String column) throws SQLException, JdbcException {
		Clob clob = rs.getClob(column);
		if (clob==null) {
			throw new JdbcException("no clob found in column ["+column+"]");
		}
		return clob;
	}
	
	public Writer getClobWriter(ResultSet rs, int column, Object clobUpdateHandle) throws SQLException, JdbcException {
		Writer out = ((Clob)clobUpdateHandle).setCharacterStream(1L);
		return out;
	}
	public Writer getClobWriter(ResultSet rs, String column, Object clobUpdateHandle) throws SQLException, JdbcException {
		Writer out = ((Clob)clobUpdateHandle).setCharacterStream(1L);
		return out;
	}
	
	public void updateClob(ResultSet rs, int column, Object clobUpdateHandle) throws SQLException, JdbcException {
		// updateClob is not implemented by the WebSphere implementation of ResultSet
		rs.updateClob(column, (Clob)clobUpdateHandle);
	}
	public void updateClob(ResultSet rs, String column, Object clobUpdateHandle) throws SQLException, JdbcException {
		// updateClob is not implemented by the WebSphere implementation of ResultSet
		rs.updateClob(column, (Clob)clobUpdateHandle);
	}

	
	public String getBlobFieldType() {
		return "LONG BINARY";
	}
	public boolean mustInsertEmptyBlobBeforeData() {
		return false;
	}
	public String getUpdateBlobQuery(String table, String blobField, String keyField) {
		return null;
	}
	public String emptyBlobValue() {
		return null;
	}

	public Object getBlobUpdateHandle(ResultSet rs, int column) throws SQLException, JdbcException {
		Blob blob = rs.getBlob(column);
		if (blob==null) {
			throw new JdbcException("no blob found in column ["+column+"]");
		}
		return blob;
	}
	public Object getBlobUpdateHandle(ResultSet rs, String column) throws SQLException, JdbcException {
		Blob blob = rs.getBlob(column);
		if (blob==null) {
			throw new JdbcException("no blob found in column ["+column+"]");
		}
		return blob;
	}
	
	protected  OutputStream getBlobOutputStream(ResultSet rs, Object blobUpdateHandle) throws SQLException, JdbcException {
		OutputStream out = ((Blob)blobUpdateHandle).setBinaryStream(1L);
		return out;
	}
	
	public OutputStream getBlobOutputStream(ResultSet rs, int column, Object blobUpdateHandle) throws SQLException, JdbcException {
		return getBlobOutputStream(rs,blobUpdateHandle);
	}
	public OutputStream getBlobOutputStream(ResultSet rs, String column, Object blobUpdateHandle) throws SQLException, JdbcException {
		return getBlobOutputStream(rs,blobUpdateHandle);
	}
	
	public void updateBlob(ResultSet rs, int column, Object blobUpdateHandle) throws SQLException, JdbcException {
		// updateBlob is not implemented by the WebSphere implementation of ResultSet
		rs.updateBlob(column, (Blob)blobUpdateHandle);
	}
	public void updateBlob(ResultSet rs, String column, Object blobUpdateHandle) throws SQLException, JdbcException {
		// updateBlob is not implemented by the WebSphere implementation of ResultSet
		rs.updateBlob(column, (Blob)blobUpdateHandle);
	}

	
	
	public String getTextFieldType() {
		return "VARCHAR";
	}
	
	
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery) throws JdbcException {
		return prepareQueryTextForWorkQueueReading(batchSize, selectQuery, -1);
	}
	
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		log.warn("don't know how to perform prepareQueryTextForWorkQueueReading for this database type, doing a guess...");
		return selectQuery+" FOR UPDATE";
	}

	public String getFirstRecordQuery(String tableName) throws JdbcException {
		log.warn("don't know how to perform getFirstRecordQuery for this database type, doing a guess...");
		String query="select * from "+tableName+" where ROWNUM=1";
		return query;
	} 

	public String provideIndexHintAfterFirstKeyword(String tableName, String indexName) {
		return "";
	}

	public String provideFirstRowsHintAfterFirstKeyword(int rowCount) {
		return "";
	}
	public String provideTrailingFirstRowsHint(int rowCount) {
		return "";
	}


	public String getSchema(Connection conn) throws JdbcException {
		return null;
	}

	protected boolean doIsTablePresent(Connection conn, String tablesTable, String schemaColumn, String tableNameColumn, String schemaName, String tableName) throws JdbcException {
		String query="select count(*) from "+tablesTable+" where upper("+tableNameColumn+")=?";
		if (StringUtils.isNotEmpty(schemaName)) {
			if (StringUtils.isNotEmpty(schemaColumn)) {
				query+=" and upper("+schemaColumn+")='"+schemaName.toUpperCase()+"'";
			} else {
				throw new JdbcException("no schemaColumn present in table ["+tablesTable+"] to test for presence of table ["+tableName+"] in schema ["+schemaName+"]");
			}
		}
		try {
			return JdbcUtil.executeIntQuery(conn, query, tableName.toUpperCase())>=1;
		} catch (Exception e) {
			log.warn("could not determine presence of table ["+tableName+"]",e);
			return false;
		}
	}

	private final boolean useMetaDataForTableExists=false;
	public boolean isTablePresent(Connection conn, String tableName) throws JdbcException {
		try {
			PreparedStatement stmt = null;
			if (useMetaDataForTableExists) {
				DatabaseMetaData dbmeta = conn.getMetaData();
				ResultSet tableset = dbmeta.getTables(null, null, tableName, null);
				return !tableset.isAfterLast();
			} 
			String query=null;
			try {
				query="select count(*) from "+tableName;
				log.debug("create statement to check for existence of ["+tableName+"] using query ["+query+"]");
				stmt = conn.prepareStatement(query);
				log.debug("execute statement");
				ResultSet rs = stmt.executeQuery();
				log.debug("statement executed");
				rs.close();
				return true;
			} catch (SQLException e) {
				if (log.isDebugEnabled()) log.debug("exception checking for existence of ["+tableName+"] using query ["+query+"]", e);
				return false;
			} finally {
				if (stmt!=null) {
					stmt.close();
				}
			}
		}
		catch(SQLException e) {
			throw new JdbcException(e);
		}
	}

	public boolean isColumnPresent(Connection conn, String tableName, String columnName) throws SQLException {
		PreparedStatement stmt = null;
		String query=null;
		try {
			query = "SELECT count(" + columnName + ") FROM " + tableName;
			stmt = conn.prepareStatement(query);

			ResultSet rs = null;
			try {
				rs = stmt.executeQuery();
				return true;
			} catch (SQLException e) {
				if (log.isDebugEnabled()) log.debug("exception checking for existence of column ["+columnName+"] in table ["+tableName+"] executing query ["+query+"]", e);
				return false;
			} finally {
				if (rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			log.warn("exception checking for existence of column ["+columnName+"] in table ["+tableName+"] preparing query ["+query+"]", e);
			return false;
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	protected boolean doIsTableColumnPresent(Connection conn, String columnsTable, String schemaColumn, String tableNameColumn, String columnNameColumn, String schemaName, String tableName, String columnName) throws JdbcException {
		String query="select count(*) from "+columnsTable+" where upper("+tableNameColumn+")=? and upper("+columnNameColumn+")=?";
		if (StringUtils.isNotEmpty(schemaName)) {
			if (StringUtils.isNotEmpty(schemaColumn)) {
				query+=" and upper("+schemaColumn+")='"+schemaName.toUpperCase()+"'";
			} else {
				throw new JdbcException("no schemaColumn present in table ["+columnsTable+"] to test for presence of column ["+columnName+"] of table ["+tableName+"] in schema ["+schemaName+"]");
			}
		}
		try {
			return JdbcUtil.executeIntQuery(conn, query, tableName.toUpperCase(), columnName.toUpperCase())>=1;
		} catch (Exception e) {
			log.warn("could not determine correct presence of column ["+columnName+"] of table ["+tableName+"]",e);
			return false;
		}
	}

	public boolean isTableColumnPresent(Connection conn, String schemaName, String tableName, String columnName) throws JdbcException {
		log.warn("could not determine correct presence of column ["+columnName+"] of table ["+tableName+"], assuming it exists");
		return true;
	}

	public boolean isIndexPresent(Connection conn, String schemaOwner, String tableName, String indexName) {
		log.warn("could not determine presence of index ["+indexName+"] on table ["+tableName+"]");
		return true;
	}

	public boolean isSequencePresent(Connection conn, String schemaOwner, String tableName, String sequenceName) {
		log.warn("could not determine presence of sequence ["+sequenceName+"]");
		return true;
	}

	public boolean isIndexColumnPresent(Connection conn, String schemaOwner, String tableName, String indexName, String columnName) {
		log.warn("could not determine correct presence of column ["+columnName+"] of index ["+indexName+"] on table ["+tableName+"]");
		return true;
	}

	public int getIndexColumnPosition(Connection conn, String schemaOwner, String tableName, String indexName, String columnName) {
		log.warn("could not determine correct presence of column ["+columnName+"] of index ["+indexName+"] on table ["+tableName+"]");
		return -1;
	}

	public boolean hasIndexOnColumn(Connection conn, String schemaOwner, String tableName, String columnName) {
		log.warn("could not determine presence of index column ["+columnName+"] on table ["+tableName+"]");
		return true;
	}

	public boolean hasIndexOnColumns(Connection conn, String schemaOwner, String tableName, List<String> columns) {
		log.warn("could not determine presence of index columns on table ["+tableName+"]");
		return true;
	}

	public String getSchemaOwner(Connection conn) throws SQLException, JdbcException {
		log.warn("could not determine current schema");
		return "";
	}

	public boolean isUniqueConstraintViolation(SQLException e) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public String getRowNumber(String order, String sort) {
		return "";
	}

	public String getRowNumberShortName() {
		return "";
	}

	public String getLength(String column) {
		return "LENGTH("+column+")";
	}

	public String getIbisStoreSummaryQuery() {
		return "select type, slotid, to_char(MESSAGEDATE,'YYYY-MM-DD') msgdate, count(*) msgcount from ibisstore group by slotid, type, to_char(MESSAGEDATE,'YYYY-MM-DD') order by type, slotid, to_char(MESSAGEDATE,'YYYY-MM-DD')";
	}

	public String getBooleanFieldType() {
		return "BOOLEAN";
	}

	public String getBooleanValue(boolean value) {
		return (""+value).toUpperCase();
	}

	@Override
	public String convertQuery(Connection conn, String query, String dbmsFrom) throws SQLException, JdbcException {
		warnConvertQuery(dbmsFrom);
		return query;
	}
	
	protected void warnConvertQuery(String dbmsFrom) {
		log.warn("don't know how to convert queries from [" + dbmsFrom + "] to [" + getDbmsName() + "]");
	}

	@Override
	public List<String> splitQuery(String query) {
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
					int countBegin = Misc.countRegex(line.toUpperCase().replaceAll("\\s+", "  "), "\\sBEGIN\\s");
					int countEnd = Misc.countRegex(line.toUpperCase().replaceAll(";", "; "), "\\sEND;");
					if ((countApos == 0 || (countApos & 1) == 0) && countBegin==countEnd) {
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
