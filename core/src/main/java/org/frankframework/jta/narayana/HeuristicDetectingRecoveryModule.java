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
import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

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

import org.frankframework.util.AppConstants;
import org.frankframework.util.TimeProvider;

@Log4j2
public class HeuristicDetectingRecoveryModule implements RecoveryModule {

	private static int HEURISTIC_FAILURE_ATTEMPTS;
	private static Duration HEURISTIC_FAILURE_BACKOFF_DURATION;

	private final String transactionType; // StateManager/BasicAction/TwoPhaseCoordinator/AtomicAction
	private final RecoveryStore recoveryStore;
	private final TransactionStatusConnectionManager statusManager;

	// Map of transactions found in the object store of the AtomicAction type.
	private final Map<Uid, UidCacheItem> heuristicTransactionUids = new LinkedHashMap<>() {

		@Serial
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Entry<Uid, UidCacheItem> eldest) {
			return size() > 25;
		}
	};

	private static class UidCacheItem {
		private int count = 1;
		private final Instant age = TimeProvider.now();

		public UidCacheItem(Uid ignored) {
			// Required for computeIfAbsent
		}

		/**
		 * Entry has been 'hit', update or reset the count.
		 */
		public void hit() {
			Instant agePlusTenMinutes = age.plus(HEURISTIC_FAILURE_BACKOFF_DURATION);
			if (TimeProvider.now().isBefore(agePlusTenMinutes)) {
				count++;

				if (isStuck()) {
					log.warn("marking uid as stuck, count has reached threshold [{}] within [{}] seconds", () -> count, HEURISTIC_FAILURE_BACKOFF_DURATION::toSeconds);
				}
			} else {
				count = 1;
			}
		}

		/**
		 * When it's been 'seen' more than 3 times, assume the transaction is stuck and will never be committed/rolled back automatically.
		 */
		public boolean isStuck() {
			return count >= HEURISTIC_FAILURE_ATTEMPTS;
		}
	}

	public HeuristicDetectingRecoveryModule() {
		this(StoreManager.getRecoveryStore(), new TransactionStatusConnectionManager());
	}

	// For tests only!
	protected HeuristicDetectingRecoveryModule(RecoveryStore recoveryStore, TransactionStatusConnectionManager statusManager) {
		this.transactionType = new AtomicAction().type();
		this.recoveryStore = recoveryStore;
		this.statusManager = statusManager;

		AppConstants appConstants = AppConstants.getInstance();
		int defaultTxTimeout = appConstants.getInt("transactionmanager.defaultTransactionTimeout", 180);
		String heuristicFailuresBackoff = appConstants.getString("transactionmanager.narayana.heuristicFailuresBackoffDuration", null);
		HEURISTIC_FAILURE_ATTEMPTS = appConstants.getInt("transactionmanager.narayana.heuristicFailuresAttempts", 3);
		HEURISTIC_FAILURE_BACKOFF_DURATION = calculateHeuristicFailuresBackoffDuration(HEURISTIC_FAILURE_ATTEMPTS, heuristicFailuresBackoff, defaultTxTimeout);
	}

	protected static Duration calculateHeuristicFailuresBackoffDuration(int heuristicFailuresAttempts, String heuristicFailuresBackoff, int defaultTxTimeout) {
		final Duration providedDuration;
		if (StringUtils.isNotBlank(heuristicFailuresBackoff)) {
			providedDuration = Duration.ofMinutes(Integer.parseInt(heuristicFailuresBackoff));
		} else {
			providedDuration = Duration.ZERO;
		}

		Duration minimumDuration = Duration.ofSeconds( (defaultTxTimeout * heuristicFailuresAttempts) + 10L); // Add 10 seconds

		// If the providedDuration is `0` or lower than the minimum expected duration, return minimumDuration.
		if (providedDuration.compareTo(minimumDuration) < 0) {
			log.info("minimum heuristicFailuresBackoffDuration requirements not met, defaulting to [{}]", minimumDuration::toSeconds);
			return minimumDuration;
		}

		log.info("setting heuristicFailuresBackoffDuration to [{}]", providedDuration::toSeconds);
		return providedDuration;
	}

	@Override
	public void periodicWorkFirstPass() {
		try {
			log.debug("HeuristicDetectingRecoveryModule first pass");

			List<Uid> transactionUids = getUids();
			for (Uid uid: transactionUids) {
				heuristicTransactionUids.computeIfAbsent(uid, UidCacheItem::new).hit();
			}
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
		int theStatus = statusManager.getTransactionStatus(transactionType, uid);
		return theStatus == ActionStatus.H_HAZARD;
	}

	/**
	 * Calls the 'test' method doForget, which afaik is the only way to trigger `#forgetHeuristics` without wrapping the transactions (like JMX does).
	 */
	@SuppressWarnings("deprecation")
	private void doForgetTransaction(Uid uid) {
		try {
			SubordinateAtomicAction atomicAction = new SubordinateAtomicAction(uid);
			atomicAction.doForget();
			log.info("marked heuristic uid [{}] as 'completed'", uid);
		} catch (Exception ex) {
			log.warn("unable to mark heuristic uid [{}] as 'completed'", uid, ex);
		}
	}

	// Protected for Tests!
	/**
	 * Compiles a list of all Uids that are 'stuck'.
	 */
	protected List<Uid> getStuckUids() {
		return heuristicTransactionUids.entrySet().stream()
				.filter(entry -> entry.getValue().isStuck())
				.map(Entry::getKey)
				.toList();
	}


	@Override
	public void periodicWorkSecondPass() {
		for (Uid uid : getStuckUids()) {
			doForgetTransaction(uid);
			heuristicTransactionUids.remove(uid);
		}
	}

}
