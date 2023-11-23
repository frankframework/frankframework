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
package nl.nn.adapterframework.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import nl.nn.adapterframework.jndi.ResourceBasedObjectFactory;
import nl.nn.adapterframework.util.Misc;

/*
 * MongoClientFactory that retrieves its configuration from a properties file on the classpath.
 * 
 * @author Gerrit van Brakel
 */
public class ResourceBasedMongoClientFactory extends ResourceBasedObjectFactory<MongoClient, MongoClient> implements IMongoClientFactory {

	public final String MONGODB_URL_PREFIX="mongodb://";

	public final String AUTH_ALIAS_KEY="authAlias";
	public final String USERNAME_KEY="username";
	public final String PASSWORD_KEY="password";

	@Override
	public MongoClient getMongoClient(String dataSourceName) throws NamingException {
		return get(dataSourceName);
	}

	@Override
	public MongoClient getMongoClient(String dataSourceName, Properties jndiEnvironment) throws NamingException {
		return get(dataSourceName, jndiEnvironment);
	}

	@Override
	protected MongoClient createObject(Properties properties, String objectName) throws NamingException {
		String url = properties.getProperty("url");
		if (StringUtils.isEmpty(url)) {
			throw new NamingException("property url must be specified for object ["+objectName+"]");
		}
		if (!url.startsWith(MONGODB_URL_PREFIX)) {
			throw new NamingException("property url must url ["+url+"] for object ["+objectName+"] must start with '"+MONGODB_URL_PREFIX+"'");
		}
		String authAlias = properties.getProperty(AUTH_ALIAS_KEY);
		String username = properties.getProperty(USERNAME_KEY);
		String password = properties.getProperty(PASSWORD_KEY);
		url = Misc.insertAuthorityInUrlString(url, authAlias, username, password);
		return MongoClients.create(url);
	}


	@Override
	public List<String> getMongoClients() {
		return new ArrayList<>(objects.keySet());
	}

}
