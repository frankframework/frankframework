/*
   Copyright 2021, 2022 WeAreFrank!

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

import javax.transaction.UserTransaction;

import org.springframework.transaction.TransactionSystemException;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
import nl.nn.adapterframework.jta.StatusRecordingTransactionManager;

public class BtmJtaTransactionManager extends StatusRecordingTransactionManager {

	private static final long serialVersionUID = 1L;

	@Override
	protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
		return (BitronixTransactionManager)retrieveTransactionManager();
	}

	@Override
	protected BitronixTransactionManager createTransactionManager() {
		Configuration configuration = TransactionManagerServices.getConfiguration();
		configuration.setServerId(getUid());
		configuration.setJdbcProxyFactoryClass("bitronix.tm.resource.jdbc.proxy.JdbcJavaProxyFactory");
		return TransactionManagerServices.getTransactionManager();
	}

	@Override
	protected boolean shutdownTransactionManager() {
		BitronixTransactionManager transactionManager = (BitronixTransactionManager)getTransactionManager();
		if(transactionManager == null) { //TM was never created?
			return true;
		}

		transactionManager.shutdown();

		int inflightCount = transactionManager.getInFlightTransactionCount();
		return inflightCount == 0;
	}
}
