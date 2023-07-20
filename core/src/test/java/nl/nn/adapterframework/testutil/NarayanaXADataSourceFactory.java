package nl.nn.adapterframework.testutil;

import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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
		NarayanaDataSource ds = new NarayanaDataSource(xaDataSource, product);
		return pool(ds, product);
	}

	private DataSource pool(DataSource dataSource, String dataSourceName) {
		HikariConfig config = new HikariConfig();
		config.setRegisterMbeans(false);
		config.setMaxLifetime(0);
		config.setIdleTimeout(60);
		config.setMaximumPoolSize(20);
		config.setMinimumIdle(0);
		config.setDataSource(dataSource);
		config.setPoolName(dataSourceName);

		HikariDataSource poolingDataSource = new HikariDataSource(config);
		log.info("created Hikari pool [{}]", poolingDataSource);
		return poolingDataSource;
	}
}
