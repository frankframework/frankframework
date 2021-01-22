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

public class JndiDataSourceFactory extends JndiLocatorSupport {
	
	protected Map<String,DataSource> dataSources = new ConcurrentHashMap<>();
	
	public DataSource getDataSource(String dataSourceName) throws NamingException {
		return dataSources.computeIfAbsent(dataSourceName, k -> (DataSource)lookupDataSource(k));
	}
	
	protected CommonDataSource lookupDataSource(String dataSourceName) {
		try {
			return lookup(dataSourceName, CommonDataSource.class);
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
	}
	
}
