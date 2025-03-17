package org.frankframework.testutil;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

import lombok.extern.log4j.Log4j2;

import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.jdbc.datasource.DataSourceFactory;

@Log4j2
public enum TransactionManagerType {
	// Apparently we want to inject some XA commit-stopper as well as wrap the DS
	DATASOURCE(TestDataSourceFactory.class, "springTOMCAT.xml"),
	NARAYANA(TestNarayanaDataSourceFactory.class, "springTOMCATNARAYANA.xml");

	private static final Map<TransactionManagerType, TestConfiguration> transactionManagerConfigurations = new WeakHashMap<>();
	private static final Map<String, TestConfiguration> datasourceConfigurations = new WeakHashMap<>();

	private final Class<? extends IDataSourceFactory> factory;
	private final String[] springConfigurationFiles;

	TransactionManagerType(Class<? extends IDataSourceFactory> clazz, String springConfigurationFile) {
		if(springConfigurationFile == null) {
			springConfigurationFiles = new String[]{ TestConfiguration.TEST_CONFIGURATION_FILE };
		} else {
			springConfigurationFiles = new String[]{ springConfigurationFile, TestConfiguration.TEST_DATABASE_ENABLED_CONFIGURATION_FILE };
		}
		factory = clazz;
	}

	public IDataSourceFactory getDataSourceFactory(ApplicationContext ac) {
		return ac.getBean(IDataSourceFactory.class, "dataSourceFactory");
	}

	/**
	 * Create a new, fresh {@link TestConfiguration} instance. You can choose to have {@link TestConfiguration#configure()} run
	 * automatically after creation, or not.
	 *
	 * @param autoConfigure If you do not need to modify the configuration, pass {@code true}. If you
	 *                      need to add extra adapters and other beans after creating the configuration, then
	 *                      pass {@code false} so you do not have to {@link TestConfiguration#stop()} the configuration
	 *                      before adding these adapters.
	 * @return New {@link TestConfiguration} instance.
	 */
	public TestConfiguration create(boolean autoConfigure) {
		return create(autoConfigure, "H2"); // Only used to satisfy Spring startup
	}

	private synchronized TestConfiguration create(boolean autoConfigure, String productKey) {
		log.info("create new TestConfiguration for database [{}]", productKey);
		TestConfiguration config = new TestConfiguration(autoConfigure, springConfigurationFiles);
		MutablePropertySources propertySources = config.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		Properties properties = new Properties();
		properties.setProperty("TestDataSourceFactory", factory.getCanonicalName());
		properties.setProperty("DataSourceName", productKey);
		properties.setProperty("TransactionManagerType", this.name());
		propertySources.addFirst(new PropertiesPropertySource("testProperties", properties));

		removePreviousTxLogFiles(config);

		config.setName(this.name());
		config.refresh();

		return config;
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

	/**
	 * Get configuration context, cached if already created and otherwise a new one already started.
	 *
	 * @param productKey Type of configuration context to get
	 * @return The {@link TestConfiguration} instance (new or cached)
	 */
	public TestConfiguration getConfigurationContext(String productKey) {
		// If we need to create a new TestConfiguration, always created it with autoStart=true
		// because that makes it more consistent between new and cached configuration.
		if(this == TransactionManagerType.DATASOURCE) {
			return datasourceConfigurations.computeIfAbsent(productKey, ignored-> this.create(true, ignored));
		}
		return transactionManagerConfigurations.computeIfAbsent(this, ignored -> this.create(true, productKey));
	}

	public synchronized void closeConfigurationContext() {
		if(this == TransactionManagerType.DATASOURCE) {
			Iterator<TestConfiguration> it = datasourceConfigurations.values().iterator();
			while(it.hasNext()) {
				TestConfiguration ac = it.next();
				if(ac != null) {
					ac.close();
				}
				it.remove();
			}
		} else {
			TestConfiguration ac = transactionManagerConfigurations.remove(this);
			if(ac != null) {
				ac.close();
			}
			if (ac != null) {
				removePreviousTxLogFiles(ac);
			}
		}
	}

	public static synchronized void closeAllConfigurationContexts() {
		for (TransactionManagerType tmt : values()) {
			tmt.closeConfigurationContext();
		}
	}

	public List<String> getAvailableDataSources() {
		return FindAvailableDataSources.getAvailableDataSources();
	}

	/**
	 * fetch the DataSource through the configured {@link DataSourceFactory}.
	 */
	public DataSource getDataSource(String productKey) {
		ApplicationContext ac = getConfigurationContext(productKey);
		return getDataSourceFactory(ac).getDataSource(productKey);
	}
}
