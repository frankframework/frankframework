package nl.nn.adapterframework.testutil;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

public class BTMXADataSourceFactory extends URLXADataSourceFactory {

	public static void createBtmTransactionManager() {
		if(TransactionManagerServices.isTransactionManagerRunning()) {
			TransactionManagerServices.getTransactionManager().shutdown();
		}

		Configuration configuration = TransactionManagerServices.getConfiguration();
		configuration.setLogPart1Filename("target/btm1.log");
		configuration.setLogPart2Filename("target/btm2.log");
		configuration.setSkipCorruptedLogs(true);
		configuration.setGracefulShutdownInterval(3);
		configuration.setDefaultTransactionTimeout(5);
		configuration.setDisableJmx(true);

		TransactionManagerServices.getTransactionManager(); //Create the TX once, just so it's initialized
	}

	@Override
	protected DataSource augmentXADataSource(XADataSource xaDataSource, String product) {
		PoolingDataSource result = new PoolingDataSource();
		result.setUniqueName(product);
		result.setMaxPoolSize(100);
		result.setAllowLocalTransactions(true);
		result.setXaDataSource(xaDataSource);
		result.setIgnoreRecoveryFailures(true);
		result.init();
		return result;
	}

	@Override
	public void destroy() throws Exception {
		objects.values().stream().filter(ds -> ds instanceof PoolingDataSource).forEach(ds -> ((PoolingDataSource)ds).close());
	}

}
