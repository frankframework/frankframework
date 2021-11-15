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
	private T transaction;
	private R resources;

	/**
	 * Constructor, to be called from 'main' thread.
	 */
	public TransactionConnector(IThreadConnectableTransactionManager txManager) {
		super();
		parentThread=Thread.currentThread();
		if (txManager==null) {
			throw new IllegalStateException("txManager is null");
		}
		this.txManager = (IThreadConnectableTransactionManager)txManager;
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			transaction = this.txManager.getCurrentTransaction();
			resources = this.txManager.suspendTransaction(transaction);
		}
	}

	// close() to be called from parent thread, when child thread has ended
	@Override
	public void close() {
		Thread currentThread = Thread.currentThread();
		if (currentThread != parentThread) {
			throw new IllegalStateException("close() must be called from parentThread");
		}
		if (transaction!=null) {
			txManager.resumeTransaction(transaction, resources);
		}
	}
	
	// resume transaction, that was saved in parent thread, in the child thread.
	public void beginChildTread() {
		if (transaction!=null) {
			txManager.resumeTransaction(transaction, resources);
		}
	}
	

	// endThread() to be called from child thread in a finally clause
	public void endChildThread() {
//		if (transaction!=null) {
//			resources = this.txManager.suspendTransaction(transaction);
//		}
	}

}
