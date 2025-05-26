/*
   Copyright 2021-2025 WeAreFrank!

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

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.frankframework.jdbc.datasource.FrankResource;
import org.frankframework.jdbc.datasource.ObjectFactory;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CredentialFactory;

/**
 * MongoClientFactory that retrieves its configuration from either JNDI or `resources.yml`.
 *
 * @author Gerrit van Brakel
 */
public class JndiMongoClientFactory extends ObjectFactory<MongoClient, Object> {

	public static final String DEFAULT_DATASOURCE_NAME_PROPERTY = "mongodb.datasource.default";
	public static final String GLOBAL_DEFAULT_DATASOURCE_NAME_DEFAULT = "mongodb/MongoClient";
	public static final String GLOBAL_DEFAULT_DATASOURCE_NAME = AppConstants.getInstance().getProperty(DEFAULT_DATASOURCE_NAME_PROPERTY, GLOBAL_DEFAULT_DATASOURCE_NAME_DEFAULT);

	public JndiMongoClientFactory() {
		super(null, "mongodb", "MongoDB");
	}

	@Override
	protected MongoClient augment(Object object, String objectName) {
		if (object instanceof MongoClient client) {
			return client;
		}
		if (object instanceof FrankResource resource) {
			return map(resource);
		}
		throw new IllegalArgumentException("resource ["+objectName+"] not of required type");
	}

	private MongoClient map(FrankResource resource) {
		String url = resource.getUrl();
		MongoClientSettings.Builder settings = MongoClientSettings.builder()
				.applyToClusterSettings(builder -> builder.serverSelectionTimeout(2, TimeUnit.SECONDS))
				.applyToSocketSettings(builder -> builder.connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS))
				.applyConnectionString(new ConnectionString(url));

		CredentialFactory cf = resource.getCredentials();
		if (StringUtils.isNotEmpty(cf.getUsername()) && StringUtils.isNotEmpty(cf.getPassword())) {
			MongoCredential credential = MongoCredential.createScramSha256Credential(cf.getUsername(), "$external", cf.getPassword().toCharArray());
			settings.credential(credential);
		}

		return MongoClients.create(settings.build());
	}

	public MongoClient getMongoClient(String name) {
		return get(name, null);
	}
}
