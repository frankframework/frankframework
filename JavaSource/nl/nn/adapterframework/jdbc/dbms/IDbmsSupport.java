/*
 * $Log: IDbmsSupport.java,v $
 * Revision 1.2  2011-04-13 08:43:00  L190409
 * Blob and Clob support using DbmsSupport
 *
 * Revision 1.1  2011/03/16 16:47:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 */
package nl.nn.adapterframework.jdbc.dbms;

import java.io.OutputStream;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

import nl.nn.adapterframework.jdbc.JdbcException;

/**
 * Interface to define DBMS specific SQL implementations.
 * 
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public interface IDbmsSupport {

	/**
	 * Numeric value defining database type, defined in {@link DbmsSupportFactory}.
	 */
	int getDatabaseType(); 
	
	/**
	 * SQL String returning current date and time of dbms.
	 */
	String getSysDate();

	String getNumericKeyFieldType();
	
	String getAutoIncrementKeyFieldType();
	boolean autoIncrementKeyMustBeInserted();
	String autoIncrementInsertValue(String sequenceName);
	boolean autoIncrementUsesSequenceObject();
	String getInsertedAutoIncrementValueQuery(String sequenceName);

	String getTimestampFieldType();

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

	String provideIndexHintAfterFirstKeyword(String tableName, String indexName);
	String provideFirstRowsHintAfterFirstKeyword(int rowCount);
	String provideTrailingFirstRowsHint(int rowCount);

}
