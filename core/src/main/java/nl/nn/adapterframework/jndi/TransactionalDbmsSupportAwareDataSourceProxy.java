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
package nl.nn.adapterframework.jndi;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import nl.nn.adapterframework.util.LogUtil;

/**
 * DataSource that is aware of the database metadata.
 * Fetches the metadata once and caches them.
 */
public class TransactionalDbmsSupportAwareDataSourceProxy extends TransactionAwareDataSourceProxy {
	private Logger log = LogUtil.getLogger(this);
	private Map<String, String> metadata;
	private String destinationName = null;

	public TransactionalDbmsSupportAwareDataSourceProxy(DataSource delegate) {
		super(delegate);
	}

	public Map<String, String> getMetaData() throws SQLException {
		if(metadata == null) {
			try (Connection connection = getConnection()) {
				populate(connection);
			}
			log.debug("populated metadata from getMetaData");
		}
		return metadata;
	}

	/**
	 * Should only be called once, either on the first {@link #getConnection()} or when explicitly requested {@link #getMetaData()}.
	 */
	private void populate(Connection connection) throws SQLException {
		metadata = new HashMap<>();
		DatabaseMetaData md = connection.getMetaData();
		metadata.put("catalog", connection.getCatalog());

		metadata.put("user", md.getUserName());
		metadata.put("url", md.getURL());
		metadata.put("product", md.getDatabaseProductName());
		metadata.put("product-version", md.getDatabaseProductVersion());
		metadata.put("driver", md.getDriverName());
		metadata.put("driver-version", md.getDriverVersion());
	}

	public String getDestinationName() throws SQLException {
		if(destinationName == null) {
			StringBuilder builder = new StringBuilder();
			builder.append(getMetaData().get("url"));

			String catalog = getMetaData().get("catalog");
			if(catalog != null) builder.append("/"+catalog);

			destinationName = builder.toString();
		}
		return destinationName;
	}

	@Override
	public Connection getConnection() throws SQLException {
		Connection conn = super.getConnection();
		if(metadata == null) {
			populate(conn);
			log.debug("populated metadata from getConnection");
		}

		return conn;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());

		if(metadata != null && log.isInfoEnabled()) {
			builder.append(" user ["+metadata.get("user")+"]");
			builder.append(" url ["+metadata.get("url")+"]");
			builder.append(" product ["+metadata.get("product")+"]");
			builder.append(" product version ["+metadata.get("product-version")+"]");
			builder.append(" driver ["+metadata.get("driver")+"]");
			builder.append(" driver version ["+metadata.get("driver-version")+"]");
		}

		return builder.toString();
	}
}
