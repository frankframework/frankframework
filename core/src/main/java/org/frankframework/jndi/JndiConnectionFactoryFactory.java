/*
   Copyright 2021 - 2024 WeAreFrank!

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

import javax.naming.NamingException;

import jakarta.jms.ConnectionFactory;

import org.frankframework.jdbc.datasource.ObjectFactory;
import org.frankframework.jms.IConnectionFactoryFactory;

public class JndiConnectionFactoryFactory extends ObjectFactory<ConnectionFactory, ConnectionFactory> implements IConnectionFactoryFactory {

	public JndiConnectionFactoryFactory() {
		super(ConnectionFactory.class);
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName) throws NamingException {
		return getConnectionFactory(connectionFactoryName, null);
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName, Properties environment) throws NamingException {
		return get(connectionFactoryName, environment);
	}

	@Override
	public List<String> getConnectionFactoryNames() {
		return getObjectNames();
	}

}
