package nl.nn.adapterframework.jdbc.dbms;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.ResourceTransactionManager;

import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.testutil.ConcurrentActionTester;
import nl.nn.adapterframework.util.SpringTxManagerProxy;

public abstract class ConcurrentManagedTransactionTester extends ConcurrentActionTester {
	
	private ResourceTransactionManager txManager;
	private IbisTransaction mainItx;

	public ConcurrentManagedTransactionTester(ResourceTransactionManager txManager) {
		super();
		this.txManager=txManager;
	}
	
	@Override
	public void initAction() throws Exception {
		TransactionDefinition txDef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW,20);
		mainItx = IbisTransaction.getTransaction(txManager, txDef, "ConcurrentManagedTransactionTester");
	}

	@Override
	public void finalizeAction() throws Exception {
		if(mainItx != null) {
			mainItx.commit();
		}
	}
	
}
