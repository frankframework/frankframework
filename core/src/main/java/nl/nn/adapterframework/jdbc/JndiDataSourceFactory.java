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
import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import nl.nn.adapterframework.jms.JndiObjectFactory;
import nl.nn.adapterframework.util.AppConstants;

/**
 * would be nice if we could have used JndiObjectFactoryBean but it has too much overhead
 *
 */
public class JndiDataSourceFactory extends JndiObjectFactory<DataSource,CommonDataSource> implements IDataSourceFactory {

	public static final String GLOBAL_DEFAULT_DATASOURCE_NAME = AppConstants.getInstance().getProperty("jdbc.datasource.default");

	public JndiDataSourceFactory() {
		super(CommonDataSource.class);
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
		return new ArrayList<String>(objects.keySet());
	}

}
