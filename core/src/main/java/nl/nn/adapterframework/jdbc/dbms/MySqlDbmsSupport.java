/*
Copyright 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;

//@formatter:off
/**
* Support for MySql/MariaDB.
* 
*/
public class MySqlDbmsSupport extends GenericDbmsSupport {

	@Override
	public int getDatabaseType() {
		return DbmsSupportFactory.DBMS_MYSQL;
	}

	@Override
	public String getDbmsName() {
		return DbmsSupportFactory.PRODUCT_NAME_MYSQL;
	}

	@Override
	public String getSchema(Connection conn) throws JdbcException {
		return JdbcUtil.executeStringQuery(conn, "SELECT DATABASE()");
	}

	@Override
	public String getIbisStoreSummaryQuery() {
		String messageDateConverter = "date_format(MESSAGEDATE,'%Y-%m-%d')";
		return "select type, slotid, " + messageDateConverter + " msgdate, count(*) msgcount from IBISSTORE group by slotid, type, " + messageDateConverter + " order by type, slotid, " + messageDateConverter;
	}


	@Override
	public String getClobFieldType() {
		return "TEXT";
	}

	@Override
	public String getBlobFieldType() {
		return "BLOB";
	}

	@Override
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		if (wait < 0) {
			return selectQuery+(batchSize>0?" LIMIT "+batchSize:"")+" FOR UPDATE SKIP LOCKED";
		} else {
			throw new IllegalArgumentException("MySQL does not support setting lock wait timeout in query");
		}
	}

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		if (wait < 0) {
			return selectQuery+(batchSize>0?" LIMIT "+batchSize:"")+" FOR SHARE SKIP LOCKED";
		} else {
			throw new IllegalArgumentException("MySQL does not support setting lock wait timeout in query");
		}
	}



//	private int retrieveAutoIncrement(Connection connection, String tableName) throws JdbcException {
//		String query = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='" + getSchema(connection) + "' AND TABLE_NAME='" + tableName + "' AND EXTRA LIKE '%auto_increment%'";
//		int countAutoIncrement = JdbcUtil.executeIntQuery(connection, query);
//		if (countAutoIncrement == 0) {
//			throw new JdbcException("could not find auto_increment column for table [" + tableName + "]");
//		} else if (countAutoIncrement > 1) {
//			throw new JdbcException("could not have multiple auto_increment columns for table [" + tableName + "]");
//		}
//
//		query = "SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='" + getSchema(connection) + "' AND TABLE_NAME='" + tableName + "'";
//		int autoIncrement = JdbcUtil.executeIntQuery(connection, query);
//		if (autoIncrement < 1) {
//			return 1;
//		}
//		return autoIncrement;
//	}


	public int alterAutoIncrement(Connection connection, String tableName, int startWith) throws JdbcException {
		String query = "ALTER TABLE " + tableName + " AUTO_INCREMENT=" + startWith;
		return JdbcUtil.executeIntQuery(connection, query);
	}
}
