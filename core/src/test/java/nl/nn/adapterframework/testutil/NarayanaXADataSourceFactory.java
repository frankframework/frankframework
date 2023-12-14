package nl.nn.adapterframework.testutil;

import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import lombok.Setter;
import nl.nn.adapterframework.jta.narayana.DataSourceXAResourceRecoveryHelper;
import nl.nn.adapterframework.jta.narayana.NarayanaConfigurationBean;
import nl.nn.adapterframework.jta.narayana.NarayanaDataSource;
import nl.nn.adapterframework.jta.narayana.NarayanaJtaTransactionManager;

public class NarayanaXADataSourceFactory extends URLXADataSourceFactory {

	static {
		NarayanaConfigurationBean narayana = new NarayanaConfigurationBean();
		Properties properties = new Properties();
		properties.put("JDBCEnvironmentBean.isolationLevel", "2");
		properties.put("ObjectStoreEnvironmentBean.objectStoreDir", "target/narayana");
		properties.put("ObjectStoreEnvironmentBean.stateStore.objectStoreDir", "target/narayana");
		properties.put("ObjectStoreEnvironmentBean.communicationStore.objectStoreDir", "target/narayana");
		narayana.setProperties(properties);

		try {
			narayana.afterPropertiesSet();
		} catch (ObjectStoreException e) {
			throw new IllegalStateException("unable to configure Narayana", e);
		}
	}

	private @Setter NarayanaJtaTransactionManager txManagerReal;

	@Override
	protected DataSource augmentXADataSource(XADataSource xaDataSource, String product) {
		XAResourceRecoveryHelper recoveryHelper = new DataSourceXAResourceRecoveryHelper(xaDataSource);
		this.txManagerReal.registerXAResourceRecoveryHelper(recoveryHelper);
		return new NarayanaDataSource(xaDataSource, product);
	}
}
