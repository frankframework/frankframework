/*
   Copyright 2022-2025 WeAreFrank!

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

import java.io.IOException;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.springframework.transaction.TransactionSystemException;

import com.arjuna.ats.arjuna.AtomicAction;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.objectstore.RecoveryStore;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.state.InputObjectState;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import lombok.Getter;

import org.frankframework.jta.AbstractStatusRecordingTransactionManager;
import org.frankframework.util.AppConstants;

public class NarayanaJtaTransactionManager extends AbstractStatusRecordingTransactionManager {

	private final boolean heuristicDetectorEnabled = AppConstants.getInstance().getBoolean("transactionmanager.narayana.detectStuckTransactions", false);

	private static final long serialVersionUID = 1L;

	private @Getter RecoveryManager recoveryManager;

	private boolean initialized = false;

	@Override
	protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
		initialize();
		return com.arjuna.ats.jta.UserTransaction.userTransaction();
	}

	@Override
	protected TransactionManager createTransactionManager() throws TransactionSystemException {
		initialize();
		return com.arjuna.ats.jta.TransactionManager.transactionManager();
	}

	private void initialize() throws TransactionSystemException {
		if (!initialized) {
			initialized = true;
			determineTmUid();

			try {
				arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(getUid());
			} catch (CoreEnvironmentBeanException e) {
				throw new TransactionSystemException("Cannot set TmUid", e);
			}

			log.debug("TMUID [{}]", arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier());
			log.debug("ObjectStoreDir [{}]", arjPropertyManager.getObjectStoreEnvironmentBean().getObjectStoreDir());

			recoveryManager = RecoveryManager.manager();

			if (heuristicDetectorEnabled) {
				recoveryManager.addModule(new HeuristicDetectingRecoveryModule());
			}

			recoveryManager.initialize();
			recoveryManager.startRecoveryManagerThread();
		}
	}

	@Override
	protected boolean shutdownTransactionManager() {
		try {
			if (recoveryManager!=null) {
				if (!recoveryStoreEmpty()) {
					log.debug("RecoveryStore not empty. Performing recovery manager scan to clean up");
					recoveryManager.scan();
					if (!recoveryStoreEmpty()) {
						log.debug("RecoveryStore still not empty after scan. Waiting 10 seconds...");
						Thread.sleep(10000);
						if (!recoveryStoreEmpty()) {
							log.debug("RecoveryStore still not empty after waiting, scanning again");
							recoveryManager.scan();
						}
					}
				}
				recoveryManager.terminate();
				recoveryManager=null;
			}
			return recoveryStoreEmpty();
		} catch (Exception e) {
			log.warn("could not shutdown transaction manager", e);
			return false;
		}
	}

	/**
	 * See {@link com.arjuna.ats.arjuna.AtomicAction#type() AtomicAction#type}.
	 */
	private boolean recoveryStoreEmpty() throws ObjectStoreException, IOException {
		RecoveryStore store = StoreManager.getRecoveryStore();
		InputObjectState buff = new InputObjectState();

		String transactionType = new AtomicAction().type(); // StateManager/BasicAction/TwoPhaseCoordinator/AtomicAction
		if (!store.allObjUids(transactionType, buff)) {
			return false; // if an error occurred, consider the recovery store not completed
		}
		if (!buff.notempty()) {
			return true;
		}
		byte[] objUid=buff.unpackBytes();
		return !isNotBlankArray(objUid);
	}

	private boolean isNotBlankArray(byte[] arr) {
		for(int i=0; i<arr.length; i++) {
			if (arr[i]!=0) {
				return true;
			}
		}
		return false;
	}


	public void registerXAResourceRecoveryHelper(XAResourceRecoveryHelper xaResourceRecoveryHelper) {
		log.info("registering XAResourceRecoveryHelper {}", xaResourceRecoveryHelper);
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
