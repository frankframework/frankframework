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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import lombok.Setter;
import lombok.SneakyThrows;
import nl.nn.adapterframework.util.AppConstants;

/**
 * would be nice if we could have used JndiObjectFactoryBean but it has too much overhead
 *
 */
public class JndiDataSourceFactory implements IDataSourceFactory {

	public static final String DEFAULT_DATASOURCE_NAME = AppConstants.getInstance().getProperty("jdbc.datasource.default");
	protected Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
	private @Setter String jndiContextPrefix = null;

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
		if(StringUtils.isNotEmpty(jndiContextPrefix)) {
			return JndiDataSourceLocator.lookup(jndiContextPrefix+jndiName, jndiEnvironment);
		}

		//Fallback without prefix
		return JndiDataSourceLocator.lookup(jndiName, jndiEnvironment);
	}

	/**
	 * Add a wrapper around a DataSource such as LazyLoading / Pooling etc
	 */
	protected DataSource augmentDataSource(CommonDataSource dataSource, String dataSourceName) {
		return (DataSource)dataSource;
	}

	/**
	 * Add and augment a DataSource to this factory so it can be used without the need of a JNDI lookup.
	 * Should only be called during jUnit Tests or when registering a DataSource through Spring. Never through a JNDI lookup
	 */
	public DataSource addDataSource(CommonDataSource dataSource, String dataSourceName) {
		return dataSources.computeIfAbsent(dataSourceName, k -> augmentDataSource(dataSource, dataSourceName));
	}

	@Override
	public List<String> getDataSourceNames() {
		return new ArrayList<String>(dataSources.keySet());
	}
}
