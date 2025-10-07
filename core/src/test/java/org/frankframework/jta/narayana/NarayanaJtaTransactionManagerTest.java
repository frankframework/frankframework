package org.frankframework.jta.narayana;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import org.junit.jupiter.api.Assertions;

import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.DatabaseTestOptions;
import org.frankframework.testutil.junit.NarayanaArgumentSource;

public class NarayanaJtaTransactionManagerTest {

	@DatabaseTestOptions
	@NarayanaArgumentSource
	public void testDefaultRecoveryNode(DatabaseTestEnvironment env) {
		SpringTxManagerProxy txManagerProxy = assertInstanceOf(SpringTxManagerProxy.class, env.getTxManager());
		NarayanaJtaTransactionManager txManager = assertInstanceOf(NarayanaJtaTransactionManager.class, txManagerProxy.getRealTxManager());
		Assertions.assertIterableEquals(List.of(txManager.getUid()), txManager.getRecoveryNodes());
	}
}
