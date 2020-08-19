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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;

/**
* Support for MySql/MariaDB.
* 
*/
public class MySqlDbmsSupport extends GenericDbmsSupport {

	@Override
	public Dbms getDbms() {
		return Dbms.MYSQL;
	}

	@Override
	public String getSchema(Connection conn) throws JdbcException {
		return JdbcUtil.executeStringQuery(conn, "SELECT DATABASE()");
	}

	@Override
	public String getDatetimeLiteral(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formattedDate = formatter.format(date);
		return "TIMESTAMP('" + formattedDate + "')";
	}

	@Override
	public String getTimestampAsDate(String columnName) {
		return "date_format("+columnName+",'%Y-%m-%d')";
	}

	@Override
	public String getDateAndOffset(String dateValue, int daysOffset) {
		return "DATE_ADD("+dateValue+ ", INTERVAL " + daysOffset + " DAY)";
	}

	@Override
	public String getClobFieldType() {
		return "LONGTEXT";
	}

	@Override
	public String getBlobFieldType() {
		return "LONGBLOB";
	}

	@Override
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		if (wait < 0) {
			return selectQuery+(batchSize>0?" LIMIT "+batchSize:"")+" FOR UPDATE SKIP LOCKED";
		} else {
			throw new IllegalArgumentException(getDbms()+" does not support setting lock wait timeout in query");
		}
	}

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		if (wait < 0) {
			return selectQuery+(batchSize>0?" LIMIT "+batchSize:"")+" FOR SHARE SKIP LOCKED"; // take shared lock, to be able to use 'skip locked'
		} else {
			throw new IllegalArgumentException(getDbms()+" does not support setting lock wait timeout in query");
		}
	}

	// commented out prepareSessionForNonLockingRead(), see https://dev.mysql.com/doc/refman/8.0/en/innodb-consistent-read.html
//	@Override
//	public JdbcSession prepareSessionForNonLockingRead(Connection conn) throws JdbcException {
//		JdbcUtil.executeStatement(conn, "SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
//		JdbcUtil.executeStatement(conn, "START TRANSACTION");
//		return new JdbcSession() {
//
//			@Override
//			public void close() throws Exception {
//				JdbcUtil.executeStatement(conn, "COMMIT");
//			}
//			
//		};
//	}


	public int alterAutoIncrement(Connection connection, String tableName, int startWith) throws JdbcException {
		String query = "ALTER TABLE " + tableName + " AUTO_INCREMENT=" + startWith;
		return JdbcUtil.executeIntQuery(connection, query);
	}

	@Override
	public String getAutoIncrementKeyFieldType() {
		return "INT AUTO_INCREMENT";
	}

	@Override
	public String getInsertedAutoIncrementValueQuery(String sequenceName) {
		return "SELECT LAST_INSERT_ID()";
	}


	@Override
	public String emptyClobValue() {
		return "";
	}

	@Override
	public String emptyBlobValue() {
		return "";
	}
}
