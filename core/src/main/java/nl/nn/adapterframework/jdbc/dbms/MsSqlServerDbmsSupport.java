/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020 WeAreFrank!


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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * @author  Gerrit van Brakel
 */
public class MsSqlServerDbmsSupport extends GenericDbmsSupport {

	protected static final String NEXT_VALUE_FOR = "NEXT VALUE FOR ";
	protected static final String SELECT_CURRENT_VALUE = "SELECT CURRENT_VALUE FROM SYS.SEQUENCES WHERE NAME = ";
	protected static final String DEFAULT_BLOB_VALUE = "0x";
	protected static final String WITH_UPDLOCK_ROWLOCK = "WITH (UPDLOCK, ROWLOCK)";
	protected static final String GET_DATE = "GETDATE()";
	protected static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";
	

	@Override
	public Dbms getDbms() {
		return Dbms.MSSQL;
	}

	@Override
	public boolean hasSkipLockedFunctionality() {
		return true;
	}

	@Override
	public String getSysDate() {
		return "CURRENT_TIMESTAMP";
	}

	@Override
	public String getDateAndOffset(String dateValue, int daysOffset) {
		return "DATEADD(day, "+daysOffset+ "," + dateValue + ")";
	}

	@Override
	public String getNumericKeyFieldType() {
		return "INT";
	}

	@Override
	public String getAutoIncrementKeyFieldType() {
		return "INT IDENTITY";
	}
	
	@Override
	public boolean autoIncrementKeyMustBeInserted() {
		return false;
	}

	@Override
	public String getInsertedAutoIncrementValueQuery(String sequenceName) {
		return "SELECT @@IDENTITY";
	}

	@Override
	public String getTimestampFieldType() {
		return "DATETIME";
	}
	
	@Override
	public String getDatetimeLiteral(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formattedDate = formatter.format(date);
		return "CONVERT(datetime, '" + formattedDate + "', 120)";
	}
	@Override
	public String getTimestampAsDate(String columnName) {
		return "CONVERT(VARCHAR(10), "+columnName+", 120)";
	}


	@Override
	public String getBlobFieldType() {
		return "VARBINARY(MAX)";
	}
	@Override
	public String emptyBlobValue() {
		return "0x";
	}

	@Override
	public String getClobFieldType() {
		return "VARCHAR(MAX)";
	}
	

	@Override
	public String getTextFieldType() {
		return "VARCHAR";
	}
	
	
	@Override
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

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		// see http://www.mssqltips.com/tip.asp?tip=1257
		String result=selectQuery.substring(0,KEYWORD_SELECT.length())+(batchSize>0?" TOP "+batchSize:"")+selectQuery.substring(KEYWORD_SELECT.length());
		int wherePos=result.toLowerCase().indexOf("where");
		if (wherePos<0) {
			result+=" WITH (readpast)";
		} else {
			result=result.substring(0,wherePos)+" WITH (readpast) "+result.substring(wherePos);
		}
		return result;
	}

	@Override
	public String getFirstRecordQuery(String tableName) throws JdbcException {
		String query="select top(1) * from "+tableName;
		return query;
	} 

	@Override
	public String prepareQueryTextForNonLockingRead(String selectQuery) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		String result=selectQuery;
		int wherePos=result.toLowerCase().indexOf("where");
		if (wherePos<0) {
			result+=" WITH (nolock)";
		} else {
			result=result.substring(0,wherePos)+" WITH (nolock) "+result.substring(wherePos);
		}
		return result;
	}

	@Override
	public String provideTrailingFirstRowsHint(int rowCount) {
		return " OPTION (FAST "+rowCount+")";
	}

	@Override
	public String getSchema(Connection conn) throws JdbcException {
		return JdbcUtil.executeStringQuery(conn, "SELECT DB_NAME()");
	}

	@Override
	public boolean isUniqueConstraintViolation(SQLException e) {
		if (e.getErrorCode()==2627) {
			// Violation of %ls constraint '%.*ls'. Cannot insert duplicate key in object '%.*ls'.
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String getRowNumber(String order, String sort) {
		return "row_number() over (order by "+order+(sort==null?"":" "+sort)+") "+getRowNumberShortName();
	}

	@Override
	public String getRowNumberShortName() {
		return "rn";
	}

	@Override
	public String getLength(String column) {
		return "LEN("+column+")";
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
	
	@Override
	public String getBooleanFieldType() {
		return "BIT";
	}
	
	@Override
	public String getBooleanValue(boolean value) {
		return value? "1":"0";
	}

}
