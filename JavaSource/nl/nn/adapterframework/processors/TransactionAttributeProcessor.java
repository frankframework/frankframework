/*
 * $Log: TransactionAttributeProcessor.java,v $
 * Revision 1.2  2010-09-07 15:55:13  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class TransactionAttributeProcessor {
	protected Logger log = LogUtil.getLogger(this);
	protected PlatformTransactionManager txManager;
	
	public void setTxManager(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}
	
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}

}
