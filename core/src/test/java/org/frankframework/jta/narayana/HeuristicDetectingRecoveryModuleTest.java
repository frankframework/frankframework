package org.frankframework.jta.narayana;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.coordinator.ActionStatus;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.objectstore.RecoveryStore;
import com.arjuna.ats.arjuna.recovery.TransactionStatusConnectionManager;
import com.arjuna.ats.arjuna.state.OutputObjectState;
import com.arjuna.ats.internal.arjuna.objectstore.VolatileStore;

import lombok.extern.log4j.Log4j2;

import org.frankframework.testutil.TestAppender;

@Log4j2
public class HeuristicDetectingRecoveryModuleTest {

	private static final String STATE_TYPE = "/StateManager/BasicAction/TwoPhaseCoordinator/AtomicAction";

	private void addToRecoveryStore(RecoveryStore store, Uid uid) throws ObjectStoreException {
		OutputObjectState buff = new OutputObjectState(uid, STATE_TYPE);
		store.write_committed(uid, STATE_TYPE, buff);
	}

	@ParameterizedTest
	@NullAndEmptySource
	public void testEmptyBackoffDuration(String heuristicFailuresBackoff) {
		Duration backoffDuration = HeuristicDetectingRecoveryModule.calculateHeuristicFailuresBackoffDuration(2, heuristicFailuresBackoff, 180);
		assertEquals(370L, backoffDuration.getSeconds());
	}

	@ParameterizedTest
	@CsvSource({"10,910", "15,910", "16,960", "25,1500", "1000,60000"})
	public void testBackoffDuration(String heuristicFailuresBackoff, String expected) {
		Duration backoffDuration = HeuristicDetectingRecoveryModule.calculateHeuristicFailuresBackoffDuration(5, heuristicFailuresBackoff, 180);
		assertEquals(Long.parseLong(expected), backoffDuration.getSeconds());
	}

	@Test
	public void newTxNonHeuristic() throws Exception {
		ObjectStoreEnvironmentBean environmentBean = spy(ObjectStoreEnvironmentBean.class);
		doReturn(true).when(environmentBean).isVolatileStoreSupportAllObjUids();
		RecoveryStore store = spy(new VolatileStore(environmentBean));

		addToRecoveryStore(store, new Uid());

		TransactionStatusConnectionManager statusManager = mock(TransactionStatusConnectionManager.class);
		HeuristicDetectingRecoveryModule recoveryModule = new HeuristicDetectingRecoveryModule(store, statusManager);

		try (TestAppender appender = TestAppender.newBuilder().build()) {
			recoveryModule.periodicWorkFirstPass();

			List<String> logEvents = appender.getLogLines();
			assertEquals(2, logEvents.size(), "found messages "+logEvents);
			assertTrue(logEvents.get(1).contains("skipping non heuristic transaction"));
			assertEquals(0, recoveryModule.getStuckUids().size());
		}
	}

	@Test
	public void multipleTxNonHeuristic() throws Exception {
		ObjectStoreEnvironmentBean environmentBean = spy(ObjectStoreEnvironmentBean.class);
		doReturn(true).when(environmentBean).isVolatileStoreSupportAllObjUids();
		RecoveryStore store = spy(new VolatileStore(environmentBean));

		addToRecoveryStore(store, new Uid());
		addToRecoveryStore(store, new Uid());
		addToRecoveryStore(store, new Uid());

		TransactionStatusConnectionManager statusManager = mock(TransactionStatusConnectionManager.class);
		HeuristicDetectingRecoveryModule recoveryModule = new HeuristicDetectingRecoveryModule(store, statusManager);

		try (TestAppender appender = TestAppender.newBuilder().build()) {
			for (int i = 0; i < 10; i++) {
				recoveryModule.periodicWorkFirstPass();
			}

			List<String> logEvents = appender.getLogLines();
			assertEquals(40, logEvents.size(), "found messages "+logEvents);
			assertTrue(logEvents.get(1).contains("skipping non heuristic transaction"));
			assertEquals(0, recoveryModule.getStuckUids().size());
		}
	}

	@Test
	public void logWithHeuristicTransaction() throws Exception {
		ObjectStoreEnvironmentBean environmentBean = spy(ObjectStoreEnvironmentBean.class);
		doReturn(true).when(environmentBean).isVolatileStoreSupportAllObjUids();
		RecoveryStore store = spy(new VolatileStore(environmentBean));

		addToRecoveryStore(store, new Uid()); // Normal TX
		Uid heuristicUid = new Uid();
		addToRecoveryStore(store, heuristicUid);

		TransactionStatusConnectionManager statusManager = mock(TransactionStatusConnectionManager.class);
		doAnswer(e -> ActionStatus.H_HAZARD).when(statusManager).getTransactionStatus(eq(STATE_TYPE), eq(heuristicUid));
		HeuristicDetectingRecoveryModule recoveryModule = new HeuristicDetectingRecoveryModule(store, statusManager);

		recoveryModule.periodicWorkFirstPass();
		recoveryModule.periodicWorkFirstPass();

		try (TestAppender appender = TestAppender.newBuilder().build()) {
			recoveryModule.periodicWorkFirstPass(); // 3rd attempt

			List<String> logEvents = appender.getLogLines();
			assertEquals(4, logEvents.size(), "found messages "+logEvents);
			assertTrue(appender.contains("skipping non heuristic transaction"));
			assertTrue(appender.contains("found heuristic transaction "+heuristicUid.stringForm().trim()));
		}

		// Out of two transactions, only one is heuristic
		assertEquals(1, recoveryModule.getStuckUids().size());
		log.debug("we've detected 1 heuristic transaction!");

		// Execute step 2, dealing with it...
		recoveryModule.periodicWorkSecondPass();

		// Transaction should be gone
		assertEquals(0, recoveryModule.getStuckUids().size());
	}

}
