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
package org.frankframework.jdbc.factory;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.tomcat.dbcp.dbcp2.managed.ManagedDataSource;
import org.apache.tomcat.dbcp.pool2.impl.GenericObjectPool;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import lombok.experimental.UtilityClass;

import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.jta.narayana.NarayanaDataSource;

@UtilityClass
public class JdbcPoolUtil {

	private static final String CLOSE = "], ";

	/** Returns pool info or NULL when it's not able to do so. */
	public static @Nullable String getConnectionPoolInfo(@Nullable DataSource datasource) {
		StringBuilder info = new StringBuilder();

		switch (datasource) {
			case OpenManagedDataSource<?> targetDataSource -> addPoolMetadata(targetDataSource.getPool(), info);
			case OpenPoolingDataSource<?> poolingDataSource -> addPoolMetadata(poolingDataSource.getPool(), info);
			case DelegatingDataSource source -> {
				return getConnectionPoolInfo(source.getTargetDataSource());  // Perhaps it's wrapped?
			}
			case null, default -> {
				return null;
			}
		}

		return info.toString();
	}

	static void addPoolMetadata(@NonNull GenericObjectPool<?> pool, @NonNull StringBuilder info) {
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
		info.append("removeAbandonedTimeoutDuration [").append(pool.getRemoveAbandonedTimeoutDuration()).append("]"); // TODO decide if we should make this human readable
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
