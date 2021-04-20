/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import nl.nn.adapterframework.util.LogUtil;

/**
 * proxy class for transaction manager.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class SpringTxManagerProxy<T> implements PlatformTransactionManager, BeanFactoryAware, IThreadConnectingTransactionManager {
	private static final Logger log = LogUtil.getLogger(SpringTxManagerProxy.class);
	
	private @Setter BeanFactory beanFactory;
	private @Getter @Setter String realTxManagerBeanName;
	private PlatformTransactionManager realTxManager;
//	private ThreadConnectorHelperTransactionManagerProxyHandler proxyHandler;

	private boolean trace=false;

	/**
	 * @param txOption e.q. TransactionDefinition.PROPAGATION_REQUIRES_NEW
	 * @param timeout Set the timeout to apply in seconds. Default timeout is {@link org.springframework.transaction.TransactionDefinition#TIMEOUT_DEFAULT TIMEOUT_DEFAULT} (-1).
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
	public TransactionStatus getTransaction(TransactionDefinition txDef) throws TransactionException {
		if (trace && log.isDebugEnabled()) log.debug("getting transaction definition ["+txDef+"]");
		return getRealTxManager().getTransaction(txDef);
	}

	public Object getCurrentTransaction() throws TransactionException {
		if (getRealTxManager() instanceof IThreadConnectingTransactionManager) {
			System.out.println("IThreadConnectingTransactionManager.getCurrentTransaction" );
			return ((IThreadConnectingTransactionManager)getRealTxManager()).getCurrentTransaction();
		}
		return null;
//		try {
//			return getProxyHandler().doGetTransaction();
//		} catch (TransactionException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//			throw new TransactionSystemException("Cannot get current Transaction", e);
//		}
	}

	@Override
	public Object getCurrentSynchronizedResources(Object transaction) throws TransactionException {
		if (getRealTxManager() instanceof IThreadConnectingTransactionManager) {
			System.out.println("IThreadConnectingTransactionManager.getCurrentSynchronizedResources" );
			return ((IThreadConnectingTransactionManager)getRealTxManager()).getCurrentSynchronizedResources(transaction);
		}
		return null;
	}


	@Override
	public void joinParentThreadsTransaction(Object transaction, Object resources) throws TransactionException {
		if (getRealTxManager() instanceof IThreadConnectingTransactionManager) {
			System.out.println("IThreadConnectingTransactionManager.joinParentThreadsTransaction" );
			((IThreadConnectingTransactionManager)getRealTxManager()).joinParentThreadsTransaction(transaction, resources);
			return;
		}
		return;
//		getProxyHandler().joinParentThreadsTransaction(transaction);
//		PlatformTransactionManager platformTxManager = getRealTxManager();
//		if (platformTxManager instanceof IThreadConnectingTransactionManager) {
//			System.out.println("--> found IThreadConnectingTransactionManager "+platformTxManager.getClass().getTypeName());
//			IThreadConnectingTransactionManager threadConnectingTransactionManager = (IThreadConnectingTransactionManager)platformTxManager;
//			threadConnectingTransactionManager.joinParentThreadsTransaction(transaction);
//		}
	}

	@Override
	public void commit(TransactionStatus txStatus) throws TransactionException {
		if (txStatus.isRollbackOnly()) {
			rollback(txStatus);
		} else {
			if (trace && log.isDebugEnabled()) log.debug("commiting transaction ["+txStatus+"]");
			getRealTxManager().commit(txStatus);
		}
	}

	@Override
	public void rollback(TransactionStatus txStatus) throws TransactionException {
		if (trace && log.isDebugEnabled()) log.debug("rolling back transaction ["+txStatus+"]");
		getRealTxManager().rollback(txStatus);
	}

//	protected ThreadConnectorHelperTransactionManagerProxyHandler getProxyHandler() {
//		if (proxyHandler==null) {
//			getRealTxManager();
//		}
//		return proxyHandler;
//	}
	
	@SneakyThrows
	public PlatformTransactionManager getRealTxManager() {
		// This can be called from multiple threads, however
		// not synchronized for performance-reasons.
		// I consider this safe, because the TX manager should
		// be retrieved as a singleton-bean from the Spring
		// Bean Factory and thus each thread should always
		// get the same instance.
		if (realTxManager == null) {
			AbstractPlatformTransactionManager txManager = (AbstractPlatformTransactionManager) beanFactory.getBean(realTxManagerBeanName);
//			if (txManager instanceof IThreadConnectingTransactionManager) {
				realTxManager = txManager;
//			} else {
//				proxyHandler = new ThreadConnectorHelperTransactionManagerProxyHandler(txManager);
//				if (txManager instanceof JtaTransactionManager) {
//					realTxManager = proxyHandler.getJtaProxy();
//				} else {
//					realTxManager = proxyHandler.getProxy();
//				}
//			}
		}
		return realTxManager;
	}


}
