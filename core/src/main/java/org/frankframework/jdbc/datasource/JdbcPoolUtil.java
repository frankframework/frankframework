/*
   Copyright 2024-2026 WeAreFrank!

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

import org.apache.tomcat.dbcp.dbcp2.managed.ManagedDataSource;
import org.apache.tomcat.dbcp.pool2.impl.GenericObjectPool;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.jta.narayana.NarayanaDataSource;

public class JdbcPoolUtil {

	private static final String CLOSE = "], ";

	private JdbcPoolUtil() {
		// Empty constructor to prevent creation of instances of static utility-class.
	}

	/** Returns pool info or NULL when it's not able to do so. */
	public static @Nullable String getConnectionPoolInfo(@Nullable DataSource datasource) {
		return switch (datasource) {
			case OpenManagedDataSource<?> targetDataSource -> getPoolMetadata(targetDataSource.getPool());
			case OpenPoolingDataSource<?> poolingDataSource -> getPoolMetadata(poolingDataSource.getPool());
			case DelegatingDataSource source -> getConnectionPoolInfo(source.getTargetDataSource());  // Perhaps it's wrapped?
			case null, default -> null;
		};
	}

	static @NonNull String getPoolMetadata(@NonNull GenericObjectPool<?> pool) {
		return "DBCP2 Pool Info: " +
				"maxIdle [" + pool.getMaxIdle() + CLOSE +
				"minIdle [" + pool.getMinIdle() + CLOSE +
				"maxTotal [" + pool.getMaxTotal() + CLOSE +
				"numActive [" + pool.getNumActive() + CLOSE +
				"numIdle [" + pool.getNumIdle() + CLOSE +
				"testOnBorrow [" + pool.getTestOnBorrow() + CLOSE +
				"testOnCreate [" + pool.getTestOnCreate() + CLOSE +
				"testOnReturn [" + pool.getTestOnReturn() + CLOSE +
				"testWhileIdle [" + pool.getTestWhileIdle() + CLOSE +
				"removeAbandonedOnBorrow [" + pool.getRemoveAbandonedOnBorrow() + CLOSE +
				"removeAbandonedOnMaintenance [" + pool.getRemoveAbandonedOnMaintenance() + CLOSE +
				"removeAbandonedTimeoutDuration [" + pool.getRemoveAbandonedTimeoutDuration() + "]"; // TODO decide if we should make this human readable
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
		return innerDs instanceof XADataSource || innerDs instanceof ManagedDataSource || innerDs instanceof NarayanaDataSource;
	}

	public static boolean isXaCapable(PlatformTransactionManager transactionManager) {
		if (transactionManager instanceof JtaTransactionManager) {
			return true;
		} else if (transactionManager instanceof SpringTxManagerProxy txManagerProxy) {
			return isXaCapable(txManagerProxy.getRealTxManager());
		} else {
			return false;
		}
	}
}
