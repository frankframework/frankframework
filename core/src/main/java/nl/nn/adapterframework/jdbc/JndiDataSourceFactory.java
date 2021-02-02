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
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import org.springframework.jndi.JndiLocatorSupport;

import lombok.SneakyThrows;

public class JndiDataSourceFactory extends JndiLocatorSupport implements IDataSourceFactory {
	
	protected Map<String,DataSource> dataSources = new ConcurrentHashMap<>();
	
	{
		setResourceRef(true); //the prefix "java:comp/env/" will be added if the JNDI name doesn't already contain it. 
	}
	
	@Override
	public DataSource getDataSource(String dataSourceName) throws NamingException {
		return dataSources.computeIfAbsent(dataSourceName, k -> compute(k));
	}
	
	@SneakyThrows(NamingException.class)
	private DataSource compute(String dataSourceName) {
		return augmentDataSource(lookupDataSource(dataSourceName), dataSourceName);
	}

	protected CommonDataSource lookupDataSource(String jndiName) throws NamingException {
		return super.lookup(jndiName, CommonDataSource.class);
	}

	protected DataSource augmentDataSource(CommonDataSource dataSource, String dataSourceName) {
		return (DataSource)dataSource;
	}

}
