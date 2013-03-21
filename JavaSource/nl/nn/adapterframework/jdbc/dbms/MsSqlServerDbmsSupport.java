/*
   Copyright 2013 Nationale-Nederlanden

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

import java.sql.Connection;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;

import org.apache.commons.lang.StringUtils;

/**
 * @author  Gerrit van Brakel
 * @since  
 * @version $Id$
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
