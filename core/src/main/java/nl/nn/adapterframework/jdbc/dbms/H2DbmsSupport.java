/*
   Copyright 2015, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * Support for H2.
 * 
 * @author Jaco de Groot
 */
public class H2DbmsSupport extends GenericDbmsSupport {
	public final static String dbmsName = "H2";

	@Override
	public int getDatabaseType() {
		return DbmsSupportFactory.DBMS_H2;
	}

	@Override
	public String getDbmsName() {
		return dbmsName;
	}

	@Override
	public String getSchema(Connection conn) throws JdbcException {
		return JdbcUtil.executeStringQuery(conn, "SELECT SCHEMA()");
	}

	@Override
	public String getIbisStoreSummaryQuery() {
		return "select type, slotid, formatdatetime(MESSAGEDATE,'yyyy-MM-dd') msgdate, count(*) msgcount from ibisstore group by slotid, type, formatdatetime(MESSAGEDATE,'yyyy-MM-dd') order by type, slotid, formatdatetime(MESSAGEDATE,'yyyy-MM-dd')";
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
