package org.frankframework.jta.xa;

import java.util.concurrent.Phaser;

import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class XaDatasourceCommitStopper {

	private static XaDatasourceCommitStopper instance;

	private boolean stop;
	private Phaser commitGuard;

	public static synchronized XaDatasourceCommitStopper createInstance() {
		if (instance != null)
		{
			throw new  IllegalStateException("Already created");
		}
		log.info("Creating new instance");
		instance = new XaDatasourceCommitStopper();
		return instance;
	}

	public static void destroyInstance() {
		if (instance != null) {
			log.info("Destroying instance");
			instance.unblockPendingCommits();
			instance = null;
			log.info("Instance destroyed");
		}
	}

	public static XADataSource augmentXADataSource(XADataSource dataSource) {
		if (instance == null) {
			log.trace("No instance, returning datasource unchanged");
			return dataSource;
		}
		return instance.wrapXADataSource(dataSource);
	}

	private XADataSource wrapXADataSource(XADataSource dataSource) {
		log.info("Wrap XADataSource");
		return new XaDatasourceObserver(dataSource, c -> new XaConnectionObserver(c, XaCommitStoppingWrapper::new));
	}

	public int getNumberOfParticipants() {
		if (commitGuard == null) {
			return 0;
		}
		return commitGuard.getRegisteredParties();
	}

	public void blockCommits() {
		this.stop = true;
		this.commitGuard = new Phaser(1);
		log.info("Blocking commits");
	}

	public int proceed() {
		if (commitGuard == null) {
			return 0;
		}
		log.info("Proceeding");
		return commitGuard.arriveAndAwaitAdvance();
	}

	public void allowNewCommits() {
		this.stop = false;
	}

	public void unblockPendingCommits() {
		if (this.commitGuard == null) {
			return;
		}

		this.stop = false;

		// Signal all waiting that they can continue
		log.info("Cleaning up pending commits that are on hold -- Signalling other participants they can proceed to do commit");
		int cs1 = commitGuard.arriveAndAwaitAdvance();
		log.info (" *> Phaser at CS1: {}", cs1);
		log.info("  > Waiting for other participants to have finished their commits");
		// Wait for all commits to have completed before continuing
		int cs2 = commitGuard.arriveAndAwaitAdvance();
		log.info(" *> Phaser at CS2: {}", cs2);
		log.info("<*> All pending transactions should now be completed");

		this.commitGuard = null;
	}

	public void register() {
		if (commitGuard == null) {
			return;
		}
		log.info("Register commit-stopper participant");
		commitGuard.register();
	}

	class XaCommitStoppingWrapper extends XaResourceObserver {

		public XaCommitStoppingWrapper(XAResource target) {
			super(target);
			log.trace("XaCommitStoppingWrapper created");
		}


		@Override
		public void commit(Xid xid, boolean onePhase) throws XAException {
			// Local shadow of the "stop" flag in case it is changed while we're in the call
			boolean inStoppingMode = stop;
			Phaser commitGuard = XaDatasourceCommitStopper.this.commitGuard;
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
				log.info ("Committing");
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
}
