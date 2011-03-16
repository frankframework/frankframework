/*
 * $Log: GenericDbmsSupport.java,v $
 * Revision 1.1  2011-03-16 16:47:26  L190409
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 */
package nl.nn.adapterframework.jdbc.dbms;

import nl.nn.adapterframework.jdbc.JdbcException;
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

}
