/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.jta;

import org.apache.logging.log4j.Logger;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import nl.nn.adapterframework.util.LogUtil;

/**
 * This TransactionConnector suspends the transaction in the main thread,
 * and resumes it in the child thread, for as long as it runs. The calling party 
 * (e.g. the party that is using a streaming transformer, or is using a provided 
 * OutputStream that may lead to a ContentHandlerOutputStream or JsonEventHandlerOutputStream)
 * must make sure that no transaction related code (e.g. obtaining connections) is executed
 * during the life of the TransactionConnector (between construction and calling of close()).
 * 
 * @author Gerrit van Brakel
 *
 * @param <T>
 * @param <R>
 */
public class TransactionConnector<T,R> implements AutoCloseable {
	protected Logger log = LogUtil.getLogger(this);

	private IThreadConnectableTransactionManager<T,R> txManager;
	private Thread parentThread;
	private Thread childThread;
	private static ThreadLocal<Object> transactions=new ThreadLocal<>();
	private static ThreadLocal<Object> resourceHolders=new ThreadLocal<>();
	private T transaction;
	private R resourceHolder;
	private boolean childThreadTransactionSuspended;

	/**
	 * Constructor, to be called from 'main' thread.
	 * 
	 */
	// When a transaction connector has been set up, new transactional resources can only be introduced after beginChildThread() has been called,
	// not on the main thread anymore, because the transaction must be suspended there.
	// It cannot be resumed here, because then the state conflicts with the resume in close().
	public TransactionConnector(IThreadConnectableTransactionManager txManager) {
		super();
		parentThread=Thread.currentThread();
		if (txManager==null) {
			throw new IllegalStateException("txManager is null");
		}
		transaction = (T)transactions.get();
		resourceHolder = (R)resourceHolders.get();
		this.txManager = txManager;
		if (transaction==null && TransactionSynchronizationManager.isSynchronizationActive()) {
			log.debug("[{}] suspending transaction of parent thread [{}]", ()->hashCode(), ()->parentThread.getName());
			transaction = this.txManager.getCurrentTransaction();
			resourceHolder = this.txManager.suspendTransaction(transaction);
			transactions.set(transaction);
			resourceHolders.set(resourceHolder);
		} else {
			log.debug("[{}] no active transaction in parent thread [{}]", ()->hashCode(), ()->parentThread.getName());
		}
	}

	// close() to be called from parent thread, when child thread has ended
	@Override
	public void close() {
		Thread currentThread = Thread.currentThread();
		if (currentThread != parentThread) {
			throw new IllegalStateException("["+hashCode()+"] close() must be called from parentThread");
		}
		if (childThread!=null && !childThreadTransactionSuspended) {
			log.warn("childThread transaction was not suspended");
		}
		if (transaction!=null && transactions.get()!=null) {
			log.debug("[{}] close() resuming transaction in thread [{}] after child thread ended", ()->hashCode(), ()->parentThread.getName());
			txManager.resumeTransaction(transaction, resourceHolder);
			transaction=null;
			transactions.remove();
			resourceHolders.remove();
		} else {
			log.debug("[{}] close() already called, or parentThread does not need resume", ()->hashCode());
		}
	}

	// resume transaction, that was saved in parent thread, in the child thread.
	// After beginChildThread() has been called, new transactional resources cannot be enlisted in the parentThread, 
	// because the transaction context has been transferred to the childThread.
	public void beginChildThread() {
		if (transaction!=null) {
			childThread = Thread.currentThread();
			log.debug("[{}] beginChildThread() resuming transaction of parent thread [{}] in child thread [{}]", ()->hashCode(), ()->parentThread.getName(), ()->childThread.getName());
			txManager.resumeTransaction(transaction, resourceHolder);
		} else {
			log.debug("[{}] beginChildThread() no transaction to resume", ()->hashCode());
		}
	}

	// endThread() to be called from child thread in a finally clause
	public void endChildThread() {
		if (childThread==null || transaction==null) {
			log.debug("[{}] endChildThread() in thread [{}], no childThread started or no transaction", ()->hashCode(), ()->Thread.currentThread().getName());
			return;
		}
		Thread currentThread = Thread.currentThread();
		if (currentThread != childThread) {
			throw new IllegalStateException("["+hashCode()+"] endChildThread() must be called from childThread ["+childThread.getName()+"]");
		}
		log.debug("[{}] endChildThread() collecting current resources in thread [{}]", ()->hashCode(), ()->Thread.currentThread().getName());
		resourceHolder = this.txManager.suspendTransaction(transaction);
		childThreadTransactionSuspended = true;
	}

}
