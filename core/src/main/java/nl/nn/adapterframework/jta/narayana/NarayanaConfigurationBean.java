/*
   Copyright 2021, 2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.common.util.propertyservice.PropertiesFactory;
import com.arjuna.common.util.propertyservice.PropertiesFactoryStax;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

public class NarayanaConfigurationBean implements InitializingBean {
	protected Logger log = LogUtil.getLogger(this);

	private @Getter @Setter Properties properties;

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
	public void afterPropertiesSet() {
		PropertiesFactory.setDelegatePropertiesFactory(new NarayanaPropertiesFactory());

		String dfaultTimeoutStr = getProperties().getProperty("CoordinatorEnvironmentBean.defaultTransactionTimeout");
		if(StringUtils.isNotBlank(dfaultTimeoutStr)) {
			int timeout = Integer.parseInt(dfaultTimeoutStr);
			arjPropertyManager.getCoordinatorEnvironmentBean().setDefaultTimeout(timeout);
		}
	}
}
