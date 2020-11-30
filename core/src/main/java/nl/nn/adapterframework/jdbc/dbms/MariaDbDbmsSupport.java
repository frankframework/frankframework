/*
Copyright 2020 WeAreFrank!

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

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.jdbc.JdbcException;

/**
* Support for MariaDB.
* 
*/
public class MariaDbDbmsSupport extends MySqlDbmsSupport {

	@Override
	public Dbms getDbms() {
		return Dbms.MARIADB;
	}

	@Override
	public boolean hasSkipLockedFunctionality() {
		return false;
	}

	@Override
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		if (wait < 0) {
			return selectQuery+(batchSize>0?" LIMIT "+batchSize:"")+" FOR UPDATE WAIT 0"; // Mariadb has no 'skip locked'
		} else {
			return selectQuery+(batchSize>0?" LIMIT "+batchSize:"")+" FOR UPDATE WAIT "+wait; 
		}
	}

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		if (wait < 0) {
			return selectQuery+(batchSize>0?" LIMIT "+batchSize:""); // Mariadb has no 'skip locked'
		} else {
			throw new IllegalArgumentException(getDbms()+" does not support setting lock wait timeout in query");
		}
	}

	@Override
	public Object getClobUpdateHandle(ResultSet rs, int column) throws SQLException, JdbcException {
		Clob clob=rs.getStatement().getConnection().createClob();
		return clob;
	}

	@Override
	public Object getClobUpdateHandle(ResultSet rs, String column) throws SQLException, JdbcException {
		Clob clob=rs.getStatement().getConnection().createClob();
		return clob;
	}

	
	@Override
	public Object getBlobUpdateHandle(ResultSet rs, int column) throws SQLException, JdbcException {
		Blob blob=rs.getStatement().getConnection().createBlob();
		return blob;
	}
	@Override
	public Object getBlobUpdateHandle(ResultSet rs, String column) throws SQLException, JdbcException {
		Blob blob=rs.getStatement().getConnection().createBlob();
		return blob;
	}

}
