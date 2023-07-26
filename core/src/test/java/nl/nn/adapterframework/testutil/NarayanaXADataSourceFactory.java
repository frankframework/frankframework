package nl.nn.adapterframework.testutil;

import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import nl.nn.adapterframework.jta.narayana.NarayanaConfigurationBean;
import nl.nn.adapterframework.jta.narayana.NarayanaDataSource;

public class NarayanaXADataSourceFactory extends URLXADataSourceFactory {

	static {
		NarayanaConfigurationBean narayana = new NarayanaConfigurationBean();
		Properties properties = new Properties();
		properties.put("JDBCEnvironmentBean.isolationLevel", "2");
		properties.put("ObjectStoreEnvironmentBean.objectStoreDir", "target/narayana");
		properties.put("ObjectStoreEnvironmentBean.stateStore.objectStoreDir", "target/narayana");
		properties.put("ObjectStoreEnvironmentBean.communicationStore.objectStoreDir", "target/narayana");
		narayana.setProperties(properties);
		narayana.afterPropertiesSet();
	}

	@Override
	protected DataSource augmentXADataSource(XADataSource xaDataSource, String product) {
		return new NarayanaDataSource(xaDataSource, product);
	}
}
