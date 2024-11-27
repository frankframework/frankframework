/*
Copyright 2019 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.util.DateFormatUtils;


/**
 * Support for MySql/MariaDB.
 */
public class MySqlDbmsSupport extends GenericDbmsSupport {

	@Override
	public Dbms getDbms() {
		return Dbms.MYSQL;
	}

	@Override
	public boolean hasSkipLockedFunctionality() {
		return true;
	}

	@Override
	public String getSchema(Connection conn) throws DbmsException {
		return DbmsUtil.executeStringQuery(conn, "SELECT DATABASE()");
	}

	@Override
	public String getDatetimeLiteral(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat(DateFormatUtils.FORMAT_DATETIME_GENERIC);
		String formattedDate = formatter.format(date);
		return "TIMESTAMP('" + formattedDate + "')";
	}

	@Override
	public String getTimestampAsDate(String columnName) {
		return "date_format(" + columnName + ",'%Y-%m-%d')";
	}

	@Override
	public String getDateAndOffset(String dateValue, int daysOffset) {
		return "DATE_ADD(" + dateValue + ", INTERVAL " + daysOffset + " DAY)";
	}

	@Override
	public String getBlobFieldType() {
		return "LONGBLOB";
	}

	@Override
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws DbmsException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new DbmsException("query [" + selectQuery + "] must start with keyword [" + KEYWORD_SELECT + "]");
		}
		if (wait < 0) {
			return selectQuery + (batchSize > 0 ? " LIMIT " + batchSize : "") + " FOR UPDATE SKIP LOCKED";
		}
		throw new IllegalArgumentException(getDbms() + " does not support setting lock wait timeout in query");
	}

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws DbmsException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new DbmsException("query [" + selectQuery + "] must start with keyword [" + KEYWORD_SELECT + "]");
		}
		if (wait < 0) {
			return selectQuery + (batchSize > 0 ? " LIMIT " + batchSize : "") + " FOR SHARE SKIP LOCKED"; // take shared lock, to be able to use 'skip locked'
		}
		throw new IllegalArgumentException(getDbms() + " does not support setting lock wait timeout in query");
	}

	// commented out prepareSessionForNonLockingRead(), see https://dev.mysql.com/doc/refman/8.0/en/innodb-consistent-read.html
//	@Override
//	public JdbcSession prepareSessionForNonLockingRead(Connection conn) throws DbmsException {
//		DbmsUtil.executeStatement(conn, "SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED");
//		DbmsUtil.executeStatement(conn, "START TRANSACTION");
//		return new JdbcSession() {
//
//			@Override
//			public void close() throws DbmsException {
//				DbmsUtil.executeStatement(conn, "COMMIT");
//			}
//
//		};
//	}

	@Override
	public boolean hasIndexOnColumns(Connection conn, String schemaOwner, String tableName, List<String> columns) {
		StringBuilder query = new StringBuilder("select count(*) from information_schema.statistics is1");
		for (int i = 2; i <= columns.size(); i++) {
			query.append(", information_schema.statistics is").append(i);
		}
		query.append(" where is1.TABLE_SCHEMA='").append(schemaOwner).append("' and is1.TABLE_NAME='").append(tableName).append("'");
		for (int i = 2; i <= columns.size(); i++) {
			query.append(" and is1.TABLE_CATALOG=is").append(i).append(".TABLE_CATALOG");
			query.append(" and is1.INDEX_SCHEMA=is").append(i).append(".INDEX_SCHEMA");
			query.append(" and is1.INDEX_NAME=is").append(i).append(".INDEX_NAME");
		}
		for (int i = 1; i <= columns.size(); i++) {
			query.append(" and is").append(i).append(".COLUMN_NAME='").append(columns.get(i - 1)).append("'");
			query.append(" and is").append(i).append(".SEQ_IN_INDEX=").append(i);
		}
		try {
			return DbmsUtil.executeIntQuery(conn, query.toString()) >= 1;
		} catch (Exception e) {
			log.warn("could not determine presence of index columns on table [{}] using query [{}]", tableName, query, e);
			return false;
		}
	}

	public int alterAutoIncrement(Connection connection, String tableName, int startWith) throws DbmsException {
		String query = "ALTER TABLE " + tableName + " AUTO_INCREMENT=" + startWith;
		return DbmsUtil.executeIntQuery(connection, query);
	}

	@Override
	public String getAutoIncrementKeyFieldType() {
		return "INT AUTO_INCREMENT";
	}

}
