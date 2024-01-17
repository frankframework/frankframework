/*
   Copyright 2021-2023 WeAreFrank!

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
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import lombok.extern.log4j.Log4j2;

/**
 * DataSource that is aware of the database metadata.
 * Fetches the metadata once and caches them.
 */
@Log4j2
public class TransactionalDbmsSupportAwareDataSourceProxy extends TransactionAwareDataSourceProxy {
	private Map<String, String> metadata;
	private String destinationName = null;

	public TransactionalDbmsSupportAwareDataSourceProxy(DataSource delegate) {
		super(delegate);
	}

	public Map<String, String> getMetaData() throws SQLException {
		if (metadata == null) {
			log.debug("populating metadata from getMetaData");
			try (Connection connection = getConnection()) {
				populateMetadata(connection);
			}
		}
		return metadata;
	}

	/**
	 * Should only be called once, either on the first {@link #getConnection()} or when explicitly requested {@link #getMetaData()}.
	 */
	private void populateMetadata(Connection connection) throws SQLException {
		Map<String, String> databaseMetadata = new HashMap<>();
		DatabaseMetaData md = connection.getMetaData();
		databaseMetadata.put("catalog", connection.getCatalog());

		databaseMetadata.put("user", md.getUserName());
		databaseMetadata.put("url", md.getURL());
		databaseMetadata.put("product", md.getDatabaseProductName());
		databaseMetadata.put("product-version", md.getDatabaseProductVersion());
		databaseMetadata.put("driver", md.getDriverName());
		databaseMetadata.put("driver-version", md.getDriverVersion());

		this.metadata = databaseMetadata;
	}

	public String getDestinationName() throws SQLException {
		if (destinationName == null) {
			StringBuilder builder = new StringBuilder();
			builder.append(getMetaData().get("url"));

			String catalog = getMetaData().get("catalog");
			if (catalog != null) builder.append("/").append(catalog);

			destinationName = builder.toString();
		}
		return destinationName;
	}

	@Override
	public Connection getConnection() throws SQLException {
		Connection conn = super.getConnection();
		if (metadata == null) {
			log.debug("populating metadata from getConnection");
			populateMetadata(conn);
		}

		return conn;
	}

	@Override
	public String toString() {
		if (metadata != null && log.isInfoEnabled()) {
			return getInfo();
		}
		return obtainTargetDataSource().toString();
	}

	public String getInfo() {
		StringBuilder info = new StringBuilder();
		if (metadata != null) {
			info.append("user [").append(metadata.get("user")).append("], ");
			info.append("url [").append(metadata.get("url")).append("], ");
			info.append("product [").append(metadata.get("product")).append("], ");
			info.append("product version [").append(metadata.get("product-version")).append("], ");
			info.append("driver [").append(metadata.get("driver")).append("], ");
			info.append("driver version [").append(metadata.get("driver-version")).append("], ");
		}

		if (getTargetDataSource() instanceof OpenManagedDataSource) {
			OpenManagedDataSource targetDataSource = (OpenManagedDataSource) getTargetDataSource();
			GenericObjectPool pool = targetDataSource.getPool();
			if (pool != null) {
				info.append("Pool Info: ");
				info.append("maxIdle [").append(pool.getMaxIdle()).append("], ");
				info.append("minIdle [").append(pool.getMinIdle()).append("], ");
				info.append("maxTotal [").append(pool.getMaxTotal()).append("], ");
				info.append("numActive [").append(pool.getNumActive()).append("], ");
				info.append("numIdle [").append(pool.getNumIdle()).append("], ");
			}
		} else if (getTargetDataSource() instanceof BasicDataSource) { // Tomcat instance
			BasicDataSource dataSource = (BasicDataSource) getTargetDataSource();
			info.append("Pool Info: ");
			if (dataSource != null) {
				info.append("maxIdle [").append(dataSource.getMaxIdle()).append("], ");
				info.append("minIdle [").append(dataSource.getMinIdle()).append("], ");
				info.append("maxTotal [").append(dataSource.getMaxTotal()).append("], ");
				info.append("numActive [").append(dataSource.getNumActive()).append("], ");
				info.append("numIdle [").append(dataSource.getNumIdle()).append("], ");
			}
		}

		info.append(" datasource [").append(obtainTargetDataSource().getClass().getName()).append("]");
		return info.toString();
	}

}
