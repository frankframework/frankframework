/*
   Copyright 2024-2025 WeAreFrank!

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

import javax.sql.DataSource;
import javax.sql.XADataSource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.tomcat.dbcp.dbcp2.managed.ManagedDataSource;
import org.apache.tomcat.dbcp.pool2.impl.GenericObjectPool;
import org.springframework.jdbc.datasource.DelegatingDataSource;

public class JdbcPoolUtil {

	private static final String CLOSE = "], ";

	/** Returns pool info or NULL when it's not able to do so. */
	public static @Nullable String getConnectionPoolInfo(@Nullable DataSource datasource) {
		StringBuilder info = new StringBuilder();

		if (datasource instanceof OpenManagedDataSource targetDataSource) {
			addPoolMetadata(targetDataSource.getPool(), info);
		} else if (datasource instanceof org.apache.tomcat.dbcp.dbcp2.PoolingDataSource) {
			OpenPoolingDataSource<?> dataSource = (OpenPoolingDataSource<?>) datasource;
			addPoolMetadata(dataSource.getPool(), info);
		} else if (datasource instanceof DelegatingDataSource source) { // Perhaps it's wrapped?
			return getConnectionPoolInfo(source.getTargetDataSource());
		} else {
			return null;
		}

		return info.toString();
	}

	static void addPoolMetadata(@Nonnull GenericObjectPool<?> pool, @Nonnull StringBuilder info) {
		info.append("DBCP2 Pool Info: ");
		info.append("maxIdle [").append(pool.getMaxIdle()).append(CLOSE);
		info.append("minIdle [").append(pool.getMinIdle()).append(CLOSE);
		info.append("maxTotal [").append(pool.getMaxTotal()).append(CLOSE);
		info.append("numActive [").append(pool.getNumActive()).append(CLOSE);
		info.append("numIdle [").append(pool.getNumIdle()).append(CLOSE);
		info.append("testOnBorrow [").append(pool.getTestOnBorrow()).append(CLOSE);
		info.append("testOnCreate [").append(pool.getTestOnCreate()).append(CLOSE);
		info.append("testOnReturn [").append(pool.getTestOnReturn()).append(CLOSE);
		info.append("testWhileIdle [").append(pool.getTestWhileIdle()).append(CLOSE);
		info.append("removeAbandonedOnBorrow [").append(pool.getRemoveAbandonedOnBorrow()).append(CLOSE);
		info.append("removeAbandonedOnMaintenance [").append(pool.getRemoveAbandonedOnMaintenance()).append(CLOSE);
		info.append("removeAbandonedTimeoutDuration [").append(pool.getRemoveAbandonedTimeoutDuration()).append("]"); //TODO decide if we should make this human readable
	}

	// Try and find the inner DataSource if it's wrapped
	private static DataSource getInnerDataSource(DataSource datasource) {
		if (datasource instanceof DelegatingDataSource source) {
			return getInnerDataSource(source.getTargetDataSource());
		}

		return datasource;
	}

	public static boolean isXaCapable(DataSource dataSource) {
		DataSource innerDs = getInnerDataSource(dataSource);
		return innerDs instanceof XADataSource || innerDs instanceof ManagedDataSource || innerDs instanceof OpenManagedDataSource;
	}
}
