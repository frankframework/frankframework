package nl.nn.adapterframework.jdbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.transaction.TransactionDefinition;

import nl.nn.adapterframework.jta.IThreadConnectableTransactionManager;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.testutil.TransactionManagerType;

public abstract class TransactionManagerTestBase extends JdbcTestBase {

	protected IThreadConnectableTransactionManager txManager;

	private static TransactionManagerType singleTransactionManagerType = null; // set to a specific transaction manager type, to speed up testing

	@Parameters(name= "{0}: {1}")
	public static Collection data() throws NamingException {
		TransactionManagerType[] transactionManagerTypes = { singleTransactionManagerType };
		if (singleTransactionManagerType==null) {
			transactionManagerTypes = TransactionManagerType.values();
		}
		List<Object[]> matrix = new ArrayList<>();

		for(TransactionManagerType type: transactionManagerTypes) {
			List<String> datasources;
			if (StringUtils.isNotEmpty(singleDatasource)) {
				datasources = new ArrayList<>();
				datasources.add(singleDatasource);
			} else {
				datasources = type.getAvailableDataSources();
			}
			for(String ds : datasources) {
				matrix.add(new Object[] {type, ds});
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

	public TransactionDefinition getTxDef(int transactionAttribute, int timeout) {
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute, timeout);
	}

	public TransactionDefinition getTxDef(int transactionAttribute) {
		return getTxDef(transactionAttribute, 20);
	}
}
