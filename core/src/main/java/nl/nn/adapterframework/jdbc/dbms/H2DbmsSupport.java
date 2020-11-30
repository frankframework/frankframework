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
import java.text.SimpleDateFormat;
import java.util.Date;

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
	public Dbms getDbms() {
		return Dbms.H2;
	}

	@Override
	public String getSchema(Connection conn) throws JdbcException {
		return JdbcUtil.executeStringQuery(conn, "SELECT SCHEMA()");
	}

	@Override
	public String getDatetimeLiteral(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formattedDate = formatter.format(date);
		return "parsedatetime('" + formattedDate + "', 'yyyy-MM-dd HH:mm:ss')";
	}

	@Override
	public String getTimestampAsDate(String columnName) {
		return "formatdatetime("+columnName+",'yyyy-MM-dd')";
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

//	@Override
//	// 2020-07-13 GvB: Did not get "SET SESSION CHARACTERISTICS" to work
//	public JdbcSession prepareSessionForDirtyRead(Connection conn) throws JdbcException {
//		JdbcUtil.executeStatement(conn, "SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL READ UNCOMMITTED");
//		return new AutoCloseable() {
//
//			@Override
//			public void close() throws Exception {
//				JdbcUtil.executeStatement(conn, "SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL READ COMMITTED");
//			}
//			
//		}
//	}

}
