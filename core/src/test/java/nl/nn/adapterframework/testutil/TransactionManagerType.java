package nl.nn.adapterframework.testutil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

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
			springConfigurationFiles = new String[]{ "testConfigurationContext.xml" };
		} else {
			springConfigurationFiles = new String[]{ springConfigurationFile, "testTXConfigurationContext.xml" };
		}
		factory = clazz;
	}

	public URLDataSourceFactory getDataSourceFactory(ApplicationContext ac) {
		return ac.getBean(URLDataSourceFactory.class, "dataSourceFactory");
	}

	private TestConfiguration create() {
		return create("H2");
	}

	private synchronized TestConfiguration create(String productKey) {
		TestConfiguration config = new TestConfiguration(springConfigurationFiles);
		MutablePropertySources propertySources = config.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		Properties properties = new Properties();
		properties.setProperty("URLDataSourceFactory", factory.getCanonicalName());
		properties.setProperty("DataSourceName", productKey);
		propertySources.addFirst(new PropertiesPropertySource("testProperties", properties));

		config.setName(this.name());
		config.refresh();

		return config;
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
		}
	}

	public List<DataSource> getAvailableDataSources() throws NamingException {
		URLDataSourceFactory dataSourceFactory = new URLDataSourceFactory();
		List<String> availableDataSources = dataSourceFactory.getDataSourceNames();

		List<DataSource> datasources = new ArrayList<>();
		for (String datasourceName : availableDataSources) {
			datasources.add(getDataSource(datasourceName));
		}
		return datasources;
	}

	/**
	 * fetch the DataSource through the configured {@link JndiDataSourceFactory}.
	 */
	public DataSource getDataSource(String productKey) throws NamingException {
		ApplicationContext ac = getConfigurationContext(productKey);
		return getDataSourceFactory(ac).get(productKey);
	}
}