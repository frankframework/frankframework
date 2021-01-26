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
package nl.nn.adapterframework.util;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * proxy class for transaction manager.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class SpringTxManagerProxy implements PlatformTransactionManager, BeanFactoryAware {
	private static final Logger log = LogUtil.getLogger(SpringTxManagerProxy.class);
	
	private BeanFactory beanFactory;
	private String realTxManagerBeanName;
	private PlatformTransactionManager realTxManager;

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
	
	/* (non-Javadoc)
	 * @see org.springframework.transaction.PlatformTransactionManager#getTransaction(org.springframework.transaction.TransactionDefinition)
	 */
	@Override
	public TransactionStatus getTransaction(TransactionDefinition txDef) throws TransactionException {
		if (trace && log.isDebugEnabled()) log.debug("getting transaction definition ["+txDef+"]");
		return getRealTxManager().getTransaction(txDef);
	}

	/* (non-Javadoc)
	 * @see org.springframework.transaction.PlatformTransactionManager#commit(org.springframework.transaction.TransactionStatus)
	 */
	@Override
	public void commit(TransactionStatus txStatus) throws TransactionException {
		if (trace && log.isDebugEnabled()) log.debug("commiting transaction ["+txStatus+"]");
		getRealTxManager().commit(txStatus);
	}

	/* (non-Javadoc)
	 * @see org.springframework.transaction.PlatformTransactionManager#rollback(org.springframework.transaction.TransactionStatus)
	 */
	@Override
	public void rollback(TransactionStatus txStatus) throws TransactionException {
		if (trace && log.isDebugEnabled()) log.debug("rolling back transaction ["+txStatus+"]");
		getRealTxManager().rollback(txStatus);
	}

	/**
	 */
	public PlatformTransactionManager getRealTxManager() {
		// This can be called from multiple threads, however
		// not synchronized for performance-reasons.
		// I consider this safe, because the TX manager should
		// be retrieved as a singleton-bean from the Spring
		// Bean Factory and thus each thread should always
		// get the same instance.
		if (realTxManager == null) {
			realTxManager = (PlatformTransactionManager) beanFactory.getBean(realTxManagerBeanName);
		}
		return realTxManager;
	}

	public String getRealTxManagerBeanName() {
		return realTxManagerBeanName;
	}

	public void setRealTxManagerBeanName(String string) {
		realTxManagerBeanName = string;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
