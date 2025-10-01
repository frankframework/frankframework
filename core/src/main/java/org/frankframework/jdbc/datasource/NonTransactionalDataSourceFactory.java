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
package org.frankframework.jdbc.datasource;

import java.util.List;
import java.util.Properties;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.frankframework.jdbc.IDataSourceFactory;

/**
 * Factory through which <em>NON-TRANSACTIONAL</em> DataSources can be retrieved.
 * Does not use pooling, and does NOT wrap the DataSource in a TransactionAwareDataSourceProxy.
 *
 */
public class NonTransactionalDataSourceFactory extends ObjectFactory<DataSource, CommonDataSource> implements IDataSourceFactory {

	public NonTransactionalDataSourceFactory() {
		super(CommonDataSource.class, "jdbc", "DataSources");
	}

	@Override
	public DataSource getDataSource(String dataSourceName) {
		return getDataSource(dataSourceName, null);
	}

	@Override
	public DataSource getDataSource(@Nonnull String dataSourceName, @Nullable Properties environment) {
		return get(dataSourceName, environment);
	}

	@Override
	public List<String> getDataSourceNames() {
		return getObjectNames();
	}
}
