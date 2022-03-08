package nl.nn.adapterframework.testutil;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

import lombok.Getter;
import nl.nn.adapterframework.lifecycle.SpringContextScope;
import nl.nn.adapterframework.util.SpringUtils;

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
//		try {
//			dataSourceFactory = clazz.newInstance();
//		} catch (Exception e) {
//			fail(ExceptionUtils.getStackTrace(e));
//		}
	}

	private TransactionManagerType(Class<? extends URLDataSourceFactory> clazz, String springConfigurationFile) {
		springConfigurationFiles = new String[]{ springConfigurationFile, "testTXConfigurationContext.xml" };
		factory = clazz;
//		try {
//			dataSourceFactory = clazz.newInstance();
//		} catch (Exception e) {
//			fail(ExceptionUtils.getStackTrace(e));
//		}
	}

	public URLDataSourceFactory getDataSourceFactory(ApplicationContext ac) {
		return ac.getBean(URLDataSourceFactory.class, "dataSourceFactory");
	}

	private TestConfiguration create() {
		return create("H2");
	}

	private synchronized TestConfiguration create(String productKey) {
		System.out.println("================== creating ==================");
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

		System.err.println(config);
		System.out.println("================== finished ==================");

		return config;
	}

//	public void updateTransactionManager() {
//		if(this == TransactionManagerType.DATASOURCE) {
//			getConfigurationContext();
//		}
//	}

	public TestConfiguration getConfigurationContext(String productKey) {
		if(this == TransactionManagerType.DATASOURCE) {
			return datasourceConfigurations.computeIfAbsent(productKey, key -> create(key));
		}
		return transactionManagerConfigurations.computeIfAbsent(this, TransactionManagerType::create);
	}

	public List<DataSource> getAvailableDataSources() throws NamingException {
		URLDataSourceFactory dataSourceFactory = new URLDataSourceFactory();
		List<String> names = dataSourceFactory.getDataSourceNames();
		List<DataSource> datasources = new ArrayList<>();
		for (String datasourceName : names) {
			datasources.add(getDataSource(datasourceName));
		}
		return datasources;
//		try {
//			dataSourceFactory = URLDataSourceFactory.class.newInstance();
//		} catch (Exception e) {
//			fail(ExceptionUtils.getStackTrace(e));
//		}
//		List<DataSource> datasources;
//		return dataSourceFactory.getAvailableDataSources();
	}

	public DataSource getDataSource(String productKey) throws NamingException {
		ApplicationContext ac = getConfigurationContext(productKey);
		return getDataSourceFactory(ac).get(productKey);
	}
}