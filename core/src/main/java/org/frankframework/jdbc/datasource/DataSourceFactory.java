/*
   Copyright 2021 - 2026 WeAreFrank!

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

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import org.frankframework.util.StringUtil;

/**
 * Factory through which (TX-enabled) DataSources can be retrieved.
 * Default implementation, does not use pooling, wraps the {@link DataSource} in a {@link TransactionAwareDataSourceProxy).
 *
 */
public class DataSourceFactory extends NonTransactionalDataSourceFactory {

	/**
	 * Allow implementing classes to augment the DataSource.
	 * See {@link #augment(CommonDataSource, String)}.
	 */
	@SuppressWarnings("java:S1172")
	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		return (DataSource) dataSource;
	}

	/**
	 * Ensure that the outermost DataSource proxy/wrapper in the chain is the {@link TransactionAwareDataSourceProxy}.
	 * Otherwise the {@link TransactionAwareDataSourceProxy} will NOT work!
	 */
	@NonNull
	@Override
	protected final DataSource augment(@NonNull CommonDataSource dataSource, @NonNull String dataSourceName) {
		return new TransactionalDbmsSupportAwareDataSourceProxy(augmentDatasource(dataSource, dataSourceName));
	}

	@NonNull
	@Override
	protected ObjectInfo toObjectInfo(String name) {
		DataSource datasource = getDataSource(name);
		if (datasource instanceof TransactionalDbmsSupportAwareDataSourceProxy ds) {
			return new ObjectInfo(name, ds.getInfo(), ds.getPoolInfo());
		}

		return new ObjectInfo(name, StringUtil.reflectionToString(datasource), null);
	}
}
