package nl.nn.adapterframework.testutil;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

import bitronix.tm.TransactionManagerServices;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;

public enum TransactionManagerType {
	DATASOURCE(URLDataSourceFactory.class, "springTOMCAT.xml"),
	BTM(BTMXADataSourceFactory.class, "springTOMCATBTM.xml"),
	NARAYANA(NarayanaXADataSourceFactory.class, "springTOMCATNARAYANA.xml");

	private static Map<TransactionManagerType, TestConfiguration> transactionManagerConfigurations = new WeakHashMap<>();
	private static Map<String, TestConfiguration> datasourceConfigurations = new WeakHashMap<>();

	private Class<? extends URLDataSourceFactory> factory;
	private String[] springConfigurationFiles;

	private TransactionManagerType(Class<? extends URLDataSourceFactory> clazz) {
		this(clazz, null);
	}

	private TransactionManagerType(Class<? extends URLDataSourceFactory> clazz, String springConfigurationFile) {
		if(springConfigurationFile == null) {
			springConfigurationFiles = new String[]{ TestConfiguration.TEST_CONFIGURATION_FILE };
		} else {
			springConfigurationFiles = new String[]{ springConfigurationFile, TestConfiguration.TEST_DATABASE_ENABLED_CONFIGURATION_FILE };
		}
		factory = clazz;
	}

	public URLDataSourceFactory getDataSourceFactory(ApplicationContext ac) {
		return ac.getBean(URLDataSourceFactory.class, "dataSourceFactory");
	}

	private TestConfiguration create() {
		return create("H2"); //only used to satisfy Spring startup
	}

	private synchronized TestConfiguration create(String productKey) {
		try {
			TestConfiguration config = new TestConfiguration(springConfigurationFiles);
			MutablePropertySources propertySources = config.getEnvironment().getPropertySources();
			propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
			propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
			Properties properties = new Properties();
			properties.setProperty("URLDataSourceFactory", factory.getCanonicalName());
			properties.setProperty("DataSourceName", productKey);
			properties.setProperty("TransactionManagerType", this.name());
			propertySources.addFirst(new PropertiesPropertySource("testProperties", properties));

			removePreviousTxLogFiles(config);

			config.setName(this.name());
			config.refresh();

			return config;
		} catch (Throwable t) {
			fail(t.getMessage());
			throw t;
		}
	}

	private void removePreviousTxLogFiles(TestConfiguration config) {
		String txDir = config.getEnvironment().getProperty("transactionmanager.log.dir");
		try {
			LogManager.getLogger(this.getClass()).debug("Cleaning up old TX log files at [{}]", txDir);
			if (txDir != null) {
				FileUtils.deleteDirectory(new File(txDir));
			}
		} catch (IOException e) {
			LogManager.getLogger(this.getClass()).warn("Could not remove previous TX manager log dirs:", e);
		}
	}

	public TestConfiguration getConfigurationContext(String productKey) {
		if(this == TransactionManagerType.DATASOURCE) {
			return datasourceConfigurations.computeIfAbsent(productKey, key -> create(key));
		}
		return transactionManagerConfigurations.computeIfAbsent(this, TransactionManagerType::create);
	}

	public synchronized void closeConfigurationContext() {
		if(this == TransactionManagerType.DATASOURCE) {
			for (String productKey : datasourceConfigurations.keySet()) {
				TestConfiguration ac = datasourceConfigurations.remove(productKey);
				if(ac != null) {
					ac.close();
				}
			}
		} else {
			TestConfiguration ac = transactionManagerConfigurations.remove(this);
			if(ac != null) {
				ac.close();
			}
			if (this == BTM && TransactionManagerServices.isTransactionManagerRunning()) {
				TransactionManagerServices.getTransactionManager().shutdown();
			}
			if (ac != null) {
				removePreviousTxLogFiles(ac);
			}
		}
	}

	public List<String> getAvailableDataSources() throws NamingException {
		URLDataSourceFactory dataSourceFactory = new URLDataSourceFactory();
		return dataSourceFactory.getDataSourceNames();
	}

	/**
	 * fetch the DataSource through the configured {@link JndiDataSourceFactory}.
	 */
	public DataSource getDataSource(String productKey) throws NamingException {
		ApplicationContext ac = getConfigurationContext(productKey);
		return getDataSourceFactory(ac).get(productKey);
	}
}
