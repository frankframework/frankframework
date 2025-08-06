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

import org.frankframework.jdbc.datasource.FrankResource;
import org.frankframework.jdbc.datasource.ObjectFactory;
import org.frankframework.util.AppConstants;

/**
 * MongoClientFactory that retrieves its configuration from either JNDI or `resources.yml`.
 *
 * @author Gerrit van Brakel
 */
public class MongoClientFactoryFactory extends ObjectFactory<MongoClientFactory, Object> {

	public static final String DEFAULT_DATASOURCE_NAME_PROPERTY = "mongodb.datasource.default";
	public static final String GLOBAL_DEFAULT_DATASOURCE_NAME_DEFAULT = "mongodb/MongoClient";
	public static final String GLOBAL_DEFAULT_DATASOURCE_NAME = AppConstants.getInstance().getProperty(DEFAULT_DATASOURCE_NAME_PROPERTY, GLOBAL_DEFAULT_DATASOURCE_NAME_DEFAULT);

	public MongoClientFactoryFactory() {
		super(null, "mongodb", "MongoDB");
	}

	@Override
	protected MongoClientFactory augment(Object object, String objectName) {
		if (object instanceof MongoClientFactory factory) {
			return factory;
		}
		if (object instanceof FrankResource resource) {
			return new MongoClientFactory(objectName, resource);
		}
		throw new IllegalArgumentException("resource ["+objectName+"] not of required type");
	}

	public MongoClientFactory getMongoClientFactory(String name) {
		return get(name, null);
	}
}
