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
package nl.nn.adapterframework.jms;

import javax.jms.ConnectionFactory;
import javax.naming.NamingException;

import nl.nn.adapterframework.jdbc.JndiObjectFactory;

public class JndiConnectionFactoryFactory extends JndiObjectFactory<ConnectionFactory,ConnectionFactory> implements IConnectionFactoryFactory {

	public JndiConnectionFactoryFactory() {
		super(ConnectionFactory.class);
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName) throws NamingException {
		return get(connectionFactoryName);
	}
	

}
