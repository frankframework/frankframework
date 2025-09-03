/*
   Copyright 2021-2024 WeAreFrank!

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
package org.frankframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.annotation.Nullable;

import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.StringUtil;

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
			try (Connection connection = super.getConnection()) {
				log.trace("populating metadata from getMetaData");
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
		databaseMetadata.put("XA", String.valueOf(JdbcPoolUtil.isXaCapable(this)));

		metadata = databaseMetadata;
	}

	/**
	 * LazyLoaded method. Only populate destinationName if a connection has
	 * previously been established and the metadata has been populated.
	 */
	public @Nullable String getDestinationName() {
		if(destinationName == null && metadata != null) {
			StringBuilder builder = new StringBuilder();
			builder.append(metadata.get("url"));

			String catalog = metadata.get("catalog");
			if (catalog != null) builder.append("/").append(catalog);

			destinationName = builder.toString();
		}
		return destinationName;
	}

	@Override
	public Connection getConnection() throws SQLException {
		Connection conn = super.getConnection();
		if (metadata == null) {
			log.trace("populating metadata from getConnection");
			populateMetadata(conn);
		}
		return conn;
	}

	@Override
	public String toString() {
		if (metadata != null && log.isInfoEnabled()) {
			return getInfo();
		}

		return StringUtil.reflectionToString(obtainTargetDataSource());
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
			info.append("XA-capable [").append(metadata.get("XA")).append(CLOSE);
		}
		info.append("targetDataSource [").append(obtainTargetDataSource().getClass().getName()).append("]");

		return info.toString();
	}

	public String getPoolInfo() {
		return JdbcPoolUtil.getConnectionPoolInfo(getTargetDataSource());
	}
}
