/*
 * $Log: SpringTxManagerProxy.java,v $
 * Revision 1.6  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2008/02/06 16:39:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for setting of transaction timeout
 *
 * Revision 1.3  2008/01/11 09:55:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added utility functions
 *
 * Revision 1.2  2007/11/22 09:14:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.util;

import org.apache.log4j.Logger;
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
 * @version Id
 */
public class SpringTxManagerProxy implements PlatformTransactionManager, BeanFactoryAware {
	private static final Logger log = LogUtil.getLogger(SpringTxManagerProxy.class);
	
	private BeanFactory beanFactory;
	private String realTxManagerBeanName;
	private PlatformTransactionManager realTxManager;

	private boolean trace=false;
	
	public static TransactionDefinition getTransactionDefinition(int txOption, int timeout) {
		DefaultTransactionDefinition result=new DefaultTransactionDefinition(txOption);
		if (timeout>0) {
			result.setTimeout(timeout);
		}
		return result; 
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.transaction.PlatformTransactionManager#getTransaction(org.springframework.transaction.TransactionDefinition)
	 */
	public TransactionStatus getTransaction(TransactionDefinition txDef) throws TransactionException {
		if (trace && log.isDebugEnabled()) log.debug("getting transaction definition ["+txDef+"]");
		return getRealTxManager().getTransaction(txDef);
	}

	/* (non-Javadoc)
	 * @see org.springframework.transaction.PlatformTransactionManager#commit(org.springframework.transaction.TransactionStatus)
	 */
	public void commit(TransactionStatus txStatus) throws TransactionException {
		if (trace && log.isDebugEnabled()) log.debug("commiting transaction ["+txStatus+"]");
		getRealTxManager().commit(txStatus);
	}

	/* (non-Javadoc)
	 * @see org.springframework.transaction.PlatformTransactionManager#rollback(org.springframework.transaction.TransactionStatus)
	 */
	public void rollback(TransactionStatus txStatus) throws TransactionException {
		if (trace && log.isDebugEnabled()) log.debug("rolling back transaction ["+txStatus+"]");
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
