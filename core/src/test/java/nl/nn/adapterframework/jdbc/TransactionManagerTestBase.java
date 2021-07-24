package nl.nn.adapterframework.jdbc;

import java.sql.SQLException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.ResourceTransactionManager;

import nl.nn.adapterframework.util.SpringTxManagerProxy;

public abstract class TransactionManagerTestBase extends JdbcTestBase {

	protected ResourceTransactionManager txManager;
	protected DataSource txManagedDataSource;

	public TransactionManagerTestBase(DataSource dataSource) throws SQLException, NamingException {
		super(dataSource);

		// setup a TransactionManager like in springTOMCAT.xml
		DataSourceTransactionManager dataSourceTransactionManager;
		dataSourceTransactionManager = new DataSourceTransactionManager(dataSource);
		dataSourceTransactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		txManager = dataSourceTransactionManager;

		txManagedDataSource = new TransactionAwareDataSourceProxy(dataSource);
	}

	public TransactionDefinition getTxDef(int transactionAttribute, int timeout) {
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute, timeout);
	}
	
	public TransactionDefinition getTxDef(int transactionAttribute) {
		return getTxDef(transactionAttribute, 20);
	}
}
