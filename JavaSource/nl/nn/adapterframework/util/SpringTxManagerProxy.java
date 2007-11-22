/*
 * $Log: SpringTxManagerProxy.java,v $
 * Revision 1.2  2007-11-22 09:14:09  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * proxy class for transaction manager.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class SpringTxManagerProxy implements PlatformTransactionManager, BeanFactoryAware {
	
	private BeanFactory beanFactory;
	private String realTxManagerBeanName;
	private PlatformTransactionManager realTxManager;
	
	/* (non-Javadoc)
	 * @see org.springframework.transaction.PlatformTransactionManager#getTransaction(org.springframework.transaction.TransactionDefinition)
	 */
	public TransactionStatus getTransaction(TransactionDefinition txDef)
		throws TransactionException {
		return getRealTxManager().getTransaction(txDef);
	}

	/* (non-Javadoc)
	 * @see org.springframework.transaction.PlatformTransactionManager#commit(org.springframework.transaction.TransactionStatus)
	 */
	public void commit(TransactionStatus txStatus) throws TransactionException {
		getRealTxManager().commit(txStatus);
	}

	/* (non-Javadoc)
	 * @see org.springframework.transaction.PlatformTransactionManager#rollback(org.springframework.transaction.TransactionStatus)
	 */
	public void rollback(TransactionStatus txStatus) throws TransactionException {
		getRealTxManager().rollback(txStatus);
	}

	/**
	 * @return
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

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
