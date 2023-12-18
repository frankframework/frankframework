package org.frankframework.jdbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import org.frankframework.jta.IThreadConnectableTransactionManager;
import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.testutil.TransactionManagerType;

public abstract class TransactionManagerTestBase extends JdbcTestBase {

	@Rule
	public Timeout testTimeout = Timeout.seconds(60);

	protected IThreadConnectableTransactionManager txManager;
	private final List<TransactionStatus> transactionsToClose = new ArrayList<>();

	private static final TransactionManagerType singleTransactionManagerType = null; // set to a specific transaction manager type, to speed up testing

	@Parameters(name= "{0}: {1}")
	public static Collection data() throws NamingException {
		final TransactionManagerType[] transactionManagerTypes;
		if (singleTransactionManagerType != null) {
			transactionManagerTypes = new TransactionManagerType[]{ singleTransactionManagerType };
		} else {
			transactionManagerTypes = TransactionManagerType.values();
		}
		List<Object[]> matrix = new ArrayList<>();

		for(TransactionManagerType type: transactionManagerTypes) {
			List<String> datasourceNames;
			if (StringUtils.isNotEmpty(singleDatasource)) {
				datasourceNames = new ArrayList<>();
				datasourceNames.add(singleDatasource);
			} else {
				datasourceNames = type.getAvailableDataSources();
			}
			for(String name : datasourceNames) {
				matrix.add(new Object[] {type, name});
			}
		}

		return matrix;
	}

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		txManager = getConfiguration().getBean(SpringTxManagerProxy.class, "txManager");

		prepareDatabase();
	}

	@After
	@Override
	public void teardown() throws Exception {
		Collections.reverse(transactionsToClose);
		transactionsToClose
				.forEach(this::completeSafely);
		super.teardown();
	}

	private void completeSafely(final TransactionStatus tx) {
		if (!tx.isCompleted()) {
			try {
				txManager.rollback(tx);
			} catch (Exception e) {
				log.warn("Exception rolling back non-completed transaction", e);
			}
		}
	}

	public TransactionDefinition getTxDef(int transactionAttribute, int timeout) {
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute, timeout);
	}

	public TransactionDefinition getTxDef(int transactionAttribute) {
		return getTxDef(transactionAttribute, 20);
	}

	protected TransactionStatus startTransaction(final int transactionAttribute) {
		return startTransaction(getTxDef(transactionAttribute));
	}

	protected TransactionStatus startTransaction(final TransactionDefinition txDef) {
		TransactionStatus tx = txManager.getTransaction(txDef);
		registerForCleanup(tx);
		return tx;
	}

	protected void registerForCleanup(final TransactionStatus tx) {
		transactionsToClose.add(tx);
	}
}
