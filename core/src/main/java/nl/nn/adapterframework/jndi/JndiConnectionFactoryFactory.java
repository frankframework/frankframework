/*
   Copyright 2021, 2023 WeAreFrank!

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
package nl.nn.adapterframework.jndi;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.naming.NamingException;

import nl.nn.adapterframework.jms.IConnectionFactoryFactory;

public class JndiConnectionFactoryFactory extends JndiObjectFactory<ConnectionFactory,ConnectionFactory> implements IConnectionFactoryFactory {

	public static final String MIN_POOL_SIZE_PROPERTY="jms.connection.minPoolSize";
	public static final String MAX_POOL_SIZE_PROPERTY="jms.connection.maxPoolSize";
	public static final String MAX_IDLE_TIME_PROPERTY="jms.connection.maxIdleTime";
	public static final String MAX_LIFE_TIME_PROPERTY="jms.connection.maxLifeTime";

	public JndiConnectionFactoryFactory() {
		super(ConnectionFactory.class);
	}

	protected ConnectionFactory augmentConnectionFactory(ConnectionFactory connectionFactory, String connectionFactoryName) {
		return connectionFactory;
	}

	@Override
	protected final ConnectionFactory augment(ConnectionFactory object, String objectName) throws NamingException {
		return augmentConnectionFactory(object, objectName);
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName) throws NamingException {
		return get(connectionFactoryName);
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName, Properties jndiEnvironment) throws NamingException {
		return get(connectionFactoryName, jndiEnvironment);
	}

	@Override
	public List<String> getConnectionFactoryNames() {
		return new ArrayList<>(objects.keySet());
	}

}
