/*
   Copyright 2021-2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.jta.narayana;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jndi.JndiTemplate;

import com.arjuna.ats.arjuna.common.MetaObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.accessors.DataSourceJDBCAccess;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import com.arjuna.common.util.propertyservice.PropertiesFactory;
import com.arjuna.common.util.propertyservice.PropertiesFactoryStax;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.JndiContextPrefixFactory;
import org.frankframework.jdbc.datasource.PoolingDataSourceFactory;
import org.frankframework.util.AppConstants;

@Log4j2
public class NarayanaConfigurationBean implements InitializingBean, ApplicationContextAware {

	private @Getter @Setter Properties properties;
	private @Setter ApplicationContext applicationContext;

	private class NarayanaPropertiesFactory extends PropertiesFactoryStax {
		@Override
		public Properties getPropertiesFromFile(String propertyFileName, ClassLoader classLoader) {
			Properties localProperties;
			try {
				localProperties = super.getPropertiesFromFile(propertyFileName, classLoader); // Loads the default narayana-jta.jar/jbossts-properties.xml properties.
			} catch (Throwable t) {
				log.warn("unable to load properties file, manually trying to set default values", t);
				localProperties = readXmlFile(propertyFileName);
			}
			localProperties.putAll(AppConstants.getInstance()); // Override with properties set in the Ibis
			localProperties.putAll(properties); // Override with spring configured properties
			return localProperties;
		}

		private Properties readXmlFile(String propertyFileName) {
			Properties outputProperties = new Properties();
			Properties tempProperties = new Properties();
			try (InputStream is = NarayanaConfigurationBean.class.getClassLoader().getResourceAsStream(propertyFileName)) {
				loadFromXML(tempProperties, is);
			} catch (IOException e) {
				log.error("unable to read XML file [{}]", propertyFileName, e);
			}
			tempProperties.forEach((k, v) -> outputProperties.put(k, ((String)v).trim()));
			return outputProperties;
		}
	}

	/**
	 * Populate all the jBossTS EnvironmentBeans.
	 */
	@Override
	public void afterPropertiesSet() throws ObjectStoreException {
		PropertiesFactory.setDelegatePropertiesFactory(new NarayanaPropertiesFactory());

		// Set/Update the JdbcAccess value
		final MetaObjectStoreEnvironmentBean jdbcStoreEnvironment = BeanPopulator.getDefaultInstance(MetaObjectStoreEnvironmentBean.class);
		if(JDBCStore.class.getCanonicalName().equals(jdbcStoreEnvironment.getObjectStoreType())) {
			jdbcStoreEnvironment.setJdbcAccess(getObjectStoreJndiName());

			String tablePrefix = AppConstants.getInstance().getString("transactionmanager.narayana.objectStoreTablePrefix", null);
			if (StringUtils.isNotEmpty(tablePrefix)) {
				jdbcStoreEnvironment.setTablePrefix(tablePrefix + "_");
			}
		}
	}

	/**
	 * Tests if we can find a (valid) DataSource. Takes ApplicationServer specific jndiPrefixes into account.
	 * If the DataSource is valid it will be returned with the {@link PoolingDataSourceJDBCAccess} prefix.
	 */
	private String getObjectStoreJndiName() throws ObjectStoreException {
		String objectStoreDatasource = AppConstants.getInstance().getProperty("transactionmanager.narayana.objectStoreDatasource");
		if (StringUtils.isBlank(objectStoreDatasource)) {
			throw new ObjectStoreException("no datasource name provided, please set property [transactionmanager.narayana.objectStoreDatasource]");
		}

		if (applicationContext == null) {
			throw new ObjectStoreException("no ApplicationContext to retrieve DataSource from");
		}

		JndiContextPrefixFactory jndiContextFactory = applicationContext.getBean("jndiContextPrefixFactory", JndiContextPrefixFactory.class);
		String jndiPrefix = jndiContextFactory.getContextPrefix();
		String fullJndiName = StringUtils.isNotEmpty(jndiPrefix) ? jndiPrefix + objectStoreDatasource : objectStoreDatasource;
		try {
			JndiTemplate locator = new JndiTemplate();
			DataSource dataSource = locator.lookup(fullJndiName, DataSource.class);
			boolean isPooled = PoolingDataSourceFactory.isPooledDataSource(dataSource);
			log.info("found Narayana ObjectStoreDatasource [{}] pooled [{}]", dataSource, isPooled);
			if (isPooled) {
				return DataSourceJDBCAccess.class.getCanonicalName() + ";datasourceName=" + fullJndiName;
			}

			return PoolingDataSourceJDBCAccess.class.getCanonicalName() + ';' + fullJndiName;
		} catch (NamingException e) {
			throw new ObjectStoreException("unable to find datasource", e);
		}
	}
}
