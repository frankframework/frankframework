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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;

import lombok.Getter;
import nl.nn.adapterframework.util.LogUtil;

public class TransactionConnector implements AutoCloseable {
	protected Logger log = LogUtil.getLogger(this);

	private SpringTxManagerProxy txManager;
	private Object parentThreadTransaction;
	private Object parentResources;
	
	private Object childThreadTransaction;
	private Object childResources;

	private TransactionStatus parentTxStatus;
	private TransactionStatus childTxStatus;
	private boolean rolledBack;

	private @Getter TransactionDefinition txDef;

	public TransactionConnector(PlatformTransactionManager txManager) {
		super();
		this.txManager = (SpringTxManagerProxy)txManager;
		if (txManager==null) {
			throw new IllegalStateException("txManager is null");
		}
		txDef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW, 0);
		//parentTxStatus = txManager.getTransaction(txDef);
		parentThreadTransaction = this.txManager.getCurrentTransaction();
		//parentResources = this.txManager.getCurrentSynchronizedResources(parentThreadTransaction);
		parentResources = this.txManager.suspendTransaction(parentThreadTransaction);
	}

	
	public void applyTransactionInfo() {
		if (txManager!=null) {
			childTxStatus = txManager.getTransaction(txDef);
		}
		if (txManager!=null) {
			childThreadTransaction = this.txManager.getCurrentTransaction();
			childResources = txManager.suspendTransaction(childThreadTransaction);
			//txManager.joinParentThreadsTransaction(parentThreadTransaction, parentResources);
			txManager.resumeTransaction(parentThreadTransaction, parentResources);
		}
	}
	
	public void commit() {
		parentResources = this.txManager.suspendTransaction(parentThreadTransaction);
		txManager.resumeTransaction(childThreadTransaction, childResources);
		if (childTxStatus!=null) {
			try {
				log.debug("commit");
				txManager.commit(childTxStatus);
			} catch (UnexpectedRollbackException e) {
				rolledBack=true;
				throw e;
			}
		}
	}

	public void rollback() {
		parentResources = this.txManager.suspendTransaction(parentThreadTransaction);
		txManager.resumeTransaction(childThreadTransaction, childResources);
		if (childTxStatus!=null) {
			log.debug("rollback");
			rolledBack=true;
			txManager.rollback(childTxStatus);
		}
	}
	
	public boolean isRollbackOnly() {
		log.debug("isRollbackOnly [" + (childTxStatus!=null && childTxStatus.isRollbackOnly()) +"]");
		return childTxStatus!=null && childTxStatus.isRollbackOnly();
	}

	// close() to be called from parent thread
	@Override
	public void close() {
		txManager.resumeTransaction(parentThreadTransaction, parentResources);
		//txManager.joinParentThreadsTransaction(childThreadTransaction, childResources);
		if (parentTxStatus!=null) {
			if (rolledBack || isRollbackOnly()) {
				log.debug("close rolling back parent transaction");
				txManager.rollback(parentTxStatus);
			} else {
				log.debug("close commit parent transaction");
				txManager.commit(parentTxStatus);
			}
		}
	}
}
