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
package org.frankframework.jndi;

import java.util.List;
import java.util.Properties;

import jakarta.jms.ConnectionFactory;

import org.frankframework.jdbc.datasource.ObjectFactory;
import org.frankframework.jms.IConnectionFactoryFactory;
import org.frankframework.jms.TransactionalMetadataAwareConnectionFactoryProxy;
import org.frankframework.util.StringUtil;

/**
 * A factory for creating JMS ConnectionFactory objects.
 */
public class JmsConnectionFactoryFactory extends ObjectFactory<ConnectionFactory, ConnectionFactory> implements IConnectionFactoryFactory {

	public JmsConnectionFactoryFactory() {
		super(ConnectionFactory.class, "jms", "Connection Factories");
	}

	/**
	 * Allow implementing classes to augment the ConnectionFactory.
	 * See {@link #augment(ConnectionFactory, String)}.
	 */
	@SuppressWarnings("java:S1172")
	protected ConnectionFactory augmentConnectionFactory(ConnectionFactory cf, String objectName) {
		return cf;
	}

	@Override
	protected final ConnectionFactory augment(ConnectionFactory connectionFactory, String objectName) {
		return new TransactionalMetadataAwareConnectionFactoryProxy(augmentConnectionFactory(connectionFactory, objectName));
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName) {
		return getConnectionFactory(connectionFactoryName, null);
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName, Properties environment) {
		return get(connectionFactoryName, environment);
	}

	@Override
	public List<String> getConnectionFactoryNames() {
		return getObjectNames();
	}

	@Override
	protected ObjectInfo toObjectInfo(String name) {
		ConnectionFactory cf = getConnectionFactory(name);
		if (cf instanceof TransactionalMetadataAwareConnectionFactoryProxy mcf) {
			return new ObjectInfo(name, mcf.getInfo(), mcf.getPoolInfo());
		}

		return new ObjectInfo(name, StringUtil.reflectionToString(cf), null);
	}
}
