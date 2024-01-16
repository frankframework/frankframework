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
package org.frankframework.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;

import com.mongodb.client.MongoClient;

import org.frankframework.jndi.JndiObjectFactory;
import org.frankframework.util.AppConstants;

/**
 * MongoClientFactory that retrieves its configuration from JNDI.
 *
 * @author Gerrit van Brakel
 */
public class JndiMongoClientFactory extends JndiObjectFactory<MongoClient,MongoClient> implements IMongoClientFactory {

	public static final String DEFAULT_DATASOURCE_NAME_PROPERTY = "mongodb.datasource.default";
	public static final String GLOBAL_DEFAULT_DATASOURCE_NAME_DEFAULT = "mongodb/MongoClient";
	public static final String GLOBAL_DEFAULT_DATASOURCE_NAME = AppConstants.getInstance().getProperty(DEFAULT_DATASOURCE_NAME_PROPERTY, GLOBAL_DEFAULT_DATASOURCE_NAME_DEFAULT);

	public JndiMongoClientFactory() {
		super(MongoClient.class);
	}

	@Override
	public MongoClient getMongoClient(String dataSourceName) throws NamingException {
		return get(dataSourceName);
	}

	@Override
	public MongoClient getMongoClient(String dataSourceName, Properties jndiEnvironment) throws NamingException {
		return get(dataSourceName, jndiEnvironment);
	}


	@Override
	public List<String> getMongoClients() {
		return new ArrayList<>(objects.keySet());
	}

}
