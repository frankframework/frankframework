/*
   Copyright 2021 - 2024 WeAreFrank!

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

import java.util.List;
import java.util.Properties;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import org.frankframework.jdbc.IDataSourceFactory;

/**
 * Factory through which (TX-enabled) DataSources can be retrieved.
 * Default implementation, does not use pooling, wraps the DataSource in a TransactionAwareDataSourceProxy.
 *
 */
public class DataSourceFactory extends ObjectFactory<CommonDataSource, CommonDataSource> implements IDataSourceFactory {

	public DataSourceFactory() {
		super(CommonDataSource.class);
	}

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
	@Override
	protected final DataSource augment(CommonDataSource dataSource, String dataSourceName) {
		return new TransactionalDbmsSupportAwareDataSourceProxy(augmentDatasource(dataSource, dataSourceName));
	}

	@Override
	public DataSource getDataSource(String dataSourceName) {
		return getDataSource(dataSourceName, null);
	}

	@Override
	public DataSource getDataSource(@Nonnull String dataSourceName, @Nullable Properties environment) {
		return (DataSource) get(dataSourceName, environment);
	}

	@Override
	public List<String> getDataSourceNames() {
		return getObjectNames();
	}
}
