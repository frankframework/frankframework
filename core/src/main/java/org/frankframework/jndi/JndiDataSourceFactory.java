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
package org.frankframework.jndi;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import org.frankframework.dbms.TransactionalDbmsSupportAwareDataSourceProxy;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.util.AppConstants;

/**
 * Factory through which (TX-enabled) DataSources can be retrieved.
 *
 * Already created DataSources are stored in a ConcurrentHashMap.
 * Every DataSource can be augmented before it is added.
 */
public class JndiDataSourceFactory extends JndiObjectFactory<DataSource,CommonDataSource> implements IDataSourceFactory {

	public static final String DEFAULT_DATASOURCE_NAME_PROPERTY = "jdbc.datasource.default";
	public static final String GLOBAL_DEFAULT_DATASOURCE_NAME = AppConstants.getInstance().getProperty(DEFAULT_DATASOURCE_NAME_PROPERTY);

	public JndiDataSourceFactory() {
		super(CommonDataSource.class);
	}

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
	public DataSource getDataSource(String dataSourceName) throws NamingException {
		return get(dataSourceName);
	}

	@Override
	public DataSource getDataSource(String dataSourceName, Properties jndiEnvironment) throws NamingException {
		return get(dataSourceName, jndiEnvironment);
	}


	@Override
	public List<String> getDataSourceNames() {
		return new ArrayList<>(objects.keySet());
	}

}
