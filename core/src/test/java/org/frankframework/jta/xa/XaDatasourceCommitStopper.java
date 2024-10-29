package org.frankframework.jta.xa;

import java.util.concurrent.Phaser;

import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.logging.log4j.Logger;

import org.frankframework.util.LogUtil;

public class XaDatasourceCommitStopper extends XaResourceObserver{
	protected static Logger LOG = LogUtil.getLogger(XaDatasourceCommitStopper.class);

	private static boolean stop;
	public static Phaser commitGuard;


	public XaDatasourceCommitStopper(XAResource target) {
		super(target);
	}

	public static void stop(boolean stop) {
		if (XaDatasourceCommitStopper.stop && !stop && commitGuard != null) {
			// Signal all waiting that they can continue
			LOG.info("Cleaning up pending commits that are on hold -- Signalling other participants they can proceed to do commit");
			int cs1 = commitGuard.arriveAndAwaitAdvance();
			LOG.info ("<*> Phase at CS1: {}", cs1);
			LOG.info("Waiting for other participants to have finished their commits");
			// Wait for all commits to have completed before continuing
			int cs2 = commitGuard.arriveAndAwaitAdvance();
			LOG.info("<*> Phase at CS2: {}", cs2);
			LOG.info("All pending transactions should now be completed");
		}
		XaDatasourceCommitStopper.stop = stop;
		if (stop) {
			// Create new phaser and register the "commit stopper" as party
			commitGuard = new Phaser(1);
		}
	}

	public static XADataSource augmentXADataSource(XADataSource dataSource) {
		return new XaDatasourceObserver(dataSource, c -> new XaConnectionObserver(c, XaDatasourceCommitStopper::new));
	}

	@Override
	public void commit(Xid xid, boolean onePhase) throws XAException {
		// Local shadow of the "stop" flag in case it is changed while we're in the call
		boolean inStoppingMode = stop;
		Phaser commitGuard = XaDatasourceCommitStopper.commitGuard;
		if (inStoppingMode) {
			try {
				LOG.warn("commit() waiting 'endless' to perform commit to simulate unresponsive RM");
				// Arrive at this phaser to signal controlling test that it can proceed
				// Block our own commit
				LOG.info("Signalling other participants we have arrived in commit and wait for them");
				int cmt1 = commitGuard.arriveAndAwaitAdvance();
				LOG.info("<*> Phase at CMT1: {}", cmt1);

				LOG.info("Wait until we can proceed to complete our commit");
				// Second "arrive" and await on the guard will block until the "stop" method will arrive and advance
				int cmt2 = commitGuard.awaitAdvanceInterruptibly(commitGuard.arrive());
				LOG.info("<*> Phase at CMT2: {}", cmt2);
			} catch (InterruptedException e) {
				throw new XAException(e.getMessage());
			}
		}
		try {
			super.commit(xid, onePhase);
		} finally {
			if (inStoppingMode) {
				// 2nd arrive() signals that we done the commit and that the "stop" method can advance to completion.
				LOG.info("Signalling other participants that we are finished with commit");
				int cmt3 = commitGuard.arriveAndDeregister();
				LOG.info("<*> Phase at CMT3: {}", cmt3);
			}
		}
	}
}
