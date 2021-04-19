package nl.nn.adapterframework.jdbc;

import java.sql.SQLException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.ResourceTransactionManager;

import nl.nn.adapterframework.jta.SpringTxManagerProxy;

public abstract class TransactionManagerTestBase extends JdbcTestBase {

	public final String DEFAULT_DATASOURCE_NAME="testDataSource";

	protected ResourceTransactionManager txManager;
	protected SpringDataSourceFactory dataSourceFactory;
	protected DataSource txManagedDataSource;

	public TransactionManagerTestBase(String productKey, String url, String userid, String password, boolean testPeekDoesntFindRecordsAlreadyLocked) throws SQLException, NamingException {
		super(productKey, url, userid, password, testPeekDoesntFindRecordsAlreadyLocked);
		
		// setup a DataSourceFactory like in springTOMCAT.xml
		dataSourceFactory = new SpringDataSourceFactory();
		dataSourceFactory.add(targetDataSource, DEFAULT_DATASOURCE_NAME);
			
		// setup a defaultDataSource, produced by dataSourceFactory, like in springTOMCAT.xml
		DataSource defaultDataSource = dataSourceFactory.getDataSource(DEFAULT_DATASOURCE_NAME);
			
		// setup a TransactionManager like in springTOMCAT.xml
		DataSourceTransactionManager dataSourceTransactionManager;
		dataSourceTransactionManager = new DataSourceTransactionManager(defaultDataSource);
		dataSourceTransactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		txManager = dataSourceTransactionManager;
		
		txManagedDataSource = new TransactionAwareDataSourceProxy(defaultDataSource);
	}

	public TransactionDefinition getTxDef(int transactionAttribute, int timeout) {
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute, timeout);
	}
	
	public TransactionDefinition getTxDef(int transactionAttribute) {
		return getTxDef(transactionAttribute, 20);
	}
}
