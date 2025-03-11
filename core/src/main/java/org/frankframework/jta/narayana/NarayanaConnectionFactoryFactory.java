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
package org.frankframework.jta.narayana;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.XAConnectionFactory;

import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.jboss.narayana.jta.jms.JmsXAResourceRecoveryHelper;
import org.jboss.narayana.jta.jms.TransactionHelper;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolXAConnectionFactory;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.jndi.JndiConnectionFactoryFactory;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;

public class NarayanaConnectionFactoryFactory extends JndiConnectionFactoryFactory {

	private @Setter NarayanaJtaTransactionManager transactionManager;

	private @Getter @Setter int maxIdleTime = AppConstants.getInstance().getInt("transactionmanager.narayana.jms.connection.maxIdleTime", 60);
	private @Getter @Setter int maxPoolSize = AppConstants.getInstance().getInt("transactionmanager.narayana.jms.connection.maxPoolSize", 20);
	private @Getter @Setter int connectionCheckInterval = AppConstants.getInstance().getInt("transactionmanager.narayana.jms.connection.checkInterval", 300);
	private @Getter @Setter int maxSessions = AppConstants.getInstance().getInt("transactionmanager.narayana.jms.connection.maxSessions", 500);
	private @Getter @Setter int sessionWaitTimeout = AppConstants.getInstance().getInt("transactionmanager.narayana.jms.connections.sessionWaitTimeout", 15);

	@Override
	protected ConnectionFactory augmentConnectionFactory(ConnectionFactory connectionFactory, String connectionFactoryName) {
		if (connectionFactory instanceof XAConnectionFactory factory) {
			XAResourceRecoveryHelper recoveryHelper = new JmsXAResourceRecoveryHelper(factory);
			this.transactionManager.registerXAResourceRecoveryHelper(recoveryHelper);

			if(maxPoolSize > 1) {
				return createConnectionFactoryPool(connectionFactory);
			}

			TransactionHelper transactionHelper = new NarayanaTransactionHelper(transactionManager.getTransactionManager());
			log.info("add TransactionHelper [{}] to ConnectionFactory", transactionHelper);
			return new ConnectionFactoryProxy(factory, transactionHelper);
		}

		log.info("ConnectionFactory [{}] is not XA enabled, unable to register with an Transaction Manager", connectionFactoryName);
		if(maxPoolSize > 1) {
			return createConnectionFactoryPool(connectionFactory);
		}
		return connectionFactory;
	}

	private ConnectionFactory createConnectionFactoryPool(ConnectionFactory connectionFactory) {
		if(connectionFactory instanceof XAConnectionFactory) {
			JmsPoolXAConnectionFactory pooledConnectionFactory = new JmsPoolXAConnectionFactory();
			pooledConnectionFactory.setTransactionManager(this.transactionManager.getTransactionManager());
			pooledConnectionFactory.setConnectionFactory(connectionFactory);
			return augmentPool(pooledConnectionFactory);
		}

		JmsPoolConnectionFactory pooledConnectionFactory = new JmsPoolConnectionFactory();
		pooledConnectionFactory.setConnectionFactory(connectionFactory);
		return augmentPool(pooledConnectionFactory);
	}

	private ConnectionFactory augmentPool(JmsPoolConnectionFactory pooledConnectionFactory) {
		pooledConnectionFactory.setMaxConnections(maxPoolSize);
		pooledConnectionFactory.setConnectionIdleTimeout(getMaxIdleTime() * 1000);
		pooledConnectionFactory.setConnectionCheckInterval(connectionCheckInterval * 1000L);
		pooledConnectionFactory.setUseProviderJMSContext(false); // indicates whether the pool should include JMSContext in the pooling, when set to true, it disables connection pooling

		pooledConnectionFactory.setMaxSessionsPerConnection(maxSessions);
		pooledConnectionFactory.setBlockIfSessionPoolIsFull(true);
		pooledConnectionFactory.setBlockIfSessionPoolIsFullTimeout(sessionWaitTimeout * 1000L);

		pooledConnectionFactory.setUseAnonymousProducers(true);

		log.info("created pooled {} [{}]", ClassUtils.classNameOf(pooledConnectionFactory), pooledConnectionFactory);
		return pooledConnectionFactory;
	}
}
