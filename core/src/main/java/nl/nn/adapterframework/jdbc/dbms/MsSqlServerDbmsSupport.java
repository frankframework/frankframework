/*
   Copyright 2013, 2018 Nationale-Nederlanden

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
import java.sql.SQLException;
import java.util.List;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;

import org.apache.commons.lang.StringUtils;

/**
 * @author  Gerrit van Brakel
 * @since  
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
	
	
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		// see http://www.mssqltips.com/tip.asp?tip=1257
		String result=selectQuery.substring(0,KEYWORD_SELECT.length())+(batchSize>0?" TOP "+batchSize:"")+selectQuery.substring(KEYWORD_SELECT.length());
		int wherePos=result.toLowerCase().indexOf("where");
		boolean rowlock = AppConstants.getInstance().getBoolean("dbmssupport.mssql.queuereading.rowlock", true);
		if (wherePos<0) {
			result+=" WITH ("+(rowlock ? "rowlock," : "")+"updlock,readpast)";
		} else {
			result=result.substring(0,wherePos)+" WITH ("+(rowlock ? "rowlock," : "")+"updlock,readpast) "+result.substring(wherePos);
		}
		return result;
	}

	public String getFirstRecordQuery(String tableName) throws JdbcException {
		String query="select top(1) * from "+tableName;
		return query;
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

	public boolean isUniqueConstraintViolation(SQLException e) {
		if (e.getErrorCode()==2627) {
			// Violation of %ls constraint '%.*ls'. Cannot insert duplicate key in object '%.*ls'.
			return true;
		} else {
			return false;
		}
	}

	public String getRowNumber(String order, String sort) {
		return "row_number() over (order by "+order+(sort==null?"":" "+sort)+") "+getRowNumberShortName();
	}

	public String getRowNumberShortName() {
		return "rn";
	}

	public String getLength(String column) {
		return "LEN("+column+")";
	}

	public String getIbisStoreSummaryQuery() {
		return "select type, slotid, CONVERT(VARCHAR(10), MESSAGEDATE, 120) msgdate, count(*) msgcount from ibisstore group by slotid, type, CONVERT(VARCHAR(10), MESSAGEDATE, 120) order by type, slotid, CONVERT(VARCHAR(10), MESSAGEDATE, 120)";
	}

	@Override
	public boolean isIndexPresent(Connection conn, String schemaOwner, String tableName, String indexName) {
		String query="select * from sys.indexes where name = '"+indexName+"' and object_id = object_id('"+tableName+"')";
		try {
			return JdbcUtil.executeIntQuery(conn, query)>=1;
		} catch (Exception e) {
			log.warn("could not determine presence of identity on table ["+tableName+"]",e);
			return false;
		}
	}

	@Override
	public boolean isSequencePresent(Connection conn, String schemaOwner, String tableName, String sequenceName) {
		String query="select objectproperty(object_id('"+tableName+"'), 'TableHasIdentity')";
		try {
			return JdbcUtil.executeIntQuery(conn, query)>=1;
		} catch (Exception e) {
			log.warn("could not determine presence of identity on table ["+tableName+"]",e);
			return false;
		}
	}

	@Override
	public boolean hasIndexOnColumn(Connection conn, String schemaOwner, String tableName, String columnName) {
		String query="select count(*) from sys.index_columns where object_id = object_id('"+tableName+"') and col_name(object_id, column_id)=?";
		try {
			return JdbcUtil.executeIntQuery(conn, query, columnName)>=1;
		} catch (Exception e) {
			log.warn("could not determine presence of index column ["+columnName+"] on table ["+tableName+"] using query ["+query+"]",e);
			return false;
		}
	}

	@Override
	public boolean hasIndexOnColumns(Connection conn, String schemaOwner, String tableName, List<String> columns) {
		StringBuilder query= new StringBuilder("select count(*) from sys.indexes si");
		for (int i=1;i<=columns.size();i++) {
			query.append(", sys.index_columns sic"+i);
		}
		query.append(" where si.object_id = object_id('"+tableName+"')");
		for (int i=1;i<=columns.size();i++) {
			query.append(" and si.object_id=sic"+i+".object_id");
			query.append(" and si.index_id=sic"+i+".index_id");
			query.append(" and col_name(sic"+i+".object_id, sic"+i+".column_id)='"+(String)columns.get(i-1)+"'");
			query.append(" and sic"+i+".index_column_id="+i);
		}
		try {
			return JdbcUtil.executeIntQuery(conn, query.toString())>=1;
		} catch (Exception e) {
			log.warn("could not determine presence of index columns on table ["+tableName+"] using query ["+query+"]",e);
			return false;
		}
	}
}
