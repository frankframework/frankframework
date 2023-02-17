/*
   Copyright 2021-2023 WeAreFrank!

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
package nl.nn.adapterframework.jta.narayana;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;

import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.jboss.narayana.jta.jms.JmsXAResourceRecoveryHelper;
import org.jboss.narayana.jta.jms.TransactionHelper;
import org.messaginghub.pooled.jms.JmsPoolXAConnectionFactory;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.jndi.JndiConnectionFactoryFactory;
import nl.nn.adapterframework.util.AppConstants;

public class NarayanaConnectionFactoryFactory extends JndiConnectionFactoryFactory {

	private @Setter NarayanaJtaTransactionManager transactionManager;

	private @Getter @Setter int maxPoolSize=20;
	private @Getter @Setter int maxIdleTime=60;

	public NarayanaConnectionFactoryFactory() {
		AppConstants appConstants = AppConstants.getInstance();
		maxPoolSize = appConstants.getInt(MAX_POOL_SIZE_PROPERTY, maxPoolSize);
		maxIdleTime = appConstants.getInt(MAX_IDLE_TIME_PROPERTY, maxIdleTime);
	}

	@Override
	protected ConnectionFactory augmentConnectionFactory(ConnectionFactory connectionFactory, String connectionFactoryName) {
		if (connectionFactory instanceof XAConnectionFactory) {
			XAResourceRecoveryHelper recoveryHelper = new JmsXAResourceRecoveryHelper((XAConnectionFactory) connectionFactory);
			this.transactionManager.registerXAResourceRecoveryHelper(recoveryHelper);

			if(maxPoolSize > 1) {
				return createConnectionFactoryPool(connectionFactory);
			}

			TransactionHelper transactionHelper = new NarayanaTransactionHelper(transactionManager.getTransactionManager());
			return new ConnectionFactoryProxy((XAConnectionFactory) connectionFactory, transactionHelper);
		}
		log.warn("ConnectionFactory [{}] is not XA enabled", connectionFactoryName);
		return connectionFactory;
	}

	private ConnectionFactory createConnectionFactoryPool(ConnectionFactory xaConnectionFactory) {
		JmsPoolXAConnectionFactory pooledConnectionFactory = new JmsPoolXAConnectionFactory();
		pooledConnectionFactory.setTransactionManager(this.transactionManager.getTransactionManager());
		pooledConnectionFactory.setConnectionFactory(xaConnectionFactory);

		pooledConnectionFactory.setMaxConnections(getMaxPoolSize());
		pooledConnectionFactory.setConnectionIdleTimeout(getMaxIdleTime() * 1000);
		pooledConnectionFactory.setConnectionCheckInterval(-1);
		pooledConnectionFactory.setUseProviderJMSContext(false); // indicates whether the pool should include JMSContext in the pooling, when set to true, it disables connection pooling

		pooledConnectionFactory.setMaxSessionsPerConnection(20); // defaults to 500
		pooledConnectionFactory.setBlockIfSessionPoolIsFull(true);
		pooledConnectionFactory.setBlockIfSessionPoolIsFullTimeout(-1L);

		pooledConnectionFactory.setUseAnonymousProducers(true);
		return pooledConnectionFactory;
	}
}
