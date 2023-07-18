package nl.nn.adapterframework.testutil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import com.arjuna.ats.internal.jdbc.ConnectionManager;
import com.arjuna.ats.jdbc.TransactionalDriver;

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
		DataSource result = new DelegatingDataSource() { //Cannot use NarayanaDatasource as the PGSQL driver does not implement the Datasource interface
			@Override
			public Connection getConnection() throws SQLException {
				Properties properties = new Properties();
				properties.put(TransactionalDriver.XADataSource, xaDataSource);
				properties.setProperty(TransactionalDriver.poolConnections, "true");
				properties.setProperty(TransactionalDriver.maxConnections, "100");
				return ConnectionManager.create(null, properties);
			}
		};
		NarayanaDataSource.checkModifiers(result);
		return result;
	}
}
