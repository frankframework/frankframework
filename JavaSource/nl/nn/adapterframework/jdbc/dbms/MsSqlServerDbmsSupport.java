/*
 * $Log: MsSqlServerDbmsSupport.java,v $
 * Revision 1.5  2011-11-30 13:51:45  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2011/10/04 09:54:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getDbmsName()
 *
 * Revision 1.2  2011/08/09 08:07:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getSchema(), isTablePresent() and isTableColumnPresent()
 *
 * Revision 1.1  2011/03/16 16:47:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 */
package nl.nn.adapterframework.jdbc.dbms;

import java.sql.Connection;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;

import org.apache.commons.lang.StringUtils;

/**
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class MsSqlServerDbmsSupport extends GenericDbmsSupport {

	public int getDatabaseType() {
		return DbmsSupportFactory.DBMS_MSSQLSERVER;
	}

	public String getDbmsName() {
		return "MS SQL";
	}

	public String getSysDate() {
		return "CURRENT_TIMESTAMP";
	}

	public String getNumericKeyFieldType() {
		return "INT";
	}

	public String getAutoIncrementKeyFieldType() {
		return "INT IDENTITY";
	}
	
	public boolean autoIncrementKeyMustBeInserted() {
		return false;
	}

	public String getInsertedAutoIncrementValueQuery(String sequenceName) {
		return "SELECT @@IDENTITY";
	}

	public String getTimestampFieldType() {
		return "DATETIME";
	}

	public String getBlobFieldType() {
		return "VARBINARY(MAX)";
	}

	public boolean mustInsertEmptyBlobBeforeData() {
		return false;
	}

	public String getTextFieldType() {
		return "VARCHAR";
	}
	
	
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		// see http://www.mssqltips.com/tip.asp?tip=1257
		String result=selectQuery.substring(0,KEYWORD_SELECT.length())+(batchSize>0?" TOP "+batchSize:"")+selectQuery.substring(KEYWORD_SELECT.length());
		int wherePos=result.toLowerCase().indexOf("where");
		if (wherePos<0) {
			result+=" WITH (updlock,readpast)";
		} else {
			result=result.substring(0,wherePos)+" WITH (updlock,readpast) "+result.substring(wherePos);
		}
		return result;
	}

	public String provideTrailingFirstRowsHint(int rowCount) {
		return " OPTION (FAST "+rowCount+")";
	}

	public String getSchema(Connection conn) throws JdbcException {
		return JdbcUtil.executeStringQuery(conn, "SELECT DB_NAME()");
	}

	public boolean isTablePresent(Connection conn, String schemaName, String tableName) throws JdbcException {
		return doIsTablePresent(conn, "INFORMATION_SCHEMA.TABLES", "TABLE_CATALOG", "TABLE_NAME", schemaName, tableName.toUpperCase());
	}
	
	public boolean isTableColumnPresent(Connection conn, String schemaName, String tableName, String columnName) throws JdbcException {
		return doIsTableColumnPresent(conn, "INFORMATION_SCHEMA.COLUMNS", "TABLE_CATALOG", "TABLE_NAME", "COLUMN_NAME", schemaName, tableName, columnName);
	}

}
