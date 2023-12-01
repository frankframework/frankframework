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
package nl.nn.adapterframework.jta.narayana;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.arjuna.ats.arjuna.common.MetaObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.objectstore.ObjectStoreAPI;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import com.arjuna.common.util.propertyservice.PropertiesFactory;
import com.arjuna.common.util.propertyservice.PropertiesFactoryStax;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.SpringUtils;

public class NarayanaConfigurationBean implements InitializingBean, ApplicationContextAware {
	protected Logger log = LogUtil.getLogger(this);

	private @Getter @Setter Properties properties;
	private @Setter ApplicationContext applicationContext;

	private class NarayanaPropertiesFactory extends PropertiesFactoryStax {
		@Override
		public Properties getPropertiesFromFile(String propertyFileName, ClassLoader classLoader) {
			Properties localProperties;
			try {
				localProperties = super.getPropertiesFromFile(propertyFileName, classLoader); //Loads the default narayana-jta.jar/jbossts-properties.xml properties.
			} catch (Throwable t) {
				log.warn("unable to load properties file, manually trying to set default values", t);
				localProperties = readXmlFile(propertyFileName);
			}
			localProperties.putAll(AppConstants.getInstance()); //Override with properties set in the Ibis
			localProperties.putAll(properties); //Override with spring configured properties
			return localProperties;
		}

		private Properties readXmlFile(String propertyFileName) {
			Properties outputProperties = new Properties();
			Properties tempProperties = new Properties();
			try (InputStream is = NarayanaConfigurationBean.class.getClassLoader().getResourceAsStream(propertyFileName)) {
				loadFromXML(tempProperties, is);
			} catch (IOException e) {
				log.error("unable to read XML file ["+propertyFileName+"]", e);
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

		final MetaObjectStoreEnvironmentBean jdbcStoreEnvironment = BeanPopulator.getDefaultInstance(MetaObjectStoreEnvironmentBean.class);
		if(JndiObjectStore.class.getCanonicalName().equals(jdbcStoreEnvironment.getObjectStoreType())) {
			configureJndiObjectStore(jdbcStoreEnvironment);
		}
	}

	private DataSource getObjectStoreDatasource() throws ObjectStoreException {
		String objectStoreDatasource = AppConstants.getInstance().getProperty("transactionmanager.narayana.objectStoreDatasource");
		if(StringUtils.isBlank(objectStoreDatasource)) {
			throw new ObjectStoreException("no datasource name provided, please set property [transactionmanager.narayana.objectStoreDatasource]");
		}

		if(applicationContext == null) {
			throw new ObjectStoreException("no ApplicationContext to retrieve DataSource from");
		}

		PoolingDataSourceFactory dataSourceFactory = SpringUtils.createBean(applicationContext, PoolingDataSourceFactory.class);
		SpringUtils.registerSingleton(applicationContext, "NarayanaObjectStoreDataSourceFactory", dataSourceFactory); //Register it so close will be called.

		try {
			return dataSourceFactory.getDataSource(objectStoreDatasource);
		} catch (NamingException e) {
			throw new ObjectStoreException("unable to find datasource", e);
		}
	}

	private void configureJndiObjectStore(ObjectStoreEnvironmentBean jdbcStoreEnvironment) throws ObjectStoreException {
		DataSource datasource = getObjectStoreDatasource();
		log.info("found Narayana ObjectStoreDatasource [{}]", datasource);

		ObjectStoreAPI actionStore = new JndiObjectStore(datasource, "DefaultStore", jdbcStoreEnvironment);
		new StoreManager(actionStore, actionStore, actionStore); //Sets STATIC JDBC stores

		if(StoreManager.getRecoveryStore() != actionStore) { //Validate that statics have been updated
			throw new IllegalStateException("RecoverStore should be the same as the just created ActionStore [TX_Action]");
		}
	}
}
