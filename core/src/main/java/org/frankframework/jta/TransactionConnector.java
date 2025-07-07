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

import org.apache.logging.log4j.Logger;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;

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
 * @param <T> the transaction
 * @param <R> a holder of suspended resources
 */
public class TransactionConnector<T,R> implements AutoCloseable {
	protected Logger log = LogUtil.getLogger(this);

	private final Object owner;
	private final String description;
	private final TransactionConnectorCoordinator<T, R> coordinator;
	private final Thread parentThread;
	private Thread childThread;

	private boolean childThreadTransactionSuspended;

	private TransactionConnector(TransactionConnectorCoordinator<T,R> coordinator, Object owner, String description) {
		super();
		parentThread=Thread.currentThread();
		this.coordinator = coordinator;
		this.owner = owner;
		this.description = description;
		if (log.isDebugEnabled()) log.debug("created {}", this);
	}

	/**
	 * factory method, to be called from 'main' thread.
	 *
	 * When a transaction connector has been set up, new transactional resources can only be introduced after beginChildThread() has been called,
	 * not on the main thread anymore, because the transaction must be suspended there.
	 * TODO: This also means that objects further downstream might need to restore the transaction context before they can add new transactional resources.
	 * This is currently not implemented; therefore a FixedQuerySender providing an UpdateClob or UpdateBlob outputstream might behave incorrectly.
	 */
	public static <T,R> TransactionConnector<T,R> getInstance(IThreadConnectableTransactionManager<T,R> txManager, Object owner, String description) {
		if (txManager==null) {
			return null;
		}
		TransactionConnectorCoordinator<T,R> coordinator = TransactionConnectorCoordinator.getInstance(txManager);
		if (coordinator == null) {
			return null;
		}
		TransactionConnector<T,R> instance = new TransactionConnector<>(coordinator, owner, description);
		coordinator.registerConnector();
		return instance;
	}

	/**
	 * resume transaction, that was saved in parent thread, in the child thread.
	 * After beginChildThread() has been called, new transactional resources cannot be enlisted in the parentThread,
	 * because the transaction context has been prepared to be transferred to the childThread.
	 */
	public void beginChildThread() {
		if (coordinator!=null) {
			log.debug("[{}] beginChildThread() resuming transaction in child thread", this);
			coordinator.resumeTransactionInChildThread(this);
			childThread = Thread.currentThread();
		} else {
			log.debug("[{}] beginChildThread() no transaction to resume", this);
		}
	}

	/**
	 * endThread() to be called from child thread in a finally clause
	 */
	@lombok.SneakyThrows
	public void endChildThread() {
		if (childThread==null || coordinator==null) {
			log.debug("[{}] endChildThread() in thread [{}], no childThread started or no transaction or not the last in chain", this::toString, ()->Thread.currentThread().getName());
			return;
		}
		Thread currentThread = Thread.currentThread();
		if (currentThread != childThread) {
			throw new IllegalStateException("["+this+"] endChildThread() must be called from childThread ["+childThread.getName()+"]");
		}
		log.debug("[{}] endChildThread() called in thread [{}]", this::toString, ()->Thread.currentThread().getName());
		coordinator.suspendTransaction();
		childThreadTransactionSuspended=true;
	}

	/**
	 * close() to be called from parent thread, when child thread has ended.
	 */
	@Override
	public void close() {
		Thread currentThread = Thread.currentThread();
		if (currentThread != parentThread) {
			throw new IllegalStateException("["+this+"] close() must be called from parentThread");
		}
		if (childThread!=null && !childThreadTransactionSuspended) {
			log.warn("childThread transaction was not suspended");
		}
		if (coordinator!=null) {
			log.debug("[{}] closing coordinator in thread [{}] after child thread [{}] ended", this::toString, parentThread::getName, ()->childThread==null?null:childThread.getName());
			coordinator.close();
		}
	}

	@Override
	public String toString() {
		return "C-"+Integer.toHexString(hashCode())+" of "+ClassUtils.nameOf(owner)+(description!=null?"/"+description:"");
	}
}
