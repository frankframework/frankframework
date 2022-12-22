/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * Support for DB2.
 *
 * @author Gerrit van Brakel
 * @author Jaco de Groot
 */
public class Db2DbmsSupport extends GenericDbmsSupport {

	@Override
	public Dbms getDbms() {
		return Dbms.DB2;
	}

	@Override
	public boolean hasSkipLockedFunctionality() {
		return true;
	}

	@Override
	public String getFromForTablelessSelect() {
		return "FROM SYSIBM.SYSDUMMY1";
	}

	@Override
	public String emptyBlobValue() {
		return "EMPTY_BLOB";
	}

	@Override
	public String emptyClobValue() {
		return "EMPTY_CLOB";
	}

	@Override
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		return selectQuery+" FOR UPDATE SKIP LOCKED DATA";
	}

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws JdbcException {
		return selectQuery+ " SKIP LOCKED DATA";
	}

	@Override
	public String getFirstRecordQuery(String tableName) throws JdbcException {
		String query="select * from "+tableName+" fetch first 1 rows only";
		return query;
	}

	@Override
	public String getSchema(Connection conn) throws JdbcException {
		return JdbcUtil.executeStringQuery(conn, "SELECT CURRENT SCHEMA FROM SYSIBM.SYSDUMMY1");
	}

	@Override
	public boolean isTablePresent(Connection conn, String schemaName, String tableName) throws JdbcException {
		return super.isTablePresent(conn, schemaName, tableName.toUpperCase());
	}

	@Override
	public ResultSet getTableColumns(Connection conn, String schemaName, String tableName, String columnNamePattern) throws JdbcException {
		return super.getTableColumns(conn, schemaName, tableName.toUpperCase(), columnNamePattern!=null ? columnNamePattern.toUpperCase(): null);
	}

	@Override
	public boolean isColumnPresent(Connection conn, String schemaName, String tableName, String columnName) throws JdbcException {
		return doIsColumnPresent(conn, "syscat.columns", schemaName, "tabname", "colname", null, tableName.toUpperCase(), columnName.toUpperCase());
	}

	public boolean hasIndexOnColumn(Connection conn, String schemaName, String tableName, String columnName) throws JdbcException {
		List<String> columns = new LinkedList<>();
		columns.add(columnName);
		return hasIndexOnColumns(conn, schemaName, tableName, columns);
	}

	@Override
	public boolean hasIndexOnColumns(Connection conn, String schemaOwner, String tableName, List<String> columns) throws JdbcException {
		return doHasIndexOnColumns(conn, schemaOwner, tableName.toUpperCase(), columns.stream().map(s->s.toUpperCase()).collect(Collectors.toList()),
				"syscat.indexes", "syscat.indexcoluse", null, "tabname", "indname", "colname", "colseq");
	}

}
