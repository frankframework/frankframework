/*
   Copyright 2013 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package org.frankframework.core;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.transaction.TransactionManager;

import org.apache.logging.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.util.LogUtil;
import org.frankframework.util.UUIDUtil;

/**
 * Class which generates extra logging when starting and committing transactions.
 *
 * @author  Peter Leeuwenburgh
 */

public class IbisTransaction {
	protected final Logger log = LogUtil.getLogger(this);

	@Nonnull
	private final PlatformTransactionManager txManager;
	@Nonnull
	private final TransactionStatus txStatus;
	@Nonnull
	private final String object;

	private final boolean txClientIsActive;
	@Nullable
	private final String txClientName;

	private final boolean txIsActive;
	@Nullable
	private final String txName;
	private final boolean txIsNew;

	public IbisTransaction(@Nonnull PlatformTransactionManager txManager, @Nonnull TransactionDefinition txDef, @Nonnull String descriptionOfOwner) {
		this.txManager = txManager;
		this.object = descriptionOfOwner;

		txClientIsActive = TransactionSynchronizationManager.isActualTransactionActive();
		txClientName = TransactionSynchronizationManager.getCurrentTransactionName();

		txStatus = txManager.getTransaction(txDef);

		txIsActive = TransactionSynchronizationManager.isActualTransactionActive();
		txIsNew = txStatus.isNewTransaction();

		if (txIsNew) {
			txName = UUIDUtil.createSimpleUUID();
			TransactionSynchronizationManager.setCurrentTransactionName(txName);
			int txTimeout = txDef.getTimeout();
			log.debug("Transaction manager [{}] created a new transaction [{}] for {} with timeout [{}]", this::getRealTransactionManagerName, ()->txName, ()->descriptionOfOwner, ()->(txTimeout<0 ? "system default(=120s)" : ""+txTimeout));
		} else {
			txName = TransactionSynchronizationManager.getCurrentTransactionName();
			if (txClientIsActive && !txIsActive) {
				log.debug("Transaction manager [{}] suspended the transaction [{}] for {}", this::getRealTransactionManagerName, ()->txClientName, ()->descriptionOfOwner);
			}
		}
	}

	@Nonnull
	private String getRealTransactionManagerName() {
		if (txManager instanceof SpringTxManagerProxy springTxMgr) {
			PlatformTransactionManager platformTxMgr = springTxMgr.getRealTxManager();
			if (platformTxMgr == null) {
				return springTxMgr.getClass().getName();
			}
			if (platformTxMgr instanceof JtaTransactionManager jtaTxMgr) {
				TransactionManager txMgr = jtaTxMgr.getTransactionManager();
				if (txMgr == null) {
					return jtaTxMgr.getClass().getName();
				}
				return txMgr.getClass().getName();
			}
			return platformTxMgr.getClass().getName();
		}
		return txManager.getClass().getName();
	}

	public static boolean isDistributedTransactionsSupported(PlatformTransactionManager txManager) {
		if(txManager instanceof SpringTxManagerProxy proxy) {
			return isDistributedTransactionsSupported(proxy.getRealTxManager());
		}
		return txManager instanceof JtaTransactionManager;
	}

	public void setRollbackOnly() {
		txStatus.setRollbackOnly();
	}

	public boolean isRollbackOnly() {
		return txStatus.isRollbackOnly();
	}

	public boolean isCompleted() {
		return txStatus.isCompleted();
	}

	/**
	 * Complete this transaction by either committing it or rolling it back, depending on the
	 * transaction status.
	 * <p>
	 * In case a rollback is performed, a successful rollback will not raise an exception.
	 */
	public void complete() {
		boolean mustRollback = isRollbackOnly();
		if (txIsNew) {
			if (mustRollback) {
				log.debug("Transaction [{}] marked for rollback, so transaction manager [{}] is rolling back the transaction for {}", ()->txName, this::getRealTransactionManagerName, ()->object);
			} else {
				log.debug("Transaction [{}] is not marked for rollback, so transaction manager [{}] is committing the transaction for {}", ()->txName, this::getRealTransactionManagerName, ()->object);
			}
		}
		if (mustRollback) {
			txManager.rollback(txStatus);
		} else {
			txManager.commit(txStatus);
		}
		if (!txIsNew && txClientIsActive && !txIsActive) {
			log.debug("Transaction manager [{}] resumed the transaction [{}] for {}", this::getRealTransactionManagerName, ()->txClientName, ()->object);
		}
	}
}
