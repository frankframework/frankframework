package nl.nn.adapterframework.jdbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import nl.nn.adapterframework.jta.IThreadConnectableTransactionManager;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.testutil.TransactionManagerType;

public abstract class TransactionManagerTestBase extends JdbcTestBase {

	protected IThreadConnectableTransactionManager txManager;
	private List<TransactionStatus> transactionsToClose = new ArrayList<>();

	private static TransactionManagerType singleTransactionManagerType = null; // set to a specific transaction manager type, to speed up testing

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

	protected TransactionStatus getTransaction(final int transactionAttribute) {
		TransactionStatus tx = txManager.getTransaction(getTxDef(transactionAttribute));
		registerForCleanup(tx);
		return tx;
	}

	protected boolean registerForCleanup(final TransactionStatus tx) {
		return transactionsToClose.add(tx);
	}
}
