/*
   Copyright 2015, 2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import org.frankframework.util.StringUtil;


/**
 * Support for H2.
 *
 * @author Jaco de Groot
 */
public class H2DbmsSupport extends GenericDbmsSupport {

	private final boolean dbmsHasSkipLockedFunctionality;

	public H2DbmsSupport() {
		throw new IllegalStateException("H2DbmsSupport should be instantiated with product-version to determine supported featureset. Calling this constructor is a code-bug.");
	}

	public H2DbmsSupport(String productVersion) {
		dbmsHasSkipLockedFunctionality = determineSkipLockedCapability(productVersion);
	}

	// Example output `2.3.232 (2024-08-11)`
	private boolean determineSkipLockedCapability(String productVersion) {
		if (StringUtils.isEmpty(productVersion)) {
			log.debug("unable to automatically determine H2 product version none provided");
			return false;
		}

		List<String> parts = StringUtil.split(productVersion, "(");
		if (parts.size() == 2 && parts.get(1).contains(")")) {
			String version = parts.get(0).trim();
			DefaultArtifactVersion thisVersion = new DefaultArtifactVersion(version);
			DefaultArtifactVersion targetVersion = new DefaultArtifactVersion("2.2.220");
			boolean result = thisVersion.compareTo(targetVersion) >= 0;
			log.debug("based on H2 productversion [{}] dbms hasSkipLockedFunctionality [{}]", version, result);
			return result;
		}

		log.debug("unable to automatically determine H2 product version from [{}]", productVersion);
		return false;
	}

	@Override
	public Dbms getDbms() {
		return Dbms.H2;
	}

	@Override
	public boolean hasSkipLockedFunctionality() {
		return dbmsHasSkipLockedFunctionality;
	}

	// See OracleDbmsSupport
	@Override
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws DbmsException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new DbmsException("query [" + selectQuery + "] must start with keyword [" + KEYWORD_SELECT + "]");
		}

		if (wait < 0) {
			return selectQuery + " FOR UPDATE SKIP LOCKED";
		} else {
			return selectQuery + " FOR UPDATE WAIT " + wait;
		}
	}

	@Override
	public String getSchema(Connection conn) throws DbmsException {
		return DbmsUtil.executeStringQuery(conn, "SELECT SCHEMA()");
	}

	@Override
	public String getDatetimeLiteral(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formattedDate = formatter.format(date);
		return "parsedatetime('" + formattedDate + "', 'yyyy-MM-dd HH:mm:ss')";
	}

	@Override
	public String getTimestampAsDate(String columnName) {
		return "formatdatetime(" + columnName + ",'yyyy-MM-dd')";
	}

	@Override
	public Object getClobHandle(ResultSet rs, int column) throws SQLException, DbmsException {
		return rs.getStatement().getConnection().createClob();
	}


	@Override
	public Object getBlobHandle(ResultSet rs, int column) throws SQLException, DbmsException {
		return rs.getStatement().getConnection().createBlob();
	}

	@Override
	public ResultSet getTableColumns(Connection conn, String schemaName, String tableName, String columnNamePattern) throws DbmsException {
		return super.getTableColumns(conn, schemaName, tableName.toUpperCase(), columnNamePattern != null ? columnNamePattern.toUpperCase() : null);
	}

	@Override
	public boolean isTablePresent(Connection conn, String schemaName, String tableName) throws DbmsException {
		return super.isTablePresent(conn, schemaName, tableName.toUpperCase());
	}

	@Override
	public boolean isColumnPresent(Connection conn, String schemaName, String tableName, String columnName) throws DbmsException {
		return super.isColumnPresent(conn, schemaName, tableName.toUpperCase(), columnName.toUpperCase());
	}

	@Override
	public boolean hasIndexOnColumn(Connection conn, String schemaName, String tableName, String columnName) throws DbmsException {
		return super.hasIndexOnColumn(conn, schemaName, tableName.toUpperCase(), columnName.toUpperCase());
	}

	@Override
	public boolean hasIndexOnColumns(Connection conn, String schemaOwner, String tableName, List<String> columns) {
		List<String> columnsUC = columns.stream().map(String::toUpperCase).collect(Collectors.toList());
		return doHasIndexOnColumns(conn, "PUBLIC", tableName.toUpperCase(), columnsUC,
				"INFORMATION_SCHEMA.INDEXES", "INFORMATION_SCHEMA.INDEX_COLUMNS",
				"TABLE_SCHEMA", "TABLE_NAME", "INDEX_NAME", "COLUMN_NAME", "ORDINAL_POSITION");
	}

}
