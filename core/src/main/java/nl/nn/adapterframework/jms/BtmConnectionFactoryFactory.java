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
import javax.jms.XAConnectionFactory;

import org.springframework.beans.factory.DisposableBean;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import nl.nn.adapterframework.jndi.JndiConnectionFactoryFactory;

public class BtmConnectionFactoryFactory extends JndiConnectionFactoryFactory implements DisposableBean {

	@Override
	protected ConnectionFactory augment(ConnectionFactory connectionFactory, String connectionFactoryName) {
		PoolingConnectionFactory result = new PoolingConnectionFactory();
		result.setUniqueName(connectionFactoryName);
		result.setMaxPoolSize(100);
		result.setAllowLocalTransactions(true);
		result.setXaConnectionFactory((XAConnectionFactory)connectionFactory);
		result.init();
		return result;
	}

	@Override
	public void destroy() throws Exception {
		objects.values().forEach(cf -> ((PoolingConnectionFactory)cf).close());
	}
}
