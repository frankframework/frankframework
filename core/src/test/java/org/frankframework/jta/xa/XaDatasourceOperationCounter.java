package org.frankframework.jta.xa;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class XaDatasourceOperationCounter implements XaResourceObserverFactory {

	private final AtomicInteger commitCounter = new AtomicInteger(0);
	private final AtomicInteger startCounter = new AtomicInteger(0);
	private final AtomicInteger endCounter = new AtomicInteger(0);
	private final AtomicInteger forgetCounter = new AtomicInteger(0);
	private final AtomicInteger recoverCounter = new AtomicInteger(0);
	private final AtomicInteger prepareCounter = new AtomicInteger(0);
	private final AtomicInteger sameRMCounter = new AtomicInteger(0);
	private final AtomicInteger rollbackCounter = new AtomicInteger(0);

	private Phaser stage = null;

	private Set<Thread> participants = new HashSet<>();

	public void registerParticipant(Thread participant) {
		stage.register();
		participants.add(participant);
	}

	public void blockNewCommitsAndRollbacks() {
		stage = new Phaser(1);
	}

	public void awaitActions() {
		Phaser txGuard = stage;
		if (txGuard != null) {
			txGuard.arriveAndAwaitAdvance();
		}
	}

	public void allowNewOperations() {
		Phaser txGuard = stage;
		if (txGuard != null) {
			txGuard.arrive();
		}
	}

	public synchronized void awaitOperationsDone() {
		if (stage != null) {
			stage.arriveAndAwaitAdvance();
			stage = null;
		}
	}

	public int getCommitCount() {
		return commitCounter.get();
	}
	public int getStartCount() {
		return startCounter.get();
	}
	public int getEndCount() {
		return endCounter.get();
	}
	public int getForgetCount() {
		return forgetCounter.get();
	}
	public int getRecoverCount() {
		return recoverCounter.get();
	}
	public int getPrepareCount() {
		return prepareCounter.get();
	}
	public int getSameRMCount() {
		return sameRMCounter.get();
	}
	public int getRollbackCount() {
		return rollbackCounter.get();
	}

	@Override
	public XADataSource augmentXADataSource(XADataSource dataSource) {
		log.info("Wrap XADataSource");
		return new XaDatasourceObserver(dataSource, c -> new XaConnectionObserver(c, XaOperationCountingWrapper::new));
	}

	@Override
	public void destroy() {
		allowNewOperations();
		if (stage != null) {
			stage.arriveAndDeregister();
		}
		stage = null;
	}

	class XaOperationCountingWrapper extends XaResourceObserver {

		public XaOperationCountingWrapper(XAResource target) {
			super(target);
			log.trace("XaOperationCountingWrapper created");
		}

		@Override
		public void commit(Xid xid, boolean onePhase) throws XAException {
			commitCounter.incrementAndGet();
			checkForTxBlock();
			try {
				super.commit(xid, onePhase);
			} finally {
				markOperationDone();
			}
		}

		@Override
		public void rollback(Xid xid) throws XAException {
			rollbackCounter.incrementAndGet();
			checkForTxBlock();
			try {
				super.rollback(xid);
			} finally {
				markOperationDone();
			}
		}

		private void checkForTxBlock() {
			Phaser txGuard = stage;
			if (txGuard != null && participants.contains(Thread.currentThread())) {
				// First all parties synchronize on transactions being active
				txGuard.arriveAndAwaitAdvance();
				// Now synchronize on the signal to proceed with the commit
				txGuard.arriveAndAwaitAdvance();
			}
		}

		private void markOperationDone() {
			Phaser txGuard = stage;
			if (txGuard != null && participants.contains(Thread.currentThread())) {
				txGuard.arriveAndDeregister();
				participants.remove(Thread.currentThread());
			}
		}

		@Override
		public void end(Xid xid, int flags) throws XAException {
			endCounter.incrementAndGet();
			super.end(xid, flags);
		}

		@Override
		public void forget(Xid xid) throws XAException {
			forgetCounter.incrementAndGet();
			super.forget(xid);
		}

		@Override
		public boolean isSameRM(XAResource xares) throws XAException {
			sameRMCounter.incrementAndGet();
			return super.isSameRM(xares);
		}

		@Override
		public int prepare(Xid xid) throws XAException {
			prepareCounter.incrementAndGet();
			return super.prepare(xid);
		}

		@Override
		public void start(Xid xid, int flags) throws XAException {
			startCounter.incrementAndGet();
			super.start(xid, flags);
		}

		@Override
		public Xid[] recover(int flag) throws XAException {
			recoverCounter.incrementAndGet();
			return super.recover(flag);
		}
	}
}
