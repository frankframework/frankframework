/*
   Copyright 2021 WeAreFrank!

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
import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import com.arjuna.ats.internal.jdbc.ConnectionManager;
import com.arjuna.ats.jdbc.TransactionalDriver;

import lombok.Setter;
import nl.nn.adapterframework.util.LogUtil;

/**
 * {@link DataSource} implementation wrapping {@link XADataSource} and using
 * {@link ConnectionManager} to acquire connections.
 *
 */
public class NarayanaDataSource extends DelegatingDataSource {
	protected Logger log = LogUtil.getLogger(this);

	private @Setter boolean connectionPooling = true;
	private @Setter int maxConnections = 50;

	public NarayanaDataSource(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public Connection getConnection() throws SQLException {
		Properties properties = new Properties();
		properties.put(TransactionalDriver.XADataSource, getTargetDataSource());
		properties.setProperty(TransactionalDriver.poolConnections, ""+connectionPooling);
		properties.setProperty(TransactionalDriver.maxConnections, ""+maxConnections);
		return ConnectionManager.create(null, properties);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		Properties properties = new Properties();
		properties.put(TransactionalDriver.XADataSource, getTargetDataSource());
		properties.put(TransactionalDriver.userName, username);
		properties.put(TransactionalDriver.password, password);
		properties.setProperty(TransactionalDriver.poolConnections, ""+connectionPooling);
		properties.setProperty(TransactionalDriver.maxConnections, ""+maxConnections);
		return ConnectionManager.create(null, properties);
	}
}
