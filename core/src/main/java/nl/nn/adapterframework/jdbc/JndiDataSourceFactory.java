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
package nl.nn.adapterframework.jdbc;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import lombok.SneakyThrows;
import nl.nn.adapterframework.util.AppConstants;

/**
 * would be nice if we could have used JndiObjectFactoryBean but it has too much overhead
 *
 */
public class JndiDataSourceFactory implements IDataSourceFactory {

	public static final String DEFAULT_DATASOURCE_NAME = AppConstants.getInstance().getProperty("jdbc.default.datasource");
	protected Map<String,DataSource> dataSources = new ConcurrentHashMap<>();

	@Override
	public DataSource getDataSource(String dataSourceName) throws NamingException {
		return dataSources.computeIfAbsent(dataSourceName, k -> compute(k, null));
	}

	@Override
	public DataSource getDataSource(String dataSourceName, Properties jndiEnvironment) throws NamingException {
		return dataSources.computeIfAbsent(dataSourceName, k -> compute(k, jndiEnvironment));
	}

	@SneakyThrows(NamingException.class)
	private DataSource compute(String dataSourceName, Properties jndiEnvironment) {
		return augmentDataSource(lookupDataSource(dataSourceName, jndiEnvironment), dataSourceName);
	}

	/**
	 * Performs the actual JNDI lookup
	 */
	private CommonDataSource lookupDataSource(String jndiName, Properties jndiEnvironment) throws NamingException {
		return JndiDataSourceLocator.lookup(jndiName, jndiEnvironment);
	}

	/**
	 * Add a wrapper around a DataSource such as LazyLoading / Pooling etc
	 */
	protected DataSource augmentDataSource(CommonDataSource dataSource, String dataSourceName) {
		return (DataSource)dataSource;
	}

	/**
	 * Add a DataSource to this factory so it can be used without the need of a JNDI lookup.
	 * Only to be used when registering a DataSource through Spring and never through a JNDI lookup
	 */
	public void addDataSource(CommonDataSource dataSource, String dataSourceName) {
		dataSources.putIfAbsent(dataSourceName, augmentDataSource(dataSource, dataSourceName));
	}
}
