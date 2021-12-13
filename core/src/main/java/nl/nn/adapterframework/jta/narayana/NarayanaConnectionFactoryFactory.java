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
package nl.nn.adapterframework.jta.narayana;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;

import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.jboss.narayana.jta.jms.JmsXAResourceRecoveryHelper;
import org.jboss.narayana.jta.jms.TransactionHelper;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import nl.nn.adapterframework.jndi.JndiConnectionFactoryFactory;

public class NarayanaConnectionFactoryFactory extends JndiConnectionFactoryFactory {

	private NarayanaRecoveryManager recoveryManager;
	private TransactionHelper transactionHelper;

	@Override
	protected ConnectionFactory augment(ConnectionFactory connectionFactory, String connectionFactoryName) {
		if (connectionFactory instanceof XAConnectionFactory) {
			XAResourceRecoveryHelper recoveryHelper = new JmsXAResourceRecoveryHelper((XAConnectionFactory) connectionFactory);
			this.recoveryManager.registerXAResourceRecoveryHelper(recoveryHelper);
			return new ConnectionFactoryProxy((XAConnectionFactory) connectionFactory, transactionHelper);
		}
		log.warn("ConnectionFactory [{}] is not XA enabled", connectionFactoryName);
		return connectionFactory;
	}

	public void setRecoveryManager(NarayanaRecoveryManager recoveryManager) {
		this.recoveryManager = recoveryManager;
	}

	public void setTransactionHelper(TransactionHelper transactionHelper) {
		this.transactionHelper = transactionHelper;
	}
}
