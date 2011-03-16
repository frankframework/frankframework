/*
 * $Log: MsSqlServerDbmsSupport.java,v $
 * Revision 1.1  2011-03-16 16:47:26  L190409
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 */
package nl.nn.adapterframework.jdbc.dbms;

import nl.nn.adapterframework.jdbc.JdbcException;

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

}
