/*
   Copyright 2013 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.dbms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;


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
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws DbmsException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new DbmsException("query [" + selectQuery + "] must start with keyword [" + KEYWORD_SELECT + "]");
		}
		return selectQuery + " FOR UPDATE SKIP LOCKED DATA";
	}

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws DbmsException {
		return selectQuery + " SKIP LOCKED DATA";
	}

	@Override
	public String getSchema(Connection conn) throws DbmsException {
		return DbmsUtil.executeStringQuery(conn, "SELECT CURRENT SCHEMA FROM SYSIBM.SYSDUMMY1");
	}

	@Override
	public boolean isTablePresent(Connection conn, String schemaName, String tableName) throws DbmsException {
		return super.isTablePresent(conn, schemaName, tableName.toUpperCase());
	}

	@Override
	public ResultSet getTableColumns(Connection conn, String schemaName, String tableName, String columnNamePattern) throws DbmsException {
		return super.getTableColumns(conn, schemaName, tableName.toUpperCase(), columnNamePattern != null ? columnNamePattern.toUpperCase() : null);
	}

	@Override
	public boolean isColumnPresent(Connection conn, String schemaName, String tableName, String columnName) throws DbmsException {
		return doIsColumnPresent(conn, "syscat.columns", schemaName, "tabname", "colname", null, tableName.toUpperCase(), columnName.toUpperCase());
	}

	@Override
	public boolean hasIndexOnColumn(Connection conn, String schemaName, String tableName, String columnName) throws DbmsException {
		return hasIndexOnColumns(conn, schemaName, tableName, Collections.singletonList(columnName));
	}

	@Override
	public boolean hasIndexOnColumns(Connection conn, String schemaOwner, String tableName, List<String> columns) throws DbmsException {
		return doHasIndexOnColumns(conn, schemaOwner, tableName.toUpperCase(), columns.stream().map(String::toUpperCase).collect(Collectors.toList()),
				"syscat.indexes", "syscat.indexcoluse", null, "tabname", "indname", "colname", "colseq");
	}
}
