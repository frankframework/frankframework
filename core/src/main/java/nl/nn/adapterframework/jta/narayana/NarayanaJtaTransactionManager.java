/*
   Copyright 2022 WeAreFrank!

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

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.jboss.narayana.jta.jms.TransactionHelper;
import org.jboss.narayana.jta.jms.TransactionHelperImpl;
import org.springframework.transaction.TransactionSystemException;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import lombok.Getter;
import nl.nn.adapterframework.jta.StatusRecordingTransactionManager;

public class NarayanaJtaTransactionManager extends StatusRecordingTransactionManager {

	private static final long serialVersionUID = 1L;

//	private @Getter @Setter int shutdownTimeout = 10000;

	private @Getter RecoveryManager recoveryManager;
	private @Getter TransactionHelper transactionHelper;

	private boolean initialized=false;


	@Override
	protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
		initialize();
		return com.arjuna.ats.jta.UserTransaction.userTransaction();
	}

	@Override
	protected TransactionManager createTransactionManager() throws TransactionSystemException {
		initialize();
		TransactionManager result = com.arjuna.ats.jta.TransactionManager.transactionManager();
		transactionHelper = new TransactionHelperImpl(result);
		return result;
	}

	private void initialize() throws TransactionSystemException {
		if (!initialized) {
			initialized=true;
			determineTmUid();
			try {
				arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(getUid());
			} catch (CoreEnvironmentBeanException e) {
				throw new TransactionSystemException("Cannot set TmUid", e);
			}

			recoveryManager = RecoveryManager.manager();
			recoveryManager.initialize();
			recoveryManager.startRecoveryManagerThread();

			XARecoveryModule recoveryModule = new XARecoveryModule();
			recoveryManager.addModule(recoveryModule);
		}
	}

	@Override
	protected boolean shutdownTransactionManager() {
//		TimeoutGuard tg = new TimeoutGuard("shutdown NarayanaJtaTransactionManager");
		try {
//			tg.activateGuard(getShutdownTimeout());
			recoveryManager.terminate();
		} catch (Exception e) {
			log.warn("could not shut down transaction manager", e);
			return false;
//		} finally {
//			if (tg.cancel()) {
//				return false;
//			}
		}
		return true;
	}


	public void registerXAResourceRecoveryHelper(XAResourceRecoveryHelper xaResourceRecoveryHelper) {
		getXARecoveryModule().addXAResourceRecoveryHelper(xaResourceRecoveryHelper);
	}

	private XARecoveryModule getXARecoveryModule() {
		XARecoveryModule xaRecoveryModule = XARecoveryModule.getRegisteredXARecoveryModule();
		if (xaRecoveryModule != null) {
			return xaRecoveryModule;
		}
		throw new IllegalStateException("XARecoveryModule is not registered with recovery manager");
	}

}
