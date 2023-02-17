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
package nl.nn.adapterframework.jta.btm;


import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;

import org.springframework.beans.factory.DisposableBean;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.jndi.JndiConnectionFactoryFactory;
import nl.nn.adapterframework.util.AppConstants;

public class BtmConnectionFactoryFactory extends JndiConnectionFactoryFactory implements DisposableBean {

	private @Getter @Setter int minPoolSize=0;
	private @Getter @Setter int maxPoolSize=20;
	private @Getter @Setter int maxIdleTime=60;
	private @Getter @Setter int maxLifeTime=0;

	public BtmConnectionFactoryFactory() {
		AppConstants appConstants = AppConstants.getInstance();
		minPoolSize = appConstants.getInt(MIN_POOL_SIZE_PROPERTY, minPoolSize);
		maxPoolSize = appConstants.getInt(MAX_POOL_SIZE_PROPERTY, maxPoolSize);
		maxIdleTime = appConstants.getInt(MAX_IDLE_TIME_PROPERTY, maxIdleTime);
		maxLifeTime = appConstants.getInt(MAX_LIFE_TIME_PROPERTY, maxLifeTime);
	}

	@Override
	protected ConnectionFactory augment(ConnectionFactory connectionFactory, String connectionFactoryName) {
		if (connectionFactory instanceof XAConnectionFactory) {
			PoolingConnectionFactory result = new PoolingConnectionFactory();
			result.setUniqueName(connectionFactoryName);
			result.setMinPoolSize(getMinPoolSize());
			result.setMaxPoolSize(getMaxPoolSize());
			result.setMaxIdleTime(getMaxIdleTime());
			result.setMaxLifeTime(getMaxLifeTime());
			result.setAllowLocalTransactions(true);
			result.setXaConnectionFactory((XAConnectionFactory)connectionFactory);
			result.init();
			return result;
		}
		log.warn("ConnectionFactory [{}] is not XA enabled", connectionFactoryName);
		return connectionFactory;
	}

	@Override
	public void destroy() throws Exception {
		objects.values().stream().filter(PoolingConnectionFactory.class::isInstance).forEach(cf -> ((PoolingConnectionFactory)cf).close());
	}
}
