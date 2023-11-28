package nl.nn.adapterframework.jdbc.dbms;

import java.sql.SQLException;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.testutil.ConcurrentActionTester;

public abstract class ConcurrentManagedTransactionTester extends ConcurrentActionTester {

	private final PlatformTransactionManager txManager;
	private IbisTransaction mainItx;

	public ConcurrentManagedTransactionTester(PlatformTransactionManager txManager) {
		super();
		this.txManager=txManager;
	}

	@Override
	public void initAction() throws SQLException {
		TransactionDefinition txDef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW,20);
		mainItx = new IbisTransaction(txManager, txDef, "ConcurrentManagedTransactionTester");
	}

	@Override
	public void finalizeAction() throws SQLException {
		if(mainItx != null) {
			mainItx.complete();
		}
	}

}
