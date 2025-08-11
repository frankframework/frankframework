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
import org.frankframework.util.CredentialFactory;

public class MongoClientFactory {


	private final String resourceName;
	private final FrankResource resource;

	public MongoClientFactory(String resourceName, FrankResource resource) {
		this.resourceName = resourceName;
		this.resource = resource;
	}

	public MongoClient createMongoClient() {
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

	@Override
	public String toString() {
		return "MongoClientFactory [resourceName=" + resourceName + ", resource=" + resource + "]";
	}
}
