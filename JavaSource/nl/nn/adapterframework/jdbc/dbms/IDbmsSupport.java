/*
 * $Log: IDbmsSupport.java,v $
 * Revision 1.1  2011-03-16 16:47:26  L190409
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 */
package nl.nn.adapterframework.jdbc.dbms;

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

	String getBlobFieldType();
	boolean mustInsertEmptyBlobBeforeData();
	String emptyBlobValue();
	String getUpdateBlobQuery(String table, String blobField, String keyField);
	String getTextFieldType();
	
	String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery) throws JdbcException;

	String provideIndexHintAfterFirstKeyword(String tableName, String indexName);
	String provideFirstRowsHintAfterFirstKeyword(int rowCount);
	String provideTrailingFirstRowsHint(int rowCount);

}
