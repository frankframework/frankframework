/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.jms;

import java.util.HashMap;
import java.util.Map;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.JMSException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TransactionalMetadataAwareConnectionFactoryProxy extends TransactionAwareConnectionFactoryProxy {
	private static final String CLOSE = "], ";

	private Map<String, String> metadata;

	public TransactionalMetadataAwareConnectionFactoryProxy(ConnectionFactory delegate) {
		super(delegate);
	}

	public Map<String, String> getMetaData() throws JMSException {
		if (metadata == null) {
			try (Connection connection = super.createConnection()) {
				log.trace("populating metadata from getMetaData");
				populateMetadata(connection);
			}
		}
		return metadata;
	}

	/**
	 * Should only be called once, either on the first {@link #createConnection()} or when explicitly requested {@link #getMetaData()}.
	 */
	private void populateMetadata(Connection connection) throws JMSException {
		Map<String, String> databaseMetadata = new HashMap<>();
		ConnectionMetaData md = connection.getMetaData();
		databaseMetadata.put("id", connection.getClientID());

		databaseMetadata.put("version", md.getJMSVersion());
		databaseMetadata.put("provider-name", md.getJMSProviderName());
		databaseMetadata.put("provider-version", md.getProviderVersion());

		metadata = databaseMetadata;
	}

	@Override
	public Connection createConnection() throws JMSException {
		Connection conn = super.createConnection();
		if (metadata == null) {
			log.trace("populating metadata from createConnection");
			populateMetadata(conn);
		}
		return conn;
	}

	@Override
	public String toString() {
		if (metadata != null && log.isInfoEnabled()) {
			return getInfo();
		}

		return reflectionToString();
	}

	/**
	 * Attempt to find the most outer factory and return the reflectionToString result.
	 */
	private String reflectionToString() {
		return JmsPoolUtil.reflectionToString(getTargetConnectionFactory());
	}

	public String getInfo() {
		StringBuilder info = new StringBuilder();

		if (metadata != null) {
			if (StringUtils.isNotEmpty(metadata.get("id"))) {
				info.append("id [").append(metadata.get("id")).append(CLOSE);
			}

			info.append("version [").append(metadata.get("version")).append(CLOSE);
			info.append("provider-name [").append(metadata.get("provider-name")).append(CLOSE);
			info.append("provider-version [").append(metadata.get("provider-version")).append(CLOSE);
		}

		info.append(reflectionToString());

		return info.toString();
	}

	public String getPoolInfo() {
		return JmsPoolUtil.getConnectionPoolInfo(getTargetConnectionFactory());
	}
}
