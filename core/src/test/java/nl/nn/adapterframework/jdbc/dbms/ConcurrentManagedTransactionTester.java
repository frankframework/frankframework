package nl.nn.adapterframework.jdbc.dbms;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.testutil.ConcurrentActionTester;

public abstract class ConcurrentManagedTransactionTester extends ConcurrentActionTester {

	private PlatformTransactionManager txManager;
	private IbisTransaction mainItx;

	public ConcurrentManagedTransactionTester(PlatformTransactionManager txManager) {
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
			mainItx.complete();
		}
	}

}
