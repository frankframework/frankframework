package nl.nn.adapterframework.testutil;

import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

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

	private static Map<TransactionManagerType, TestConfiguration> configurations = new WeakHashMap<>();
	private URLDataSourceFactory dataSourceFactory;
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

	public URLDataSourceFactory getDataSourceFactory() {
		return getConfigurationContext().getBean(URLDataSourceFactory.class, "dataSourceFactory");
	}

	private synchronized TestConfiguration create() {
		System.out.println("================== creating ==================");
		TestConfiguration config = new TestConfiguration(springConfigurationFiles);
		MutablePropertySources propertySources = config.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		Properties properties = new Properties();
		properties.setProperty("URLDataSourceFactory", factory.getCanonicalName());
		propertySources.addFirst(new PropertiesPropertySource("testProperties", properties));

		config.setName(this.name());
		config.refresh();

		System.err.println(config);
		System.out.println("================== finished ==================");

		return config;
	}

	public TestConfiguration getConfigurationContext() {
		return configurations.computeIfAbsent(this, TransactionManagerType::create);
	}

	public List<DataSource> getAvailableDataSources() {
		return getDataSourceFactory().getAvailableDataSources();
	}

	String[] getSpringConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}
}