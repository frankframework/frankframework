/*
 * $Log: GenericDbmsSupport.java,v $
 * Revision 1.6  2011-11-30 13:51:45  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2011/10/04 09:54:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getDbmsName()
 *
 * Revision 1.3  2011/08/09 08:07:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getSchema(), isTablePresent() and isTableColumnPresent()
 *
 * Revision 1.2  2011/04/13 08:43:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Blob and Clob support using DbmsSupport
 *
 * Revision 1.1  2011/03/16 16:47:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 */
package nl.nn.adapterframework.jdbc.dbms;

import java.io.OutputStream;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
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
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		log.warn("don't know how to perform prepareQueryTextForWorkQueueReading for this database type, doing a guess...");
		return selectQuery+" FOR UPDATE";
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
	
	public boolean isTablePresent(Connection conn, String schemaName, String tableName) throws JdbcException {
		try {
			return JdbcUtil.tableExists(conn, tableName);
		} catch (SQLException e) {
			throw new JdbcException(e);
		}
	}

	public boolean doIsTableColumnPresent(Connection conn, String columnsTable, String schemaColumn, String tableNameColumn, String columnNameColumn, String schemaName, String tableName, String columnName) throws JdbcException {
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
	
	
	public boolean isIndexPresent(Connection conn, int databaseType, String schemaOwner, String tableName, String indexName) {
		if (databaseType==DbmsSupportFactory.DBMS_ORACLE) {
			String query="select count(*) from all_indexes where owner='"+schemaOwner.toUpperCase()+"' and table_name='"+tableName.toUpperCase()+"' and index_name='"+indexName.toUpperCase()+"'";
			try {
				if (JdbcUtil.executeIntQuery(conn, query)>=1) {
					return true;
				} 
				return false;
			} catch (Exception e) {
				log.warn("could not determine presence of index ["+indexName+"] on table ["+tableName+"]",e);
				return false;
			}
		} 
		log.warn("could not determine presence of index ["+indexName+"] on table ["+tableName+"] (not an Oracle database)");
		return true;
	}

	public boolean isSequencePresent(Connection conn, int databaseType, String schemaOwner, String sequenceName) {
		if (databaseType==DbmsSupportFactory.DBMS_ORACLE) {
			String query="select count(*) from all_sequences where sequence_owner='"+schemaOwner.toUpperCase()+"' and sequence_name='"+sequenceName.toUpperCase()+"'";
			try {
				if (JdbcUtil.executeIntQuery(conn, query)>=1) {
					return true;
				}  
				return false;
			} catch (Exception e) {
				log.warn("could not determine presence of sequence ["+sequenceName+"]",e);
				return false;
			}
		} 
		log.warn("could not determine presence of sequence ["+sequenceName+"] (not an Oracle database)");
		return true;
	}


	public boolean isIndexColumnPresent(Connection conn, int databaseType, String schemaOwner, String tableName, String indexName, String columnName) {
		if (databaseType==DbmsSupportFactory.DBMS_ORACLE) {
			String query="select count(*) from all_ind_columns where index_owner='"+schemaOwner.toUpperCase()+"' and table_name='"+tableName.toUpperCase()+"' and index_name='"+indexName.toUpperCase()+"' and column_name=?";
			try {
				if (JdbcUtil.executeIntQuery(conn, query, columnName.toUpperCase())>=1) {
					return true;
				} 
				return false;
			} catch (Exception e) {
				log.warn("could not determine correct presence of column ["+columnName+"] of index ["+indexName+"] on table ["+tableName+"]",e);
				return false;
			}
		} 
		log.warn("could not determine correct presence of column ["+columnName+"] of index ["+indexName+"] on table ["+tableName+"] (not an Oracle database)");
		return true;
	}

	public int getIndexColumnPosition(Connection conn, int databaseType, String schemaOwner, String tableName, String indexName, String columnName) {
		if (databaseType==DbmsSupportFactory.DBMS_ORACLE) {
			String query="select column_position from all_ind_columns where index_owner='"+schemaOwner.toUpperCase()+"' and table_name='"+tableName.toUpperCase()+"' and index_name='"+indexName.toUpperCase()+"' and column_name=?";
			try {
				return JdbcUtil.executeIntQuery(conn, query, columnName.toUpperCase());
			} catch (Exception e) {
				log.warn("could not determine correct presence of column ["+columnName+"] of index ["+indexName+"] on table ["+tableName+"]",e);
				return -1;
			}
		} 
		log.warn("could not determine correct presence of column ["+columnName+"] of index ["+indexName+"] on table ["+tableName+"] (not an Oracle database)");
		return -1;
	}

	public boolean hasIndexOnColumn(Connection conn, int databaseType, String schemaOwner, String tableName, String columnName) {
		if (databaseType==DbmsSupportFactory.DBMS_ORACLE) {
			String query="select count(*) from all_ind_columns";
			query+=" where TABLE_OWNER='"+schemaOwner.toUpperCase()+"' and TABLE_NAME='"+tableName.toUpperCase()+"'";
			query+=" and column_name=?";
			query+=" and column_position=1";
			try {
				if (JdbcUtil.executeIntQuery(conn, query, columnName.toUpperCase())>=1) {
					return true;
				} 
				return false;
			} catch (Exception e) {
				log.warn("could not determine presence of index column ["+columnName+"] on table ["+tableName+"] using query ["+query+"]",e);
				return false;
			}
		}
		log.warn("could not determine presence of index column ["+columnName+"] on table ["+tableName+"] (not an Oracle database)");
		return true;
	}
	public boolean hasIndexOnColumns(Connection conn, int databaseType, String schemaOwner, String tableName, List columns) {
		if (databaseType==DbmsSupportFactory.DBMS_ORACLE) {
			String query="select count(*) from all_indexes ai";
			for (int i=1;i<=columns.size();i++) {
				query+=", all_ind_columns aic"+i;
			}
			query+=" where ai.TABLE_OWNER='"+schemaOwner.toUpperCase()+"' and ai.TABLE_NAME='"+tableName.toUpperCase()+"'";
			for (int i=1;i<=columns.size();i++) {
				query+=" and ai.OWNER=aic"+i+".INDEX_OWNER";
				query+=" and ai.INDEX_NAME=aic"+i+".INDEX_NAME";
				query+=" and aic"+i+".column_name='"+((String)columns.get(i-1)).toUpperCase()+"'";
				query+=" and aic"+i+".column_position="+i;
			}
			try {
				if (JdbcUtil.executeIntQuery(conn, query)>=1) {
					return true;
				} 
				return false;
			} catch (Exception e) {
				log.warn("could not determine presence of index columns on table ["+tableName+"] using query ["+query+"]",e);
				return false;
			}
		} 
		log.warn("could not determine presence of index columns on table ["+tableName+"] (not an Oracle database)");
		return true;
	}
	
	
}
