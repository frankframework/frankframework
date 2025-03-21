/*
   Copyright 2025 WeAreFrank!

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
import java.util.ArrayList;
import java.util.List;

import com.arjuna.ats.arjuna.AtomicAction;
import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.coordinator.ActionStatus;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.objectstore.ObjectStoreIterator;
import com.arjuna.ats.arjuna.objectstore.RecoveryStore;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.arjuna.recovery.TransactionStatusConnectionManager;
import com.arjuna.ats.internal.jta.transaction.arjunacore.subordinate.jca.SubordinateAtomicAction;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class HeuristicDetectingRecoveryModule implements RecoveryModule {

	private final String transactionType; // StateManager/BasicAction/TwoPhaseCoordinator/AtomicAction
	private final RecoveryStore recoveryStore;
	private final TransactionStatusConnectionManager transactionStatusConnectionManager;

	// Array of transactions found in the object store of the AtomicAction type.
	private List<Uid> heuristicTransactionUids = null;

	public HeuristicDetectingRecoveryModule() {
		transactionType = new AtomicAction().type();
		recoveryStore = StoreManager.getRecoveryStore();
		transactionStatusConnectionManager = new TransactionStatusConnectionManager();
	}

	@Override
	public void periodicWorkFirstPass() {
		try {
			log.debug("HeuristicDetectingRecoveryModule first pass");
			heuristicTransactionUids = getUids();

		} catch (ObjectStoreException | IOException ex) {
			log.warn("unable to get all Uids from recoveryStore [{}]", recoveryStore, ex);
		}
	}

	/**
	 * Obtain all of the Uids for a specified type, regardless of their state.
	 */
	private List<Uid> getUids() throws ObjectStoreException, IOException {
		List<Uid> uids = new ArrayList<>();
		ObjectStoreIterator iter = new ObjectStoreIterator(recoveryStore, transactionType);

		while (true) {
			Uid uid = iter.iterate();

			if (uid == null || Uid.nullUid().equals(uid)) break;

			if (isHeuristicHazard(uid)) {
				log.debug("found heuristic transaction {}", uid::toString);
				uids.add(uid);
			} else {
				log.debug("skipping non heuristic transaction: {}", uid::toString);
			}
		}

		return uids;
	}

	/**
	 * Retrieve the transaction status from its original process. Note: this can be the status of the transaction from the object store
	 */
	private boolean isHeuristicHazard(Uid uid) {
		int theStatus = transactionStatusConnectionManager.getTransactionStatus(transactionType, uid);
		return theStatus == ActionStatus.H_HAZARD;
	}

	private void doForgetTransaction(Uid uid) {
		try {
			SubordinateAtomicAction atomicAction = new SubordinateAtomicAction(uid);
			atomicAction.doForget();
		} catch (Exception ex) {
			// TODO
		}
	}


	@Override
	public void periodicWorkSecondPass() {
		// TODO Auto-generated method stub
		for (Uid uid : heuristicTransactionUids) {
			System.err.println(">>> " + uid.getBytes());
		}
	}

}
