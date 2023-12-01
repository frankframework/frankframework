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

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DataSourceConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import nl.nn.adapterframework.jndi.JndiObjectFactory;

public class PoolingDataSourceFactory extends JndiObjectFactory<DataSource, DataSource> {

	public PoolingDataSourceFactory() {
		super(DataSource.class);
	}

	public DataSource getDataSource(String dataSourceName) throws NamingException {
		return get(dataSourceName);
	}

	@Override
	protected DataSource augment(DataSource dataSource, String objectName) {
		ConnectionFactory cf = new DataSourceConnectionFactory(dataSource);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(cf, null);

		ObjectPool<PoolableConnection> connectionPool = createConnectionPool(poolableConnectionFactory);

		PoolingDataSource<PoolableConnection> ds = new PoolingDataSource<>(connectionPool);
		log.info("created PoolingDataSource [{}]", ds);
		return ds;
	}

	private ObjectPool<PoolableConnection> createConnectionPool(PoolableConnectionFactory poolableConnectionFactory) {
		poolableConnectionFactory.setAutoCommitOnReturn(false);
		poolableConnectionFactory.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		poolableConnectionFactory.setRollbackOnReturn(true);
		GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
		connectionPool.setMinIdle(1);
		connectionPool.setMaxTotal(10);
		connectionPool.setBlockWhenExhausted(true);
		poolableConnectionFactory.setPool(connectionPool);
		return connectionPool;
	}
}
