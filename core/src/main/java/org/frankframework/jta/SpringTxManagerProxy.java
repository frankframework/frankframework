/*
   Copyright 2013 Nationale-Nederlanden, 2021-2022 WeAreFrank!

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

import jakarta.annotation.Nonnull;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.util.LogUtil;

/**
 * proxy class for transaction manager.
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class SpringTxManagerProxy implements IThreadConnectableTransactionManager, InitializingBean {
	private static final Logger log = LogUtil.getLogger(SpringTxManagerProxy.class);

	private IThreadConnectableTransactionManager threadConnectableProxy;
	private @Getter @Setter PlatformTransactionManager realTxManager;

	/**
	 * @param txOption e.q. TransactionDefinition.PROPAGATION_REQUIRES_NEW
	 * @param timeout Set the timeout to apply in seconds. Default timeout is {@value org.springframework.transaction.TransactionDefinition#TIMEOUT_DEFAULT}.
	 * @return IbisTransaction
	 */
	public static TransactionDefinition getTransactionDefinition(int txOption, int timeout) {
		DefaultTransactionDefinition result=new DefaultTransactionDefinition(txOption);
		if (timeout > 0) {
			result.setTimeout(timeout);
		}
		return result;
	}

	@Override
	@Nonnull
	public TransactionStatus getTransaction(TransactionDefinition txDef) throws TransactionException {
		log.trace("getting transaction definition [{}]", txDef);
		return getRealTxManager().getTransaction(txDef);
	}

	@Override
	public void commit(TransactionStatus txStatus) throws TransactionException {
		if (log.isTraceEnabled()) {
			if (txStatus.isRollbackOnly())
				log.trace("<TX> Executing rollback from tx.commit. TransactionStatus: [{}], Stacktrace:", txStatus, new Exception("<TX> Rollback from commit"));
			else
				log.trace("committing transaction [{}]", txStatus);
		}
		getRealTxManager().commit(txStatus);
	}

	@Override
	public void rollback(@Nonnull TransactionStatus txStatus) throws TransactionException {
		log.trace("rolling back transaction [{}]", txStatus);
		getRealTxManager().rollback(txStatus);
	}

	public IThreadConnectableTransactionManager getThreadConnectableProxy() {
		if (threadConnectableProxy==null) {
			PlatformTransactionManager realTxManager = getRealTxManager();
			if (realTxManager instanceof IThreadConnectableTransactionManager manager) {
				threadConnectableProxy = manager;
			} else if (realTxManager instanceof JtaTransactionManager manager) {
				threadConnectableProxy = new ThreadConnectableJtaTransactionManager(manager);
			} else {
				throw new IllegalStateException("Don't know how to make ["+realTxManager.getClass().getTypeName()+"] thread connectable");
			}
		}
		return threadConnectableProxy;
	}

	@Override
	public Object getCurrentTransaction() throws TransactionException {
		return getThreadConnectableProxy().getCurrentTransaction();
	}

	@Override
	public Object suspendTransaction(Object transaction) {
		return getThreadConnectableProxy().suspendTransaction(transaction);
	}

	@Override
	public void resumeTransaction(Object transaction, Object resources) {
		getThreadConnectableProxy().resumeTransaction(transaction, resources);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(realTxManager == null) {
			throw new IllegalStateException("RealTxManager not set");
		}
	}
}
