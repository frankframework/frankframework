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
package org.frankframework.jta;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.frankframework.functional.ThrowingRunnable;
import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.util.LogUtil;

public class TransactionConnectorCoordinator<T,R> implements AutoCloseable {
	protected static Logger log = LogUtil.getLogger(TransactionConnectorCoordinator.class);

	private final IThreadConnectableTransactionManager<T, R> txManager;
	private final Thread parentThread;

	private static final ThreadLocal<TransactionConnectorCoordinator<?, ?>> coordinators = new ThreadLocal<>();

	private T transaction;
	private R resourceHolder;
	private boolean suspended;
	private TransactionConnector<T,R> lastInThread;
	private int connectorCount=0;
	private int numBeginChildThreadsCalled=0;
	private List<ThrowingRunnable<?>> onEndChildThreadActions;


	private TransactionConnectorCoordinator(IThreadConnectableTransactionManager<T,R> txManager) {
		super();
		parentThread=Thread.currentThread();
		if (txManager==null) {
			throw new IllegalStateException("txManager is null");
		}
		this.txManager = txManager;
		transaction = this.txManager.getCurrentTransaction();
		suspendTransaction();
	}

	public static <T,R> TransactionConnectorCoordinator<T,R> getInstance(IThreadConnectableTransactionManager<T,R> txManager) {
		if (txManager==null) {
			throw new IllegalStateException("txManager is null");
		}
		TransactionConnectorCoordinator<T,R> coordinator = (TransactionConnectorCoordinator<T,R>)coordinators.get();
		if (coordinator==null) {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				log.debug("no active transaction in thread [{}]", ()->Thread.currentThread().getName());
				return null;
			}
			coordinator = new TransactionConnectorCoordinator<>(txManager);
			coordinators.set(coordinator);
		}
		return coordinator;
	}

	public void registerConnector(TransactionConnector connector) {
		connectorCount++;
	}

	/**
	 * Execute an action with the thread prepared for enlisting transactional resources.
	 * To be called for obtaining transactional resources (like JDBC connections) if a TransactionConnector might already have been created on the thread.
	 */
	public static <T, R, E extends Exception> T doInUnsuspendedTransationContext(ThrowingSupplier<T, E> action) throws E {
		TransactionConnectorCoordinator<T,R> coordinator = (TransactionConnectorCoordinator<T,R>)coordinators.get();
		if (coordinator!=null && coordinator.suspended) {
			try {
				coordinator.resumeTransaction();
				log.debug("executing action in resumed context");
				return action.get();
			} finally {
				log.debug("resuspending context after executing action");
				coordinator.suspendTransaction();
			}
		}
		return action.get();
	}

	/**
	 * Execute an action when the thread ends, if it is guarded by a TransactionConnector
	 */
	public static <T, R, E extends Exception> boolean onEndChildThread(ThrowingRunnable<E> action) {
		TransactionConnectorCoordinator<T,R> coordinator = (TransactionConnectorCoordinator<T,R>)coordinators.get();
		if (coordinator!=null) {
			if (coordinator.onEndChildThreadActions==null) {
				coordinator.onEndChildThreadActions = new ArrayList<>();
			}
			coordinator.onEndChildThreadActions.add(action);
			return true;
		}
		return false;
	}


	public void resumeTransactionInChildThread(TransactionConnector<T,R> requester) {
		numBeginChildThreadsCalled++;
		if (numBeginChildThreadsCalled==connectorCount) {
			lastInThread = requester;
			log.debug("resumeTransactionInChildThread() requester [{}] is last in thread, so resuming transaction", requester);
			if (onEndChildThreadActions!=null) {
				lastInThread.onEndChildThread(onEndChildThreadActions);
			}
			resumeTransaction();
		} else {
			log.debug("resumeTransactionInChildThread() requester [{}] is not last in thread, not resuming transaction", requester);
		}
	}

	public void suspendTransaction() {
		if (!suspended) {
			log.debug("suspending transaction of parent thread [{}], current thread [{}]", ()->parentThread.getName(), ()->Thread.currentThread().getName());
			resourceHolder = this.txManager.suspendTransaction(transaction);
			suspended = true;
		} else {
			log.debug("transaction of parent thread [{}] was already suspended, current thread [{}]", ()->parentThread.getName(), ()->Thread.currentThread().getName());
		}
	}

	public void resumeTransaction() {
		if (suspended) {
			log.debug("resumeTransaction() resuming transaction of parent thread [{}], current thread [{}]", ()->parentThread.getName(), ()->Thread.currentThread().getName());
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				txManager.resumeTransaction(transaction, resourceHolder);
			}
			suspended = false;
		} else {
			log.warn("resumeTransaction() transaction of parent thread [{}] was already resumed, current thread [{}]", ()->parentThread.getName(), ()->Thread.currentThread().getName());
		}
	}

	@Override
	public void close() {
		log.debug("close() numBeginChildThreadsCalled [{}] connectorCount [{}]", numBeginChildThreadsCalled, connectorCount);
		Thread currentThread = Thread.currentThread();
		if (currentThread != parentThread) {
			throw new IllegalStateException("close() must be called from parentThread");
		}
		if (transaction!=null) {
			log.debug("close() resuming transaction in thread [{}] after child thread ended", ()->parentThread.getName());
			txManager.resumeTransaction(transaction, resourceHolder);
			transaction=null;
			coordinators.remove();
		} else {
			log.debug("close() already called");
		}
	}
}
