package org.frankframework.jdbc.dbms;

import java.sql.SQLException;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import org.frankframework.core.IbisTransaction;
import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.testutil.ConcurrentActionTester;

public class ConcurrentManagedTransactionTester extends ConcurrentActionTester {

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
