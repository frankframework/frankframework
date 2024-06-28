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
package org.frankframework.jdbc;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.frankframework.util.AppConstants;
import org.springframework.jndi.JndiLocatorSupport;

public interface IDataSourceFactory {

	String DEFAULT_DATASOURCE_NAME_PROPERTY = "jdbc.datasource.default";
	String GLOBAL_DEFAULT_DATASOURCE_NAME = AppConstants.getInstance().getProperty(DEFAULT_DATASOURCE_NAME_PROPERTY);

	/**
	 * Look up a DataSource from the JNDI
	 */
	DataSource getDataSource(String dataSourceName) throws IllegalStateException;

	/**
	 * Set the JNDI environment to use for JNDI lookups.
	 * <p>Uses a Spring JndiTemplate with the given environment settings.
	 * @see JndiLocatorSupport#setJndiTemplate
	 */
	DataSource getDataSource(String dataSourceName, Properties jndiEnvironment) throws IllegalStateException;

	/**
	 * Return all known/registered DataSources
	 */
	List<String> getDataSourceNames();
}
