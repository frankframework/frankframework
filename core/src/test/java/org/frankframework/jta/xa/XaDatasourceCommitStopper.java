package org.frankframework.jta.xa;

import java.util.concurrent.Phaser;

import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class XaDatasourceCommitStopper extends XaResourceObserver{

	private static boolean stop;
	public static volatile Phaser commitGuard;


	public XaDatasourceCommitStopper(XAResource target) {
		super(target);
	}

	public static synchronized void stop(boolean stop) {
		if (XaDatasourceCommitStopper.stop && !stop && commitGuard != null) {
			// Signal all waiting that they can continue
			log.info("Cleaning up pending commits that are on hold -- Signalling other participants they can proceed to do commit");
			int cs1 = commitGuard.arriveAndAwaitAdvance();
			log.info (" *> Phaser at CS1: {}", cs1);
			log.info("  > Waiting for other participants to have finished their commits");
			// Wait for all commits to have completed before continuing
			int cs2 = commitGuard.arriveAndAwaitAdvance();
			log.info(" *> Phaser at CS2: {}", cs2);
			log.info("<*> All pending transactions should now be completed");
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
				log.warn("commit() waiting 'endless' to perform commit to simulate unresponsive RM");
				// Arrive at this phaser to signal controlling test that it can proceed
				// Block our own commit
				log.info("Signalling other participants we have arrived in commit and wait for them");
				int cmt1 = commitGuard.arriveAndAwaitAdvance();
				log.info("<*> Phaser at CMT1: {}, waiting until we may commit", cmt1);

				log.info("Wait until we can proceed to complete our commit");
				// Second "arrive" and await on the guard will block until the "stop" method will arrive and advance
				int cmt2 = commitGuard.awaitAdvanceInterruptibly(commitGuard.arrive());
				log.info("<*> Phaser at CMT2: {}, we can now commit", cmt2);
			} catch (InterruptedException e) {
				log.warn("commit() interrupted");
				throw new XAException(e.getMessage());
			}
		}
		try {
			super.commit(xid, onePhase);
		} finally {
			if (inStoppingMode) {
				// 2nd arrive() signals that we done the commit and that the "stop" method can advance to completion.
				log.info("Signalling other participants that we are finished with commit");
				int cmt3 = commitGuard.arriveAndDeregister();
				log.info("<*> Phaser at CMT3: {}, we have committed", cmt3);
			}
		}
	}
}
