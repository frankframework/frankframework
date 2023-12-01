/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.jta.narayana;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.StringTokenizer;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbcp2.managed.ManagedDataSource;

import com.arjuna.ats.arjuna.objectstore.jdbc.JDBCAccess;

public class JndiDataSourceJDBCAccess implements JDBCAccess {
	private final DataSource datasource;

	public JndiDataSourceJDBCAccess(DataSource datasource) {
		if(!(datasource instanceof PoolingDataSource<?>)) {
			throw new IllegalStateException("datasource is not pooled");
		}
		if(datasource instanceof ManagedDataSource<?>) {
			throw new IllegalStateException("datasource may not be managed (XA enabled)");
		}
		this.datasource = datasource;
	}

	@Override
	public Connection getConnection() throws SQLException {
		Connection connection = datasource.getConnection();
		connection.setAutoCommit(false);
		return connection;
	}

	@Override
	public void initialise(StringTokenizer stringTokenizer) {
		// Nothing to initialize, the DataSource should be configured via a DataSourceFactory
	}
}