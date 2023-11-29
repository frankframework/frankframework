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
package nl.nn.adapterframework.jta;

import javax.annotation.Nonnull;

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
import nl.nn.adapterframework.util.LogUtil;

/**
 * proxy class for transaction manager.
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class SpringTxManagerProxy implements IThreadConnectableTransactionManager<Object,Object>, InitializingBean {
	private static final Logger log = LogUtil.getLogger(SpringTxManagerProxy.class);

	private IThreadConnectableTransactionManager<Object,Object> threadConnectableProxy;
	private @Getter @Setter PlatformTransactionManager realTxManager;

	private final boolean trace = false;

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
		if (trace && log.isDebugEnabled()) log.debug("getting transaction definition [{}]", txDef);
		return getRealTxManager().getTransaction(txDef);
	}

	@Override
	public void commit(TransactionStatus txStatus) throws TransactionException {
		if (txStatus.isRollbackOnly()) {
			Exception e = new Exception("<TX> Warning - Silent rollback from commit");
			log.warn("<TX> Executing silent rollback without exception from tx.commit! TransactionStatus: [{}], Stacktrace:", txStatus, e);
			rollback(txStatus);
		} else {
			if (trace && log.isDebugEnabled()) log.debug("committing transaction [{}]", txStatus);
			getRealTxManager().commit(txStatus);
		}
	}

	@Override
	public void rollback(@Nonnull TransactionStatus txStatus) throws TransactionException {
		if (trace && log.isDebugEnabled()) log.debug("rolling back transaction [{}]", txStatus);
		getRealTxManager().rollback(txStatus);
	}

	public IThreadConnectableTransactionManager<Object,Object> getThreadConnectableProxy() {
		if (threadConnectableProxy==null) {
			PlatformTransactionManager realTxManager = getRealTxManager();
			if (realTxManager instanceof IThreadConnectableTransactionManager) {
				//noinspection rawtypes
				threadConnectableProxy = (IThreadConnectableTransactionManager)realTxManager;
			} else if (realTxManager instanceof JtaTransactionManager) {
				threadConnectableProxy = new ThreadConnectableJtaTransactionManager((JtaTransactionManager)realTxManager);
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
