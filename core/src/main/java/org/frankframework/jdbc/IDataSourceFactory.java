/*
   Copyright 2021-2026 WeAreFrank!

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
package org.frankframework.jdbc;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;
import org.springframework.jndi.JndiLocatorSupport;

import org.frankframework.util.AppConstants;

public interface IDataSourceFactory {

	String DEFAULT_DATASOURCE_NAME_PROPERTY = "jdbc.datasource.default";
	String GLOBAL_DEFAULT_DATASOURCE_NAME = AppConstants.getInstance().getProperty(DEFAULT_DATASOURCE_NAME_PROPERTY);

	/**
	 * Look up a {@link DataSource} by name.
	 *
	 * @throws NoSuchElementException if the DataSource cannot be found in any of the configured sources
	 * @throws IllegalStateException if the DataSource cannot be created
	 */
	@NonNull
	default DataSource getDataSource(String dataSourceName) throws IllegalStateException, NoSuchElementException {
		return getDataSource(dataSourceName, null);
	}

	/**
	 * Look up a {@link DataSource} by name with optional JNDI Environment or other additional properties to be used for the lookup of the datasource.
	 * @see org.frankframework.jdbc.datasource.ObjectFactory#get(String, Properties)
	 * @see JndiLocatorSupport#setJndiTemplate
	 *
	 * @throws NoSuchElementException if the DataSource cannot be found in any of the configured sources
	 * @throws IllegalStateException if the DataSource cannot be created
	 */
	@NonNull
	DataSource getDataSource(String dataSourceName, Properties jndiEnvironment) throws IllegalStateException, NoSuchElementException;

	/**
	 * Return all known/registered DataSources
	 */
	@NonNull
	List<String> getDataSourceNames();
}
