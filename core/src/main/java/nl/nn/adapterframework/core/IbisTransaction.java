/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.core;

import javax.transaction.TransactionManager;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.SpringTxManagerProxy;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Class which generates extra logging when starting and committing transactions.
 *
 * @author  Peter Leeuwenburgh
 */

public class IbisTransaction {
	protected Logger log = LogUtil.getLogger(this);

	private PlatformTransactionManager txManager;
	private TransactionStatus txStatus;
	private TransactionDefinition txDef;
	private String object;

	private boolean txClientIsActive;
	private String txClientName;

	private boolean txIsActive;
	private String txName;
	private boolean txIsNew;

	public IbisTransaction(PlatformTransactionManager txManager, TransactionDefinition txDef, String object) {
		this.txManager = txManager;
		this.txDef = txDef;
		this.object = object;

		txClientIsActive = TransactionSynchronizationManager.isActualTransactionActive();
		txClientName = TransactionSynchronizationManager.getCurrentTransactionName();

		txStatus = txManager.getTransaction(txDef);

		txIsActive = TransactionSynchronizationManager.isActualTransactionActive();
		txIsNew = txStatus.isNewTransaction();

		if (txIsNew) {
			txName = Misc.createSimpleUUID();
			TransactionSynchronizationManager.setCurrentTransactionName(txName);
			int txTimeout = txDef.getTimeout();
			log.debug("Transaction manager ["+getRealTransactionManager()+"] created a new transaction ["+txName+"] for " + object + " with timeout [" + (txTimeout<0?"system default(=120s)":""+txTimeout) + "]");
		} else {
			txName = TransactionSynchronizationManager.getCurrentTransactionName();
			if (txClientIsActive && !txIsActive) {
				log.debug("Transaction manager ["+getRealTransactionManager()+"] suspended the transaction [" + txClientName + "] for " + object);
			}
		}
	}

	public TransactionStatus getStatus() {
		return txStatus;
	}

	private String getRealTransactionManager() {
		if (txManager == null) {
			return null;
		}
		if (txManager instanceof SpringTxManagerProxy) {
			SpringTxManagerProxy springTxMgr = (SpringTxManagerProxy) txManager;
			PlatformTransactionManager platformTxMgr = springTxMgr.getRealTxManager();
			if (platformTxMgr == null) {
				return springTxMgr.getClass().getName();
			}
			if (platformTxMgr instanceof JtaTransactionManager) {
				JtaTransactionManager jtaTxMgr = (JtaTransactionManager) platformTxMgr;
				TransactionManager txMgr = jtaTxMgr.getTransactionManager();
				if (txMgr == null) {
					return jtaTxMgr.getClass().getName();
				}
				return txMgr.getClass().getName();
			} else {
				return platformTxMgr.getClass().getName();
			}
		} else {
			return txManager.getClass().getName();
		}
	}

	public void commit() {
		if (txIsNew) {
			if (txStatus.isRollbackOnly()) {
				log.debug("Transaction ["+txName+"] marked for rollback, so transaction manager ["+getRealTransactionManager()+"] is rolling back the transaction for " + object);
			} else {
				log.debug("Transaction ["+txName+"] is not marked for rollback, so transaction manager ["+getRealTransactionManager()+"] is committing the transaction for " + object);
			}
		}
		txManager.commit(txStatus);
		if (!txIsNew && txClientIsActive && !txIsActive) {
			log.debug("Transaction manager ["+getRealTransactionManager()+"] resumed the transaction [" + txClientName + "] for " + object);
		}
	}
}
