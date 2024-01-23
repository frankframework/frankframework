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
package org.frankframework.jdbc.dbms;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import lombok.extern.log4j.Log4j2;

/**
 * DataSource that is aware of the database metadata.
 * Fetches the metadata once and caches them.
 */
@Log4j2
public class TransactionalDbmsSupportAwareDataSourceProxy extends TransactionAwareDataSourceProxy {
	private static final String CLOSE = "], ";

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

		metadata = databaseMetadata;
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
			info.append("user [").append(metadata.get("user")).append(CLOSE);
			info.append("url [").append(metadata.get("url")).append(CLOSE);
			info.append("product [").append(metadata.get("product")).append(CLOSE);
			info.append("product version [").append(metadata.get("product-version")).append(CLOSE);
			info.append("driver [").append(metadata.get("driver")).append(CLOSE);
			info.append("driver version [").append(metadata.get("driver-version")).append(CLOSE);
		}

		if (getTargetDataSource() instanceof OpenManagedDataSource) {
			OpenManagedDataSource targetDataSource = (OpenManagedDataSource) getTargetDataSource();
			targetDataSource.addPoolMetadata(info);
		} else if (getTargetDataSource() instanceof org.apache.commons.dbcp2.PoolingDataSource) {
			OpenPoolingDataSource dataSource = (OpenPoolingDataSource) getTargetDataSource();
			dataSource.addPoolMetadata(info);
		} else if (getTargetDataSource() instanceof BasicDataSource) { // Tomcat instance
			addTomcatDatasourceInfo(info);
		} else if (getTargetDataSource() instanceof PoolingDataSource) { // BTM instance
			addBTMDatasourceInfo(info);
		}

		info.append(" datasource [").append(obtainTargetDataSource().getClass().getName()).append("]");
		return info.toString();
	}

	private void addBTMDatasourceInfo(StringBuilder info) {
		PoolingDataSource dataSource = (PoolingDataSource) getTargetDataSource();
		info.append("BTM Pool Info: ");
		if (dataSource == null) {
			return;
		}
		info.append("maxPoolSize [").append(dataSource.getMaxPoolSize()).append(CLOSE);
		info.append("minPoolSize [").append(dataSource.getMinPoolSize()).append(CLOSE);
		info.append("totalPoolSize [").append(dataSource.getTotalPoolSize()).append(CLOSE);
		info.append("inPoolSize [").append(dataSource.getInPoolSize()).append(CLOSE);
	}

	private void addTomcatDatasourceInfo(StringBuilder info) {
		BasicDataSource dataSource = (BasicDataSource) getTargetDataSource();
		info.append("Tomcat Pool Info: ");
		if (dataSource == null) {
			return;
		}
		info.append("maxIdle [").append(dataSource.getMaxIdle()).append(CLOSE);
		info.append("minIdle [").append(dataSource.getMinIdle()).append(CLOSE);
		info.append("maxTotal [").append(dataSource.getMaxTotal()).append(CLOSE);
		info.append("numActive [").append(dataSource.getNumActive()).append(CLOSE);
		info.append("numIdle [").append(dataSource.getNumIdle()).append(CLOSE);
		info.append("testOnBorrow [").append(dataSource.getTestOnBorrow()).append(CLOSE);
		info.append("testOnCreate [").append(dataSource.getTestOnCreate()).append(CLOSE);
		info.append("testOnReturn [").append(dataSource.getTestOnReturn()).append(CLOSE);
	}

}
